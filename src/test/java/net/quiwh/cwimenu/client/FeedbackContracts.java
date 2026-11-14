package net.quiwh.cwimenu.client;

/** Input/motion behavior independent of rendering and frame cadence. */
public final class FeedbackContracts {
    public static void main(String[] args) { run(); }

    public static void run() {
        ButtonFeedback sparse = new ButtonFeedback();
        ButtonFeedback frequent = new ButtonFeedback();
        sparse.select(true, 1000, false);
        frequent.select(true, 1000, false);
        for (int now = 1000; now < 1060; now++) frequent.amount(now);
        near(sparse.amount(1060), frequent.amount(1060), "selection is independent of frame cadence");
        if (sparse.amount(1060) <= 0 || sparse.amount(1060) >= 1) fail("hover must ease between endpoints");
        float before = sparse.amount(1060);
        sparse.select(false, 1060, false);
        near(before, sparse.amount(1060), "reversing hover must not jump");
        near(0, sparse.amount(1300), "hover release settles");
        sparse.select(true, 1400, true);
        near(1, sparse.amount(1400), "motion off retains immediate selection feedback");
        if (sparse.helpReady(1699, false)) fail("pointer help should wait briefly");
        if (!sparse.helpReady(1700, false) || !sparse.helpReady(1400, true)) fail("help timing for mouse and keyboard");
        sparse.select(false, 1750, false);
        if (sparse.helpReady(2000, true)) fail("unselected help must disappear");
        sparse.press();
        if (!sparse.pressed()) fail("mouse down arms inset feedback");
        if (sparse.release(false)) fail("release outside cancels activation");
        if (sparse.release(true)) fail("a second release must not activate");
        sparse.press();
        if (!sparse.release(true) || sparse.pressed()) fail("one valid release activates and resets");
        System.out.println("Feedback contracts passed");
    }

    private static void near(float a, float b, String message) {
        if (Math.abs(a-b) > .00001f) fail(message);
    }
    private static void fail(String message) { throw new AssertionError(message); }
}
