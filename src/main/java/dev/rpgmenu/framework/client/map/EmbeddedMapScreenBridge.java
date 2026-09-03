package dev.rpgmenu.framework.client.map;

import net.minecraft.client.gui.screens.Screen;

/** Narrow, render-thread bridge used to keep third-party child screens inside the map viewport. */
public final class EmbeddedMapScreenBridge {
    private static final ThreadLocal<ScreenTransitionHost> ACTIVE = new ThreadLocal<>();

    private EmbeddedMapScreenBridge() {}

    public static void begin(ScreenTransitionHost host) { ACTIVE.set(host); }

    public static void end(ScreenTransitionHost host) {
        if (ACTIVE.get() == host) ACTIVE.remove();
    }

    public static boolean capture(Screen nextScreen) {
        ScreenTransitionHost host = ACTIVE.get();
        return host != null && host.captureScreenTransition(nextScreen);
    }

    public static boolean isActive() { return ACTIVE.get() != null; }

    public static int viewportFramebufferWidth() {
        ScreenTransitionHost host = ACTIVE.get();
        return host == null ? -1 : host.viewportFramebufferWidth();
    }

    public static int viewportFramebufferHeight() {
        ScreenTransitionHost host = ACTIVE.get();
        return host == null ? -1 : host.viewportFramebufferHeight();
    }

    public static double virtualMouseX() {
        ScreenTransitionHost host = ACTIVE.get();
        return host == null ? Double.NaN : host.virtualMouseX();
    }

    public static double virtualMouseY() {
        ScreenTransitionHost host = ACTIVE.get();
        return host == null ? Double.NaN : host.virtualMouseY();
    }

    public static int viewportGuiX() {
        ScreenTransitionHost host = ACTIVE.get();
        return host == null ? 0 : host.viewportGuiX();
    }

    public static int viewportGuiY() {
        ScreenTransitionHost host = ACTIVE.get();
        return host == null ? 0 : host.viewportGuiY();
    }
}
