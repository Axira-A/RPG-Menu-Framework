package dev.rpgmenu.framework.api.quests;

public record QuestProgress(long current, long required, boolean complete, boolean rewardClaimed) {}
