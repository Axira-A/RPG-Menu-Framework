package dev.rpgmenu.framework.api.character;

import dev.rpgmenu.framework.api.registry.Prioritized;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** Common capability query for previews; render implementations belong in client code. */
public interface CharacterPreviewProvider extends Prioritized {
    ResourceLocation id();
    boolean supports(Player player);
}
