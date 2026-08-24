package dev.rpgmenu.framework.api.stats;

import dev.rpgmenu.framework.api.registry.Prioritized;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import java.util.List;

public interface StatProvider extends Prioritized {
    ResourceLocation id();
    List<StatGroup> groups(Player player);
}
