package dev.rpgmenu.framework.api.menu;

import java.util.Collection;

/** Optional bulk registration surface for integrations that contribute tabs. */
@FunctionalInterface
public interface MenuTabProvider {
    Collection<RpgMenuTab> tabs();
}
