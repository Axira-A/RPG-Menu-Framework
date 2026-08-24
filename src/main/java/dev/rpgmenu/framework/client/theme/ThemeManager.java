package dev.rpgmenu.framework.client.theme;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.common.config.FrameworkConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/** Reload-safe theme loader. Invalid names/data fall back to the bundled original theme. */
public final class ThemeManager extends SimplePreparableReloadListener<ThemeDefinition> {
    public static final ThemeManager INSTANCE = new ThemeManager();
    private static final Pattern SAFE_NAME = Pattern.compile("[a-z0-9_-]{1,64}");
    private volatile ThemeDefinition current = ThemeDefinition.darkFantasy();

    private ThemeManager() {}
    public ThemeDefinition current() { return current; }

    @Override
    protected ThemeDefinition prepare(ResourceManager manager, ProfilerFiller profiler) {
        String configured = FrameworkConfig.ACTIVE_THEME.get().toLowerCase(Locale.ROOT);
        if (!SAFE_NAME.matcher(configured).matches()) {
            RpgMenuFramework.LOGGER.warn("Rejected unsafe theme name: {}", configured);
            return ThemeDefinition.darkFantasy();
        }
        ResourceLocation location = RpgMenuFramework.id("themes/" + configured + "/theme.json");
        try {
            var resource = manager.getResource(location);
            if (resource.isEmpty()) return ThemeDefinition.darkFantasy();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                return ThemeDefinition.fromJson(json);
            }
        } catch (Exception exception) {
            RpgMenuFramework.LOGGER.warn("Could not load theme {}; using dark_fantasy", configured, exception);
            return ThemeDefinition.darkFantasy();
        }
    }

    @Override
    protected void apply(ThemeDefinition prepared, ResourceManager manager, ProfilerFiller profiler) {
        current = prepared;
    }
}
