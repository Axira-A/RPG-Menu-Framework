package dev.rpgmenu.framework.api.inventory;

import dev.rpgmenu.framework.api.registry.Prioritized;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;

public interface ItemDetailProvider extends Prioritized {
    ResourceLocation id();
    Optional<ItemDetail> details(Player player, ItemStack stack);
}
