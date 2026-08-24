package dev.rpgmenu.framework.api.event;

import dev.rpgmenu.framework.api.inventory.InventorySource;
import dev.rpgmenu.framework.api.registry.ProviderRegistry;
import net.neoforged.bus.api.Event;

public final class RegisterInventorySourcesEvent extends Event {
    private final ProviderRegistry<InventorySource> registry;
    public RegisterInventorySourcesEvent(ProviderRegistry<InventorySource> registry) { this.registry = registry; }
    public ProviderRegistry<InventorySource> registry() { return registry; }
}
