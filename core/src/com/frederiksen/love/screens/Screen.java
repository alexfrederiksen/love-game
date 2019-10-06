package com.frederiksen.love.screens;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.frederiksen.love.Renderer;
import com.frederiksen.love.Resources;

public abstract class Screen {
    private boolean resourcesLoaded = false;

    public void loadResources(Resources resources) {
        resourcesLoaded = true;
    }

    public void update() {
        if (!resourcesLoaded)
            throw new RuntimeException("Resources have been not loaded.");
    }

    public void resize(int width, int height) {

    }

    /**
     * Called by game loop. Batch will not already be within a begin() / end() block.
     *
     * @param renderer
     * @param screenWidth
     * @param screenHeight
     */
    public void render(Renderer renderer, int screenWidth, int screenHeight) {
         if (!resourcesLoaded)
            throw new RuntimeException("Resources have been not loaded.");
    }
}
