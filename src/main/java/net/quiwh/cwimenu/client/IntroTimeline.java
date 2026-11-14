package net.quiwh.cwimenu.client;

/** First-entry fade state shared by title instances and retained through resize. */
public final class IntroTimeline {
    public enum Part {
        BAND(0, 450, .56f), LOGO(0, 600, 1), MAIN(80, 420, 1),
        UTILITY_TOP(80, 420, 1), UTILITY_MIDDLE(80, 420, 1),
        UTILITY_BOTTOM(80, 420, 1), CAPTION(80, 420, 1);

        private final long delay;
        private final long duration;
        private final float opacity;
        Part(long delay, long duration, float opacity) {
            this.delay = delay;
            this.duration = duration;
            this.opacity = opacity;
        }
    }

    private long startedAt = Long.MIN_VALUE;
    private boolean completed;

    public void enter(long now) {
        if (startedAt == Long.MIN_VALUE) startedAt = now;
    }

    public void leave() {
        if (startedAt != Long.MIN_VALUE) completed = true;
    }

    public float alpha(Part part, long now) {
        if (completed) return part.opacity;
        if (startedAt == Long.MIN_VALUE) return 0;
        long sinceDelay = now - startedAt - part.delay;
        if (sinceDelay <= 0) return 0;
        float progress = Math.min(1, (float) sinceDelay / part.duration);
        return part.opacity * progress * progress * (3 - 2 * progress);
    }

    /** Hidden or barely visible controls must not activate or receive initial focus. */
    public boolean isInteractive(long now) { return alpha(Part.MAIN, now) >= .5f; }
}
