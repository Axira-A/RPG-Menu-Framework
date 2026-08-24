package dev.rpgmenu.framework.api.stats;

import net.minecraft.resources.ResourceLocation;
import java.util.Objects;

/** Read-only stat value unless its provider separately exposes a server mutation action. */
public record StatEntry(ResourceLocation id, String titleKey, double value, double min, double max,
                        StatDisplay display, ResourceLocation icon, String tooltipKey) {
    public StatEntry {
        Objects.requireNonNull(id);
        Objects.requireNonNull(titleKey);
        Objects.requireNonNull(display);
        Objects.requireNonNull(icon);
        tooltipKey = tooltipKey == null ? "" : tooltipKey;
    }
}
