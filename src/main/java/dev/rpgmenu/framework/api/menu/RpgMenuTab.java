package dev.rpgmenu.framework.api.menu;

import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/** Immutable definition of a semantic top-level RPG menu tab. */
public final class RpgMenuTab {
    private final ResourceLocation id;
    private final String titleKey;
    private final ResourceLocation icon;
    private final int priority;
    private final String requiredMod;
    private final Predicate<TabContext> visible;
    private final Predicate<TabContext> enabled;
    private final Function<TabContext, String> disabledReason;
    private final Function<TabContext, String> tooltip;
    private final Function<TabContext, Integer> badge;
    private final TabContentFactory contentFactory;
    private final List<SubPage> subPages;

    private RpgMenuTab(Builder builder) {
        this.id = builder.id;
        this.titleKey = builder.titleKey;
        this.icon = builder.icon;
        this.priority = builder.priority;
        this.requiredMod = builder.requiredMod;
        this.visible = builder.visible;
        this.enabled = builder.enabled;
        this.disabledReason = builder.disabledReason;
        this.tooltip = builder.tooltip;
        this.badge = builder.badge;
        this.contentFactory = builder.contentFactory;
        this.subPages = builder.subPages.stream()
                .sorted(Comparator.comparingInt(SubPage::priority).reversed().thenComparing(page -> page.id().toString()))
                .toList();
    }

    public static Builder builder(ResourceLocation id, String titleKey) {
        return new Builder(id, titleKey);
    }

    public ResourceLocation id() { return id; }
    public String titleKey() { return titleKey; }
    public ResourceLocation icon() { return icon; }
    public int priority() { return priority; }
    public Optional<String> requiredMod() { return Optional.ofNullable(requiredMod); }
    public List<SubPage> subPages() { return subPages; }
    public TabContentFactory contentFactory() { return contentFactory; }

    public boolean isVisible(TabContext context) {
        return context.isModLoaded(requiredMod) && visible.test(context);
    }

    public boolean isEnabled(TabContext context) { return enabled.test(context); }
    public String disabledReason(TabContext context) { return disabledReason.apply(context); }
    public String tooltip(TabContext context) { return tooltip.apply(context); }
    public int badge(TabContext context) { return Math.max(0, badge.apply(context)); }

    /** Returns a copy with an additional provider-owned subpage. */
    public RpgMenuTab withSubPage(SubPage page) {
        Builder builder = new Builder(id, titleKey)
                .icon(icon)
                .priority(priority)
                .visibleWhen(visible)
                .enabledWhen(enabled)
                .disabledReason(disabledReason)
                .tooltip(tooltip)
                .badge(badge)
                .content(contentFactory);
        if (requiredMod != null) builder.requiredMod(requiredMod);
        subPages.forEach(builder::addSubPage);
        builder.addSubPage(page);
        return builder.build();
    }

    /** Builder deliberately exposes only safe declarative callbacks. */
    public static final class Builder {
        private final ResourceLocation id;
        private final String titleKey;
        private ResourceLocation icon;
        private int priority;
        private String requiredMod;
        private Predicate<TabContext> visible = context -> true;
        private Predicate<TabContext> enabled = context -> true;
        private Function<TabContext, String> disabledReason = context -> "";
        private Function<TabContext, String> tooltip = context -> "";
        private Function<TabContext, Integer> badge = context -> 0;
        private TabContentFactory contentFactory = TabContentFactory.marker("empty");
        private final List<SubPage> subPages = new ArrayList<>();

        private Builder(ResourceLocation id, String titleKey) {
            this.id = Objects.requireNonNull(id, "id");
            this.titleKey = Objects.requireNonNull(titleKey, "titleKey");
            this.icon = id;
        }

        public Builder icon(ResourceLocation value) { this.icon = Objects.requireNonNull(value); return this; }
        public Builder priority(int value) { this.priority = value; return this; }
        public Builder requiredMod(String value) { this.requiredMod = value; return this; }
        public Builder visibleWhen(Predicate<TabContext> value) { this.visible = Objects.requireNonNull(value); return this; }
        public Builder enabledWhen(Predicate<TabContext> value) { this.enabled = Objects.requireNonNull(value); return this; }
        public Builder disabledReason(Function<TabContext, String> value) { this.disabledReason = Objects.requireNonNull(value); return this; }
        public Builder tooltip(Function<TabContext, String> value) { this.tooltip = Objects.requireNonNull(value); return this; }
        public Builder badge(Function<TabContext, Integer> value) { this.badge = Objects.requireNonNull(value); return this; }
        public Builder content(TabContentFactory value) { this.contentFactory = Objects.requireNonNull(value); return this; }
        public Builder addSubPage(SubPage value) { this.subPages.add(Objects.requireNonNull(value)); return this; }
        public RpgMenuTab build() { return new RpgMenuTab(this); }
    }
}
