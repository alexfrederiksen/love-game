package com.frederiksen.love.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.frederiksen.love.App;
import com.frederiksen.love.Renderer;
import com.frederiksen.love.Resources;
import com.frederiksen.love.effects.HexRenderer;
import com.frederiksen.love.gameobjs.GameObj;
import com.frederiksen.love.gameobjs.Player;
import com.frederiksen.love.networking.Network;
import com.frederiksen.love.utils.Ref;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Game extends Screen {
	private Ref<Float> updateRate = new Ref<>(60f);

	private Network network     = new Network(this);
	private Player  localPlayer = new Player().setLocal(true);

	private HexRenderer hexRenderer = new HexRenderer();

	private List<GameObj<?, ?, ?>> gameObjs = new ArrayList<>();
	private Resources              resources;
	private App                    app;

	public Game(App app) {
		this.app = app;
	}

	@Override
	public void loadResources(Resources resources) {
		super.loadResources(resources);

		this.resources = resources;

		hexRenderer.loadResources(resources);

		localPlayer.build(this);
		addGameObj(localPlayer);


		// find servers to connect to
		app.loadScreen(new App.Task(() -> network.findServer(1000)),
					   "Finding server...");

	}

	public void onNetChange() {
		for (GameObj obj : gameObjs)
		    obj.updateNetwork(network);
	}

	public Player createPlayer() {
	    Player player = new Player();
	    player.build(this);

	    return player;
	}

	public GameObj<?, ?, ?> addGameObj(GameObj<?, ?, ?> obj) {
	    gameObjs.add(obj);

	    return obj;
	}

	public GameObj<?, ?, ?> getGameObjById(long id) {
		for (GameObj obj : gameObjs) {
			if (obj.getController().getId() == id)
				return obj;
		}

		return null;
	}

	public Player getLocalPlayer() {
	    return localPlayer;
	}


	@Override
	public void resize(int width, int height) {
		System.out.printf("Resizing window to %d x %d... \n", width, height);
	}

	@Override
	public void update() {
		super.update();


		float deltaTime = Gdx.graphics.getDeltaTime();
		updateRate.set(1f / deltaTime);

		// center camera on player

		Iterator<GameObj<?, ?, ?>> it = gameObjs.iterator();
		while (it.hasNext()) {
		    GameObj<?, ?, ?> obj = it.next();
			obj.update(deltaTime);
			// remove dead objects
			if (obj.isDead()) it.remove();
		}

		hexRenderer.update(deltaTime);
	}

	public void backgroundUpdate() {
	    network.update();
	}

	@Override
	public void render(Renderer renderer, int screenWidth, int screenHeight) {
		super.render(renderer, screenWidth, screenHeight);

		// clear screen
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// look at the player
		renderer.look(localPlayer.getCenter(), localPlayer.getRotation());

		// render game
		renderer.begin();

		hexRenderer.render(renderer);

		for (GameObj obj : gameObjs)
			obj.render(renderer);

		renderer.end();
	}


	public Ref<Float> getUpdateRate() {
		return updateRate;
	}

	public void dispose() {
		network.stop();
	}

	public Network getNetwork() {
		return network;
	}

	public Resources getResources() {
		return resources;
	}

	public List<GameObj<?, ?, ?>> getGameObjs() {
		return gameObjs;
	}

	public HexRenderer getHexRenderer() {
		return hexRenderer;
	}
}
