package dev.rpgmenu.framework.api.input;

import dev.rpgmenu.framework.api.registry.Prioritized;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public interface InputGlyphProvider extends Prioritized {
    ResourceLocation id();
    Component glyph(InputAction action);
}
