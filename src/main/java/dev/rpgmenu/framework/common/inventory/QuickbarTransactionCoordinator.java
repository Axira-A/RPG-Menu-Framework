package dev.rpgmenu.framework.common.inventory;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.RpgMenuApi;
import dev.rpgmenu.framework.api.equipment.EquipmentAction;
import dev.rpgmenu.framework.api.equipment.EquipmentChangeResult;
import dev.rpgmenu.framework.api.equipment.EquipmentProvider;
import dev.rpgmenu.framework.api.equipment.EquipmentTarget;
import dev.rpgmenu.framework.api.equipment.EquipmentTransaction;
import dev.rpgmenu.framework.api.inventory.InventorySource;
import dev.rpgmenu.framework.api.inventory.QuickSlotTarget;
import dev.rpgmenu.framework.api.inventory.QuickbarAction;
import dev.rpgmenu.framework.api.inventory.SourceContribution;
import dev.rpgmenu.framework.api.inventory.TransactionResult;
import dev.rpgmenu.framework.common.equipment.EquipmentTransactionCoordinator;
import dev.rpgmenu.framework.common.equipment.HotbarEquipmentProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Atomic operations for real hotbar and offhand provider slots. Every wire target is resolved again on the
 * logical server; an unknown group/index is rejected and can never fall back to the selected main hand.
 */
public final class QuickbarTransactionCoordinator {
    public static final QuickbarTransactionCoordinator INSTANCE = new QuickbarTransactionCoordinator();

    private QuickbarTransactionCoordinator() {}

    public TransactionResult execute(ServerPlayer player, java.util.UUID sessionId, long entryOpaqueId, long nonce,
                                     QuickbarAction action, QuickSlotTarget source, QuickSlotTarget target,
                                     MenuSessionManager.SessionAccess access) {
        var replay = MenuSessionManager.INSTANCE.replay(player, sessionId, nonce);
        if (replay.isPresent()) return replay.get();
        TransactionResult result = switch (action) {
            case PLACE_ENTRY -> place(player, sessionId, entryOpaqueId, nonce, target, access);
            case SWAP_SLOTS -> swap(player, source, target);
            case MOVE_TO_INVENTORY -> moveToInventory(player, sessionId, nonce, source);
        };
        if (result.status() == TransactionResult.Status.SUCCESS) {
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
            if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        }
        MenuSessionManager.INSTANCE.remember(player, sessionId, nonce, result);
        return result;
    }

    private TransactionResult place(ServerPlayer player, java.util.UUID sessionId, long entryOpaqueId, long nonce,
                                    QuickSlotTarget quickTarget, MenuSessionManager.SessionAccess access) {
        ResolvedTarget target = resolve(player, quickTarget);
        if (target == null) return rejectedTarget();
        if (access == null) return TransactionResult.rejected("message.rpgmenuframework.stale_session");
        ItemStack template = access.entry().displayStack();
        if (template.isEmpty()) return TransactionResult.rejected("message.rpgmenuframework.source_unavailable");
        if (!QuickEquipResolver.canPlace(player, template, quickTarget, false)) {
            return TransactionResult.rejected("message.rpgmenuframework.invalid_equipment_item");
        }
        int hotbarSlot = HotbarEquipmentProvider.resolve(target.equipmentTarget);
        if (hotbarSlot >= 0) return placeHotbar(player, hotbarSlot, access);

        EquipmentTransaction transaction = new EquipmentTransaction(sessionId, entryOpaqueId,
                target.equipmentTarget, EquipmentAction.EQUIP, nonce);
        return EquipmentTransactionCoordinator.INSTANCE.execute(player, transaction, access);
    }

    private TransactionResult swap(ServerPlayer player, QuickSlotTarget sourceQuick, QuickSlotTarget targetQuick) {
        ResolvedTarget source = resolve(player, sourceQuick);
        ResolvedTarget target = resolve(player, targetQuick);
        if (source == null || target == null) return rejectedTarget();
        if (source.equipmentTarget.equals(target.equipmentTarget)) {
            return new TransactionResult(TransactionResult.Status.SUCCESS, 0,
                    "message.rpgmenuframework.quickbar_swapped");
        }

        ItemStack sourceStack = source.provider.slot(player, source.equipmentTarget).orElseThrow().stack();
        ItemStack targetStack = target.provider.slot(player, target.equipmentTarget).orElseThrow().stack();
        if (sourceStack.isEmpty()) {
            return TransactionResult.rejected("message.rpgmenuframework.empty_equipment_slot");
        }
        if (!QuickEquipResolver.canPlace(player, sourceStack, targetQuick, false)
                || !targetStack.isEmpty() && !QuickEquipResolver.canPlace(player, targetStack, sourceQuick, false)) {
            return TransactionResult.rejected("message.rpgmenuframework.invalid_equipment_item");
        }

        EquipmentChangeResult sourcePreflight = source.provider.replace(player, source.equipmentTarget, targetStack, true);
        EquipmentChangeResult targetPreflight = target.provider.replace(player, target.equipmentTarget, sourceStack, true);
        if (!sourcePreflight.accepted()) return TransactionResult.rejected(sourcePreflight.messageKey());
        if (!targetPreflight.accepted()) return TransactionResult.rejected(targetPreflight.messageKey());
        if (!sameStack(sourcePreflight.previous(), sourceStack) || !sameStack(targetPreflight.previous(), targetStack)) {
            return TransactionResult.rejected("message.rpgmenuframework.equipment_changed");
        }

        EquipmentChangeResult sourceChanged = source.provider.replace(player, source.equipmentTarget, targetStack, false);
        if (!sourceChanged.accepted() || !sameStack(sourceChanged.previous(), sourceStack)) {
            if (sourceChanged.accepted()) {
                source.provider.rollback(player, source.equipmentTarget, targetStack, sourceChanged.previous());
            }
            return TransactionResult.rejected("message.rpgmenuframework.equipment_changed");
        }
        EquipmentChangeResult targetChanged = target.provider.replace(player, target.equipmentTarget, sourceStack, false);
        if (!targetChanged.accepted() || !sameStack(targetChanged.previous(), targetStack)) {
            if (targetChanged.accepted()) {
                target.provider.rollback(player, target.equipmentTarget, sourceStack, targetChanged.previous());
            }
            if (!source.provider.rollback(player, source.equipmentTarget, targetStack, sourceStack)) {
                RpgMenuFramework.LOGGER.error("Quick-slot swap rollback failed for {}", source.equipmentTarget);
            }
            return TransactionResult.rejected("message.rpgmenuframework.transaction_rolled_back");
        }
        return new TransactionResult(TransactionResult.Status.SUCCESS,
                (long)sourceStack.getCount() + targetStack.getCount(),
                "message.rpgmenuframework.quickbar_swapped");
    }

    private TransactionResult moveToInventory(ServerPlayer player, java.util.UUID sessionId, long nonce,
                                              QuickSlotTarget sourceQuick) {
        ResolvedTarget source = resolve(player, sourceQuick);
        if (source == null) return rejectedTarget();
        int hotbarSlot = HotbarEquipmentProvider.resolve(source.equipmentTarget);
        if (hotbarSlot >= 0) return moveHotbarToInventory(player, hotbarSlot);
        EquipmentTransaction transaction = new EquipmentTransaction(sessionId, 0, source.equipmentTarget,
                EquipmentAction.UNEQUIP, nonce);
        return EquipmentTransactionCoordinator.INSTANCE.execute(player, transaction, null);
    }

    private TransactionResult placeHotbar(ServerPlayer player, int targetSlot,
                                          MenuSessionManager.SessionAccess access) {
        ItemStack template = access.entry().displayStack();
        for (SourceContribution contribution : access.entry().sources()) {
            InventorySource source = RpgMenuApi.get().inventorySources().get(contribution.sourceId()).orElse(null);
            if (source == null || !source.available(player)) continue;
            int sourceMainSlot = source.id().equals(PlayerInventorySource.ID)
                    ? PlayerInventorySource.mainSlot(contribution.opaqueKey()) : -1;
            if (sourceMainSlot == targetSlot) continue;

            ItemStack previous = player.getInventory().getItem(targetSlot).copy();
            int desired = (int)Math.min(Math.min(contribution.amount(), template.getMaxStackSize()), Integer.MAX_VALUE);
            if (!previous.isEmpty() && ItemStack.isSameItemSameComponents(previous, template)) {
                desired = Math.min(desired, Math.max(0, previous.getMaxStackSize() - previous.getCount()));
            }
            if (desired <= 0) continue;
            ItemStack simulated = source.extract(player, contribution.opaqueKey(), desired, true);
            if (simulated.isEmpty() || !ItemStack.isSameItemSameComponents(simulated, template)) continue;
            desired = Math.min(desired, simulated.getCount());

            ReturnDestination destination = destination(player, source, contribution, previous, desired, targetSlot);
            if (!previous.isEmpty() && !ItemStack.isSameItemSameComponents(previous, template)
                    && destination == ReturnDestination.NONE) continue;

            ItemStack extracted = source.extract(player, contribution.opaqueKey(), desired, false);
            if (extracted.getCount() != desired || !ItemStack.isSameItemSameComponents(extracted, template)) {
                restoreSource(player, source, extracted, targetSlot);
                continue;
            }
            ItemStack replacement;
            if (!previous.isEmpty() && ItemStack.isSameItemSameComponents(previous, extracted)) {
                replacement = previous.copy();
                replacement.grow(extracted.getCount());
            } else {
                replacement = extracted;
            }
            player.getInventory().setItem(targetSlot, replacement);
            if (!previous.isEmpty() && !ItemStack.isSameItemSameComponents(previous, extracted)
                    && !returnPrevious(player, source, destination, previous, targetSlot)) {
                player.getInventory().setItem(targetSlot, previous);
                restoreSource(player, source, extracted, targetSlot);
                return TransactionResult.rejected("message.rpgmenuframework.transaction_rolled_back");
            }
            return new TransactionResult(TransactionResult.Status.SUCCESS, extracted.getCount(),
                    previous.isEmpty() ? "message.rpgmenuframework.equipped"
                            : "message.rpgmenuframework.replaced");
        }
        return TransactionResult.rejected("message.rpgmenuframework.inventory_full");
    }

    private TransactionResult moveHotbarToInventory(ServerPlayer player, int sourceSlot) {
        Inventory inventory = player.getInventory();
        ItemStack present = inventory.getItem(sourceSlot).copy();
        if (present.isEmpty()) return TransactionResult.rejected("message.rpgmenuframework.empty_equipment_slot");
        if (storageCapacity(inventory, present) < present.getCount()) {
            return TransactionResult.rejected("message.rpgmenuframework.inventory_full");
        }
        inventory.setItem(sourceSlot, ItemStack.EMPTY);
        long inserted = insertStorage(inventory, present, present.getCount(), false);
        if (inserted != present.getCount()) {
            inventory.setItem(sourceSlot, present);
            return TransactionResult.rejected("message.rpgmenuframework.transaction_rolled_back");
        }
        return new TransactionResult(TransactionResult.Status.SUCCESS, inserted,
                "message.rpgmenuframework.unequipped");
    }

    private ResolvedTarget resolve(ServerPlayer player, QuickSlotTarget quick) {
        if (quick == null) return null;
        EquipmentTarget target = QuickSlotTargets.equipmentTarget(quick);
        if (target == null || QuickSlotTargets.fromEquipmentTarget(target).filter(quick::equals).isEmpty()) return null;
        EquipmentProvider provider = RpgMenuApi.get().equipmentProviders().get(target.providerId()).orElse(null);
        if (provider == null || provider.slot(player, target).filter(view -> view.enabled()).isEmpty()) return null;
        return new ResolvedTarget(target, provider);
    }

    private ReturnDestination destination(ServerPlayer player, InventorySource source, SourceContribution contribution,
                                          ItemStack previous, int extractedAmount, int targetSlot) {
        if (previous.isEmpty() || ItemStack.isSameItemSameComponents(previous,
                source.extract(player, contribution.opaqueKey(), 1, true))) return ReturnDestination.NONE;
        if (!source.id().equals(PlayerInventorySource.ID) && previous.getCount() == 1
                && source.insert(player, previous, 1, true) == 1) return ReturnDestination.SOURCE;
        int sourceSlot = source.id().equals(PlayerInventorySource.ID)
                ? PlayerInventorySource.mainSlot(contribution.opaqueKey()) : -1;
        long capacity = sourceSlot >= 0
                ? PlayerInventoryOperations.capacityAfterExtract(player.getInventory(), previous,
                sourceSlot, extractedAmount, targetSlot)
                : PlayerInventoryOperations.capacity(player.getInventory(), previous, targetSlot);
        return capacity >= previous.getCount() ? ReturnDestination.PLAYER : ReturnDestination.NONE;
    }

    private boolean returnPrevious(ServerPlayer player, InventorySource source, ReturnDestination destination,
                                   ItemStack previous, int targetSlot) {
        return switch (destination) {
            case NONE -> previous.isEmpty();
            case SOURCE -> source.insert(player, previous, previous.getCount(), false) == previous.getCount();
            case PLAYER -> PlayerInventoryOperations.insert(player.getInventory(), previous,
                    previous.getCount(), targetSlot, false) == previous.getCount();
        };
    }

    private void restoreSource(ServerPlayer player, InventorySource source, ItemStack extracted, int excludedSlot) {
        if (extracted.isEmpty()) return;
        long restored = source.insert(player, extracted, extracted.getCount(), false);
        if (restored == extracted.getCount()) return;
        long playerRestored = PlayerInventoryOperations.insert(player.getInventory(), extracted,
                extracted.getCount() - restored, excludedSlot, false);
        if (restored + playerRestored != extracted.getCount()) {
            RpgMenuFramework.LOGGER.error("Quick-slot rollback could not restore {} item(s) from source {}",
                    extracted.getCount() - restored - playerRestored, source.id());
        }
    }

    private static long storageCapacity(Inventory inventory, ItemStack template) {
        if (template.isEmpty()) return 0;
        long capacity = 0;
        int limit = Math.min(PlayerInventoryOperations.MAIN_SLOT_COUNT, inventory.getContainerSize());
        for (int slot = HotbarEquipmentProvider.SLOT_COUNT; slot < limit; slot++) {
            ItemStack present = inventory.getItem(slot);
            if (present.isEmpty()) capacity += template.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(present, template)) {
                capacity += Math.max(0, present.getMaxStackSize() - present.getCount());
            }
        }
        return capacity;
    }

    private static long insertStorage(Inventory inventory, ItemStack stack, long requested, boolean simulate) {
        if (stack.isEmpty() || requested <= 0) return 0;
        int remaining = (int)Math.min(Math.min(requested, stack.getCount()), Integer.MAX_VALUE);
        int initial = remaining;
        if (!simulate && storageCapacity(inventory, stack) < initial) return 0;
        int limit = Math.min(PlayerInventoryOperations.MAIN_SLOT_COUNT, inventory.getContainerSize());
        for (int slot = HotbarEquipmentProvider.SLOT_COUNT; slot < limit && remaining > 0; slot++) {
            ItemStack present = inventory.getItem(slot);
            if (!present.isEmpty() && ItemStack.isSameItemSameComponents(present, stack)) {
                int accepted = Math.min(remaining, Math.max(0, present.getMaxStackSize() - present.getCount()));
                if (!simulate && accepted > 0) present.grow(accepted);
                remaining -= accepted;
            }
        }
        for (int slot = HotbarEquipmentProvider.SLOT_COUNT; slot < limit && remaining > 0; slot++) {
            if (!inventory.getItem(slot).isEmpty()) continue;
            int accepted = Math.min(remaining, stack.getMaxStackSize());
            if (!simulate) inventory.setItem(slot, stack.copyWithCount(accepted));
            remaining -= accepted;
        }
        return initial - remaining;
    }

    private static boolean sameStack(ItemStack left, ItemStack right) {
        return left.getCount() == right.getCount()
                && (left.isEmpty() && right.isEmpty() || ItemStack.isSameItemSameComponents(left, right));
    }

    private static TransactionResult rejectedTarget() {
        return TransactionResult.rejected("message.rpgmenuframework.invalid_equipment_target");
    }

    private enum ReturnDestination { NONE, SOURCE, PLAYER }
    private record ResolvedTarget(EquipmentTarget equipmentTarget, EquipmentProvider provider) {}
}
