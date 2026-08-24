package dev.rpgmenu.framework.api.menu;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Side-neutral visibility context supplied when menu tabs are resolved. */
public record TabContext(UUID playerId, Set<String> loadedMods, boolean controllerActive) {
    public TabContext {
        Objects.requireNonNull(playerId, "playerId");
        loadedMods = Set.copyOf(loadedMods);
    }

    public boolean isModLoaded(String modId) {
        return modId == null || modId.isBlank() || loadedMods.contains(modId);
    }
}
