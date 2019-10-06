package com.frederiksen.love.gameobjs.controllers;

import com.esotericsoftware.kryo.Kryo;
import com.frederiksen.love.networking.Network;

public abstract class SyncedServerController<
        S extends Controller.State,
        A extends Controller.Action>
        extends SyncedController<S, A, S, SyncedServerController.Action<?>, Network.Server> {

    public static class Action<A extends Controller.Action> extends Controller.Action {

        public static void register(Kryo kryo) {
            kryo.register(Action.class);
        }

        private A action;
        /* replay buffer index where state is stored */
        private int ringIndex = -1;

        public A getAction() {
            return action;
        }

        public void setAction(A action) {
            this.action = action;
        }

        public int getRingIndex() {
            return ringIndex;
        }

        public void setRingIndex(int ringIndex) {
            this.ringIndex = ringIndex;
        }
    }

    public static class Caster<S, A> implements SafeCaster<S, Action<?>> {
        private SafeCaster<S, A> caster;

        public Caster(SafeCaster<S, A> caster) {
            this.caster = caster;
        }

        @Override
        public S safeCast(State state) throws VerboseCastingException {
            return caster.safeCast(state);
        }

        @Override
        public Action<?> safeCast(Controller.Action action) throws VerboseCastingException {
            if (action instanceof Action) {
                caster.safeCast(((Action) action).action);
                return (Action<?>) action;
            }

            throw new VerboseCastingException(action.getClass(), Action.class);
        }
    }

    private SafeCaster<S, A>  caster;
    private SyncedClientController.State<S> tempPacket = new SyncedClientController.State<>();

    public SyncedServerController(S state, Network.Server endpoint, SafeCaster<S, A> caster) {
        super(state, endpoint, new Caster<>(caster));

        this.caster = caster;
        tempPacket.setState(state);
    }

    @Override
    public State pollState(boolean forPaired) {
        if (forPaired)
            return tempPacket;
        else return state;
    }

    @Override
    public void onNetAction(Action<?> action) {
        super.onNetAction(action);

        try {
            // do the action
            doAction(caster.safeCast(action.action));
        } catch (VerboseCastingException e) {
            // this will never happen
            e.printStackTrace();
        }

        System.out.printf("Got action packet: (index: %d) %s.\n", action.getRingIndex(), action.getAction());

        // label with replay index
        tempPacket.setReplayIndex(action.getRingIndex());

    }
}
