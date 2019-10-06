package com.frederiksen.love.utils;

import com.badlogic.gdx.utils.Array;

import java.util.function.Supplier;

/**
 * This pool keeps track of all objects that it loans and may take them back upon
 * {@link TempPool#freeAll()}. Note that {@link TempPool#free(Object)} is O(n) where n
 * is the size of the pool, not the best.
 *
 * @author Alexander Frederiksen
 *
 * @param <T>
 */
public class TempPool<T> {
    private Supplier<T> factory;
    private Array<T> objects;
    private int freeCount;

    public TempPool(int initialCapacity, Supplier<T> factory) {
        this.factory = factory;
        objects = new Array<>(true, initialCapacity);

        for (int i = 0; i < initialCapacity; i++) {
            objects.add(factory.get());
        }

        freeCount = initialCapacity;
    }

    public T obtain() {
        // create more objects as we need them
        while (freeCount <= 0) {
            objects.add(factory.get());
            freeCount++;
        }


        // get last object
        T obj =  objects.get(objects.size - freeCount);
        freeCount--;

        return obj;
    }

    public void free(T obj) {
        int i = objects.indexOf(obj, true);
        // swap into free zone
        objects.swap(i, objects.size - freeCount - 1);
        freeCount++;
    }

    /**
     * This method can potentially be dangerous, only use if you know
     * the objects are no longer in use.
     */
    public void freeAll() {
        freeCount = objects.size;
    }
}
