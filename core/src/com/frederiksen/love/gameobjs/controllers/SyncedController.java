package com.frederiksen.love.gameobjs.controllers;

import com.frederiksen.love.gameobjs.ObjFactory;
import com.frederiksen.love.networking.Network;
import com.frederiksen.love.utils.Setable;

public abstract class SyncedController<
        S extends Controller.State,
        A extends Controller.Action,
        NS extends Controller.State,
        NA extends Controller.Action,
        N extends Network.EndPoint>
        extends NetController<S, A, NS, NA, N> {

    public SyncedController(S state, N endpoint, SafeCaster<NS, NA> caster) {
        super(state, endpoint, caster);
    }


    /**
     * Poll state to be sent over network
     *
     * @param forFriend true when state will be sent to the controller's counterpart (friend)
     * @return state
     */
    public abstract State pollState(boolean forFriend);

    @Override
    public State pollState() {
        return pollState(false);
    }
}
