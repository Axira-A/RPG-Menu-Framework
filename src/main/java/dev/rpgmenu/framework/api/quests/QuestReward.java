package dev.rpgmenu.framework.api.quests;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public record QuestReward(ResourceLocation type, Component description, boolean claimable) {}
