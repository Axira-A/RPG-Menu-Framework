package dev.rpgmenu.framework.client.map;

import net.minecraft.client.gui.screens.Screen;

/** Active embedded hosts use this only while dispatching third-party UI code. */
public interface ScreenTransitionHost {
    boolean captureScreenTransition(Screen nextScreen);

    default int viewportFramebufferWidth() { return -1; }
    default int viewportFramebufferHeight() { return -1; }
    default double virtualMouseX() { return Double.NaN; }
    default double virtualMouseY() { return Double.NaN; }
    default int viewportGuiX() { return 0; }
    default int viewportGuiY() { return 0; }
}
