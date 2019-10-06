package com.frederiksen.love.io;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.frederiksen.love.gameobjs.Player;

public class GameInput {
    private static final GameInput instance = new GameInput();

    private static final float MOUSE_SENSATIVITY = -1f;

    public static GameInput getInstance() {
        return instance;
    }

    public Player.Action getPlayerAction(Player.Action action) {
        byte step = Player.Action.STOP;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) step = Player.Action.FORWARD;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) step = Player.Action.BACKWARD;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) step = Player.Action.RIGHT;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) step = Player.Action.LEFT;


        float rx = Gdx.input.getDeltaX() * MOUSE_SENSATIVITY;

        action.set(step, rx);
        return action;
    }
}
