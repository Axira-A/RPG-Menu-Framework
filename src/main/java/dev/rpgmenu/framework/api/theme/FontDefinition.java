package dev.rpgmenu.framework.api.theme;

import net.minecraft.resources.ResourceLocation;
import java.util.List;

public record FontDefinition(ResourceLocation primary, List<ResourceLocation> fallbacks) {
    public FontDefinition { fallbacks = List.copyOf(fallbacks); }
}
