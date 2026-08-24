package dev.rpgmenu.framework.common.equipment;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.RpgMenuApi;
import dev.rpgmenu.framework.api.equipment.EquipmentAction;
import dev.rpgmenu.framework.api.equipment.EquipmentChangeResult;
import dev.rpgmenu.framework.api.equipment.EquipmentProvider;
import dev.rpgmenu.framework.api.equipment.EquipmentTransaction;
import dev.rpgmenu.framework.api.inventory.InventorySource;
import dev.rpgmenu.framework.api.inventory.SourceContribution;
import dev.rpgmenu.framework.api.inventory.TransactionResult;
import dev.rpgmenu.framework.common.inventory.MenuSessionManager;
import dev.rpgmenu.framework.common.inventory.PlayerInventoryOperations;
import dev.rpgmenu.framework.common.inventory.PlayerInventorySource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Main-thread coordinator for authoritative equip/replace/unequip operations. No failure path drops an item. */
public final class EquipmentTransactionCoordinator {
    public static final EquipmentTransactionCoordinator INSTANCE = new EquipmentTransactionCoordinator();

    private EquipmentTransactionCoordinator() {}

    public TransactionResult execute(ServerPlayer player, EquipmentTransaction transaction,
                                     MenuSessionManager.SessionAccess entryAccess) {
        var replay = MenuSessionManager.INSTANCE.replay(player, transaction.sessionId(), transaction.nonce());
        if (replay.isPresent()) return replay.get();
        EquipmentProvider provider = RpgMenuApi.get().equipmentProviders().get(transaction.target().providerId()).orElse(null);
        if (provider == null || provider.slot(player, transaction.target()).isEmpty()) {
            return remember(player, transaction, TransactionResult.rejected("message.rpgmenuframework.invalid_equipment_target"));
        }
        TransactionResult result = transaction.action() == EquipmentAction.EQUIP
                ? equip(player, transaction, provider, entryAccess)
                : unequip(player, transaction, provider);
        if (result.status() == TransactionResult.Status.SUCCESS) {
            player.inventoryMenu.broadcastChanges();
            if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        }
        return remember(player, transaction, result);
    }

    private TransactionResult equip(ServerPlayer player, EquipmentTransaction transaction, EquipmentProvider provider,
                                    MenuSessionManager.SessionAccess access) {
        if (access == null) return TransactionResult.rejected("message.rpgmenuframework.stale_session");
        ItemStack incomingTemplate = access.entry().displayStack().copyWithCount(1);
        if (!provider.canEquip(player, transaction.target(), incomingTemplate)) {
            return TransactionResult.rejected("message.rpgmenuframework.invalid_equipment_item");
        }
        EquipmentChangeResult simulated = provider.replace(player, transaction.target(), incomingTemplate, true);
        if (!simulated.accepted()) return TransactionResult.rejected(simulated.messageKey());
        if (!simulated.previous().isEmpty()
                && ItemStack.isSameItemSameComponents(simulated.previous(), incomingTemplate)) {
            return new TransactionResult(TransactionResult.Status.SUCCESS, 0,
                    "message.rpgmenuframework.already_equipped");
        }

        ExtractionPlan plan = choosePlan(player, provider, transaction, access, simulated.previous());
        if (plan == null) return TransactionResult.rejected("message.rpgmenuframework.inventory_full");
        ItemStack extracted = plan.source.extract(player, plan.contribution.opaqueKey(), 1, false);
        if (!sameSingle(extracted, incomingTemplate)) {
            rollbackSource(player, plan.source, extracted);
            return TransactionResult.rejected("message.rpgmenuframework.source_unavailable");
        }

        EquipmentChangeResult changed = provider.replace(player, transaction.target(), extracted, false);
        if (!changed.accepted()) {
            rollbackSource(player, plan.source, extracted);
            return TransactionResult.rejected(changed.messageKey());
        }
        if (!sameStack(changed.previous(), simulated.previous())) {
            rollbackEquipmentAndSource(player, provider, transaction, plan.source, changed.previous(), extracted);
            return TransactionResult.rejected("message.rpgmenuframework.equipment_changed");
        }

        if (!changed.previous().isEmpty() && !returnPrevious(player, provider, transaction, plan, changed.previous())) {
            rollbackEquipmentAndSource(player, provider, transaction, plan.source, changed.previous(), extracted);
            return TransactionResult.rejected("message.rpgmenuframework.transaction_rolled_back");
        }
        return new TransactionResult(TransactionResult.Status.SUCCESS, 1,
                changed.previous().isEmpty() ? "message.rpgmenuframework.equipped" : "message.rpgmenuframework.replaced");
    }

    private TransactionResult unequip(ServerPlayer player, EquipmentTransaction transaction, EquipmentProvider provider) {
        // The simulated provider mutation is the authoritative preflight. Dynamic providers such as Curios
        // must construct their own current SlotContext and should not have canUnequip invoked a second time.
        EquipmentChangeResult simulated = provider.replace(player, transaction.target(), ItemStack.EMPTY, true);
        if (!simulated.accepted() || simulated.previous().isEmpty()) {
            return TransactionResult.rejected(simulated.messageKey().isBlank()
                    ? "message.rpgmenuframework.empty_equipment_slot" : simulated.messageKey());
        }
        int excluded = provider.backingInventorySlot(player, transaction.target());
        if (PlayerInventoryOperations.capacity(player.getInventory(), simulated.previous(), excluded) < simulated.previous().getCount()) {
            return TransactionResult.rejected("message.rpgmenuframework.inventory_full");
        }
        EquipmentChangeResult changed = provider.replace(player, transaction.target(), ItemStack.EMPTY, false);
        if (!changed.accepted() || !sameStack(changed.previous(), simulated.previous())) {
            if (changed.accepted()) provider.rollback(player, transaction.target(), ItemStack.EMPTY, changed.previous());
            return TransactionResult.rejected("message.rpgmenuframework.equipment_changed");
        }
        long inserted = PlayerInventoryOperations.insert(player.getInventory(), changed.previous(), changed.previous().getCount(), excluded, false);
        if (inserted != changed.previous().getCount()) {
            if (!provider.rollback(player, transaction.target(), ItemStack.EMPTY, changed.previous())) {
                RpgMenuFramework.LOGGER.error("Could not restore equipment target {} after deterministic inventory insertion failed", transaction.target());
            }
            return TransactionResult.rejected("message.rpgmenuframework.transaction_rolled_back");
        }
        return new TransactionResult(TransactionResult.Status.SUCCESS, changed.previous().getCount(),
                "message.rpgmenuframework.unequipped");
    }

    private ExtractionPlan choosePlan(ServerPlayer player, EquipmentProvider provider, EquipmentTransaction transaction,
                                      MenuSessionManager.SessionAccess access, ItemStack previous) {
        int excluded = provider.backingInventorySlot(player, transaction.target());
        for (SourceContribution contribution : access.entry().sources()) {
            InventorySource source = RpgMenuApi.get().inventorySources().get(contribution.sourceId()).orElse(null);
            if (source == null || !source.available(player)) continue;
            ItemStack simulated = source.extract(player, contribution.opaqueKey(), 1, true);
            if (!sameSingle(simulated, access.entry().displayStack())) continue;
            if (previous.isEmpty()) return new ExtractionPlan(source, contribution, ReturnDestination.NONE);

            // InventorySource#insert reports a count rather than returning a remainder. Keep the cross-source
            // exchange atomic without assuming an arbitrary third-party source can undo a partial multi-item insert.
            if (previous.getCount() == 1 && source.insert(player, previous, 1, true) == 1) {
                return new ExtractionPlan(source, contribution, ReturnDestination.SOURCE);
            }
            long capacity = source.id().equals(PlayerInventorySource.ID)
                    ? PlayerInventoryOperations.capacityAfterExtract(player.getInventory(), previous,
                            PlayerInventorySource.mainSlot(contribution.opaqueKey()), 1, excluded)
                    : PlayerInventoryOperations.capacity(player.getInventory(), previous, excluded);
            if (capacity >= previous.getCount()) return new ExtractionPlan(source, contribution, ReturnDestination.PLAYER);
        }
        return null;
    }

    private boolean returnPrevious(ServerPlayer player, EquipmentProvider provider, EquipmentTransaction transaction,
                                   ExtractionPlan plan, ItemStack previous) {
        return switch (plan.destination) {
            case NONE -> previous.isEmpty();
            case SOURCE -> plan.source.insert(player, previous, previous.getCount(), false) == previous.getCount();
            case PLAYER -> PlayerInventoryOperations.insert(player.getInventory(), previous, previous.getCount(),
                    provider.backingInventorySlot(player, transaction.target()), false) == previous.getCount();
        };
    }

    private void rollbackEquipmentAndSource(ServerPlayer player, EquipmentProvider provider, EquipmentTransaction transaction,
                                            InventorySource source, ItemStack previous, ItemStack incoming) {
        if (!provider.rollback(player, transaction.target(), incoming, previous)) {
            RpgMenuFramework.LOGGER.error("Equipment provider {} violated rollback contract for {}", provider.id(), transaction.target());
            return;
        }
        rollbackSource(player, source, incoming);
    }

    private void rollbackSource(ServerPlayer player, InventorySource source, ItemStack stack) {
        if (stack.isEmpty()) return;
        long restored = source.insert(player, stack, stack.getCount(), false);
        if (restored != stack.getCount()) {
            long playerRestored = PlayerInventoryOperations.insert(player.getInventory(), stack,
                    stack.getCount() - restored, -1, false);
            if (restored + playerRestored != stack.getCount()) {
                RpgMenuFramework.LOGGER.error("Inventory source {} violated rollback contract; {} item(s) require administrator recovery",
                        source.id(), stack.getCount() - restored - playerRestored);
            }
        }
    }

    private static boolean sameSingle(ItemStack actual, ItemStack expected) {
        return !actual.isEmpty() && actual.getCount() == 1 && ItemStack.isSameItemSameComponents(actual, expected);
    }

    private static boolean sameStack(ItemStack left, ItemStack right) {
        return left.getCount() == right.getCount()
                && (left.isEmpty() && right.isEmpty() || ItemStack.isSameItemSameComponents(left, right));
    }

    private static TransactionResult remember(ServerPlayer player, EquipmentTransaction transaction, TransactionResult result) {
        MenuSessionManager.INSTANCE.remember(player, transaction.sessionId(), transaction.nonce(), result);
        return result;
    }

    private enum ReturnDestination { NONE, SOURCE, PLAYER }
    private record ExtractionPlan(InventorySource source, SourceContribution contribution, ReturnDestination destination) {}
}
