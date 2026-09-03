package dev.rpgmenu.framework.client.map;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.RpgMenuApi;
import dev.rpgmenu.framework.api.map.EmbeddedMapProvider;
import dev.rpgmenu.framework.api.map.MapProvider;
import dev.rpgmenu.framework.api.map.MapState;
import dev.rpgmenu.framework.api.map.MapViewport;
import dev.rpgmenu.framework.api.input.InputAction;
import dev.rpgmenu.framework.client.layout.UiRect;
import dev.rpgmenu.framework.client.sound.UiSoundCue;
import dev.rpgmenu.framework.client.sound.UiSoundPlayer;
import dev.rpgmenu.framework.client.theme.ThemeDefinition;
import dev.rpgmenu.framework.common.config.FrameworkConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Owns one semantic Map tab and runs only its currently selected embedded provider. */
public final class EmbeddedMapHost {
    private static final int SELECTOR_HEIGHT = 24;
    private static ResourceLocation rememberedProviderId;
    private final Map<ResourceLocation, MapState> states = new HashMap<>();
    private final Set<ResourceLocation> initialized = new HashSet<>();
    private final Set<ResourceLocation> failed = new HashSet<>();
    private ResourceLocation activeProviderId;
    private UiRect viewport = new UiRect(0, 0, 0, 0);
    private UiRect content = new UiRect(0, 0, 0, 0);

    public boolean render(GuiGraphics graphics, UiRect target, int mouseX, int mouseY, float partialTick,
                          ThemeDefinition theme) {
        updateViewport(target);
        List<MapProvider> providers = availableProviders();
        if (providers.isEmpty()) return false;
        chooseProvider(providers);
        if (providers.size() > 1) renderSelector(graphics, providers, mouseX, mouseY, theme);

        MapProvider selected = selectedProvider(providers);
        if (!(selected instanceof EmbeddedMapProvider embedded) || !ensure(embedded)) return false;
        graphics.enableScissor(content.x(), content.y(), content.right(), content.bottom());
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(content.x(), content.y(), 0);
            embedded.render(graphics, (int) (mouseX - content.x()), (int) (mouseY - content.y()), partialTick);
        } catch (LinkageError | RuntimeException exception) {
            fail(selected, exception);
            return false;
        } finally {
            graphics.pose().popPose();
            graphics.disableScissor();
        }
        return true;
    }

    public void tick() {
        EmbeddedMapProvider provider = activeEmbedded();
        if (provider == null || !ensure(provider)) return;
        try {
            provider.tick();
        } catch (LinkageError | RuntimeException exception) {
            fail(provider, exception);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<MapProvider> providers = availableProviders();
        if (providers.size() > 1 && new UiRect(viewport.x(), viewport.y(), viewport.width(), SELECTOR_HEIGHT)
                .contains(mouseX, mouseY)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) selectAt(providers, mouseX);
            return true;
        }
        EmbeddedMapProvider provider = activeEmbedded();
        return content.contains(mouseX, mouseY) && provider != null && ensure(provider)
                && guarded(provider, () -> provider.mouseClicked(mouseX - content.x(), mouseY - content.y(), button));
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        EmbeddedMapProvider provider = activeEmbedded();
        return provider != null && ensure(provider)
                && guarded(provider, () -> provider.mouseReleased(mouseX - content.x(), mouseY - content.y(), button));
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        EmbeddedMapProvider provider = activeEmbedded();
        return provider != null && ensure(provider)
                && guarded(provider, () -> provider.mouseDragged(mouseX - content.x(), mouseY - content.y(), button, dragX, dragY));
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        EmbeddedMapProvider provider = activeEmbedded();
        return content.contains(mouseX, mouseY) && provider != null && ensure(provider)
                && guarded(provider, () -> provider.mouseScrolled(mouseX - content.x(), mouseY - content.y(), scrollX, scrollY));
    }

    public void mouseMoved(double mouseX, double mouseY) {
        EmbeddedMapProvider provider = activeEmbedded();
        if (provider == null || !ensure(provider) || !content.contains(mouseX, mouseY)) return;
        try {
            provider.mouseMoved(mouseX - content.x(), mouseY - content.y());
        } catch (LinkageError | RuntimeException exception) {
            fail(provider, exception);
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        EmbeddedMapProvider provider = activeEmbedded();
        return provider != null && ensure(provider)
                && guarded(provider, () -> provider.keyPressed(keyCode, scanCode, modifiers));
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        EmbeddedMapProvider provider = activeEmbedded();
        return provider != null && ensure(provider)
                && guarded(provider, () -> provider.keyReleased(keyCode, scanCode, modifiers));
    }

    public boolean charTyped(char codePoint, int modifiers) {
        EmbeddedMapProvider provider = activeEmbedded();
        return provider != null && ensure(provider)
                && guarded(provider, () -> provider.charTyped(codePoint, modifiers));
    }

    public boolean controllerAction(InputAction action) {
        EmbeddedMapProvider provider = activeEmbedded();
        return provider != null && ensure(provider) && guarded(provider, () -> provider.controllerAction(action));
    }

    public boolean hasTextInputFocus() {
        EmbeddedMapProvider provider = activeEmbedded();
        return provider != null && ensure(provider) && guarded(provider, provider::hasTextInputFocus);
    }

    public boolean handleBack() {
        EmbeddedMapProvider provider = activeEmbedded();
        return provider != null && ensure(provider) && guarded(provider, provider::handleBack);
    }

    public void close() {
        for (MapProvider provider : RpgMenuApi.get().mapProviders().values()) {
            if (provider instanceof EmbeddedMapProvider embedded && initialized.contains(provider.id())) {
                try {
                    MapState state = embedded.saveState();
                    if (state != null) states.put(provider.id(), state);
                    embedded.dispose();
                } catch (LinkageError | RuntimeException exception) {
                    RpgMenuFramework.LOGGER.warn("[RPGMF] Failed to dispose embedded map provider {}", provider.id(), exception);
                }
            }
        }
        initialized.clear();
        if (FrameworkConfig.PRESERVE_MAP_VIEW.get()) rememberedProviderId = activeProviderId;
        else rememberedProviderId = null;
    }

    public ResourceLocation activeProviderId() { return activeProviderId; }

    private boolean ensure(EmbeddedMapProvider provider) {
        if (failed.contains(provider.id())) return false;
        MapViewport mapViewport = mapViewport();
        try {
            if (initialized.add(provider.id())) {
                MapState state = states.get(provider.id());
                if (state != null) provider.restoreState(state);
                if (!provider.init(mapViewport)) {
                    initialized.remove(provider.id());
                    return false;
                }
            } else {
                provider.resize(mapViewport);
            }
            return true;
        } catch (LinkageError | RuntimeException exception) {
            fail(provider, exception);
            return false;
        }
    }

    private void fail(MapProvider provider, Throwable exception) {
        if (failed.add(provider.id())) {
            RpgMenuFramework.LOGGER.warn("[RPGMF] Embedded map provider {} failed; it will be disabled for this menu", provider.id(), exception);
        }
        if (initialized.remove(provider.id()) && provider instanceof EmbeddedMapProvider embedded) {
            try {
                embedded.dispose();
            } catch (LinkageError | RuntimeException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
        }
    }

    private EmbeddedMapProvider activeEmbedded() {
        MapProvider provider = selectedProvider(availableProviders());
        return provider instanceof EmbeddedMapProvider embedded ? embedded : null;
    }

    private void updateViewport(UiRect target) {
        viewport = target;
        int selector = availableProviders().size() > 1 ? SELECTOR_HEIGHT : 0;
        content = new UiRect(target.x(), target.y() + selector, target.width(), Math.max(1, target.height() - selector));
    }

    private MapViewport mapViewport() {
        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation dimension = minecraft.player == null
                ? ResourceLocation.withDefaultNamespace("overworld")
                : minecraft.player.level().dimension().location();
        MapState state = states.get(activeProviderId);
        double centerX = state == null && minecraft.player != null ? minecraft.player.getX() : state == null ? 0 : state.centerX();
        double centerZ = state == null && minecraft.player != null ? minecraft.player.getZ() : state == null ? 0 : state.centerZ();
        double zoom = state == null ? 1 : state.zoom();
        return new MapViewport(content.x(), content.y(), content.width(), content.height(), dimension, centerX, centerZ, zoom);
    }

    private List<MapProvider> availableProviders() {
        return RpgMenuApi.get().mapProviders().values().stream().filter(provider -> {
            if (failed.contains(provider.id())) return false;
            try {
                return provider.isAvailable();
            } catch (LinkageError | RuntimeException exception) {
                if (failed.add(provider.id())) RpgMenuFramework.LOGGER.warn("[RPGMF] Map provider {} is unavailable", provider.id(), exception);
                return false;
            }
        }).toList();
    }

    private void chooseProvider(List<MapProvider> providers) {
        if (selectedProvider(providers) != null) return;
        if (rememberedProviderId != null
                && providers.stream().anyMatch(provider -> provider.id().equals(rememberedProviderId))) {
            activeProviderId = rememberedProviderId;
            return;
        }
        String preferred = FrameworkConfig.PREFERRED_MAP_PROVIDER.get();
        activeProviderId = providers.stream().filter(provider -> matches(provider.id(), preferred))
                .findFirst().orElseGet(providers::getFirst).id();
    }

    private static boolean matches(ResourceLocation id, String configured) {
        return id.toString().equals(configured) || id.getNamespace().equals(configured) || id.getPath().equals(configured);
    }

    private MapProvider selectedProvider(List<MapProvider> providers) {
        if (activeProviderId == null) return null;
        return providers.stream().filter(provider -> provider.id().equals(activeProviderId)).findFirst().orElse(null);
    }

    private void renderSelector(GuiGraphics graphics, List<MapProvider> providers, int mouseX, int mouseY,
                                ThemeDefinition theme) {
        int width = Math.max(36, viewport.width() / providers.size());
        int x = viewport.x();
        for (int i = 0; i < providers.size(); i++) {
            MapProvider provider = providers.get(i);
            int right = i == providers.size() - 1 ? viewport.right() : Math.min(viewport.right(), x + width);
            UiRect button = new UiRect(x, viewport.y(), right - x, SELECTOR_HEIGHT - 2);
            boolean selected = provider.id().equals(activeProviderId);
            boolean hovered = button.contains(mouseX, mouseY);
            graphics.fill(button.x(), button.y(), button.right(), button.bottom(),
                    selected ? theme.slotSelected() : hovered ? theme.slotHover() : theme.panelAlt());
            graphics.drawCenteredString(Minecraft.getInstance().font, provider.displayName(),
                    button.x() + button.width() / 2, button.y() + 7, selected ? theme.accent() : theme.textMuted());
            x = right;
        }
    }

    private void selectAt(List<MapProvider> providers, double mouseX) {
        int index = Math.min(providers.size() - 1,
                Math.max(0, (int) ((mouseX - viewport.x()) * providers.size() / Math.max(1, viewport.width()))));
        ResourceLocation next = providers.get(index).id();
        if (!next.equals(activeProviderId)) {
            activeProviderId = next;
            rememberedProviderId = next;
            UiSoundPlayer.play(UiSoundCue.SUBTAB_SWITCH);
        }
    }

    private boolean guarded(MapProvider provider, BooleanCall call) {
        try {
            return call.run();
        } catch (LinkageError | RuntimeException exception) {
            fail(provider, exception);
            return false;
        }
    }

    @FunctionalInterface
    private interface BooleanCall { boolean run(); }
}
