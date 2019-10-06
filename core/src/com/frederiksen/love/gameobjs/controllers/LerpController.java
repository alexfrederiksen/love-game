package com.frederiksen.love.gameobjs.controllers;

import com.frederiksen.love.gameobjs.ObjFactory;
import com.frederiksen.love.networking.Network;
import com.frederiksen.love.utils.Ref;
import com.frederiksen.love.utils.VectorSpace;

public abstract class LerpController<
        S extends Controller.State & VectorSpace<S, Float>,
        A extends Controller.Action>
        extends SimpleNetController<S, A, Network.Client> {

    private S          newState = null;
    private float      time = 0;
    private Ref<Float> speed;

    public LerpController(S state, Network.Client client, float speed, SafeCaster<S, A> caster) {
        super(state, client, caster);

        this.speed = new Ref<>(speed);
    }

    public LerpController(S state, Network.Client client, Ref<Float> speed, SafeCaster<S, A> caster) {
        super(state, client, caster);

        this.speed = speed;
    }

    @Override
    public void update(float deltaTime) {
        time = Math.min(time + speed.get() * deltaTime, 1f);

        lerp(time);
    }

    @Override
    public void doAction(A action) {

    }

    @Override
    public void onNetAction(A action) {
        super.onNetAction(action);

    }

    public void lerp(float time) {
        if (newState == null) return;

        // lerp it: S = (1 - t) S + (t) S'
        // take advantage of states being a vector space lol
        state.scl(1f - time)
             .mulAdd(newState, time);
    }

    @Override
    public void onNetState(S state) {
        super.onNetState(state);

        time = 0;
        newState = state;
    }
}
