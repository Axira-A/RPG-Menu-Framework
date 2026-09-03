package dev.rpgmenu.framework.common.compat.journeymap;

import dev.rpgmenu.framework.api.map.MapProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/** Provider placeholder for JourneyMap's official API, which currently exposes overlays but not its full map renderer. */
public final class JourneyMapProvider implements MapProvider {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("journeymap", "world_map");

    @Override public ResourceLocation id() { return ID; }
    @Override public Component displayName() { return Component.literal("JourneyMap"); }
    @Override public boolean isAvailable() { return ModList.get().isLoaded("journeymap"); }
    @Override public int priority() { return 100; }
}
