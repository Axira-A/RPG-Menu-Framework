package dev.rpgmenu.framework.api.theme;

import net.minecraft.resources.ResourceLocation;
import java.util.Map;

public record TextureDefinition(Map<String, ResourceLocation> textures) {
    public TextureDefinition { textures = Map.copyOf(textures); }
}
