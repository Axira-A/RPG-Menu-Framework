package dev.rpgmenu.framework.api.theme;

import net.minecraft.resources.ResourceLocation;
import java.util.Map;

public record ThemeDefinition(ResourceLocation id, LayoutDefinition layout, FontDefinition font,
                              SoundDefinition sounds, TextureDefinition textures, Map<String, WidgetStyle> widgets) {
    public ThemeDefinition { widgets = Map.copyOf(widgets); }
}
