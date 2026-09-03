package dev.rpgmenu.framework.api.inventory;

import dev.rpgmenu.framework.api.registry.Prioritized;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Extensible first-stage classifier used by right-click/Controller automatic equipment routing. */
public interface QuickEquipProvider extends Prioritized {
    ResourceLocation id();

    Optional<QuickEquipKind> classify(Player player, ItemStack stack);
}
