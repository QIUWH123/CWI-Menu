package net.quiwh.cwimenu;

import java.lang.reflect.Method;

public final class MenuContracts {
    private static int checks;

    public static void main(String[] args) throws Exception {
        slideshow();
        slideshowComposition();
        sessions();
        try {
            Class.forName("net.quiwh.cwimenu.client.TitleContracts").getMethod("run").invoke(null);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Title contracts are missing", e);
        }
        Class.forName("net.quiwh.cwimenu.client.LoadingContracts").getMethod("run").invoke(null);
        Class.forName("net.quiwh.cwimenu.client.MusicContracts").getMethod("run").invoke(null);
        Class.forName("net.quiwh.cwimenu.client.FeedbackContracts").getMethod("run").invoke(null);
        System.out.println("Shared menu contracts passed: " + checks);
    }

    private static void slideshowComposition() throws Exception {
        Class<?> frame = Class.forName("net.quiwh.cwimenu.client.SlideshowTimeline$Frame");
        for (double fade : new double[]{0, .25, .5, 1}) {
            Object sample = frame.getConstructor(int.class, int.class, double.class).newInstance(1, 0, fade);
            for (double opacity : new double[]{0, .25, .5, 1}) {
                Object layers = frame.getMethod("blend", double.class).invoke(sample, opacity);
                double current = (double) value(layers, "current");
                double previous = (double) value(layers, "previous");
                near(opacity, current * (1 - previous) + previous, "combined background opacity");
                near(opacity * fade, previous, "old image contribution");
                near(opacity * (1 - fade), current * (1 - previous), "new image contribution");
            }
        }
    }

    private static void slideshow() throws Exception {
        Class<?> type;
        try {
            type = Class.forName("net.quiwh.cwimenu.client.SlideshowTimeline");
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Shared slideshow timeline is not implemented", e);
        }
        Object timeline = type.getConstructor(long.class, int.class).newInstance(1000L, 0);
        Method frame = type.getMethod("frame", long.class);
        Object initial = frame.invoke(timeline, 1000L);
        equal(0, value(initial, "current"), "initial image");
        equal(0, value(frame.invoke(timeline, 12999L), "current"), "initial interval");
        Object transition = frame.invoke(timeline, 13000L);
        equal(1, value(transition, "current"), "first switch at twelve seconds");
        equal(0, value(transition, "previous"), "previous image during fade");
        near(1, (double) value(transition, "previousOpacity"), "fade starts opaque");
        near(.5, (double) value(frame.invoke(timeline, 13838L), "previousOpacity"), "elapsed crossfade");
        near(0, (double) value(frame.invoke(timeline, 14675L), "previousOpacity"), "fade complete");
        equal(1, value(frame.invoke(timeline, 36999L), "current"), "crossfade included in interval");
        equal(0, value(frame.invoke(timeline, 37000L), "current"), "second switch after 24 seconds");
        equal(frame.invoke(timeline, 37500L), frame.invoke(timeline, 37500L), "draw is idempotent");
        equal(0, value(frame.invoke(timeline, 0L), "current"), "clock cannot go negative");
    }

    private static void sessions() throws Exception {
        Class<?> type;
        try {
            type = Class.forName("net.quiwh.cwimenu.client.SessionTarget");
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Session target validation is not implemented", e);
        }
        Method valid = type.getMethod("validLocalId", String.class);
        for (String id : new String[]{"New World", "工业世界", "factory-2", "safe.world"}) {
            equal(true, valid.invoke(null, id), "valid world folder " + id);
        }
        for (String id : new String[]{"", ".", "..", "../CWI/saves", "/absolute", "a/b", "a\\b", "C:world", "bad\nworld"}) {
            equal(false, valid.invoke(null, id), "unsafe world folder");
        }
        equal(false, valid.invoke(null, new Object[]{null}), "null folder");
    }

    private static Object value(Object record, String name) throws Exception {
        return record.getClass().getMethod(name).invoke(record);
    }

    private static void equal(Object expected, Object actual, String name) {
        checks++;
        if (!expected.equals(actual)) throw new AssertionError(name + ": expected " + expected + ", got " + actual);
    }

    private static void near(double expected, double actual, String name) {
        checks++;
        if (Math.abs(expected - actual) > .001) throw new AssertionError(name + ": " + actual);
    }
}
