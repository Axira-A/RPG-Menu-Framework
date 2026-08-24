package dev.rpgmenu.framework.api.event;

import dev.rpgmenu.framework.api.inventory.ItemCategoryProvider;
import dev.rpgmenu.framework.api.registry.ProviderRegistry;
import net.neoforged.bus.api.Event;

public final class RegisterItemCategoriesEvent extends Event {
    private final ProviderRegistry<ItemCategoryProvider> registry;
    public RegisterItemCategoriesEvent(ProviderRegistry<ItemCategoryProvider> registry) { this.registry = registry; }
    public ProviderRegistry<ItemCategoryProvider> registry() { return registry; }
}
