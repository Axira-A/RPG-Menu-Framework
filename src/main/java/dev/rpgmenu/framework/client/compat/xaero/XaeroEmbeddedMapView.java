package dev.rpgmenu.framework.client.compat.xaero;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.map.MapState;
import dev.rpgmenu.framework.api.map.MapViewport;
import dev.rpgmenu.framework.client.map.EmbeddedMapScreenBridge;
import dev.rpgmenu.framework.client.map.ScreenTransitionHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Runs Xaero 1.21.1's real GuiMap and its real child screens at viewport dimensions. No Xaero code or assets
 * are copied, and no RenderTarget is used by RPGMF. All third-party linkage remains reflective and optional.
 */
public final class XaeroEmbeddedMapView implements ScreenTransitionHost {
    private final Minecraft minecraft = Minecraft.getInstance();
    private final Deque<Screen> screens = new ArrayDeque<>();
    private Screen root;
    private Screen pendingScreen;
    private boolean transitionPending;
    private boolean rootExitRequested;
    private int viewportWidth = -1;
    private int viewportHeight = -1;
    private MapViewport viewport;
    private MapState restoredState;
    private Field cameraX;
    private Field cameraZ;
    private Field scale;
    private Field userScale;
    private Field destinationScale;
    private Field shouldResetCamera;
    private Field rightClickMenu;
    private Method closeRightClick;
    private Field waypointMenu;
    private Field playersMenu;
    private Field hopMenu;
    private Field hopButton;
    private Method toggleWaypointMenu;
    private Method togglePlayerMenu;
    private Method toggleHopMenu;

    public boolean init(MapViewport nextViewport) {
        viewport = nextViewport;
        if (root != null) {
            resize(nextViewport);
            return true;
        }
        if (minecraft.player == null) return false;
        try {
            Class<?> sessionClass = Class.forName("xaero.map.WorldMapSession");
            Object session = sessionClass.getMethod("getCurrentSession").invoke(null);
            if (session == null || !(boolean) sessionClass.getMethod("isUsable").invoke(session)) return false;
            Object mapProcessor = sessionClass.getMethod("getMapProcessor").invoke(session);
            if (mapProcessor == null) return false;

            Class<?> processorClass = Class.forName("xaero.map.MapProcessor");
            Class<?> guiMapClass = Class.forName("xaero.map.gui.GuiMap");
            Constructor<?> constructor = guiMapClass.getConstructor(Screen.class, Screen.class, processorClass, Entity.class);
            root = (Screen) constructor.newInstance(null, null, mapProcessor, minecraft.player);
            resolveStateAccess(guiMapClass);
            screens.addLast(root);
            viewportWidth = Math.max(1, nextViewport.width());
            viewportHeight = Math.max(1, nextViewport.height());
            dispatchVoid(() -> root.init(minecraft, viewportWidth, viewportHeight));
            applyState(restoredState);
            RpgMenuFramework.LOGGER.info("[RPGMF] Created embedded Xaero GuiMap: viewport={}x{} gui, window={}x{} framebuffer, origin=({}, {})",
                    viewportWidth, viewportHeight, minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight(),
                    nextViewport.x(), nextViewport.y());
            return true;
        } catch (ReflectiveOperationException exception) {
            throw linkageFailure("initialize Xaero GuiMap", exception);
        }
    }

    public void resize(MapViewport nextViewport) {
        viewport = nextViewport;
        if (root == null) return;
        int width = Math.max(1, nextViewport.width());
        int height = Math.max(1, nextViewport.height());
        if (width == viewportWidth && height == viewportHeight) return;
        MapState state = saveState();
        viewportWidth = width;
        viewportHeight = height;
        for (Screen screen : java.util.List.copyOf(screens)) {
            dispatchVoid(() -> screen.resize(minecraft, width, height));
        }
        applyState(state);
    }

    public void tick() {
        if (root != null) dispatchVoid(active()::tick);
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (root == null) return;
        Screen screen = active();
        dispatchVoid(() -> screen.render(graphics, mouseX, mouseY, partialTick));
    }

    public void mouseMoved(double x, double y) {
        if (root != null) dispatchVoid(() -> active().mouseMoved(x, y));
    }

    public boolean mouseClicked(double x, double y, int button) {
        return root != null && dispatch(() -> active().mouseClicked(x, y, button));
    }

    public boolean mouseReleased(double x, double y, int button) {
        return root != null && dispatch(() -> active().mouseReleased(x, y, button));
    }

    public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
        return root != null && dispatch(() -> active().mouseDragged(x, y, button, dragX, dragY));
    }

    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        return root != null && dispatch(() -> active().mouseScrolled(x, y, scrollX, scrollY));
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return root != null && dispatch(() -> active().keyPressed(keyCode, scanCode, modifiers));
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return root != null && dispatch(() -> active().keyReleased(keyCode, scanCode, modifiers));
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return root != null && dispatch(() -> active().charTyped(codePoint, modifiers));
    }

    public boolean hasTextInputFocus() {
        if (root == null) return false;
        GuiEventListener focused = active().getFocused();
        if (focused == null) return false;
        String name = focused.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
        return focused instanceof EditBox || name.contains("text") || name.contains("search");
    }

    /** Internal levels consume Back. A root-screen exit request is returned to RPGMF instead. */
    public boolean handleBack() {
        if (root == null) return false;
        if (screens.size() == 1 && hasRightClickMenu()) {
            dispatchVoid(() -> invoke(closeRightClick, root));
            return true;
        }
        if (screens.size() == 1 && booleanField(waypointMenu)) {
            dispatchVoid(() -> invoke(toggleWaypointMenu, root));
            return true;
        }
        if (screens.size() == 1 && booleanField(playersMenu)) {
            dispatchVoid(() -> invoke(togglePlayerMenu, root));
            return true;
        }
        if (screens.size() == 1 && booleanField(hopMenu)) {
            dispatchVoid(() -> invoke(toggleHopMenu, root, objectField(hopButton)));
            return true;
        }
        int depth = screens.size();
        boolean handled = dispatch(() -> active().keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0));
        if (depth > 1) {
            if (screens.size() == depth) popActive();
            return true;
        }
        return !rootExitRequested && handled;
    }

    public MapState saveState() {
        if (root == null) return restoredState;
        try {
            ResourceLocation dimension = viewport == null ? ResourceLocation.withDefaultNamespace("overworld") : viewport.dimension();
            return new MapState(dimension, cameraX.getDouble(root), cameraZ.getDouble(root), userScale.getDouble(root), null);
        } catch (IllegalAccessException exception) {
            throw linkageFailure("read Xaero map state", exception);
        }
    }

    public void restoreState(MapState state) {
        restoredState = state;
        if (root != null) applyState(state);
    }

    public void close() {
        while (!screens.isEmpty()) {
            Screen screen = screens.removeLast();
            try {
                screen.removed();
            } catch (RuntimeException exception) {
                RpgMenuFramework.LOGGER.debug("[RPGMF] Xaero child cleanup failed", exception);
            }
        }
        root = null;
        pendingScreen = null;
        transitionPending = false;
        viewportWidth = -1;
        viewportHeight = -1;
    }

    @Override
    public boolean captureScreenTransition(Screen nextScreen) {
        pendingScreen = nextScreen;
        transitionPending = true;
        return true;
    }

    @Override
    public int viewportFramebufferWidth() {
        return physicalPixels(viewportWidth);
    }

    @Override
    public int viewportFramebufferHeight() {
        return physicalPixels(viewportHeight);
    }

    @Override
    public double virtualMouseX() {
        double framebufferX = minecraft.mouseHandler.xpos() * minecraft.getWindow().getWidth()
                / Math.max(1, minecraft.getWindow().getScreenWidth());
        return framebufferX - physicalPixels(viewport == null ? 0 : viewport.x());
    }

    @Override
    public double virtualMouseY() {
        double framebufferY = minecraft.mouseHandler.ypos() * minecraft.getWindow().getHeight()
                / Math.max(1, minecraft.getWindow().getScreenHeight());
        return framebufferY - physicalPixels(viewport == null ? 0 : viewport.y());
    }

    private Screen active() { return screens.getLast(); }

    private int physicalPixels(int guiPixels) {
        return Math.max(0, (int) (Math.max(0, guiPixels) * minecraft.getWindow().getGuiScale()));
    }

    private boolean hasRightClickMenu() {
        try {
            return rightClickMenu.get(root) != null;
        } catch (IllegalAccessException exception) {
            throw linkageFailure("inspect Xaero context menu", exception);
        }
    }

    private void resolveStateAccess(Class<?> guiMapClass) throws NoSuchFieldException, NoSuchMethodException {
        cameraX = field(guiMapClass, "cameraX");
        cameraZ = field(guiMapClass, "cameraZ");
        scale = field(guiMapClass, "scale");
        userScale = field(guiMapClass, "userScale");
        destinationScale = field(guiMapClass, "destScale");
        shouldResetCamera = field(guiMapClass, "shouldResetCameraPos");
        rightClickMenu = field(guiMapClass, "rightClickMenu");
        closeRightClick = guiMapClass.getMethod("closeRightClick");
        waypointMenu = field(guiMapClass, "waypointMenu");
        playersMenu = field(guiMapClass, "playersMenu");
        hopMenu = field(guiMapClass, "hopMenu");
        hopButton = field(guiMapClass, "hopButton");
        toggleWaypointMenu = method(guiMapClass, "toggleWaypointMenu");
        togglePlayerMenu = method(guiMapClass, "togglePlayerMenu");
        toggleHopMenu = method(guiMapClass, "onHopButton", Button.class);
    }

    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Method method(Class<?> owner, String name, Class<?>... parameters) throws NoSuchMethodException {
        Method method = owner.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    private boolean booleanField(Field field) {
        try {
            return field.getBoolean(root);
        } catch (IllegalAccessException exception) {
            throw linkageFailure("inspect Xaero menu state", exception);
        }
    }

    private Object objectField(Field field) {
        try {
            return field.get(root);
        } catch (IllegalAccessException exception) {
            throw linkageFailure("inspect Xaero menu widget", exception);
        }
    }

    private void applyState(MapState state) {
        if (state == null || root == null) return;
        try {
            cameraX.setDouble(root, state.centerX());
            cameraZ.setDouble(root, state.centerZ());
            double zoom = Math.max(0.0001, state.zoom());
            scale.setDouble(root, zoom);
            userScale.setDouble(root, zoom);
            destinationScale.setDouble(null, zoom);
            shouldResetCamera.setBoolean(root, false);
        } catch (IllegalAccessException exception) {
            throw linkageFailure("restore Xaero map state", exception);
        }
    }

    private boolean dispatch(BooleanCall call) {
        rootExitRequested = false;
        EmbeddedMapScreenBridge.begin(this);
        try {
            return call.run();
        } finally {
            EmbeddedMapScreenBridge.end(this);
            applyPendingTransition();
        }
    }

    private void dispatchVoid(VoidCall call) {
        dispatch(() -> {
            call.run();
            return true;
        });
    }

    private void applyPendingTransition() {
        if (!transitionPending) return;
        Screen next = pendingScreen;
        transitionPending = false;
        pendingScreen = null;
        Screen owner = minecraft.screen;
        if (next == null || next == owner) {
            if (screens.size() > 1) popActive();
            else rootExitRequested = true;
            return;
        }
        if (screens.contains(next)) {
            while (screens.size() > 1 && active() != next) popActive();
            return;
        }
        screens.addLast(next);
        EmbeddedMapScreenBridge.begin(this);
        try {
            next.init(minecraft, Math.max(1, viewportWidth), Math.max(1, viewportHeight));
        } finally {
            EmbeddedMapScreenBridge.end(this);
        }
        if (transitionPending) applyPendingTransition();
    }

    private void popActive() {
        if (screens.size() <= 1) return;
        Screen removed = screens.removeLast();
        removed.removed();
    }

    private static void invoke(Method method, Object receiver, Object... arguments) {
        try {
            method.invoke(receiver, arguments);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw linkageFailure("invoke Xaero UI method", exception);
        }
    }

    private static IllegalStateException linkageFailure(String action, ReflectiveOperationException exception) {
        Throwable cause = exception instanceof InvocationTargetException invocation && invocation.getCause() != null
                ? invocation.getCause() : exception;
        return new IllegalStateException("Unable to " + action + " for the installed Xaero World Map 1.21.1 build", cause);
    }

    @FunctionalInterface private interface BooleanCall { boolean run(); }
    @FunctionalInterface private interface VoidCall { void run(); }
}
