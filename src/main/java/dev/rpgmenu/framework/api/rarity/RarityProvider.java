package dev.rpgmenu.framework.api.rarity;

import dev.rpgmenu.framework.api.registry.Prioritized;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;

public interface RarityProvider extends Prioritized {
    ResourceLocation id();
    Optional<RarityStyle> style(ItemStack stack);
}
