package dev.rpgmenu.framework.client.sound;

/** Stable semantic keys used by screens and data-driven themes. */
public enum UiSoundCue {
    MENU_OPEN("menu_open", "rpg_menu_open", 0),
    MENU_CLOSE("menu_close", "rpg_menu_close", 0),
    TAB_SWITCH("tab_switch", "tab_switch", 0),
    SUBTAB_SWITCH("subtab_switch", "subtab_switch", 0),
    FOCUS_MOVE("focus", "focus_move", 65),
    ITEM_SELECT("select", "item_select", 0),
    EQUIP("equip", "equip", 0),
    REPLACE_EQUIPMENT("replace_equipment", "replace_equipment", 0),
    UNEQUIP("unequip", "unequip", 0),
    FAVORITE("favorite", "favorite", 0),
    CONFIRM("confirm", "confirm", 0),
    CANCEL("cancel", "cancel", 0),
    ERROR("error", "error", 120),
    MODAL_OPEN("modal_open", "modal_open", 0),
    MODAL_CLOSE("modal_close", "modal_close", 0);

    private final String themeKey;
    private final String eventPath;
    private final long cooldownMillis;

    UiSoundCue(String themeKey, String eventPath, long cooldownMillis) {
        this.themeKey = themeKey;
        this.eventPath = eventPath;
        this.cooldownMillis = cooldownMillis;
    }

    public String themeKey() { return themeKey; }
    public String eventPath() { return eventPath; }
    public long cooldownMillis() { return cooldownMillis; }
}
