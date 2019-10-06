package com.frederiksen.love.networking;

import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryo.Kryo;
import com.frederiksen.love.gameobjs.controllers.Controller;

public class StatePacket {
    public static void register(Kryo kryo) {
        kryo.register(StatePacket.class);
    }

    private long id;
    private Controller.State state;

    public StatePacket create(long id, Controller.State state) {
        this.id = id;
        this.state = state;

        return this;
    }

    public long getId() {
        return id;
    }

    public Controller.State getState() {
        return state;
    }
}
