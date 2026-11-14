package net.quiwh.cwimenu.client;

/** Music must belong to a custom-menu visit, never arbitrary screens or gameplay. */
public final class MusicContracts {
    public static void main(String[] args) throws Exception { run(); }

    public static void run() throws Exception {
        Class<?> type;
        try {
            type = Class.forName("net.quiwh.cwimenu.client.MenuMusicPolicy");
        } catch (ClassNotFoundException missing) {
            throw new AssertionError("Menu music visit continuity is not implemented", missing);
        }
        Object policy = type.getConstructor().newInstance();
        var opening = type.getMethod("screenOpening", boolean.class, boolean.class);
        var mayPlay = type.getMethod("mayPlay", boolean.class, boolean.class, boolean.class, float.class, float.class);
        // An Options screen opened before CWI's title must not start this soundtrack.
        opening.invoke(policy, false, true);
        expect(false, mayPlay.invoke(policy, false, true, false, 1f, 1f), "unrelated menu cannot start track");
        opening.invoke(policy, true, false);
        expect(true, mayPlay.invoke(policy, false, true, false, 1f, 1f), "custom title starts visit");
        opening.invoke(policy, false, true);
        opening.invoke(policy, false, true);
        expect(true, mayPlay.invoke(policy, false, true, false, 1f, 1f), "settings descendants preserve visit");
        expect(false, mayPlay.invoke(policy, false, true, false, 1f, 0f), "music mute stops track");
        expect(false, mayPlay.invoke(policy, false, true, false, 0f, 1f), "master mute stops track");
        expect(false, mayPlay.invoke(policy, false, true, true, 1f, 1f), "resource reload waits for completion");
        expect(false, mayPlay.invoke(policy, false, false, false, 1f, 1f), "missing artwork cannot play custom music");
        expect(false, mayPlay.invoke(policy, true, true, false, 1f, 1f), "gameplay cannot play menu track");
        expect(true, mayPlay.invoke(policy, false, true, false, 1f, 1f), "mute or reload does not end menu visit");
        opening.invoke(policy, false, false);
        expect(false, mayPlay.invoke(policy, false, true, false, 1f, 1f), "connection or diagnostic ends visit");
        opening.invoke(policy, false, true);
        expect(false, mayPlay.invoke(policy, false, true, false, 1f, 1f), "later menu cannot revive ended visit");
        opening.invoke(policy, true, false);
        expect(true, mayPlay.invoke(policy, false, true, false, 1f, 1f), "return to actual CWI title starts a new visit");
        System.out.println("Music contracts passed");
    }

    private static void expect(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) throw new AssertionError(message + ": expected " + expected + ", got " + actual);
    }
}
