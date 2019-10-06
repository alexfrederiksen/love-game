package com.frederiksen.love.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.frederiksen.love.Renderer;
import com.frederiksen.love.Resources;
import com.frederiksen.love.utils.Oscillator;

public class LoadingScreen extends Screen {
    private BitmapFont font;
    private String msg;
    private Oscillator fader = new Oscillator(Oscillator.SINE, 0.5f, 0.5f);

    @Override
    public void loadResources(Resources resources) {
        super.loadResources(resources);

        font = resources.getNow("fonts/spookyFont.fnt", BitmapFont.class);
    }

    @Override
    public void update() {
        super.update();
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public void render(Renderer renderer, int screenWidth, int screenHeight) {
        super.render(renderer, screenWidth, screenHeight);

        // update projection matrix
        // TODO: could optimize here maybe

        Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		renderer.begin2d(screenWidth, screenHeight);

		float x = fader.get();
		font.setColor(1f, 0.5f + 0.5f * x, 0.5f + 0.5f * x, x);
        font.draw(renderer, msg, 0, screenHeight * 0.5f + font.getLineHeight() * 0.5f, screenWidth, Align.center, false);

        renderer.end();
    }
}
