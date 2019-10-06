package com.frederiksen.love.utils;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class CameraTurner {

    private float rotate = (float) Math.toRadians(70.0);
    private Vector3 direction = new Vector3(0f, (float) Math.sin(rotate), -(float) Math.cos(rotate));

    public void update(float deltaTime, OrthographicCamera camera) {

    }

    public void follow(OrthographicCamera camera, Vector2 pos) {
        camera.direction.set(direction);
        camera.position.set(pos, 10f).sub(0f, 15f, 0f);
    }
}
