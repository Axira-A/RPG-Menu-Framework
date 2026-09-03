package dev.rpgmenu.framework.api.skills;

import dev.rpgmenu.framework.api.input.InputAction;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Client-side extension for a real skill-tree UI rendered directly at GUI logical resolution.
 * Implementations must not replace Minecraft's current screen during ordinary browsing.
 */
public interface EmbeddedSkillProvider extends SkillProvider {
    boolean init(SkillViewport viewport);
    void resize(SkillViewport viewport);
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
    default boolean hasTextInputFocus() { return false; }
    default boolean handleBack() { return false; }
    default void dispose() {}
}
