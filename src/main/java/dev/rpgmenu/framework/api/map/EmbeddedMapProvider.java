package dev.rpgmenu.framework.api.map;

import dev.rpgmenu.framework.api.input.InputAction;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Client-side contract for a real, directly rendered map view. Implementations receive local GUI-logical
 * coordinates and must never replace Minecraft's current screen as their normal browsing flow.
 */
public interface EmbeddedMapProvider extends MapProvider {
    boolean init(MapViewport viewport);
    void resize(MapViewport viewport);
    void tick();
    void render(GuiGraphics graphics, int localMouseX, int localMouseY, float partialTick);

    default void mouseMoved(double localX, double localY) {}
    default boolean mouseClicked(double localX, double localY, int button) { return false; }
    default boolean mouseReleased(double localX, double localY, int button) { return false; }
    default boolean mouseDragged(double localX, double localY, int button, double dragX, double dragY) { return false; }
    default boolean mouseScrolled(double localX, double localY, double scrollX, double scrollY) { return false; }
    default boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }
    default boolean keyReleased(int keyCode, int scanCode, int modifiers) { return false; }
    default boolean charTyped(char codePoint, int modifiers) { return false; }
    default boolean controllerAction(InputAction action) { return false; }

    /** True while map-owned text input must suppress RPGMF's world movement passthrough. */
    default boolean hasTextInputFocus() { return false; }
    /** True when Back closed a map child screen, context menu, or modal. Root Back returns false. */
    default boolean handleBack() { return false; }
    default MapState saveState() { return null; }
    default void restoreState(MapState state) {}
    default void dispose() {}
}
