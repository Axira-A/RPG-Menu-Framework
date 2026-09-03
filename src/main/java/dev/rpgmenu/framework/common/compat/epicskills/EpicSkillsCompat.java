package dev.rpgmenu.framework.common.compat.epicskills;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.RpgMenuApi;

/** Loaded reflectively only when both Epic Fight and Epic Fight: Skill Tree are installed. */
public final class EpicSkillsCompat {
    private EpicSkillsCompat() {}

    public static void register() {
        EpicSkillsSkillProvider provider = new EpicSkillsSkillProvider();
        RpgMenuApi.get().skillProviders().register(provider.id(), provider);
        RpgMenuFramework.LOGGER.info("[RPGMF] EpicSkills compat bootstrap: success");
        RpgMenuFramework.LOGGER.info("[RPGMF] SkillProvider registered: epicskills");
        RpgMenuFramework.LOGGER.info("[RPGMF] Tab activated: {}", RpgMenuFramework.id("skills"));
    }
}
