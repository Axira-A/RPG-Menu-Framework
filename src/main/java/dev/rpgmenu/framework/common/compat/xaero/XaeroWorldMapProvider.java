package dev.rpgmenu.framework.common.compat.xaero;

import dev.rpgmenu.framework.api.map.EmbeddedMapProvider;
import dev.rpgmenu.framework.api.map.MapCapability;
import dev.rpgmenu.framework.api.map.MapState;
import dev.rpgmenu.framework.api.map.MapViewport;
import dev.rpgmenu.framework.client.compat.xaero.XaeroEmbeddedMapView;
import dev.rpgmenu.framework.common.config.FrameworkConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

import java.util.EnumSet;
import java.util.Set;

/** Reflection-isolated adapter: the framework jar has no hard link to an Xaero class. */
public final class XaeroWorldMapProvider implements EmbeddedMapProvider {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("xaeroworldmap", "world_map");
    private XaeroEmbeddedMapView view;
    private MapState persistedState;

    @Override public ResourceLocation id() { return ID; }
    @Override public Component displayName() { return Component.literal("Xaero's World Map"); }
    @Override public boolean isAvailable() { return ModList.get().isLoaded("xaeroworldmap"); }
    @Override public int priority() { return 1_000; }

    @Override
    public Set<MapCapability> capabilities() {
        EnumSet<MapCapability> capabilities = EnumSet.of(MapCapability.CAN_RENDER_EMBEDDED,
                MapCapability.CAN_ZOOM, MapCapability.CAN_PAN, MapCapability.CAN_CONTEXT_MENU);
        if (ModList.get().isLoaded("xaerominimap")) {
            capabilities.add(MapCapability.CAN_READ_WAYPOINTS);
            capabilities.add(MapCapability.CAN_CREATE_WAYPOINTS);
            capabilities.add(MapCapability.CAN_EDIT_WAYPOINTS);
        }
        return Set.copyOf(capabilities);
    }

    @Override
    public boolean init(MapViewport viewport) {
        if (view == null) view = new XaeroEmbeddedMapView();
        if (persistedState != null) view.restoreState(persistedState);
        return view.init(viewport);
    }

    @Override public void resize(MapViewport viewport) { if (view != null) view.resize(viewport); }
    @Override public void tick() { if (view != null) view.tick(); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (view != null) view.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public void mouseMoved(double x, double y) { if (view != null) view.mouseMoved(x, y); }
    @Override public boolean mouseClicked(double x, double y, int button) { return view != null && view.mouseClicked(x, y, button); }
    @Override public boolean mouseReleased(double x, double y, int button) { return view != null && view.mouseReleased(x, y, button); }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return view != null && view.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseScrolled(double x, double y, double sx, double sy) {
        return view != null && view.mouseScrolled(x, y, sx, sy);
    }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return view != null && view.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return view != null && view.keyReleased(keyCode, scanCode, modifiers);
    }
    @Override public boolean charTyped(char codePoint, int modifiers) {
        return view != null && view.charTyped(codePoint, modifiers);
    }
    @Override public boolean hasTextInputFocus() { return view != null && view.hasTextInputFocus(); }
    @Override public boolean handleBack() { return view != null && view.handleBack(); }
    @Override public MapState saveState() { return view == null ? persistedState : view.saveState(); }
    @Override public void restoreState(MapState state) { persistedState = state; if (view != null) view.restoreState(state); }

    @Override
    public void dispose() {
        if (view == null) return;
        if (FrameworkConfig.PRESERVE_MAP_VIEW.get()) persistedState = view.saveState();
        else persistedState = null;
        view.close();
        view = null;
    }
}
