package dev.rpgmenu.framework.api.menu;

import net.minecraft.resources.ResourceLocation;
import java.util.Objects;
import java.util.function.Predicate;

/** A provider-owned page nested beneath one semantic top-level tab. */
public record SubPage(
        ResourceLocation id,
        String titleKey,
        ResourceLocation icon,
        int priority,
        Predicate<TabContext> visible,
        TabContentFactory contentFactory) {
    public SubPage {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(titleKey, "titleKey");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(visible, "visible");
        Objects.requireNonNull(contentFactory, "contentFactory");
    }
}
