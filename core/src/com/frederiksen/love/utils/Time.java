package com.frederiksen.love.utils;

import static java.lang.System.nanoTime;

public class Time {
    private static final long NANOS_PER_MILLIS = 1000000;

    private long start;
    private long end;
    private boolean loop;

    public Time(boolean loop) {
        this.loop = loop;
    }

    /**
     *
     * @param loop true if should loop
     * @param interval in seconds
     */
    public Time(boolean loop, float interval) {
        this(loop);
        start(interval);
    }

    /**
     * Starts the time
     *
     * @param interval in seconds
     * @return
     */
    public Time start(float interval) {
        start = nanoTime();
        end = start + secsToNanos(interval);

        return this;
    }

    public float getElapsed() {
        long time = nanoTime();

        if (time >= end) {
            if (loop) {
                long diff = start - end;
                start = time - (time - start) % diff;
                end = start + diff;
            } else {
                time = end;
            }
        }

        return nanosToSecs(time - start);
    }

    public boolean isFinished() {
        long time = nanoTime();
        return time >= end;
    }

    public float getProgress() {
        return getElapsed() / nanosToSecs(start - end);
    }

    public static float nanosToSecs(long nanos) {
        return (float) (nanos / NANOS_PER_MILLIS) / 1000;
    }

    public static long secsToNanos(float secs) {
        return (long) (secs * 1000) * NANOS_PER_MILLIS;
    }
}
