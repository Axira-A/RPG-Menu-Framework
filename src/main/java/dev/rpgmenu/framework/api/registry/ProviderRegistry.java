package dev.rpgmenu.framework.api.registry;

import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.Optional;

/** Thread-safe, ordered registry used by the common provider APIs. */
public interface ProviderRegistry<T> {
    void register(ResourceLocation id, T provider);

    boolean unregister(ResourceLocation id);

    Optional<T> get(ResourceLocation id);

    List<T> values();
}
