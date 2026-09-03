package dev.rpgmenu.framework.common.compat;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.RpgMenuApi;
import net.neoforged.fml.ModList;

/**
 * Loads optional integrations only after their owner mod has been confirmed present.  Class names are
 * intentionally strings: a missing optional jar can never cause the framework itself to link its classes.
 */
public final class CompatBootstrap {
    private static final String CURIOS = "curios";
    private static final String FTB_QUESTS = "ftbquests";
    private static final String IRONS_SPELLBOOKS = "irons_spellbooks";
    private static final String XAERO_WORLD_MAP = "xaeroworldmap";
    private static final String JOURNEYMAP = "journeymap";
    private static final String EPIC_SKILLS = "epicskills";
    private static final String EPIC_FIGHT = "epicfight";
    private static final String MORE_OFFHAND_SLOTS = "moreoffhandslots";
    private static boolean bootstrapped;

    private CompatBootstrap() {}

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;
        loadIfPresent(CURIOS, "dev.rpgmenu.framework.common.compat.curios.CuriosCompat");
        loadIfPresent(FTB_QUESTS, "dev.rpgmenu.framework.common.compat.ftbquests.FtbQuestsCompat");
        loadIfPresent(IRONS_SPELLBOOKS, "dev.rpgmenu.framework.common.compat.ironsspellbooks.IronSpellsCompat");
        loadIfPresent(MORE_OFFHAND_SLOTS,
                "dev.rpgmenu.framework.common.compat.moreoffhandslots.MoreOffhandSlotsCompat");
        boolean epicSkillsLoaded = ModList.get().isLoaded(EPIC_SKILLS);
        boolean epicFightLoaded = ModList.get().isLoaded(EPIC_FIGHT);
        RpgMenuFramework.LOGGER.info("[RPGMF] epicskills={}", epicSkillsLoaded);
        RpgMenuFramework.LOGGER.info("[RPGMF] epicfight={}", epicFightLoaded);
        if (epicFightLoaded) {
            loadIfPresent(EPIC_FIGHT,
                    "dev.rpgmenu.framework.common.compat.epicfight.EpicFightQuickEquipCompat");
        }
        if (epicSkillsLoaded && epicFightLoaded) {
            loadIfPresent(EPIC_SKILLS, "dev.rpgmenu.framework.common.compat.epicskills.EpicSkillsCompat");
        } else if (epicSkillsLoaded) {
            RpgMenuFramework.LOGGER.warn("[RPGMF] EpicSkills compat unavailable: required mod epicfight is not loaded");
        }
        boolean xaeroLoaded = ModList.get().isLoaded(XAERO_WORLD_MAP);
        RpgMenuFramework.LOGGER.info("[RPGMF] xaeroworldmap={}", xaeroLoaded);
        if (xaeroLoaded) loadIfPresent(XAERO_WORLD_MAP, "dev.rpgmenu.framework.common.compat.xaero.XaeroWorldMapCompat");
        loadIfPresent(JOURNEYMAP, "dev.rpgmenu.framework.common.compat.journeymap.JourneyMapCompat");
    }

    private static void loadIfPresent(String modId, String className) {
        if (!ModList.get().isLoaded(modId)) return;
        try {
            Class.forName(className).getMethod("register").invoke(null);
            RpgMenuFramework.LOGGER.info("[RPGMF] Loaded compat: {}", modId);
        } catch (ReflectiveOperationException | LinkageError exception) {
            RpgMenuFramework.LOGGER.warn("[RPGMF] Failed to initialize optional compat {}", modId, exception);
        }
    }

    public static void logDiagnostics(RpgMenuApi api) {
        RpgMenuFramework.LOGGER.info("[RPGMF] Loaded optional mods: epicskills={}, epicfight={}, ftbquests={}, irons_spellbooks={}, xaeroworldmap={}, journeymap={}, moreoffhandslots={}",
                ModList.get().isLoaded(EPIC_SKILLS), ModList.get().isLoaded(EPIC_FIGHT),
                ModList.get().isLoaded(FTB_QUESTS), ModList.get().isLoaded(IRONS_SPELLBOOKS),
                ModList.get().isLoaded(XAERO_WORLD_MAP), ModList.get().isLoaded(JOURNEYMAP),
                ModList.get().isLoaded(MORE_OFFHAND_SLOTS));
        RpgMenuFramework.LOGGER.info("[RPGMF] Registered tabs: {}", api.tabs().all().stream()
                .map(tab -> tab.id().getPath()).toList());
        RpgMenuFramework.LOGGER.info("[RPGMF] Registered providers: stats={}, skills={}, spells={}, quests={}, maps={}",
                api.statProviders().values().stream().map(provider -> provider.id().toString()).toList(),
                api.skillProviders().values().stream().map(provider -> provider.id().toString()).toList(),
                api.spellProviders().values().stream().map(provider -> provider.id().toString()).toList(),
                api.questProviders().values().stream().map(provider -> provider.id().toString()).toList(),
                api.mapProviders().values().stream().map(provider -> provider.id().toString()).toList());
        RpgMenuFramework.LOGGER.info("[RPGMF] Registered equipment providers: {}", api.equipmentProviders().values().stream()
                .map(provider -> provider.id().toString()).toList());
        RpgMenuFramework.LOGGER.info("[RPGMF] Registered quick-equip providers: {}", api.quickEquipProviders().values().stream()
                .map(provider -> provider.id().toString()).toList());
    }
}
