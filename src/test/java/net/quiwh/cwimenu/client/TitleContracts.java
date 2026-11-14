package net.quiwh.cwimenu.client;

/** Headless contracts for rendering/input geometry and session intro state. */
public final class TitleContracts {
    public static void main(String[] args) { run(); }

    public static void run() {
        originalCompositionAndInput();
        captionAndArtworkShareOneHitbox();
        viewportChangesKeepControlsReachable();
        gateRemainsUnobstructed();
        controlsAndCaptionsAppearTogether();
        rebuildAndReturnKeepIntroState();
        System.out.println("Title contracts passed");
    }

    private static void originalCompositionAndInput() {
        MenuLayout layout = MenuLayout.create(1920, 1080, 480, 270);
        // Four separate square machine doors remain in the lower part of the scene.
        for (int index = 0; index < 4; index++) {
            MenuLayout.Rect tile = layout.main(index);
            equal(tile.width(), tile.height(), "steel tiles remain square");
            if (tile.y() < layout.height() * .65) throw new AssertionError("controls cover the scene center");
            if (index > 0 && tile.x() <= layout.main(index - 1).x() + tile.width()) {
                throw new AssertionError("main controls must have visible gaps");
            }
        }
        equal(0, layout.band().x(), "stretched band origin");
        equal(layout.width(), layout.band().width(), "band spans viewport");
        // Global Minecraft GUI scale must not change title-relative geometry or clicks.
        MenuLayout scaleFive = MenuLayout.create(1920, 1080, 384, 216);
        equal(layout.main(0).x(), scaleFive.main(0).x(), "local anchor survives GUI scale");
        equal(layout.main(0).width(), scaleFive.main(0).width(), "local size survives GUI scale");
        MenuLayout.Rect tile = scaleFive.main(0);
        double x = tile.x() + tile.width() / 2.0;
        double y = tile.y() + tile.height() / 2.0;
        close(x, scaleFive.toLocalX(scaleFive.toScreenX(x)), "horizontal input inverse");
        close(y, scaleFive.toLocalY(scaleFive.toScreenY(y)), "vertical input inverse");
        if (!tile.contains(scaleFive.toLocalX(scaleFive.toScreenX(x)),
                scaleFive.toLocalY(scaleFive.toScreenY(y)))) {
            throw new AssertionError("click mapping missed rendered singleplayer");
        }
    }

    private static void captionAndArtworkShareOneHitbox() {
        MenuLayout layout = MenuLayout.create(1920, 1080, 480, 270);
        for (int index = 0; index < 4; index++) {
            MenuLayout.Rect tile = layout.main(index);
            MenuLayout.Rect caption = layout.caption(index);
            MenuLayout.Rect bounds = layout.mainBounds(index);
            contains(bounds, tile, "main artwork");
            contains(bounds, caption, "caption");
            if (!bounds.contains(tile.x() + tile.width() / 2.0, (tile.y() + tile.height() + caption.y()) / 2.0)) {
                throw new AssertionError("gap between tile and caption is not clickable");
            }
            if (index < 3 && overlaps(bounds, layout.mainBounds(index + 1))) {
                throw new AssertionError("adjacent main controls share input bounds");
            }
        }
    }

    private static void viewportChangesKeepControlsReachable() {
        for (int[] size : viewports()) {
            MenuLayout layout = MenuLayout.create(size[0], size[1], Math.max(1, size[0] / 4), Math.max(1, size[1] / 4));
            inBounds(layout.logo(), layout, "logo");
            inBounds(layout.band(), layout, "band");
            inBounds(layout.help(), layout, "contextual help");
            for (int index = 0; index < 4; index++) {
                inBounds(layout.mainBounds(index), layout, "main and caption " + index);
            }
            for (int row = 0; row < 3; row++) {
                inBounds(layout.utility(false, row), layout, "left utility");
                inBounds(layout.utility(true, row), layout, "right utility");
            }
        }
    }

    private static void gateRemainsUnobstructed() {
        // Source-art gate bounds normalized against the original stretched background.
        for (int[] size : viewports()) {
            MenuLayout layout = MenuLayout.create(size[0], size[1], Math.max(1, size[0] / 3), Math.max(1, size[1] / 3));
            MenuLayout.Rect gate = new MenuLayout.Rect((int) Math.floor(layout.width() * .377),
                    (int) Math.floor(layout.height() * .264),
                    (int) Math.ceil(layout.width() * .246), (int) Math.ceil(layout.height() * .337));
            clearOf(gate, layout.logo(), "logo");
            clearOf(gate, layout.band(), "band");
            for (int index = 0; index < 4; index++) clearOf(gate, layout.mainBounds(index), "main control");
            for (int row = 0; row < 3; row++) {
                clearOf(gate, layout.utility(false, row), "left utility");
                clearOf(gate, layout.utility(true, row), "right utility");
            }
        }
    }

    private static void controlsAndCaptionsAppearTogether() {
        IntroTimeline intro = new IntroTimeline();
        if (intro.isInteractive(1000)) throw new AssertionError("unentered intro exposes ghost controls");
        intro.enter(1000);
        close(0, intro.alpha(IntroTimeline.Part.MAIN, 1000), "controls initially hidden");
        if (intro.isInteractive(1100)) throw new AssertionError("barely visible controls accept input");
        double previous = 0;
        for (long time = 1000; time <= 1700; time += 25) {
            double alpha = intro.alpha(IntroTimeline.Part.MAIN, time);
            if (alpha < previous || alpha < 0 || alpha > 1) throw new AssertionError("fade is not monotonic and bounded");
            for (IntroTimeline.Part part : new IntroTimeline.Part[]{IntroTimeline.Part.CAPTION,
                    IntroTimeline.Part.UTILITY_TOP, IntroTimeline.Part.UTILITY_MIDDLE, IntroTimeline.Part.UTILITY_BOTTOM}) {
                close(alpha, intro.alpha(part, time), "control and label fades must stay synchronized");
            }
            previous = alpha;
        }
        close(.5, intro.alpha(IntroTimeline.Part.MAIN, 1290), "control midpoint");
        if (!intro.isInteractive(1290)) throw new AssertionError("readable controls remain blocked");
        close(1, intro.alpha(IntroTimeline.Part.MAIN, 1500), "controls fully visible within half a second");
        close(1, intro.alpha(IntroTimeline.Part.LOGO, 1600), "logo fully visible within six tenths");
    }

    private static void rebuildAndReturnKeepIntroState() {
        IntroTimeline intro = new IntroTimeline();
        intro.enter(500);
        intro.enter(700);
        close(.5, intro.alpha(IntroTimeline.Part.MAIN, 790), "resize preserves first-entry clock");
        intro.leave();
        intro.enter(5000);
        close(1, intro.alpha(IntroTimeline.Part.MAIN, 5000), "return does not replay main intro");
        close(1, intro.alpha(IntroTimeline.Part.CAPTION, 5000), "return completes caption fade");
        if (!intro.isInteractive(5000)) throw new AssertionError("returning controls are not interactive");
        close(.56, intro.alpha(IntroTimeline.Part.BAND, 5000), "return retains band opacity");
    }

    private static int[][] viewports() {
        return new int[][] {{1920, 1080}, {1280, 799}, {2560, 1080}, {600, 1200}, {320, 180}, {128, 80}};
    }

    private static void contains(MenuLayout.Rect outer, MenuLayout.Rect inner, String name) {
        if (!outer.contains(inner.x(), inner.y()) || !outer.contains(inner.x() + inner.width() - 1, inner.y() + inner.height() - 1)) {
            throw new AssertionError(name + " outside combined control bounds");
        }
    }

    private static boolean overlaps(MenuLayout.Rect a, MenuLayout.Rect b) {
        return a.x() < b.x() + b.width() && a.x() + a.width() > b.x()
                && a.y() < b.y() + b.height() && a.y() + a.height() > b.y();
    }

    private static void clearOf(MenuLayout.Rect gate, MenuLayout.Rect control, String name) {
        if (overlaps(gate, control)) throw new AssertionError(name + " blocks the Stargate");
    }

    private static void inBounds(MenuLayout.Rect rect, MenuLayout layout, String name) {
        if (rect.x() < 0 || rect.y() < 0 || rect.x() + rect.width() > layout.width() || rect.y() + rect.height() > layout.height()) {
            throw new AssertionError(name + " outside " + layout + ": " + rect);
        }
    }

    private static void equal(int expected, int actual, String name) {
        if (expected != actual) throw new AssertionError(name + ": expected " + expected + ", got " + actual);
    }

    private static void close(double expected, double actual, String name) {
        if (Math.abs(expected - actual) > .00001) throw new AssertionError(name + ": expected " + expected + ", got " + actual);
    }
}
