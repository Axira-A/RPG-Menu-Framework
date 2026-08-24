package dev.rpgmenu.framework.api.quests;

import net.minecraft.resources.ResourceLocation;
import java.util.List;

public record QuestEntry(ResourceLocation id, String titleKey, String descriptionKey, ResourceLocation icon,
                         QuestProgress progress, List<ResourceLocation> dependencies, List<String> rewardDescriptions) {
    public QuestEntry { dependencies = List.copyOf(dependencies); rewardDescriptions = List.copyOf(rewardDescriptions); }
}
