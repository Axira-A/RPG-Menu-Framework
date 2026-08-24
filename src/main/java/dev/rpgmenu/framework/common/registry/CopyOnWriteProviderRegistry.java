package dev.rpgmenu.framework.common.registry;

import dev.rpgmenu.framework.api.registry.Prioritized;
import dev.rpgmenu.framework.api.registry.ProviderRegistry;
import net.minecraft.resources.ResourceLocation;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Small synchronized registry with immutable read snapshots. Registration is never done per tick. */
public final class CopyOnWriteProviderRegistry<T> implements ProviderRegistry<T> {
    private final Map<ResourceLocation, T> entries = new LinkedHashMap<>();
    private volatile List<T> snapshot = List.of();

    @Override
    public synchronized void register(ResourceLocation id, T provider) {
        if (entries.containsKey(id)) throw new IllegalArgumentException("Duplicate provider id: " + id);
        entries.put(id, provider);
        rebuild();
    }

    @Override
    public synchronized boolean unregister(ResourceLocation id) {
        boolean removed = entries.remove(id) != null;
        if (removed) rebuild();
        return removed;
    }

    @Override
    public synchronized Optional<T> get(ResourceLocation id) {
        return Optional.ofNullable(entries.get(id));
    }

    @Override
    public List<T> values() {
        return snapshot;
    }

    private void rebuild() {
        snapshot = entries.entrySet().stream()
                .sorted(Comparator.<Map.Entry<ResourceLocation, T>>comparingInt(entry -> priority(entry.getValue())).reversed()
                        .thenComparing(entry -> entry.getKey().toString()))
                .map(Map.Entry::getValue)
                .toList();
    }

    private static int priority(Object value) {
        return value instanceof Prioritized prioritized ? prioritized.priority() : 0;
    }
}
