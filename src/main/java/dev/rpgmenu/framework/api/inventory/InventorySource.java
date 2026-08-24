package dev.rpgmenu.framework.api.inventory;

import dev.rpgmenu.framework.api.registry.Prioritized;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side inventory source. Large sources must apply filter/sort/pagination (including an
 * optional {@link InventoryQuery#equipmentTarget()}) internally and must never return an unbounded snapshot.
 */
public interface InventorySource extends Prioritized {
    ResourceLocation id();

    default boolean available(ServerPlayer player) { return true; }

    InventoryPage query(ServerPlayer player, InventoryQuery query);

    /**
     * Extracts at most {@code amount}; the returned stack is the exact extracted identity. A source that accepts
     * extraction must accept that same stack through {@link #insert} if the coordinating transaction rolls back.
     */
    default ItemStack extract(ServerPlayer player, String opaqueKey, long amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    /** Returns the accepted amount without mutating when simulate is true. */
    default long insert(ServerPlayer player, ItemStack stack, long amount, boolean simulate) {
        return 0;
    }

    default long revision(ServerPlayer player) { return 0; }
}
