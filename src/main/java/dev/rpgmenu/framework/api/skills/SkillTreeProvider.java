package dev.rpgmenu.framework.api.skills;

import dev.rpgmenu.framework.api.registry.Prioritized;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import java.util.List;

public interface SkillTreeProvider extends Prioritized {
    ResourceLocation id();
    List<SkillNode> nodes(Player player);
    List<SkillEdge> edges(Player player);
}
