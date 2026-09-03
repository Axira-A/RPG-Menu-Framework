package dev.rpgmenu.framework.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FrameworkConfig {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec.BooleanValue REPLACE_VANILLA_INVENTORY;
    public static final ModConfigSpec.BooleanValue ENABLE_ANIMATIONS;
    public static final ModConfigSpec.IntValue SEARCH_DEBOUNCE_TICKS;
    public static final ModConfigSpec.ConfigValue<String> ACTIVE_THEME;
    public static final ModConfigSpec.ConfigValue<String> PREFERRED_MAP_PROVIDER;
    public static final ModConfigSpec.BooleanValue PRESERVE_MAP_VIEW;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("menu");
        REPLACE_VANILLA_INVENTORY = builder.comment("Open the RPG menu instead of the vanilla inventory screen.")
                .define("replaceVanillaInventory", true);
        ENABLE_ANIMATIONS = builder.comment("Enable subtle menu transitions.").define("enableAnimations", true);
        SEARCH_DEBOUNCE_TICKS = builder.comment("Delay before a server inventory query is sent.")
                .defineInRange("searchDebounceTicks", 6, 2, 40);
        ACTIVE_THEME = builder.comment("Theme directory name; path separators are rejected.")
                .define("activeTheme", "dark_fantasy");
        PREFERRED_MAP_PROVIDER = builder.comment("Preferred embedded map provider id or mod id.")
                .define("preferredMapProvider", "xaeroworldmap");
        PRESERVE_MAP_VIEW = builder.comment("Keep map center and zoom when the RPG menu is reopened.")
                .define("preserveMapView", true);
        builder.pop();
        CLIENT_SPEC = builder.build();
    }

    private FrameworkConfig() {}
}
