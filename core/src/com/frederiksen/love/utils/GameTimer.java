package com.frederiksen.love.utils;

public class GameTimer {
    private float elapsed = 0f;
    private float delay = 0f;

    public GameTimer() {

    }

    public GameTimer(float delay) {
        this.delay = delay;
    }

    public void start(float delay) {
        this.delay = delay;
        elapsed = 0f;
    }

    public void start() {
        start(delay);
    }

    public void update(float deltaTime) {
        elapsed = Math.min(elapsed + deltaTime, delay);
    }

    public float getProgress() {
        return elapsed / delay;
    }

    public boolean isFinished() {
        return elapsed >= delay;
    }
}
