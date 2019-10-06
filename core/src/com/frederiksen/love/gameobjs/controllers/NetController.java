package com.frederiksen.love.gameobjs.controllers;

import com.frederiksen.love.gameobjs.ObjFactory;
import com.frederiksen.love.networking.Network;

/**
 * Controller for networked agents
 * @param <S> state for agent
 * @param <A> action for agent
 * @param <N> network endpoint
 * @param <NS> state for network
 * @param <NA> action for network
 */
public abstract class NetController<
        S extends Controller.State,
        A extends Controller.Action,
        NS extends Controller.State,
        NA extends Controller.Action,
        N extends Network.EndPoint>
        extends Controller<S, A> {


    public static class VerboseCastingException extends Exception {
        public VerboseCastingException(Class<?> from, Class<?> to) {
            super("Could not cast from " + from.getName() + " to " + to.getName() + ".");
        }
    }

    /*  class for making type casts the user's problem */
    public interface SafeCaster<S, A> {
        S safeCast(State state) throws VerboseCastingException;
        A safeCast(Action action) throws VerboseCastingException;
    }



    protected N endpoint;
    private SafeCaster<NS, NA> caster;

    public NetController(S state, N endpoint, SafeCaster<NS, NA> caster) {
        super(state);

        this.endpoint = endpoint;
        this.caster = caster;
    }

    /**
     * Called on incoming states from network
     *
     * @param state
     */
    public void onNetState(NS state) {

    }

    /**
     * Called on incoming actions from network
     *
     * @param action
     */
    public void onNetAction(NA action) {

    }

    /**
     * Polls states to be sent over network
     *
     * @return state to be sent
     */
    public State pollState() {
        return state;
    }

    /* network delivery methods that convert vague types to usable ones */

    public void onNetDeliver(Action action) throws VerboseCastingException {
        onNetAction(safeCast(action));
    }

    public void onNetDeliver(State state) throws VerboseCastingException {
        onNetState(safeCast(state));
    }

    /* safe casters for network casting */

    public NS safeCast(State state) throws VerboseCastingException {
        return caster.safeCast(state);
    }

    public NA safeCast(Action action) throws VerboseCastingException {
        return caster.safeCast(action);
    }

    public N getEndpoint() {
        return endpoint;
    }
}
