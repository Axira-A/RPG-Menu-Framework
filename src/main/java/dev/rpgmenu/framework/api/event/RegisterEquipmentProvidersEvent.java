package dev.rpgmenu.framework.api.event;

import dev.rpgmenu.framework.api.equipment.EquipmentProvider;
import dev.rpgmenu.framework.api.registry.ProviderRegistry;
import net.neoforged.bus.api.Event;

/** Registration point for vanilla, Curios and future equipment surfaces. */
public final class RegisterEquipmentProvidersEvent extends Event {
    private final ProviderRegistry<EquipmentProvider> registry;

    public RegisterEquipmentProvidersEvent(ProviderRegistry<EquipmentProvider> registry) {
        this.registry = registry;
    }

    public ProviderRegistry<EquipmentProvider> registry() {
        return registry;
    }
}
