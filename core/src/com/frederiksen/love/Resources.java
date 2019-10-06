package com.frederiksen.love;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.BitmapFontLoader;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class Resources extends AssetManager {

    public Resources() {

        // enqueue resources to load

        load("fonts/spookyFont.fnt", BitmapFont.class);
        load("game-pics/man-stand_0.png", Texture.class);
        load("redpill.png", Texture.class);
        load("hex.png", Texture.class);
        load("hex-solid.png", Texture.class);
    }

    /**
     * Use this to get resources without waiting for all to load. Use
     * sparingly.
     *
     * @param filename
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T getNow(String filename, Class<T> clazz) {
        finishLoadingAsset(filename);
        return get(filename, clazz);
    }
}
