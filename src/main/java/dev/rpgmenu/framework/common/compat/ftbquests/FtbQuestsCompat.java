package dev.rpgmenu.framework.common.compat.ftbquests;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.RpgMenuApi;
import dev.rpgmenu.framework.api.menu.RpgMenuTab;
import dev.rpgmenu.framework.api.menu.TabContentFactory;

/** Loaded only by {@link dev.rpgmenu.framework.common.compat.CompatBootstrap} when FTB Quests is present. */
public final class FtbQuestsCompat {
    public static final String MOD_ID = "ftbquests";

    private FtbQuestsCompat() {}

    public static void register() {
        RpgMenuApi api = RpgMenuApi.get();
        api.questProviders().register(FtbQuestProvider.ID, new FtbQuestProvider());
        api.tabs().registerTab(RpgMenuTab.builder(RpgMenuFramework.id("quests"), "tab.rpgmenuframework.quests")
                // MenuTabRegistry sorts descending; keep the semantic order Inventory, Attributes, Spells, Quests.
                .priority(700)
                .requiredMod(MOD_ID)
                .content(TabContentFactory.marker("quests"))
                .build());
        RpgMenuFramework.LOGGER.info("[RPGMF] Registered tab: {}", RpgMenuFramework.id("quests"));
    }
}
