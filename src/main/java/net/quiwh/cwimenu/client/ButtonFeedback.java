package net.quiwh.cwimenu.client;

/** Elapsed-time steel feedback, shared by pointer and keyboard selection. */
public final class ButtonFeedback {
    private boolean selected;
    private float from;
    private long changedAt;
    private long selectedAt;
    private boolean pressed;

    public void select(boolean next, long now, boolean immediate) {
        if (next != selected) {
            from = amount(now);
            selected = next;
            changedAt = now;
            selectedAt = now;
        }
        if (immediate) from = selected ? 1 : 0;
    }

    public float amount(long now) {
        float target = selected ? 1 : 0;
        float t = Math.clamp((now-changedAt) / (selected ? 120f : 180f), 0, 1);
        t = t*t*(3-2*t);
        return from + (target-from)*t;
    }

    public boolean helpReady(long now, boolean keyboard) {
        return selected && (keyboard || now-selectedAt >= 300);
    }

    public void press() { pressed = true; }
    public boolean pressed() { return pressed; }
    public boolean release(boolean inside) {
        boolean activate = pressed && inside;
        pressed = false;
        return activate;
    }
}
