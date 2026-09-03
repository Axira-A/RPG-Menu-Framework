package dev.rpgmenu.framework.common.compat.xaero;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.RpgMenuApi;

/** Loaded by name only when the exact Xaero World Map mod id is present. */
public final class XaeroWorldMapCompat {
    private XaeroWorldMapCompat() {}

    public static void register() {
        XaeroWorldMapProvider provider = new XaeroWorldMapProvider();
        RpgMenuApi.get().mapProviders().register(provider.id(), provider);
        RpgMenuFramework.LOGGER.info("[RPGMF] Registered MapProvider: XaeroWorldMapProvider");
    }
}
