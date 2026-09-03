package dev.rpgmenu.framework.api.map;

import dev.rpgmenu.framework.api.registry.Prioritized;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import java.util.List;
import java.util.Set;

public interface MapProvider extends Prioritized {
    ResourceLocation id();
    default Component displayName() { return Component.literal(id().toString()); }
    default ResourceLocation icon() { return id(); }
    default boolean isAvailable() { return true; }
    default Set<MapCapability> capabilities() { return Set.of(); }
    default List<MapMarker> markers(Player player, MapViewport viewport) { return List.of(); }
    default boolean supportsEmbeddedRendering() {
        return capabilities().contains(MapCapability.CAN_RENDER_EMBEDDED);
    }
}
