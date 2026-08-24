package dev.rpgmenu.framework.api.menu;

import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.Optional;

/** Mutable registry for semantic top-level tabs and provider-owned subpages. */
public interface MenuTabRegistry {
    void registerTab(RpgMenuTab tab);

    boolean unregisterTab(ResourceLocation id);

    Optional<RpgMenuTab> get(ResourceLocation id);

    List<RpgMenuTab> all();

    List<RpgMenuTab> visible(TabContext context);

    void addSubPage(ResourceLocation tabId, SubPage subPage);
}
