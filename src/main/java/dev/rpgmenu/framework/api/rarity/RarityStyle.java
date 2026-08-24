package dev.rpgmenu.framework.api.rarity;

import net.minecraft.resources.ResourceLocation;

public record RarityStyle(ResourceLocation id, String titleKey, int textColor, int backgroundColor, int borderColor) {}
