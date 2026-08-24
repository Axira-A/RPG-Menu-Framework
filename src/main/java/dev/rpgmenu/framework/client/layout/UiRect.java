package dev.rpgmenu.framework.client.layout;

public record UiRect(int x, int y, int width, int height) {
    public int right() { return x + width; }
    public int bottom() { return y + height; }
    public boolean contains(double px, double py) { return px >= x && px < right() && py >= y && py < bottom(); }
    public UiRect inset(int amount) { return new UiRect(x + amount, y + amount, Math.max(0, width - amount * 2), Math.max(0, height - amount * 2)); }
}
