package com.frederiksen.love.utils;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.in;
import static java.lang.System.nanoTime;

/**
 * Timer that can run in paused state
 */
public class Timer {

    private static Timer instance = new Timer();

    public static Timer getInstance() {
        return instance;
    }

    public static class Task {
        private Runnable runnable;
        private long interval;
        private long start;

        public Task(float intervalSecs) {
            interval = Time.secsToNanos(intervalSecs);
        }

        public Task(Runnable runnable, float intervalSecs) {
            this(intervalSecs);
            this.runnable = runnable;
        }

        public void start() {
            start = nanoTime();
        }

        public void run() {
            if (runnable != null)
                runnable.run();
        }

        public void update() {
            long time = nanoTime();
            long diff = time - start;

            if (diff > interval) {
                start = time - diff % interval;
                run();
            }
        }
    }

    private List<Task> tasks = new ArrayList<>();

    public static void schedule(Task task) {
        getInstance().tasks.add(task);
        task.start();
    }

    public static void cancel(Task task) {
        getInstance().tasks.remove(task);
    }

    public void update() {
        for (Task task : tasks)
            task.update();
    }

}
