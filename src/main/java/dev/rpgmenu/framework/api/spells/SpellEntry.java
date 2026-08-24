package dev.rpgmenu.framework.api.spells;

import net.minecraft.resources.ResourceLocation;

public record SpellEntry(ResourceLocation id, String titleKey, String descriptionKey, ResourceLocation icon,
                         ResourceLocation school, int level, double manaCost, int cooldownTicks, int castTimeTicks) {}
