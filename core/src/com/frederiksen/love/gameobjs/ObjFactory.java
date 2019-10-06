package com.frederiksen.love.gameobjs;

import com.frederiksen.love.gameobjs.controllers.NetController;
import com.frederiksen.love.screens.Game;

import java.util.HashMap;
import java.util.function.Supplier;

public class ObjFactory {
    public static final byte PLAYER = 0x00;
    public static final byte MONSTER = 0x01;

    public static class Builder {
        public byte objType;
    }

    public static class MissinFactory extends Exception {
        public MissinFactory(byte type) {
            super(String.format("Factory for type (%X) could not be found.", type));
        }
    }

    private HashMap<Byte, Supplier<? extends GameObj>> factories = new HashMap<>();

    public ObjFactory() {
        factories.put(PLAYER, Player::new);
    }

    public GameObj buildObj(Game game, Builder builder) throws NetController.VerboseCastingException, MissinFactory {
        Supplier<? extends GameObj> factory = factories.get(builder.objType);

        if (factory != null) {
            GameObj obj = factory.get();
            obj.tryBuild(game, builder);

            return obj;
        } else {
            throw new MissinFactory(builder.objType);
        }
    }
}
