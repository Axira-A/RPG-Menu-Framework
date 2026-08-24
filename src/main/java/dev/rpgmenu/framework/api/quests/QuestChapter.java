package dev.rpgmenu.framework.api.quests;

import net.minecraft.resources.ResourceLocation;
import java.util.List;

public record QuestChapter(ResourceLocation id, String titleKey, List<QuestEntry> quests) {
    public QuestChapter { quests = List.copyOf(quests); }
}
