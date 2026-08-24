package dev.rpgmenu.framework.api.inventory;

/** Explicit operation names carried over the protocol; no arbitrary client actions exist. */
public enum InventoryOperation {
    WITHDRAW,
    WITHDRAW_HALF,
    WITHDRAW_ONE,
    DROP,
    MOVE_TO_HOTBAR,
    DEPOSIT
}
