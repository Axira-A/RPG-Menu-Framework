package dev.rpgmenu.framework.api.event;

import dev.rpgmenu.framework.api.quests.QuestProvider;
import dev.rpgmenu.framework.api.registry.ProviderRegistry;
import net.neoforged.bus.api.Event;

/** Fired after built-in optional integrations have registered their quest providers. */
public final class RegisterQuestProvidersEvent extends Event {
    private final ProviderRegistry<QuestProvider> registry;
    public RegisterQuestProvidersEvent(ProviderRegistry<QuestProvider> registry) { this.registry = registry; }
    public ProviderRegistry<QuestProvider> registry() { return registry; }
}
