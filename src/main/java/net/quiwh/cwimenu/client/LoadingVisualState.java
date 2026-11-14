package net.quiwh.cwimenu.client;

/** Pure layout/fade rules; it never advances or writes the native reload state. */
public record LoadingVisualState(float backgroundAlpha, float elementsAlpha) {
    public static LoadingVisualState reload(long now, boolean fadeIn, long fadeInStart, long fadeOutStart) {
        if (fadeOutStart >= 0) return startup(now, fadeOutStart);
        float entered = fadeIn && fadeInStart >= 0 ? unit((now - fadeInStart) / 500f) : 1;
        return new LoadingVisualState(fadeIn ? Math.max(.15f, entered) : 1, entered);
    }

    public static LoadingVisualState startup(long now, long fadeOutStart) {
        if (fadeOutStart < 0) return new LoadingVisualState(1, 1);
        float elapsed = Math.max(0, now - fadeOutStart) / 1000f;
        // Legacy Drippy removes the elements first, then reveals the native screen.
        return new LoadingVisualState(1 - unit(elapsed - 1), 1 - unit(elapsed));
    }

    public static Geometry geometry(int width, int height) {
        double quarter = Math.min(width * .75, height) * .25;
        int halfWidth = (int)(quarter * 2);
        return new Geometry(width / 2 - halfWidth, width / 2, halfWidth,
                height / 2 - (int)(quarter * .5), (int)quarter, (int)(height * .8325) - 5);
    }

    public static int progressPixels(Geometry geometry, float progress) {
        return (int)Math.ceil(Math.max(0, geometry.halfWidth * 2 - 2) * unit(progress));
    }

    private static float unit(float value) { return Math.max(0, Math.min(1, value)); }

    public record Geometry(int left, int centerX, int halfWidth, int logoTop, int logoHeight, int barTop) {
        public int right() { return centerX + halfWidth; }
    }
}
