package com.frederiksen.love.utils;

import java.util.function.Supplier;

/**
 * Simple implementation class for {@link com.badlogic.gdx.utils.Pool}.
 *
 * @author Alexander Frederiksen
 *
 * @param <T>
 */
public class Pool<T> extends com.badlogic.gdx.utils.Pool<T> {

    private Supplier<T> factory;

    public Pool(int initialCapacity, Supplier<T> factory) {
        super(initialCapacity);
        this.factory = factory;

        for (int i = 0; i < initialCapacity; i++) {
            free(newObject());
        }
    }

    @Override
    protected T newObject() {
        T obj = factory.get();
        reset(obj);

        return obj;
    }
}
