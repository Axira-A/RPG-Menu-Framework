package dev.rpgmenu.framework.api.spells;

import net.minecraft.resources.ResourceLocation;

public record SpellSchool(ResourceLocation id, String titleKey, int color) {}
