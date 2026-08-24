package dev.rpgmenu.framework.api.equipment;

import dev.rpgmenu.framework.api.registry.Prioritized;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Optional;

/** Dynamic equipment surface; all mutations must be server-authoritative. */
public interface EquipmentProvider extends Prioritized {
    ResourceLocation id();
    List<EquipmentSlotView> slots(Player player);

    default Optional<EquipmentSlotView> slot(Player player, EquipmentTarget target) {
        if (!id().equals(target.providerId())) return Optional.empty();
        return slots(player).stream().filter(view -> view.target().equals(target)).findFirst();
    }

    /** Client calls are advisory for presentation; the server always calls this again before mutation. */
    default boolean canEquip(Player player, EquipmentTarget target, ItemStack stack) {
        return !stack.isEmpty() && slot(player, target).map(EquipmentSlotView::enabled).orElse(false);
    }

    default boolean canUnequip(Player player, EquipmentTarget target) {
        return slot(player, target).filter(EquipmentSlotView::enabled).map(view -> !view.stack().isEmpty()).orElse(false);
    }

    /**
     * Atomically replaces one provider-local slot and returns its previous stack. Implementations must not
     * mutate when simulate is true and must support replacing the just-written value with the returned previous stack.
     */
    default EquipmentChangeResult replace(ServerPlayer player, EquipmentTarget target, ItemStack replacement, boolean simulate) {
        return EquipmentChangeResult.rejected("message.rpgmenuframework.equipment_read_only");
    }

    /**
     * Compare-and-restore hook used only after this provider accepted a replacement in the same server tick.
     * Implementations must bypass normal unequip restrictions, but only when the slot still equals expectedCurrent.
     */
    default boolean rollback(ServerPlayer player, EquipmentTarget target, ItemStack expectedCurrent, ItemStack previous) {
        EquipmentChangeResult result = replace(player, target, previous, false);
        return result.accepted() && sameStack(result.previous(), expectedCurrent);
    }

    /** Main-inventory backing slot to exclude when an unequipped item is returned, or -1. */
    default int backingInventorySlot(Player player, EquipmentTarget target) { return -1; }

    private static boolean sameStack(ItemStack left, ItemStack right) {
        return left.getCount() == right.getCount()
                && (left.isEmpty() && right.isEmpty() || ItemStack.isSameItemSameComponents(left, right));
    }
}
