package com.frederiksen.love;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class Renderer extends SpriteBatch {

    private Matrix4 objMatrix = new Matrix4();
    private FollowCamera camera = new FollowCamera();

    public void resize(int width, int height) {
        camera.resize(width, height);
    }

    public void look(Vector2 pos, float angleY) {
        camera.update(pos, angleY);
    }

    public void begin2d(float width, float height) {
        reset();
        getProjectionMatrix().setToOrtho2D(0f, 0f, width, height);

        super.begin();
    }

    public void begin() {
        reset();
        setProjectionMatrix(camera.combined);

        super.begin();
    }

    public void beginObj(Vector2 base, float angleZ) {
        beginObj(base.x, base.y, angleZ);
    }

    public void beginObj(float baseX, float baseY, float angleZ) {
        objMatrix.idt()
                 .translate(baseX, baseY, 0f)
                 .rotate(Vector3.Z, angleZ + 90f)
                 .rotate(Vector3.X, 90f)
                 .translate(-baseX, -baseY, 0f);

        setTransformMatrix(objMatrix);
    }

    /**
     * Draws object sprites perpendicular to the ground
     *
     * @param sprite
     */
    public void drawObj(Sprite sprite, float angleZ) {
        beginObj(sprite.getX() + sprite.getWidth() * 0.5f, sprite.getY(), angleZ);
        sprite.draw(this);
    }

    private Matrix4 identity = new Matrix4();

    public void reset() {
        setTransformMatrix(identity);
    }
}
