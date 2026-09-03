package dev.rpgmenu.framework.api.event;

import dev.rpgmenu.framework.api.inventory.QuickEquipProvider;
import dev.rpgmenu.framework.api.registry.ProviderRegistry;
import net.neoforged.bus.api.Event;

public final class RegisterQuickEquipProvidersEvent extends Event {
    private final ProviderRegistry<QuickEquipProvider> registry;

    public RegisterQuickEquipProvidersEvent(ProviderRegistry<QuickEquipProvider> registry) {
        this.registry = registry;
    }

    public ProviderRegistry<QuickEquipProvider> registry() {
        return registry;
    }
}
