package com.frederiksen.love;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class FollowCamera extends PerspectiveCamera {

    private float followDistance = 7f;
    private float angleX = 65f;

    private Vector3 tempVec3 = new Vector3();

    public void resize(float width, float height) {
        viewportWidth = width;
        viewportHeight = height;
    }

    public void update(Vector2 target, float rot) {
        update(target, rot, true);
    }

    public void update(Vector2 target, float rot, boolean updateFrustum) {
        float run = followDistance * MathUtils.sinDeg(angleX);
        float height = followDistance * MathUtils.cosDeg(angleX);

        float dx = run * MathUtils.cosDeg(rot);
        float dy = run * MathUtils.sinDeg(rot);

        position.set(target, height).add(dx, dy, 0f);

        float aspect = viewportWidth / viewportHeight;
        projection.setToProjection(Math.abs(near), Math.abs(far), fieldOfView, aspect);

        view.setToLookAt(position, tempVec3.set(target, 0f), Vector3.Z);

        // adapted from original perspective camera
        combined.set(projection);
        Matrix4.mul(combined.val, view.val);

		if (updateFrustum) {
			invProjectionView.set(combined);
			Matrix4.inv(invProjectionView.val);
			frustum.update(invProjectionView);
		}
    }

    public float getFollowDistance() {
        return followDistance;
    }

    public void setFollowDistance(float followDistance) {
        this.followDistance = followDistance;
    }

    public float getAngleX() {
        return angleX;
    }

    public void setAngleX(float angleX) {
        this.angleX = angleX;
    }
}
