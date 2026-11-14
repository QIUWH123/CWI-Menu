package net.quiwh.cwimenu.client;

/** Shared wall-clock selection; rendering it twice never advances the state. */
public final class SlideshowTimeline {
    private static final long FIRST_INTERVAL_MS = 12_000;
    private static final long INTERVAL_MS = 24_000;
    private static final double FADE_MS = 1_675;
    private final long start;
    private final int initial;

    public SlideshowTimeline(long start, int initial) {
        this.start = start;
        this.initial = initial & 1;
    }

    public Frame frame(long now) {
        long elapsed = Math.max(0, now - start);
        if (elapsed < FIRST_INTERVAL_MS) return new Frame(initial, initial, 0);
        long sinceFirst = elapsed - FIRST_INTERVAL_MS;
        int current = initial ^ (int) ((sinceFirst / INTERVAL_MS + 1) & 1);
        double oldAlpha = Math.max(0, 1 - (sinceFirst % INTERVAL_MS) / FADE_MS);
        return new Frame(current, current ^ 1, oldAlpha);
    }

    public record Frame(int current, int previous, double previousOpacity) {
        /** Draw current first, then previous, preserving the whole group's opacity. */
        public Blend blend(double opacity) {
            double group = Math.clamp(opacity, 0, 1);
            double previous = group * previousOpacity;
            double current = previous == 1 ? 0 : group * (1 - previousOpacity) / (1 - previous);
            return new Blend(current, previous);
        }
    }

    public record Blend(double current, double previous) {}
}
