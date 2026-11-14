package net.quiwh.cwimenu.client;

/** Headless boundary cases for loading geometry and the two-stage visual exit. */
public final class LoadingContracts {
    public static void main(String[] args) { run(); }

    public static void run() {
        geometryUsesTheCurrentGuiSize();
        nativeFadeInRemainsTranslucent();
        elementsDisappearBeforeTheBackground();
        startupAndReloadShareExitTiming();
        System.out.println("Loading contracts passed");
    }

    private static void geometryUsesTheCurrentGuiSize() {
        LoadingVisualState.Geometry regular = LoadingVisualState.geometry(427, 267);
        equal(80, regular.left(), "logo/bar left edge");
        equal(133, regular.halfWidth(), "responsive logo half width");
        equal(66, regular.logoHeight(), "responsive logo height");
        equal(100, regular.logoTop(), "centered logo top");
        equal(217, regular.barTop(), "native progress row");
        LoadingVisualState.Geometry narrow = LoadingVisualState.geometry(200, 600);
        equal(75, narrow.halfWidth(), "narrow screen constrains logo by width");
        LoadingVisualState.Geometry wide = LoadingVisualState.geometry(1200, 200);
        equal(100, wide.halfWidth(), "wide screen constrains logo by height");
        equal(0, LoadingVisualState.progressPixels(regular, -1), "negative progress clamps");
        equal(264, LoadingVisualState.progressPixels(regular, 2), "finished progress clamps");
        equal(132, LoadingVisualState.progressPixels(regular, .5f), "progress uses native interior width");
    }

    private static void nativeFadeInRemainsTranslucent() {
        LoadingVisualState start = LoadingVisualState.reload(1000, true, 1000, -1);
        close(.15, start.backgroundAlpha(), "reload starts over the native underlying screen");
        close(0, start.elementsAlpha(), "elements start hidden");
        LoadingVisualState middle = LoadingVisualState.reload(1250, true, 1000, -1);
        close(.5, middle.backgroundAlpha(), "reload background fade-in");
        close(.5, middle.elementsAlpha(), "reload element fade-in");
        close(1, LoadingVisualState.reload(1500, true, 1000, -1).elementsAlpha(), "fade-in completes at 500 ms");
        close(1, LoadingVisualState.reload(1000, false, -1, -1).backgroundAlpha(), "initial native overlay is opaque");
    }

    private static void elementsDisappearBeforeTheBackground() {
        LoadingVisualState first = LoadingVisualState.startup(1499, 1000);
        close(1, first.backgroundAlpha(), "exit first half keeps full background");
        close(.501, first.elementsAlpha(), "elements fade during first second");
        LoadingVisualState boundary = LoadingVisualState.startup(2000, 1000);
        close(1, boundary.backgroundAlpha(), "background starts fading after elements");
        close(0, boundary.elementsAlpha(), "elements disappear at one second");
        close(.5, LoadingVisualState.startup(2500, 1000).backgroundAlpha(), "background reveals the underlying screen");
        close(0, LoadingVisualState.startup(3000, 1000).backgroundAlpha(), "background disappears at removal boundary");
        close(0, LoadingVisualState.startup(4000, 1000).backgroundAlpha(), "opacity cannot become negative");
    }

    private static void startupAndReloadShareExitTiming() {
        for (long now : new long[] {1000, 1500, 2000, 2500, 3000}) {
            LoadingVisualState startup = LoadingVisualState.startup(now, 1000);
            LoadingVisualState reload = LoadingVisualState.reload(now, true, 0, 1000);
            close(startup.backgroundAlpha(), reload.backgroundAlpha(), "own overlay state gives same exit background");
            close(startup.elementsAlpha(), reload.elementsAlpha(), "own overlay state gives same exit elements");
        }
        close(1, LoadingVisualState.startup(1000, 1000).elementsAlpha(), "newly assigned completion timestamp starts fully visible");
    }

    private static void equal(int expected, int actual, String name) {
        if (expected != actual) throw new AssertionError(name + ": expected " + expected + ", got " + actual);
    }

    private static void close(double expected, double actual, String name) {
        if (Math.abs(expected - actual) > .00001) throw new AssertionError(name + ": expected " + expected + ", got " + actual);
    }
}
