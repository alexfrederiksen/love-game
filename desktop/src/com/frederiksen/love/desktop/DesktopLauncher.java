package com.frederiksen.love.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.frederiksen.love.App;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();

		config.width = App.DEFAULT_WIDTH;
		config.height = App.DEFAULT_HEIGHT;

		new LwjglApplication(new App(), config);
	}
}
