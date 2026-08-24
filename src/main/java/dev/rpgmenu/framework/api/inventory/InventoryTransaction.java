package dev.rpgmenu.framework.api.inventory;

import java.util.UUID;

/** Client request metadata. Item identity and source are resolved from the server session. */
public record InventoryTransaction(UUID sessionId, long entryOpaqueId, long requestedAmount, InventoryOperation operation, long nonce) {
    public InventoryTransaction {
        if (requestedAmount <= 0) throw new IllegalArgumentException("requestedAmount must be positive");
    }
}
