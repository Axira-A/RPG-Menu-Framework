package dev.rpgmenu.framework.api.map;

import dev.rpgmenu.framework.api.registry.Prioritized;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import java.util.List;

public interface MapProvider extends Prioritized {
    ResourceLocation id();
    List<MapMarker> markers(Player player, MapViewport viewport);
    default boolean supportsEmbeddedRendering() { return false; }
}
