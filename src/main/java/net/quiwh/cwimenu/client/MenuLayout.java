package net.quiwh.cwimenu.client;

/** Original CWI composition, mapped locally without changing Minecraft's GUI scale. */
public record MenuLayout(int width, int height, double scaleX, double scaleY) {
    public record Rect(int x, int y, int width, int height) {
        public boolean contains(double px, double py) {
            return px >= x && py >= y && px < x + width && py < y + height;
        }
    }

    public static MenuLayout create(int framebufferWidth, int framebufferHeight, int screenWidth, int screenHeight) {
        if (framebufferWidth < 1 || framebufferHeight < 1 || screenWidth < 1 || screenHeight < 1)
            throw new IllegalArgumentException("Menu dimensions must be positive");
        double scale = Math.min(framebufferWidth / 480.0, framebufferHeight / 270.0);
        int width = (int) Math.ceil(framebufferWidth / scale);
        int height = (int) Math.ceil(framebufferHeight / scale);
        return new MenuLayout(width, height, (double) screenWidth / width, (double) screenHeight / height);
    }

    public Rect logo() { return new Rect(width / 2 - 94, (int) Math.round(height * .0574), 188, 48); }
    public Rect band() { return new Rect(0, height - 91, width, 76); }
    public Rect main(int index) {
        if (index < 0 || index > 3) throw new IllegalArgumentException("Main button index");
        return new Rect(width / 2 - 114 + 60 * index, height - 84, 48, 48);
    }
    public Rect utility(boolean right, int row) {
        if (row < 0 || row > 2) throw new IllegalArgumentException("Utility row");
        return new Rect(width / 2 + (right ? 128 : -142), height - 84 + 17 * row, 14, 14);
    }
    public Rect caption(int index) {
        Rect parent = main(index);
        return new Rect(parent.x(), parent.y() + 53, parent.width(), 9);
    }
    public Rect mainBounds(int index) {
        Rect tile = main(index);
        Rect label = caption(index);
        return new Rect(tile.x(), tile.y(), tile.width(), label.y() + label.height() - tile.y());
    }
    public Rect help() { return new Rect(width / 2 - 142, height - 22, 284, 7); }
    public Rect copyright() { return new Rect(width - 102, height - 6, 98, 6); }
    public double toLocalX(double screenX) { return screenX / scaleX; }
    public double toLocalY(double screenY) { return screenY / scaleY; }
    public double toScreenX(double localX) { return localX * scaleX; }
    public double toScreenY(double localY) { return localY * scaleY; }
}
