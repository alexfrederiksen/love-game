package com.frederiksen.love.utils;

import java.util.ArrayList;
import java.util.function.Consumer;

public class Ref<T> {
    private T object;

    private ArrayList<Consumer<Ref<T>>> modifyListeners = new ArrayList<>();

    public Ref(T object) {
        this.object = object;
    }

    public T get() {
        return object;
    }

    public void set(T object) {
        this.object = object;

        for (Consumer<Ref<T>> listener : modifyListeners) listener.accept(this);
    }

    public boolean addListener(Consumer<Ref<T>> tConsumer) {
        return modifyListeners.add(tConsumer);
    }

    public boolean removeListener(Object o) {
        return modifyListeners.remove(o);
    }
}
