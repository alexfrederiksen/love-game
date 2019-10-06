package com.frederiksen.love.utils;

import com.badlogic.gdx.math.Vector2;

/**
 * Basic 2x2 matrix implementation
 *
 * @author Alexander Frederiksen
 */
public class Matrix2 {

    private float a = 0;
    private float b = 0;
    private float c = 0;
    private float d = 0;

    public Matrix2() {

    }

    public Matrix2(float a, float b, float c, float d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public Matrix2 set(Matrix2 other) {
        return set(other.a, other.b, other.c, other.d);
    }

    public Matrix2 set(float a, float b, float c, float d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;

        return this;
    }

    public Vector2 mul(Vector2 v) {
        return v.set(a * v.x + b * v.y,
                     c * v.x + d * v.y);
    }

    public Matrix2 scl(float scalar) {
        this.a *= scalar;
        this.b *= scalar;
        this.c *= scalar;
        this.d *= scalar;

        return this;
    }

    public Matrix2 inv() {
        float det = det();

        set(d, -b,
            -c, a);
        scl(1f / det);

        return this;
    }

    public float det() {
        return a * d - b * c;
    }


    public Matrix2 t() {
        return set(a, c, b, d);
    }

}
