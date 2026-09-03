package dev.rpgmenu.framework.api.event;

import dev.rpgmenu.framework.api.registry.ProviderRegistry;
import dev.rpgmenu.framework.api.skills.SkillProvider;
import net.neoforged.bus.api.Event;

/** Fired after built-in optional integrations have registered their skill providers. */
public final class RegisterSkillProvidersEvent extends Event {
    private final ProviderRegistry<SkillProvider> registry;
    public RegisterSkillProvidersEvent(ProviderRegistry<SkillProvider> registry) { this.registry = registry; }
    public ProviderRegistry<SkillProvider> registry() { return registry; }
}
