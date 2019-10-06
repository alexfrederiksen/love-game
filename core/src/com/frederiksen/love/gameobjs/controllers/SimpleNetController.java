package com.frederiksen.love.gameobjs.controllers;

import com.frederiksen.love.gameobjs.ObjFactory;
import com.frederiksen.love.networking.Network;

/**
 * Helper class for simpler network controllers
 *
 * @param <S>
 * @param <A>
 * @param <N>
 */
public abstract class SimpleNetController<
        S extends Controller.State,
        A extends Controller.Action,
        N extends Network.EndPoint>
        extends NetController<S, A, S, A, N> {

    public SimpleNetController(S state, N endpoint, SafeCaster<S, A> caster) {
        super(state, endpoint, caster);
    }
}
