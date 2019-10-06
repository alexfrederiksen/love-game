package com.frederiksen.love.gameobjs.controllers;

import com.esotericsoftware.kryo.Kryo;
import com.frederiksen.love.gameobjs.ObjFactory;
import com.frederiksen.love.networking.Network;
import com.frederiksen.love.utils.*;

import java.lang.reflect.Array;


/**
 * Client-networked controller that syncs with server using predictive technique.
 *
 * @param <S>
 * @param <A>
 */
public abstract class SyncedClientController<
        S extends Controller.State & Setable<S> & InnerSpace<S, Float>,
        A extends Controller.Action & Setable<A>>
        extends SyncedController<S, A, SyncedClientController.State<?>, A, Network.Client> {

    public static final int   REPLAY_BUFFER_SIZE   = 500;
    public static final float STATE_DIFF_THRESHOLD = 0.01f;

    /* state class that attaches custom fields to the states automatically */
    public static class State<S extends Controller.State> extends Controller.State {

        public static void register(Kryo kryo) {
            kryo.register(State.class);
        }

        /* state before action was done */
        private S state;
        /* replay buffer index where state is stored */
        private int replayIndex = -1;

        public State() {

        }

        public State(S state) {
            this.state = state;
        }

        public S getState() {
            return state;
        }

        public void setState(S state) {
            this.state = state;
        }

        public int getReplayIndex() {
            return replayIndex;
        }

        public void setReplayIndex(int replayIndex) {
            this.replayIndex = replayIndex;
        }
    }

    public static class Caster<S, A> implements SafeCaster<State<?>, A> {
        private SafeCaster<S, A> caster;

        public Caster(SafeCaster<S, A> caster) {
            this.caster = caster;
        }

        @Override
        public State<?> safeCast(Controller.State state) throws VerboseCastingException {
            if (state instanceof State) {
                // attempt to cast inner state, just to see
                caster.safeCast(((State) state).state);
                return (State<?>) state;
            }

            throw new VerboseCastingException(state.getClass(), State.class);
        }

        @Override
        public A safeCast(Action action) throws VerboseCastingException {
            return caster.safeCast(action);
        }
    }

    public static class Pair<S extends Setable<S>, A extends Setable<A>> implements Setable<Pair<S, A>> {
        private S state;
        private A action;

        public Pair(S state, A action) {
            this.state = state;
            this.action = action;
        }

        @Override
        public Pair<S, A> set(Pair<S, A> obj) {
            return set(obj.state, obj.action);
        }

        public Pair<S, A> set(S state, A action) {
            this.state.set(state);
            this.action.set(action);

            return this;
        }

        public S getState() {
            return state;
        }

        public A getAction() {
            return action;
        }
    }

    public class Poller extends Timer.Task {
        private A tempAction = newAction();
        private SyncedServerController.Action<A> tempPacket = new SyncedServerController.Action<>();

        public Poller(float interval) {
            super(interval);
        }

        @Override
        public void run() {
            // poll for action
            tempAction = pollAction(tempAction);

            int nextIndex = replayBuffer.getNextIndex();
            System.out.printf("Sending action packet: (index: %d) %s...\n", nextIndex, tempAction);
            // send to server
            tempPacket.setRingIndex(nextIndex);
            tempPacket.setAction(tempAction);
            endpoint.pushAction(tempPacket);
            // do action
            doAction(tempAction);
        }
    }

    private SafeCaster<S, A> caster;

    /* stores state-action pairs, where action takes place in state */
    private RingBuffer<Pair<S, A>> replayBuffer = new RingBuffer<>((Pair<S, A>[]) Array.newInstance(Pair.class, REPLAY_BUFFER_SIZE), this::newPair);

    private Poller poller;
    private Ref<Float> updateRate;
    private Ref<Float> actionRate;


    private S correctedState = newState();

    public SyncedClientController(S state, Network.Client endpoint, SafeCaster<S, A> caster,
                                  Ref<Float> updateRate, Ref<Float> actionRate) {
        super(state, endpoint, new Caster<>(caster));

        this.caster = caster;
        this.updateRate = updateRate;
        this.actionRate = actionRate;

        poller = new Poller(1f / actionRate.get());
        //poller = new Poller(5f);
        Timer.schedule(poller);
    }

    private S tempDiffState = newState();
    private S tempDiffState2 = newState();
    private S tempOldState = newState();
    public void replay(int index, S newState) {
        //System.out.println("Replaying actions...");


        int targetIndex = replayBuffer.getNextIndex();

        if (index == targetIndex) {
            state.set(newState);
            return;
        }

        tempOldState.set(state);

        // compute difference
        S oldState = replayBuffer.get(index).getState();
        tempDiffState.set(newState).sub(oldState);
        tempDiffState2.set(state).sub(oldState);

        // move just a little
        InnerSpace.normalize(tempDiffState).scl(0.01f);

        System.out.printf("Syncing (index: %d) from old state %s to new state %s (difference of %s)...\n", index, oldState, newState, tempDiffState);

        // replay to now
        for (int i = index; i != targetIndex; i = replayBuffer.clampIndex(i + 1)) {
            //System.out.printf(" Adding %s to (i = %d) %s...\n", tempDiffState, i, replayBuffer.get(i).getState());
            replayBuffer.get(i).getState().add(tempDiffState);
        }

        state.add(tempDiffState);
        //System.out.printf(" State is now %s + %s.\n", newState, tempDiffState2);
        //state.set(newState).add(tempDiffState2);

        System.out.printf(" Shifted by (targetIndex: %d) %s.\n", targetIndex, tempOldState.sub(state).scl(-1f));

        /*
        // very primitive replay
        while (replayBuffer.getNextIndex() != targetIndex) {
            doAction(replayBuffer.peek(-1).getAction());

            int steps = MathUtils.ceil(updateRate.get() / actionRate.get());
            float stepLen = 1f / updateRate.get();
            System.out.printf("Gonna do %d steps of %f secs...\n", steps, stepLen);
            for (int i = 0; i < steps; i++) {
                update(stepLen);
            }
        }
        */
    }

    private S tempState = newState();
    public void sync(int replayIndex, S state) {
        // ignore negative replay indices (sentinel values)
        if (replayIndex < 0) return;
        // the state in the next pair is affected by the action
        int changeIndex = replayIndex;


        Pair<S, A> pair = replayBuffer.get(changeIndex);

        System.out.printf("Corrected state received: (index: %d) %s.\n", replayIndex, state);

        // use inner product to compare states: (distance)^2 = <S' - S, S' - S>
        tempState.set(state).sub(pair.getState());
        if (InnerSpace.len2(tempState) > STATE_DIFF_THRESHOLD * STATE_DIFF_THRESHOLD) {
            //System.out.printf("Syncing with change index: %d (current next: %d)...\n", changeIndex, replayBuffer.getNextIndex());
            // states are different enough
            replay(changeIndex, state);
        }
    }

    private Pair<S, A> tempPair = newPair();
    @Override
    public void doAction(A action) {
        replayBuffer.add(tempPair.set(state, action));
    }

    @Override
    public Controller.State pollState(boolean forFriend) {
        return state;
    }

    public abstract A pollAction(A action);

    @Override
    public void update(float deltaTime) {

    }

    @Override
    public void onNetState(State<?> state) {
        super.onNetState(state);

        try {
            sync(state.replayIndex, caster.safeCast(state.state));
        } catch (VerboseCastingException e) {
            // this will never happen
            e.printStackTrace();
        }
    }

    public Pair<S, A> newPair() {
        return new Pair<>(newState(), newAction());
    }

    /* factory methods for states and actions */

    public abstract S newState();

    public abstract A newAction();

    public void dispose() {
        Timer.cancel(poller);
    }
}
