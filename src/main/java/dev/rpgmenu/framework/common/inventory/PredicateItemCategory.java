package dev.rpgmenu.framework.common.inventory;

import dev.rpgmenu.framework.api.inventory.ItemCategoryProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public record PredicateItemCategory(ResourceLocation id, String titleKey, ResourceLocation icon, int priority,
                                    boolean fallback, Set<String> searchAliases, Predicate<ItemStack> predicate)
        implements ItemCategoryProvider {
    public PredicateItemCategory {
        searchAliases = Set.copyOf(searchAliases);
        Objects.requireNonNull(predicate);
    }

    @Override public boolean matches(ItemStack stack) { return predicate.test(stack); }
}
