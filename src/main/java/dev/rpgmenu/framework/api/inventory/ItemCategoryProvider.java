package dev.rpgmenu.framework.api.inventory;

import dev.rpgmenu.framework.api.registry.Prioritized;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.Set;

/** Dynamic item category contributed by tags, predicates, or external providers. */
public interface ItemCategoryProvider extends Prioritized {
    ResourceLocation id();

    String titleKey();

    default ResourceLocation icon() { return id(); }

    boolean matches(ItemStack stack);

    default boolean fallback() { return false; }

    default Set<String> searchAliases() { return Set.of(); }
}
