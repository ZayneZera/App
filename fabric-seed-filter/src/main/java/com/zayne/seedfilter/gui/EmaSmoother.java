package com.zayne.seedfilter.gui;

/**
 * Exponential moving average over time, for values that can jump in either direction (unlike
 * SmoothedValue, which only climbs). Used to smooth ProbabilityEstimator's expected-attempts
 * denominator: each enabled criterion switches from its static guess to its real empirical pass
 * rate independently, the moment its own sample count crosses the threshold - with several
 * criteria enabled, those switches land at different attempt counts, each one abruptly inflating
 * the denominator by itself. Since the progress percentage is attempts / denominator, an abrupt
 * denominator jump instantly craters the percentage's growth rate right at that point, which is
 * what read as the bar "getting stuck" around a fixed value for a long stretch afterwards.
 * Smoothing the denominator's own changes over a couple of seconds turns each correction into a
 * brief dip in growth speed instead of a hard stall.
 */
public class EmaSmoother {
    private final double timeConstantSeconds;
    private Double current = null;
    private long lastFrameNanos = System.nanoTime();

    public EmaSmoother(double timeConstantSeconds) {
        this.timeConstantSeconds = timeConstantSeconds;
    }

    public double update(double target) {
        long now = System.nanoTime();
        double deltaSeconds = (now - lastFrameNanos) / 1_000_000_000.0;
        lastFrameNanos = now;

        if (current == null) {
            current = target;
            return current;
        }
        double alpha = 1.0 - Math.exp(-deltaSeconds / timeConstantSeconds);
        current = current + (target - current) * alpha;
        return current;
    }
}
