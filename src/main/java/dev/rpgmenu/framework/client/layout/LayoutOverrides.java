package dev.rpgmenu.framework.client.layout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.rpgmenu.framework.RpgMenuFramework;
import net.neoforged.fml.loading.FMLPaths;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Runtime layout.json deltas authored by the in-game editor. */
public final class LayoutOverrides {
    public static final LayoutOverrides INSTANCE = new LayoutOverrides();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve(RpgMenuFramework.MOD_ID).resolve("layout.json").normalize();
    private final Map<String, Delta> values = new HashMap<>();

    private LayoutOverrides() { load(); }

    public ResponsiveLayout apply(ResponsiveLayout layout) {
        return new ResponsiveLayout(layout.profile(), layout.frame(), apply("TopTabs", layout.topTabs()),
                apply("InventoryPanel", layout.leftPanel()), apply("CharacterPanel", layout.rightPanel()),
                apply("SubTabs", layout.subTabs()), apply("SearchBar", layout.search()),
                apply("InventoryGrid", layout.grid()), apply("ItemDetail", layout.details()),
                apply("CharacterPreview", layout.character()), apply("StatusPanel", layout.status()),
                apply("FooterHints", layout.footer()));
    }

    public UiRect apply(String name, UiRect rect) {
        Delta d = values.get(name);
        if (d == null) return rect;
        return new UiRect(rect.x() + d.x, rect.y() + d.y, Math.max(16, rect.width() + d.width), Math.max(16, rect.height() + d.height));
    }

    public Delta get(String name) { return values.getOrDefault(name, new Delta(0, 0, 0, 0)); }
    public void set(String name, Delta delta) { values.put(name, delta.clamped()); }
    public void reset() { values.clear(); save(); }

    public void load() {
        values.clear();
        try {
            if (!Files.isRegularFile(FILE) || Files.size(FILE) > 1_048_576) return;
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                Map<String, Delta> read = GSON.fromJson(reader, new TypeToken<Map<String, Delta>>() {}.getType());
                if (read != null) read.forEach((key, value) -> {
                    if (key.matches("[A-Za-z0-9_]{1,48}") && value != null) values.put(key, value.clamped());
                });
            }
        } catch (Exception exception) {
            RpgMenuFramework.LOGGER.warn("Could not load layout.json; defaults remain active", exception);
        }
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) { GSON.toJson(values, writer); }
        } catch (Exception exception) {
            RpgMenuFramework.LOGGER.warn("Could not save layout.json", exception);
        }
    }

    public record Delta(int x, int y, int width, int height) {
        private Delta clamped() {
            return new Delta(clamp(x), clamp(y), clamp(width), clamp(height));
        }
        private static int clamp(int value) { return Math.max(-2_048, Math.min(2_048, value)); }
    }
}
