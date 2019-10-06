package com.frederiksen.love.gameobjs;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.frederiksen.love.Renderer;
import com.frederiksen.love.Resources;
import com.frederiksen.love.gameobjs.controllers.Controller;
import com.frederiksen.love.gameobjs.controllers.NetController;
import com.frederiksen.love.networking.Network;
import com.frederiksen.love.screens.Game;

public abstract class GameObj<S extends Controller.State, A extends Controller.Action, B extends ObjFactory.Builder> {
    private boolean built = false;

    protected S state;
    protected Controller<S, A> controller;

    public S getState() {
        return state;
    }

    public Controller<S, A> getController() {
        return controller;
    }

    public void onStartServer(Network.Server server) {

    }

    public void onStartClient(Network.Client client) {

    }

    public void onStopNetwork() {

    }

    public void updateNetwork(Network network) {
        if (network.isActive()) {
            if (network.isServer()) onStartServer((Network.Server) network.getEndpoint());
            if (network.isClient()) onStartClient((Network.Client) network.getEndpoint());
        } else onStopNetwork();
    }

    public void build(Game game, B builder, Resources resources) {
        built = true;
        updateNetwork(game.getNetwork());
    }

    public final void build(Game game, Resources resources) {
        build(game, null, resources);
    }

    public final void build(Game game, B builder) {
        build(game, builder, game.getResources());
    }

    public final void build(Game game) {
        build(game, game.getResources());
    }

    public abstract B getBuilder();

    public abstract B safeCast(ObjFactory.Builder builder) throws NetController.VerboseCastingException;

    public final void tryBuild(Game game, ObjFactory.Builder builder) throws NetController.VerboseCastingException {
        build(game, safeCast(builder));
    }

    public void update(float deltaTime) {
        if (!built)
            throw new RuntimeException("Game object must be built first.");

        if (controller != null)
            controller.update(deltaTime);
    }

    public void render(Renderer renderer) {
        if (!built)
            throw new RuntimeException("Game object must be built first.");
    }

    public boolean isDead() {
        return controller.isDead();
    }
}
