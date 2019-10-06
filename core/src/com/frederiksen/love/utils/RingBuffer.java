package com.frederiksen.love.utils;

import com.badlogic.gdx.utils.Pool;

import java.lang.reflect.Array;
import java.util.function.Supplier;

public class RingBuffer<T extends  Setable<T>> {

    private T[] buffer;
    private int capacity;
    private int size = 0;
    private int next = 0;

    public RingBuffer(T[] buffer) {
        this.buffer = buffer;
        this.capacity = buffer.length;
    }

    public RingBuffer(T[] buffer,  Supplier<T> factory) {
        this(buffer);

        fill(factory);
    }

    public void fill(Supplier<T> factory) {
        for (int i = 0; i < capacity; i++) {
            buffer[i] = factory.get();
        }
    }

    public void add(T e) {
        buffer[next].set(e);
        // go to next index
        next++;
        next %= capacity;
        // increment size
        if (size < capacity) size++;
    }

    /**
     * Peeks back in buffer
     * @param back indexes in the past, last index is back 0
     * @return element
     */
    public T peek(int back) {
        return buffer[modClamp(next - back - 1, capacity)];
    }

    public T peek() {
        return peek(0);
    }

    public T get(int index) {
        return buffer[modClamp(index, capacity)];
    }

    public int clampIndex(int index) {
        return modClamp(index, capacity);
    }

    public static int modClamp(int index, int mod) {
        index %= mod;
        if (index < 0) index += mod;
        return index;
    }

    public void setNextIndex(int next) {
        this.next = modClamp(next, capacity);
    }

    public int getNextIndex() {
        return next;
    }

    public int getCapacity() {
        return capacity;
    }
}
