package dev.rpgmenu.framework.api.skills;

import net.minecraft.resources.ResourceLocation;
import java.util.List;

public record SkillNode(ResourceLocation id, String titleKey, String descriptionKey, ResourceLocation icon,
                        int level, int maxLevel, boolean unlocked, List<ResourceLocation> requirements) {
    public SkillNode { requirements = List.copyOf(requirements); }
}
