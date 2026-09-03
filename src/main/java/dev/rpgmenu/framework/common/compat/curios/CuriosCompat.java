package dev.rpgmenu.framework.common.compat.curios;

import dev.rpgmenu.framework.api.RpgMenuApi;

/** Loaded reflectively only when Curios is installed. */
public final class CuriosCompat {
    private CuriosCompat() {}

    public static void register() {
        RpgMenuApi.get().equipmentProviders().register(CuriosEquipmentProvider.ID, new CuriosEquipmentProvider());
    }
}
