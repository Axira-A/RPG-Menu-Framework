package dev.rpgmenu.framework.api.event;

import dev.rpgmenu.framework.api.registry.ProviderRegistry;
import dev.rpgmenu.framework.api.stats.StatProvider;
import net.neoforged.bus.api.Event;

public final class RegisterStatProvidersEvent extends Event {
    private final ProviderRegistry<StatProvider> registry;
    public RegisterStatProvidersEvent(ProviderRegistry<StatProvider> registry) { this.registry = registry; }
    public ProviderRegistry<StatProvider> registry() { return registry; }
}
