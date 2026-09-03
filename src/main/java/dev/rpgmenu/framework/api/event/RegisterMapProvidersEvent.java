package dev.rpgmenu.framework.api.event;

import dev.rpgmenu.framework.api.map.MapProvider;
import dev.rpgmenu.framework.api.registry.ProviderRegistry;
import net.neoforged.bus.api.Event;

/** Fired after built-in optional integrations have registered their map providers. */
public final class RegisterMapProvidersEvent extends Event {
    private final ProviderRegistry<MapProvider> registry;
    public RegisterMapProvidersEvent(ProviderRegistry<MapProvider> registry) { this.registry = registry; }
    public ProviderRegistry<MapProvider> registry() { return registry; }
}
