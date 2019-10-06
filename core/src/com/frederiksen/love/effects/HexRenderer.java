package com.frederiksen.love.effects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.frederiksen.love.Renderer;
import com.frederiksen.love.Resources;
import com.frederiksen.love.utils.GameTimer;
import com.frederiksen.love.utils.Matrix2;
import com.frederiksen.love.utils.Pool;

import java.util.HashMap;
import java.util.Iterator;

public class HexRenderer {
    private static final float HEX_SPACING = 1.4f;
    private static final float HEX_SIZE    = 1.7f;

    private static final float DEFAULT_DELAY   = 0.1f;
    private static final float DEFAULT_SUSTAIN = 0.9f;

    private static final Vector2 tempVec = new Vector2();

    private static final Matrix4 translateMatrix = new Matrix4();

    public class Hex implements com.badlogic.gdx.utils.Pool.Poolable {
        private int x = 0;
        private int y = 0;

        private float gameX;
        private float gameY;

        private int delta = -1;
        private float activity = 0f;
        private float activationSpeed = 3.5f;

        private GameTimer timer = new GameTimer();

        private Sprite sprite = new Sprite();

        private Color color = new Color();

        public void set(int x, int y) {
            this.x = x;
            this.y = y;

            hexToGameSpace(tempVec.set(x, y));

            sprite.setRegion(hexTex);
            sprite.setSize(HEX_SIZE, HEX_SIZE);

            gameX = tempVec.x;
            gameY = tempVec.y;
        }

        @Override
        public void reset() {
            activity = 0f;
            delta = -1;

            color.set(MathUtils.random.nextInt());
        }

        public void ping(float delay, float sustain) {
            float len = 1f / activationSpeed;
            if (delta < 0 && activity <= 0)
                activity = -delay * activationSpeed;
            timer.start(-activity * len + sustain);
            delta = 1;
        }

        public boolean update(float deltaTime) {
            activity += delta * activationSpeed * deltaTime;
            activity = Math.min(activity, 1f);

            timer.update(deltaTime);
            if (timer.isFinished()) delta = -1;

            return delta < 0 && activity <= 0f;
        }

        public void render(Renderer renderer) {
            float t = MathUtils.clamp(activity, 0f, 1f);

            //float size = t * HEX_SIZE;
            //sprite.setSize(size, size);
            sprite.setCenter(gameX, gameY);

            translateMatrix.idt().translate(0f, 0f, -2.5f * (1f - t));
            renderer.setTransformMatrix(translateMatrix);


            sprite.setColor(color.a, color.g, color.b, t);
            sprite.draw(renderer);
        }

        @Override
        public int hashCode() {
            int prime = 31;
            int h = 0;

            h = prime * (h + x);
            h = prime * (h + y);

            return h;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Hex) {
                Hex hex = (Hex) obj;
                return hex.x == x && hex.y == y;
            } else return false;
        }

        public Vector2 getCenter(Vector2 v) {
            return v.set(x, y);
        }
    }

    public class Spawner implements TraceReader {
        private int centerX;
        private int centerY;

        void pingCluster(int x, int y, int radius) {
            centerX = x;
            centerY = y;

            traceHexCluster(radius, this);
        }

        @Override
        public void read(int l, int x, int y) {
            pingHex(centerX + x, centerY + y, l * DEFAULT_DELAY, DEFAULT_SUSTAIN);
        }
    }

    private Pool<Hex>         hexPool = new Pool<>(50, Hex::new);
    private HashMap<Hex, Hex> hexes   = new HashMap<>(50);

    private Spawner spawner = new Spawner();

    private Texture hexTex;

    private Matrix2 gameToHexMatrix = new Matrix2();
    private Matrix2 hexToGameMatrix = new Matrix2();

    public HexRenderer() {
        computeMatrices();
    }

    public void loadResources(Resources resources) {
        hexTex = resources.get("hex-solid.png", Texture.class);
    }

    public void computeMatrices() {
        hexToGameMatrix.set((float) Math.sqrt(3), (float) -Math.sqrt(3),
                            1f, 1f);
        hexToGameMatrix.scl(HEX_SPACING / 2f);

        gameToHexMatrix.set(hexToGameMatrix).inv();
    }


    private Hex tempHex = new Hex();
    public Hex getHex(int x, int y) {
        tempHex.set(x, y);
        return hexes.get(tempHex);
    }

    public void pingHex(int x, int y, float delay, float sustain) {
        Hex hex = getHex(x, y);
        // create new hex if needed
        if (hex == null) {
            hex = hexPool.obtain();
            hex.set(x, y);
            // uses itself as the key
            hexes.put(hex, hex);
        }

        hex.ping(delay, sustain);
    }

    public void pingHexes(Vector2 center, int radius) {
        // convert to hex space
        tempVec.set(center);
        gameToHexSpace(tempVec);
        // lock to nearest hexagon (integer position)
        nearestHex(tempVec);
        spawner.pingCluster((int) tempVec.x, (int) tempVec.y, radius);
    }

    private static final float MIN_SNAP = 1f / 3f;
    private static final float MAX_SNAP = 2f / 3f;
    public Vector2 nearestHex(Vector2 v) {
        float x0 = MathUtils.floor(v.x);
        float x1 = MathUtils.ceil(v.x);
        float y0 = MathUtils.floor(v.y);
        float y1 = MathUtils.ceil(v.y);

        float dx = v.x - x0;
        float dy = v.y - y0;
        if (dx < MIN_SNAP && dy > MAX_SNAP)
            v.set(x0, y1);
        else if (dx > MAX_SNAP && dy < MIN_SNAP)
            v.set(x1, y0);
        else if (dx < MAX_SNAP && dy < MAX_SNAP)
            v.set(x0, y0);
        else
            v.set(x1, y1);

        return v;
    }

    @FunctionalInterface
    public interface TraceReader {
        void read(int l, int x, int y);
    }

    public void traceHexCluster(int radius, TraceReader reader) {
        for (int l = 0; l <= radius; l++) {
            for (int i = 0; i < l; i++) {
                // first and third quadrant
                // vertical line
                reader.read(l, l, i);
                reader.read(l, -l, -i);
                // horizontal line
                reader.read(l, i, l);
                reader.read(l, -i, -l);
                if (i >= 1) {
                    // second and fourth quadrant
                    reader.read(l, -l + i, i);
                    reader.read(l, l - i, -i);
                }
            }
            if (l == 0) {
                reader.read(0, 0, 0);
            } else {
                // put corners
                reader.read(l, l, l);
                reader.read(l, -l, -l);
            }
        }
    }

    public void update(float deltaTime) {
        Iterator<Hex> it = hexes.values().iterator();
        while(it.hasNext()) {
            Hex hex = it.next();
            boolean dead = hex.update(deltaTime);
            if (dead) {
                hexPool.free(hex);
                it.remove();
            }
        }
    }

    public void render(Renderer renderer) {
        for (Hex hex : hexes.values())
            hex.render(renderer);
    }

    public Vector2 gameToHexSpace(Vector2 x) {
        return gameToHexMatrix.mul(x);
    }

    public Vector2 hexToGameSpace(Vector2 x) {
        return hexToGameMatrix.mul(x);
    }
}
