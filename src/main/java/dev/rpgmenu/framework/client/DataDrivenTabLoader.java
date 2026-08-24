package dev.rpgmenu.framework.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.RpgMenuApi;
import dev.rpgmenu.framework.api.menu.RpgMenuTab;
import dev.rpgmenu.framework.api.menu.SubPage;
import dev.rpgmenu.framework.api.menu.TabContentFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Loads declarative tabs from assets/&lt;namespace&gt;/rpg_menu_tabs/*.json. */
public final class DataDrivenTabLoader extends SimplePreparableReloadListener<List<RpgMenuTab>> {
    public static final DataDrivenTabLoader INSTANCE = new DataDrivenTabLoader();
    private final Set<ResourceLocation> loadedIds = new HashSet<>();
    private DataDrivenTabLoader() {}

    @Override
    protected List<RpgMenuTab> prepare(ResourceManager manager, ProfilerFiller profiler) {
        List<RpgMenuTab> result = new ArrayList<>();
        manager.listResources("rpg_menu_tabs", location -> location.getPath().endsWith(".json")).forEach((location, resource) -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                String defaultPath = location.getPath().substring("rpg_menu_tabs/".length(), location.getPath().length() - 5);
                ResourceLocation id = ResourceLocation.parse(GsonHelper.getAsString(json, "id", location.getNamespace() + ":" + defaultPath));
                String title = GsonHelper.getAsString(json, "title");
                ResourceLocation icon = ResourceLocation.parse(GsonHelper.getAsString(json, "icon", id.toString()));
                String requiredMod = GsonHelper.getAsString(json, "required_mod", "");
                String action = GsonHelper.getAsString(json, "action", "placeholder");
                if (!Set.of("placeholder", "inventory", "attributes").contains(action)) {
                    throw new IllegalArgumentException("Unregistered safe action type: " + action);
                }
                RpgMenuTab.Builder builder = RpgMenuTab.builder(id, title).icon(icon)
                        .priority(GsonHelper.getAsInt(json, "priority", 0)).content(TabContentFactory.marker(action));
                if (!requiredMod.isBlank()) builder.requiredMod(requiredMod);
                if (json.has("subpages")) {
                    JsonArray pages = GsonHelper.getAsJsonArray(json, "subpages");
                    for (int index = 0; index < pages.size(); index++) {
                        JsonObject page = pages.get(index).getAsJsonObject();
                        ResourceLocation pageId = ResourceLocation.parse(GsonHelper.getAsString(page, "id"));
                        builder.addSubPage(new SubPage(pageId, GsonHelper.getAsString(page, "title"),
                                ResourceLocation.parse(GsonHelper.getAsString(page, "icon", pageId.toString())),
                                GsonHelper.getAsInt(page, "priority", 0), context -> true,
                                TabContentFactory.marker(GsonHelper.getAsString(page, "action", "placeholder"))));
                    }
                }
                result.add(builder.build());
            } catch (Exception exception) {
                RpgMenuFramework.LOGGER.warn("Ignoring invalid data-driven tab {}", location, exception);
            }
        });
        return result;
    }

    @Override
    protected void apply(List<RpgMenuTab> prepared, ResourceManager manager, ProfilerFiller profiler) {
        loadedIds.forEach(RpgMenuApi.get().tabs()::unregisterTab);
        loadedIds.clear();
        for (RpgMenuTab tab : prepared) {
            try {
                RpgMenuApi.get().tabs().registerTab(tab);
                loadedIds.add(tab.id());
            } catch (IllegalArgumentException duplicate) {
                RpgMenuFramework.LOGGER.warn("Data-driven tab {} conflicts with a registered Java tab; keeping the Java tab", tab.id());
            }
        }
    }
}
