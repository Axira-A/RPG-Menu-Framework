package dev.rpgmenu.framework.api.inventory;

/** Built-in stable sort modes. Providers may map unsupported modes to DEFAULT. */
public enum InventorySort {
    DEFAULT,
    NAME,
    QUANTITY,
    RARITY,
    MOD_ID,
    RECENT
}
