package com.frederiksen.love;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.frederiksen.love.screens.Game;
import com.frederiksen.love.screens.LoadingScreen;
import com.frederiksen.love.utils.Timer;

public class App extends ApplicationAdapter {
	public static final int DEFAULT_WIDTH  = 1200;
	public static final int DEFAULT_HEIGHT = 800;

	public static class Task extends Thread {
	    private Runnable task;
	    private Runnable onFinished = null;

		public Task(Runnable task) {
			this.task = task;
		}

		public Task(Runnable task, Runnable onFinished) {
			this.task = task;
			this.onFinished = onFinished;
		}

		@Override
		public void run() {
		    super.run();

		    task.run();
		}

		public void onFinished() {
		    if (onFinished != null)
                onFinished.run();
		}
	}

	private Renderer renderer;
	private Resources   resources;

	private Game          game = new Game(this);
	private LoadingScreen loadingScreen = new LoadingScreen();

	private Task   loadingTask   = null;

	@Override
	public void create() {
		renderer = new Renderer();
		resources = new Resources();

		// load resources for loading screen (blocking operation)
        loadingScreen.setMsg("Loading resources...");
		loadingScreen.loadResources(resources);

		// just in case resize isn't already called on create
	    resize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	/**
	 * Runs task and displays loading screen.
	 *
	 * @param task
	 * @param loadingMsg
	 */
	public void loadScreen(Task task, String loadingMsg) {
	    loadingTask = task;
	    loadingScreen.setMsg(loadingMsg);
	    loadingTask.start();
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		System.out.printf("Resizing window to %d x %d... \n", width, height);

		renderer.resize(width, height);
		game.resize(width, height);
	}

	public void update() {

	    if (!resources.isFinished()) {
	        if (resources.update()) {
	            // resources have just finished loading
				game.loadResources(resources);
			}

	        loadingScreen.update();

		} else if (loadingTask != null) {
	    	// load stuff
            if (!loadingTask.isAlive()) {
            	loadingTask.onFinished();
				loadingTask = null;
			}

            loadingScreen.update();
		} else {
			game.update();
		}

		game.backgroundUpdate();

		Timer.getInstance().update();
	}

	@Override
	public void render() {
	    // TODO: could optimize here
		int screenWidth = Gdx.graphics.getWidth();
		int screenHeight = Gdx.graphics.getHeight();

	    // update stuff
	    update();

		if (!resources.isFinished() || loadingTask != null) {
		    // display loading screen
            loadingScreen.render(renderer, screenWidth, screenHeight);
            return;
		}

		game.render(renderer, screenWidth, screenHeight);
	}
	
	@Override
	public void dispose() {
		renderer.dispose();
		resources.dispose();
		game.dispose();
	}
}
