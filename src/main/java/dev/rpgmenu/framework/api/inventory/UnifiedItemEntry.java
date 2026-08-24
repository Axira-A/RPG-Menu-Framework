package dev.rpgmenu.framework.api.inventory;

import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Objects;

/** One display entry, aggregated only when item and all data components are equal. */
public record UnifiedItemEntry(ItemStack displayStack, long amount, List<SourceContribution> sources, long acquiredOrder) {
    public UnifiedItemEntry {
        Objects.requireNonNull(displayStack, "displayStack");
        if (displayStack.isEmpty()) throw new IllegalArgumentException("displayStack cannot be empty");
        if (amount < 0) throw new IllegalArgumentException("amount cannot be negative");
        displayStack = displayStack.copyWithCount(1);
        sources = List.copyOf(sources);
    }
}
