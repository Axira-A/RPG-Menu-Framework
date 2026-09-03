package dev.rpgmenu.framework.api.event;

import dev.rpgmenu.framework.api.registry.ProviderRegistry;
import dev.rpgmenu.framework.api.spells.SpellProvider;
import net.neoforged.bus.api.Event;

/** Fired after built-in optional integrations have registered their spell providers. */
public final class RegisterSpellProvidersEvent extends Event {
    private final ProviderRegistry<SpellProvider> registry;
    public RegisterSpellProvidersEvent(ProviderRegistry<SpellProvider> registry) { this.registry = registry; }
    public ProviderRegistry<SpellProvider> registry() { return registry; }
}
