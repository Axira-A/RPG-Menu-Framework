package dev.rpgmenu.framework.common.menu;

import dev.rpgmenu.framework.api.menu.MenuTabRegistry;
import dev.rpgmenu.framework.api.menu.RpgMenuTab;
import dev.rpgmenu.framework.api.menu.SubPage;
import dev.rpgmenu.framework.api.menu.TabContext;
import net.minecraft.resources.ResourceLocation;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MenuTabRegistryImpl implements MenuTabRegistry {
    private final Map<ResourceLocation, RpgMenuTab> entries = new LinkedHashMap<>();
    private volatile List<RpgMenuTab> snapshot = List.of();

    @Override
    public synchronized void registerTab(RpgMenuTab tab) {
        if (entries.containsKey(tab.id())) throw new IllegalArgumentException("Duplicate tab id: " + tab.id());
        entries.put(tab.id(), tab);
        rebuild();
    }

    @Override
    public synchronized boolean unregisterTab(ResourceLocation id) {
        boolean removed = entries.remove(id) != null;
        if (removed) rebuild();
        return removed;
    }

    @Override
    public synchronized Optional<RpgMenuTab> get(ResourceLocation id) {
        return Optional.ofNullable(entries.get(id));
    }

    @Override public List<RpgMenuTab> all() { return snapshot; }

    @Override
    public List<RpgMenuTab> visible(TabContext context) {
        return snapshot.stream().filter(tab -> tab.isVisible(context)).toList();
    }

    @Override
    public synchronized void addSubPage(ResourceLocation tabId, SubPage subPage) {
        RpgMenuTab tab = entries.get(tabId);
        if (tab == null) throw new IllegalArgumentException("Unknown semantic tab: " + tabId);
        entries.put(tabId, tab.withSubPage(subPage));
        rebuild();
    }

    private void rebuild() {
        snapshot = entries.values().stream()
                .sorted(Comparator.comparingInt(RpgMenuTab::priority).reversed().thenComparing(tab -> tab.id().toString()))
                .toList();
    }
}
