package dev.rpgmenu.framework.api.map;

import net.minecraft.resources.ResourceLocation;

/** GUI-logical viewport plus the provider's current world-space camera. */
public record MapViewport(int x, int y, int width, int height, ResourceLocation dimension,
                          double centerX, double centerZ, double zoom) {
    /** Source-compatible constructor for marker-only providers written against the original API. */
    public MapViewport(ResourceLocation dimension, double centerX, double centerZ, double zoom) {
        this(0, 0, 0, 0, dimension, centerX, centerZ, zoom);
    }

    public int right() { return x + width; }
    public int bottom() { return y + height; }
    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
    }
    public double localX(double mouseX) { return mouseX - x; }
    public double localY(double mouseY) { return mouseY - y; }
}
