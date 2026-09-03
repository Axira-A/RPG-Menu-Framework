package dev.rpgmenu.framework.common.compat.epicskills;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.skills.SkillEdge;
import dev.rpgmenu.framework.api.skills.SkillNode;
import dev.rpgmenu.framework.api.skills.SkillProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.util.List;

/** Semantic provider registration; the real graph is hosted from Epic Skills' own SkillTreeScreen on the client. */
public final class EpicSkillsSkillProvider implements SkillProvider {
    public static final ResourceLocation ID = RpgMenuFramework.id("epicskills");

    @Override public ResourceLocation id() { return ID; }
    @Override public Component displayName() { return Component.literal("Epic Fight: Skill Tree"); }
    @Override public boolean isAvailable() {
        return ModList.get().isLoaded("epicskills") && ModList.get().isLoaded("epicfight");
    }
    @Override public int priority() { return 1_000; }

    // These generic snapshots deliberately stay empty: RPGMF renders the authoritative Epic Skills screen itself.
    @Override public List<SkillNode> nodes(Player player) { return List.of(); }
    @Override public List<SkillEdge> edges(Player player) { return List.of(); }
}
