package dev.rpgmenu.framework.api.inventory;

import net.minecraft.resources.ResourceLocation;
import java.util.Objects;

/** The opaque, source-specific portion of one aggregated entry. */
public record SourceContribution(ResourceLocation sourceId, String opaqueKey, long amount) {
    public SourceContribution {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(opaqueKey, "opaqueKey");
        if (opaqueKey.length() > 512) throw new IllegalArgumentException("opaqueKey is too long");
        if (amount < 0) throw new IllegalArgumentException("amount cannot be negative");
    }
}
