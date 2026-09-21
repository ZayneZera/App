package com.zayne.seedfilter.gui;

/**
 * Rate-limited display value: chases a target (set every frame from the real data) at a fixed
 * points-per-second speed instead of jumping straight to it, so a step in the underlying stats
 * (e.g. progress going 0 -> 10 in one frame) reads as a smooth count-up over ~2 seconds instead
 * of an instant snap. Only moves upward - callers already only ever feed it a non-decreasing
 * target (see maxPctSeen in ScanProgressScreen/CountdownScreen).
 */
public class SmoothedValue {
    private static final double RATE_PER_SECOND = 5.0; // 10 points in ~2s, per the requested feel

    private double displayed = 0.0;
    private long lastFrameNanos = System.nanoTime();

    public double update(double target) {
        long now = System.nanoTime();
        double deltaSeconds = (now - lastFrameNanos) / 1_000_000_000.0;
        lastFrameNanos = now;

        if (displayed < target) {
            displayed = Math.min(target, displayed + RATE_PER_SECOND * deltaSeconds);
        } else {
            displayed = target;
        }
        return displayed;
    }
}
