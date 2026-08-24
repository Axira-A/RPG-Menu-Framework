package dev.rpgmenu.framework.common.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Deterministic main-inventory capacity and insertion helpers. Never uses offhand, armor, carried stack or drops. */
public final class PlayerInventoryOperations {
    public static final int MAIN_SLOT_COUNT = 36;

    private PlayerInventoryOperations() {}

    public static long capacity(Inventory inventory, ItemStack template, int excludedSlot) {
        if (template.isEmpty()) return 0;
        long capacity = 0;
        int slots = Math.min(MAIN_SLOT_COUNT, inventory.getContainerSize());
        for (int slot = 0; slot < slots; slot++) {
            if (slot == excludedSlot) continue;
            ItemStack present = inventory.getItem(slot);
            if (present.isEmpty()) capacity += template.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(present, template)) {
                capacity += Math.max(0, present.getMaxStackSize() - present.getCount());
            }
        }
        return capacity;
    }

    public static long capacityAfterExtract(Inventory inventory, ItemStack template, int sourceSlot,
                                            int extractedAmount, int excludedSlot) {
        if (sourceSlot < 0 || sourceSlot >= MAIN_SLOT_COUNT || sourceSlot == excludedSlot) {
            return capacity(inventory, template, excludedSlot);
        }
        ItemStack source = inventory.getItem(sourceSlot);
        ItemStack after = source.copy();
        after.shrink(Math.min(extractedAmount, after.getCount()));
        long capacity = 0;
        int slots = Math.min(MAIN_SLOT_COUNT, inventory.getContainerSize());
        for (int slot = 0; slot < slots; slot++) {
            if (slot == excludedSlot) continue;
            ItemStack present = slot == sourceSlot ? after : inventory.getItem(slot);
            if (present.isEmpty()) capacity += template.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(present, template)) {
                capacity += Math.max(0, present.getMaxStackSize() - present.getCount());
            }
        }
        return capacity;
    }

    public static long insert(Inventory inventory, ItemStack stack, long requested, int excludedSlot, boolean simulate) {
        if (stack.isEmpty() || requested <= 0) return 0;
        int remaining = (int)Math.min(Math.min(requested, stack.getCount()), Integer.MAX_VALUE);
        int initial = remaining;
        // Commit is deliberately all-or-nothing. Coordinators can therefore restore an equipment slot without
        // having to find and remove a partially inserted copy of its previous contents.
        if (!simulate && capacity(inventory, stack, excludedSlot) < initial) return 0;
        int slots = Math.min(MAIN_SLOT_COUNT, inventory.getContainerSize());

        for (int slot = 0; slot < slots && remaining > 0; slot++) {
            if (slot == excludedSlot) continue;
            ItemStack present = inventory.getItem(slot);
            if (!present.isEmpty() && ItemStack.isSameItemSameComponents(present, stack)) {
                int accepted = Math.min(remaining, Math.max(0, present.getMaxStackSize() - present.getCount()));
                if (!simulate && accepted > 0) present.grow(accepted);
                remaining -= accepted;
            }
        }
        for (int slot = 0; slot < slots && remaining > 0; slot++) {
            if (slot == excludedSlot || !inventory.getItem(slot).isEmpty()) continue;
            int accepted = Math.min(remaining, stack.getMaxStackSize());
            if (!simulate) inventory.setItem(slot, stack.copyWithCount(accepted));
            remaining -= accepted;
        }
        if (!simulate && remaining != initial) inventory.setChanged();
        return initial - remaining;
    }
}
