package dev.rpgmenu.framework.api.spells;

import dev.rpgmenu.framework.api.registry.Prioritized;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import java.util.List;

public interface SpellProvider extends Prioritized {
    ResourceLocation id();
    List<SpellSchool> schools(Player player);
    List<SpellEntry> spells(Player player);
}
