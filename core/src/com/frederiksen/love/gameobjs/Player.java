package com.frederiksen.love.gameobjs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.kryo.Kryo;
import com.frederiksen.love.Renderer;
import com.frederiksen.love.Resources;
import com.frederiksen.love.gameobjs.controllers.*;
import com.frederiksen.love.io.GameInput;
import com.frederiksen.love.networking.Network;
import com.frederiksen.love.screens.Game;
import com.frederiksen.love.utils.InnerSpace;
import com.frederiksen.love.utils.Setable;

public class Player extends GameObj<Player.State, Player.Action, Player.Builder> {


    /* player state and action definitions for controllers */

    public static class Builder extends ObjFactory.Builder {
        public static void register(Kryo kryo) {
            kryo.register(Builder.class);
        }
    }

    public static class State extends Controller.State implements InnerSpace<State, Float>, Setable<State> {
        public static void register(Kryo kryo) {
            kryo.register(State.class);
            kryo.register(Vector2.class);
        }

        private Vector2 pos = new Vector2();
        private float   rot = 0f;

        public Vector2 getPos() {
            return pos;
        }

        public void setPos(Vector2 pos) {
            this.pos = pos;
        }

        public float getRot() {
            return rot;
        }

        public void setRot(float rot) {
            this.rot = rot;
        }

        @Override
        public State scl(final Float scalar) {
            pos.scl(scalar);
            rot *= scalar;
            return this;
        }

        @Override
        public State mulAdd(final State other, final Float scalar) {
            pos.mulAdd(other.getPos(), scalar);
            rot += other.rot * scalar;
            return this;
        }

        @Override
        public Float getMulIdentity() {
            return 1f;
        }

        @Override
        public Float innerProduct(State other) {
            return pos.dot(other.pos) + rot * other.rot;
        }

        @Override
        public State set(State obj) {
            pos.set(obj.pos);
            rot = obj.rot;

            return this;
        }

        @Override
        public Float negate(Float scalar) {
            return -scalar;
        }

        @Override
        public Float invert(Float scalar) {
            return 1f / scalar;
        }

        @Override
        public Float sqrt(Float scalar) {
            return (float) Math.sqrt(scalar);
        }

        @Override
        public String toString() {
            return pos.toString();
        }
    }

    public static class Action extends Controller.Action implements Setable<Action> {
        public static void register(Kryo kryo) {
            kryo.register(Action.class);
        }

        public static final byte STOP     = 0x00;
        public static final byte FORWARD  = 0x01;
        public static final byte BACKWARD = 0x02;
        public static final byte RIGHT    = 0x03;
        public static final byte LEFT     = 0x04;

        // displacement
        private byte step;

        // rotation
        private float rx;

        public void set(byte step, float rx) {
            this.step = step;
            this.rx = rx;
        }

        @Override
        public Action set(Action obj) {
            step = obj.step;
            rx = obj.rx;

            return this;
        }

        public float getRx() {
            return rx;
        }

        public Vector2 get(Vector2 v, float rot) {
            v.set(MathUtils.cosDeg(rot), MathUtils.sinDeg(rot)).scl(-1);

            if (step == FORWARD) {
            } else if (step == BACKWARD) {
                v.scl(-1);
            } else if (step == RIGHT) {
                v.rotate90(-1);
            } else if (step == LEFT) {
                v.rotate90(1);
            } else if (step == STOP) {
                v.setZero();
            }

            return v;
        }
    }

    public static class SafeCaster implements NetController.SafeCaster<State, Action> {
        private static SafeCaster instance = new SafeCaster();

        public static SafeCaster getInstance() {
            return instance;
        }

        @Override
        public State safeCast(Controller.State state) throws NetController.VerboseCastingException {
            if (state instanceof State)
                return (State) state;

            throw new NetController.VerboseCastingException(state.getClass(), State.class);
        }

        @Override
        public Action safeCast(Controller.Action action) throws NetController.VerboseCastingException {
            if (action instanceof Action)
                return (Action) action;

            throw new NetController.VerboseCastingException(action.getClass(), Action.class);
        }
    }

    /* player controllers, turns out local-remote and server-client are independent properties for players */

    public class LocalClientController extends SyncedClientController<State, Action> {
        private float   speed = 5;
        private Vector2 vel   = new Vector2();

        public LocalClientController(Player.State state, Network.Client client) {
            super(state, client, Player.SafeCaster.getInstance(), game.getUpdateRate(), client.getNetwork().getTickRate());
        }

        @Override
        public Player.Action pollAction(Player.Action action) {
            return GameInput.getInstance()
                            .getPlayerAction(action);
        }

        @Override
        public void update(float deltaTime) {
            super.update(deltaTime);

            state.getPos().mulAdd(vel, deltaTime);
        }

        @Override
        public void doAction(Player.Action action) {
            super.doAction(action);

            state.rot += action.getRx();
            action.get(vel, state.rot).scl(speed);

        }

        @Override
        public Player.State newState() {
            return new Player.State();
        }

        @Override
        public Player.Action newAction() {
            return new Player.Action();
        }
    }

    public class RemoteClientController extends LerpController<State, Action> {
        public RemoteClientController(Player.State state, Network.Client client) {
            // pass tick per second reference to controller
            super(state, client, client.getNetwork().getTickRate(), Player.SafeCaster.getInstance());
        }
    }

    public class LocalServerController extends SimpleNetController<State, Action, Network.Server> {
        private float speed = 5;
        private Vector2 vel = new Vector2();
        private Player.Action tempAction = new Player.Action();

        public LocalServerController(Player.State state, Network.Server server) {
            super(state, server, Player.SafeCaster.getInstance());
        }

        @Override
        public void update(float deltaTime) {
            // do an action
            tempAction = GameInput.getInstance().getPlayerAction(tempAction);
            doAction(tempAction);

            // update position
            state.getPos().mulAdd(vel, deltaTime);
        }

        @Override
        public void doAction(Player.Action action) {
            state.rot += action.getRx();
            action.get(vel, state.rot).scl(speed);

        }
    }

    public class RemoteServerController extends SyncedServerController<State, Action> {
        private float speed = 5;
        private Vector2 vel = new Vector2();

        public RemoteServerController(Player.State state, Network.Server server) {
            super(state, server, Player.SafeCaster.getInstance());
        }

        @Override
        public void update(float deltaTime) {
            state.getPos().mulAdd(vel, deltaTime);
        }

        @Override
        public void doAction(Player.Action action) {
            state.rot += action.getRx();
            action.get(vel, state.rot).scl(speed);
        }
    }

    private Game    game;
    private boolean isLocal = false;
    private Sprite  sprite = new Sprite();

    private Builder builder = new Builder();

    public Player() {
        state = new State();
        state.getPos().set(5, 5);

        sprite.setSize(1f, 1f);
        sprite.setPosition(0f, 0f);

        builder.objType = ObjFactory.PLAYER;
    }


    @Override
    public void build(Game game, Builder builder, Resources resources) {
        super.build(game, builder, resources);

        this.game = game;

        sprite.setRegion(resources.get("game-pics/man-stand_0.png", Texture.class));
    }

    @Override
    public void onStartServer(Network.Server server) {
        super.onStartServer(server);

        if (isLocal)
            controller = new LocalServerController(state, server);
        else
            controller = new RemoteServerController(state, server);
    }

    @Override
    public void onStartClient(Network.Client client) {
        super.onStartClient(client);

        if (isLocal)
            controller = new LocalClientController(state, client);
        else
            controller = new RemoteClientController(state, client);
    }

    @Override
    public void onStopNetwork() {
        super.onStopNetwork();

        controller = new LocalServerController(state, null);
    }

    @Override
    public void updateNetwork(Network network) {
        super.updateNetwork(network);
    }

    private Vector2 tempVec = new Vector2();

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);

        sprite.setCenterX(state.getPos().x);
        sprite.setY(state.getPos().y);

        game.getHexRenderer().pingHexes(state.getPos(), 1);

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
            game.getHexRenderer().pingHexes(state.getPos(), 5);
    }

    @Override
    public void render(Renderer renderer) {
        super.render(renderer);

        renderer.drawObj(sprite, state.getRot());
        renderer.drawObj(sprite, state.getRot() + 90);
    }

    public Player setLocal(boolean local) {
        isLocal = local;

        return this;
    }

    public long getId() {
        return controller.getId();
    }

    @Override
    public Builder getBuilder() {
        return builder;
    }

    @Override
    public Builder safeCast(ObjFactory.Builder builder) throws NetController.VerboseCastingException {
        if (builder instanceof Builder)
            return (Builder) builder;

        throw new NetController.VerboseCastingException(builder.getClass(), Builder.class);
    }

    public Vector2 getCenter() {
        return state.getPos();
    }

    public float getRotation() {
        return state.getRot();
    }

}
