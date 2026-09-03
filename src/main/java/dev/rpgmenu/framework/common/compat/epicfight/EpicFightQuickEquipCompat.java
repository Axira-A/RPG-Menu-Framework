package dev.rpgmenu.framework.common.compat.epicfight;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.RpgMenuApi;

public final class EpicFightQuickEquipCompat {
    private EpicFightQuickEquipCompat() {}

    public static void register() {
        EpicFightQuickEquipProvider provider = new EpicFightQuickEquipProvider();
        RpgMenuApi.get().quickEquipProviders().register(provider.id(), provider);
        RpgMenuFramework.LOGGER.info("[RPGMF] QuickEquipProvider registered: epicfight");
    }
}
