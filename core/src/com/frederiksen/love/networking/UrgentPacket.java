package com.frederiksen.love.networking;

import com.esotericsoftware.kryo.Kryo;
import com.frederiksen.love.gameobjs.GameObj;
import com.frederiksen.love.gameobjs.ObjFactory;
import com.frederiksen.love.gameobjs.Player;
import com.frederiksen.love.gameobjs.controllers.Controller;

public class UrgentPacket {

    public static void register(Kryo kryo) {
        kryo.register(WelcomePacket.class);
        kryo.register(ObjCreatePacket.class);
        kryo.register(ObjDestroyPacket.class);
    }

    public static class WelcomePacket {
        public long             controllerId;
        public Controller.State state;

        public WelcomePacket create(Player.RemoteServerController controller) {
            controllerId = controller.getId();
            state = controller.pollState(true);
            return this;
        }
    }

    public static class ObjCreatePacket {

        public long controllerId;
        public ObjFactory.Builder builder;

        public ObjCreatePacket create(GameObj<?, ?, ?> obj) {
            return create(obj.getBuilder(), obj.getController().getId());
        }

        public ObjCreatePacket create(ObjFactory.Builder builder, long controllerId) {
            this.controllerId = controllerId;
            this.builder = builder;

            return this;
        }
    }

    public static class ObjDestroyPacket {
        public long controllerId;

        public ObjDestroyPacket create(Controller<?, ?> controller) {
            controllerId = controller.getId();

            return this;
        }

        public ObjDestroyPacket create(GameObj<?, ?, ?> obj) {
            return create(obj.getController());
        }


    }
}
