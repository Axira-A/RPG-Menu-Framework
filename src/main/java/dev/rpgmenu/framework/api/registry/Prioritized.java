package dev.rpgmenu.framework.api.registry;

/** A registry entry whose higher priority is evaluated first. */
public interface Prioritized {
    default int priority() {
        return 0;
    }
}
