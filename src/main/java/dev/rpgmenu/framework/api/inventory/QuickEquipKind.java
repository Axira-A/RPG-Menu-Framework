package dev.rpgmenu.framework.api.inventory;

/** Classification result kept separate from the destination so tool-specific item-bar rules remain possible. */
public enum QuickEquipKind {
    WEAPON(QuickSlotGroup.MAIN_HAND),
    TOOL(QuickSlotGroup.MAIN_HAND),
    SHIELD(QuickSlotGroup.OFF_HAND),
    ITEM(QuickSlotGroup.ITEM_BAR);

    private final QuickSlotGroup defaultGroup;

    QuickEquipKind(QuickSlotGroup defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    public QuickSlotGroup defaultGroup() {
        return defaultGroup;
    }
}
