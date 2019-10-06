package com.frederiksen.love.gameobjs.controllers;

import com.frederiksen.love.gameobjs.ObjFactory;

public abstract class Controller<
        S extends Controller.State,
        A extends Controller.Action> {

    public abstract static class State {

    }

    public abstract static class Action {

    }

    private static long ID_COUNTER = 0;

    protected long    id;
    protected S       state;
    protected boolean dead = false;

    public Controller(S state) {
        this.state = state;

        id = ID_COUNTER++;
    }

    /* gets called every frame for stuff like physics updates etc.*/
    public abstract void update(float deltaTime);

    /* command agent to start doing an action */
    public abstract void doAction(A action);


    public boolean isDead() {
        return dead;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
