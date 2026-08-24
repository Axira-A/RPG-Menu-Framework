package dev.rpgmenu.framework.common.inventory;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.RpgMenuApi;
import dev.rpgmenu.framework.api.inventory.InventoryOperation;
import dev.rpgmenu.framework.api.inventory.InventorySource;
import dev.rpgmenu.framework.api.inventory.SourceContribution;
import dev.rpgmenu.framework.api.inventory.TransactionResult;
import dev.rpgmenu.framework.api.inventory.UnifiedItemEntry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;

/** Main-thread, rollback-aware coordinator for external-storage withdrawals. */
public final class InventoryTransactionCoordinator {
    public static final long MAX_AMOUNT_PER_ACTION = 4_096;
    public static final InventoryTransactionCoordinator INSTANCE = new InventoryTransactionCoordinator();

    private InventoryTransactionCoordinator() {}

    public TransactionResult execute(ServerPlayer player, MenuSessionManager.SessionAccess access,
                                     InventoryOperation operation, long requestedAmount, long nonce) {
        Optional<TransactionResult> replay = access.replay(nonce);
        if (replay.isPresent()) return replay.get();
        if (requestedAmount <= 0 || requestedAmount > MAX_AMOUNT_PER_ACTION) {
            return remember(access, nonce, TransactionResult.rejected("message.rpgmenuframework.invalid_amount"));
        }
        if (operation == InventoryOperation.DEPOSIT) {
            return remember(access, nonce, TransactionResult.rejected("message.rpgmenuframework.deposit_requires_target"));
        }

        UnifiedItemEntry entry = access.entry();
        long desired = switch (operation) {
            case WITHDRAW_ONE -> 1;
            case WITHDRAW_HALF -> Math.max(1, Math.min(requestedAmount, (entry.amount() + 1) / 2));
            default -> Math.min(requestedAmount, entry.amount());
        };
        if (operation != InventoryOperation.DROP) desired = Math.min(desired, insertionCapacity(player.getInventory(), entry.displayStack()));
        if (desired <= 0) return remember(access, nonce, TransactionResult.rejected("message.rpgmenuframework.inventory_full"));

        long moved = 0;
        for (SourceContribution contribution : entry.sources()) {
            if (moved >= desired) break;
            InventorySource source = RpgMenuApi.get().inventorySources().get(contribution.sourceId()).orElse(null);
            if (source == null || source.id().equals(PlayerInventorySource.ID)) continue;
            long ask = Math.min(Math.min(desired - moved, contribution.amount()), Integer.MAX_VALUE);
            ItemStack simulated = source.extract(player, contribution.opaqueKey(), ask, true);
            if (simulated.isEmpty() || !ItemStack.isSameItemSameComponents(simulated, entry.displayStack())) continue;
            ask = Math.min(ask, simulated.getCount());
            ItemStack extracted = source.extract(player, contribution.opaqueKey(), ask, false);
            if (extracted.isEmpty() || !ItemStack.isSameItemSameComponents(extracted, entry.displayStack())) {
                RpgMenuFramework.LOGGER.warn("Inventory source {} returned a mismatched extraction", source.id());
                continue;
            }
            int extractedCount = extracted.getCount();
            if (operation == InventoryOperation.DROP) {
                player.drop(extracted, false);
                moved += extractedCount;
                continue;
            }
            player.getInventory().add(extracted);
            int inserted = extractedCount - extracted.getCount();
            moved += inserted;
            if (!extracted.isEmpty()) {
                long rolledBack = source.insert(player, extracted, extracted.getCount(), false);
                if (rolledBack < extracted.getCount()) {
                    ItemStack safeRemainder = extracted.copyWithCount((int)(extracted.getCount() - rolledBack));
                    player.drop(safeRemainder, false);
                    RpgMenuFramework.LOGGER.error("Source {} could not fully roll back {}; remainder was dropped at the player", source.id(), extracted.getCount());
                }
            }
        }
        TransactionResult result = moved == 0
                ? new TransactionResult(TransactionResult.Status.UNAVAILABLE, 0, "message.rpgmenuframework.source_unavailable")
                : new TransactionResult(moved == desired ? TransactionResult.Status.SUCCESS : TransactionResult.Status.PARTIAL,
                        moved, "message.rpgmenuframework.transaction_complete");
        return remember(access, nonce, result);
    }

    private static long insertionCapacity(Inventory inventory, ItemStack template) {
        long capacity = 0;
        int mainSlots = Math.min(36, inventory.getContainerSize());
        for (int slot = 0; slot < mainSlots; slot++) {
            ItemStack present = inventory.getItem(slot);
            if (present.isEmpty()) capacity += template.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(present, template)) capacity += Math.max(0, present.getMaxStackSize() - present.getCount());
        }
        return Math.min(capacity, MAX_AMOUNT_PER_ACTION);
    }

    private static TransactionResult remember(MenuSessionManager.SessionAccess access, long nonce, TransactionResult result) {
        access.remember(nonce, result);
        return result;
    }
}
