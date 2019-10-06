package com.frederiksen.love.utils;

import com.badlogic.gdx.math.Vector2;

/**
 * Defines an inner product space
 *
 * @param <V> vector
 * @param <F> field
 */
public interface InnerSpace<V, F extends Number> extends VectorSpace<V, F> {
    F innerProduct(V other);

    static <F extends Number, V extends InnerSpace<V, F>> V normalize(V v) {
        return v.scl(v.invert(len(v)));
    }

    static <F extends Number, V extends InnerSpace<V, F>> F len(V v) {
        return v.sqrt(len2(v));
    }

    static <F extends Number, V extends InnerSpace<V, F>> F len2(V v) {
        return v.innerProduct(v);
    }
}
