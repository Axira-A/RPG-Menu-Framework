package dev.rpgmenu.framework.common.compat.ironsspellbooks;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.RpgMenuApi;
import dev.rpgmenu.framework.api.menu.RpgMenuTab;
import dev.rpgmenu.framework.api.menu.TabContentFactory;

/** Loaded reflectively only when Iron's Spells 'n Spellbooks is installed. */
public final class IronSpellsCompat {
    public static final String MOD_ID = "irons_spellbooks";

    private IronSpellsCompat() {}

    public static void register() {
        RpgMenuApi api = RpgMenuApi.get();
        api.statProviders().register(IronSpellsStatProvider.ID, new IronSpellsStatProvider());
        api.spellProviders().register(IronSpellsSpellProvider.ID, new IronSpellsSpellProvider());
        api.tabs().registerTab(RpgMenuTab.builder(RpgMenuFramework.id("spells"), "tab.rpgmenuframework.spells")
                // MenuTabRegistry sorts descending; keep the semantic order Inventory, Attributes, Spells, Quests.
                .priority(800)
                .requiredMod(MOD_ID)
                .content(TabContentFactory.marker("spells"))
                .build());
        RpgMenuFramework.LOGGER.info("[RPGMF] Registered tab: {}", RpgMenuFramework.id("spells"));
        RpgMenuFramework.LOGGER.info("[RPGMF] Registered stat provider: {}", IronSpellsStatProvider.ID);
    }
}
