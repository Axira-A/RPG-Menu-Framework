package dev.rpgmenu.framework.common.compat.moreoffhandslots;

import dev.rpgmenu.framework.api.RpgMenuApi;

/** Loaded reflectively only after the real {@code moreoffhandslots} mod id has been detected. */
public final class MoreOffhandSlotsCompat {
    private MoreOffhandSlotsCompat() {}

    public static void register() {
        MoreOffhandSlotsEquipmentProvider provider = new MoreOffhandSlotsEquipmentProvider();
        RpgMenuApi.get().equipmentProviders().register(provider.id(), provider);
    }
}
