package dev.rpgmenu.framework.api.map;

import net.minecraft.resources.ResourceLocation;

/** Provider-neutral state retained while changing RPG tabs or reopening the menu. */
public record MapState(ResourceLocation dimension, double centerX, double centerZ, double zoom,
                       ResourceLocation selectedWaypoint) {
    public static MapState initial(ResourceLocation dimension) {
        return new MapState(dimension, 0, 0, 1, null);
    }
}
