package dev.rpgmenu.framework.api.quests;

import dev.rpgmenu.framework.api.registry.Prioritized;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import java.util.List;

public interface QuestProvider extends Prioritized {
    ResourceLocation id();
    List<QuestChapter> chapters(Player player);
}
