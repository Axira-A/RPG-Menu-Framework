package dev.rpgmenu.framework.api.quests;

import net.minecraft.resources.ResourceLocation;

public record QuestDependency(ResourceLocation questId, boolean satisfied) {}
