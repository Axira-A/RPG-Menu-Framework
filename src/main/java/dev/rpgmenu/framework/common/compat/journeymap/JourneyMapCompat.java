package dev.rpgmenu.framework.common.compat.journeymap;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.RpgMenuApi;

/** Registers the provider identity without linking JourneyMap client classes on common class paths. */
public final class JourneyMapCompat {
    private JourneyMapCompat() {}

    public static void register() {
        JourneyMapProvider provider = new JourneyMapProvider();
        RpgMenuApi.get().mapProviders().register(provider.id(), provider);
        RpgMenuFramework.LOGGER.info("[RPGMF] Registered MapProvider: JourneyMapProvider (public API has no embedded fullscreen renderer)");
    }
}
