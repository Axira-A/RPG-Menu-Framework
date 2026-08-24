package dev.rpgmenu.framework.api.inventory;

import net.minecraft.world.item.ItemStack;
import java.util.Objects;

/** Hash key that includes the complete data component patch and deliberately ignores count. */
public final class ItemIdentity {
    private final ItemStack stack;
    private final int hash;

    private ItemIdentity(ItemStack stack) {
        if (stack.isEmpty()) throw new IllegalArgumentException("empty stack has no identity");
        this.stack = stack.copyWithCount(1);
        this.hash = ItemStack.hashItemAndComponents(this.stack);
    }

    public static ItemIdentity of(ItemStack stack) { return new ItemIdentity(stack); }
    public ItemStack stack() { return stack.copy(); }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof ItemIdentity identity
                && ItemStack.isSameItemSameComponents(stack, identity.stack);
    }

    @Override public int hashCode() { return hash; }
    @Override public String toString() { return Objects.toString(stack); }
}
