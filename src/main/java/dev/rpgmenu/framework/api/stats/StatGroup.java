package dev.rpgmenu.framework.api.stats;

import net.minecraft.resources.ResourceLocation;
import java.util.List;

public record StatGroup(ResourceLocation id, String titleKey, int priority, List<StatEntry> entries) {
    public StatGroup { entries = List.copyOf(entries); }
}
