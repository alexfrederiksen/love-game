package com.frederiksen.love.utils;

import java.util.function.Function;

import static java.lang.System.nanoTime;

public class Oscillator {

    private Function<Float, Float> function;
    private float functionPeriod;
    private float desiredFrequency;
    private float phase;

    /**
     * @param template template oscillator
     * @param desiredFrequency per second
     * @param phase in seconds
     */
    public Oscillator(Oscillator template, float desiredFrequency, float phase) {
        set(template);
        this.desiredFrequency = desiredFrequency;
        this.phase = phase;
    }

    public Oscillator(Function<Float, Float> function, float functionPeriod, float desiredFrequency, float phase) {
        this.function = function;
        this.functionPeriod = functionPeriod;
        this.desiredFrequency = desiredFrequency;
        this.phase = phase;
    }

    private Oscillator set(Oscillator obj) {
        function = obj.function;
        functionPeriod = obj.functionPeriod;
        desiredFrequency = obj.desiredFrequency;
        phase = obj.phase;

        return this;
    }

    /**
     * @return value [0, 1]
     */
    public float get() {
        return function.apply(functionPeriod * desiredFrequency * (Time.nanosToSecs(nanoTime()) + phase));
    }

    public static final Oscillator SINE = new Oscillator((x) -> (float) (0.5 * (Math.sin(x) + 1)), (float) (2 * Math.PI), 1f, 0f);
}
