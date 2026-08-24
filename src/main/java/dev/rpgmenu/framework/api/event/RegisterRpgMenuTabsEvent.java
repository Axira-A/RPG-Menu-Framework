package dev.rpgmenu.framework.api.event;

import dev.rpgmenu.framework.api.menu.MenuTabRegistry;
import net.neoforged.bus.api.Event;

public final class RegisterRpgMenuTabsEvent extends Event {
    private final MenuTabRegistry registry;
    public RegisterRpgMenuTabsEvent(MenuTabRegistry registry) { this.registry = registry; }
    public MenuTabRegistry registry() { return registry; }
}
