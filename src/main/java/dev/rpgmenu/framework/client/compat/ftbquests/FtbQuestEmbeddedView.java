package dev.rpgmenu.framework.client.compat.ftbquests;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.client.layout.UiRect;
import net.minecraft.client.gui.GuiGraphics;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Hosts FTB Quests' real 2101.x BaseScreen tree without installing a ScreenWrapper as Minecraft's screen.
 * Root quest content fills the RPG viewport; child screens opened by task widgets are kept in an embedded stack.
 */
public final class FtbQuestEmbeddedView {
    private final Deque<Object> screens = new ArrayDeque<>();
    private Object questScreen;
    private Object pendingOpen;
    private Object pendingClose;
    private Method initGui;
    private Method updateGui;
    private Method tick;
    private Method draw;
    private Method getTheme;
    private Method getX;
    private Method getY;
    private Method getWidth;
    private Method getHeight;
    private Method setSize;
    private Method setRenderBlur;
    private Method refreshWidgets;
    private Method mousePressed;
    private Method mouseReleased;
    private Method mouseDragged;
    private Method mouseScrolled;
    private Method keyPressed;
    private Method keyReleased;
    private Method isViewingQuest;
    private Method anyModalPanelOpen;
    private Method onBack;
    private Method onClosed;
    private Method mouseButtonGet;
    private Constructor<?> keyConstructor;
    private UiRect lastViewport;
    private int viewportWidth = -1;
    private int viewportHeight = -1;
    private boolean unavailableLogged;

    public boolean ensure(UiRect viewport) {
        lastViewport = viewport;
        if (questScreen != null) {
            try {
                return resizeRoot(viewport);
            } catch (ReflectiveOperationException exception) {
                logUnavailable(exception);
                close();
                return false;
            }
        }
        try {
            Class<?> fileClass = Class.forName("dev.ftb.mods.ftbquests.client.ClientQuestFile");
            if (!(boolean) fileClass.getMethod("exists").invoke(null)) return false;
            Object file = fileClass.getMethod("getInstance").invoke(null);
            Class<?> screenClass = Class.forName("dev.rpgmenu.framework.client.compat.ftbquests.EmbeddedQuestScreen");
            Class<?> baseScreenClass = Class.forName("dev.ftb.mods.ftblibrary.ui.BaseScreen");
            Class<?> persistedClass = Class.forName("dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen$PersistedData");
            Field persisted = fileClass.getDeclaredField("persistedData");
            persisted.setAccessible(true);

            initGui = baseScreenClass.getMethod("initGui");
            updateGui = baseScreenClass.getMethod("updateGui", int.class, int.class, float.class);
            tick = baseScreenClass.getMethod("tick");
            draw = method(baseScreenClass, "draw", 6);
            getTheme = baseScreenClass.getMethod("getTheme");
            getX = baseScreenClass.getMethod("getX");
            getY = baseScreenClass.getMethod("getY");
            getWidth = baseScreenClass.getMethod("getWidth");
            getHeight = baseScreenClass.getMethod("getHeight");
            setSize = baseScreenClass.getMethod("setSize", int.class, int.class);
            setRenderBlur = baseScreenClass.getMethod("setRenderBlur", boolean.class);
            refreshWidgets = baseScreenClass.getMethod("refreshWidgets");
            mousePressed = method(baseScreenClass, "mousePressed", 1);
            mouseReleased = method(baseScreenClass, "mouseReleased", 1);
            mouseDragged = baseScreenClass.getMethod("mouseDragged", int.class, double.class, double.class);
            mouseScrolled = baseScreenClass.getMethod("mouseScrolled", double.class, double.class, double.class, double.class);
            keyPressed = method(baseScreenClass, "keyPressed", 1);
            keyReleased = method(baseScreenClass, "keyReleased", 1);
            anyModalPanelOpen = baseScreenClass.getMethod("anyModalPanelOpen");
            onBack = baseScreenClass.getMethod("onBack");
            onClosed = baseScreenClass.getMethod("onClosed");
            isViewingQuest = screenClass.getMethod("isViewingQuest");

            Class<?> mouseButtonClass = Class.forName("dev.ftb.mods.ftblibrary.ui.input.MouseButton");
            mouseButtonGet = mouseButtonClass.getMethod("get", int.class);
            keyConstructor = Class.forName("dev.ftb.mods.ftblibrary.ui.input.Key")
                    .getConstructor(int.class, int.class, int.class);

            questScreen = screenClass.getConstructor(fileClass, persistedClass).newInstance(file, persisted.get(file));
            screens.addLast(questScreen);
            resizeRoot(viewport);
            setRenderBlur.invoke(questScreen, false);
            initGui.invoke(questScreen);
            RpgMenuFramework.LOGGER.info("[RPGMF] Created embedded FTB Quests host");
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            logUnavailable(exception);
            close();
            return false;
        }
    }

    public void render(GuiGraphics graphics, UiRect viewport, int mouseX, int mouseY, float partialTick) {
        if (!ensure(viewport)) return;
        try {
            Object screen = activeScreen();
            update(screen, viewport, mouseX, mouseY, partialTick);
            int originX = intValue(getX, screen);
            int originY = intValue(getY, screen);
            int targetX = targetX(viewport, screen);
            int targetY = targetY(viewport, screen);
            Object theme = getTheme.invoke(screen);
            graphics.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(targetX - originX, targetY - originY, 0);
                draw.invoke(screen, graphics, theme, originX, originY,
                        intValue(getWidth, screen), intValue(getHeight, screen));
            } finally {
                graphics.pose().popPose();
                graphics.disableScissor();
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            logUnavailable(exception);
        }
    }

    public void tick(UiRect viewport, int mouseX, int mouseY) {
        if (questScreen == null) return;
        lastViewport = viewport;
        Object screen = activeScreen();
        try {
            update(screen, viewport, mouseX, mouseY, 0);
            beginDispatch();
            try {
                tick.invoke(screen);
            } finally {
                endDispatch();
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            logUnavailable(exception);
        }
    }

    public boolean mouseClicked(UiRect viewport, double mouseX, double mouseY, int button) {
        return invokeMouse(viewport, mouseX, mouseY, button, mousePressed);
    }

    public boolean mouseReleased(UiRect viewport, double mouseX, double mouseY, int button) {
        if (questScreen == null) return false;
        Object screen = activeScreen();
        try {
            update(screen, viewport, mouseX, mouseY, 0);
            beginDispatch();
            try {
                mouseReleased.invoke(screen, mouseButtonGet.invoke(null, button));
            } finally {
                endDispatch();
            }
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            logUnavailable(exception);
            return false;
        }
    }

    public boolean mouseDragged(UiRect viewport, double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (questScreen == null) return false;
        Object screen = activeScreen();
        try {
            update(screen, viewport, mouseX, mouseY, 0);
            beginDispatch();
            try {
                return (boolean) mouseDragged.invoke(screen, button, dragX, dragY);
            } finally {
                endDispatch();
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            logUnavailable(exception);
            return false;
        }
    }

    public boolean mouseScrolled(UiRect viewport, double mouseX, double mouseY, double scrollX, double scrollY) {
        if (questScreen == null) return false;
        Object screen = activeScreen();
        try {
            update(screen, viewport, mouseX, mouseY, 0);
            beginDispatch();
            try {
                return (boolean) mouseScrolled.invoke(screen, embeddedX(viewport, mouseX, screen),
                        embeddedY(viewport, mouseY, screen), scrollX, scrollY);
            } finally {
                endDispatch();
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            logUnavailable(exception);
            return false;
        }
    }

    public void mouseMoved(UiRect viewport, double mouseX, double mouseY) {
        if (questScreen == null) return;
        try {
            update(activeScreen(), viewport, mouseX, mouseY, 0);
        } catch (ReflectiveOperationException | LinkageError exception) {
            logUnavailable(exception);
        }
    }

    /** Refresh the root quest graph on a real host transition/data dirty signal, never on a timer. */
    public void markDirty() {
        if (questScreen == null) return;
        try {
            refreshWidgets.invoke(questScreen);
        } catch (ReflectiveOperationException | LinkageError exception) {
            logUnavailable(exception);
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (questScreen == null) return false;
        Object screen = activeScreen();
        try {
            beginDispatch();
            try {
                return (boolean) keyPressed.invoke(screen, keyConstructor.newInstance(keyCode, scanCode, modifiers));
            } finally {
                endDispatch();
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            logUnavailable(exception);
            return false;
        }
    }

    public void keyReleased(int keyCode, int scanCode, int modifiers) {
        if (questScreen == null) return;
        Object screen = activeScreen();
        try {
            beginDispatch();
            try {
                keyReleased.invoke(screen, keyConstructor.newInstance(keyCode, scanCode, modifiers));
            } finally {
                endDispatch();
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            logUnavailable(exception);
        }
    }

    /** Returns true only when an FTB child/detail/modal consumed Back; root Back belongs to RPG Menu. */
    public boolean handleBack() {
        if (questScreen == null) return false;
        Object screen = activeScreen();
        try {
            if (screen != questScreen || (boolean) isViewingQuest.invoke(questScreen)
                    || (boolean) anyModalPanelOpen.invoke(questScreen)) {
                beginDispatch();
                try {
                    onBack.invoke(screen);
                } finally {
                    endDispatch();
                }
                return true;
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            logUnavailable(exception);
        }
        return false;
    }

    public void close() {
        while (!screens.isEmpty()) {
            Object screen = screens.removeLast();
            try {
                if (onClosed != null) onClosed.invoke(screen);
            } catch (ReflectiveOperationException | LinkageError exception) {
                logUnavailable(exception);
            }
        }
        questScreen = null;
        pendingOpen = null;
        pendingClose = null;
        viewportWidth = -1;
        viewportHeight = -1;
    }

    boolean queueNestedOpen(Object screen) {
        if (questScreen == null) return false;
        if (screen != questScreen && !screens.contains(screen)) pendingOpen = screen;
        return true;
    }

    boolean queueNestedClose(Object screen) {
        if (questScreen == null || activeScreen() != screen) return false;
        pendingClose = screen;
        return true;
    }

    void finishFtbDispatch() {
        if (pendingClose != null) {
            Object closing = pendingClose;
            pendingClose = null;
            if (screens.size() > 1 && screens.peekLast() == closing) {
                screens.removeLast();
                try {
                    onClosed.invoke(closing);
                } catch (ReflectiveOperationException | LinkageError exception) {
                    logUnavailable(exception);
                }
            }
        }
        if (pendingOpen != null) {
            Object opening = pendingOpen;
            pendingOpen = null;
            screens.addLast(opening);
            try {
                setRenderBlur.invoke(opening, false);
                initGui.invoke(opening);
                constrainNestedToViewport(opening);
            } catch (ReflectiveOperationException | LinkageError exception) {
                screens.removeLastOccurrence(opening);
                logUnavailable(exception);
            }
        }
    }

    private void constrainNestedToViewport(Object screen) throws ReflectiveOperationException {
        if (lastViewport == null) return;
        int width = intValue(getWidth, screen);
        int height = intValue(getHeight, screen);
        int constrainedWidth = width <= 0 ? lastViewport.width() : Math.min(width, lastViewport.width());
        int constrainedHeight = height <= 0 ? lastViewport.height() : Math.min(height, lastViewport.height());
        if (constrainedWidth != width || constrainedHeight != height) {
            setSize.invoke(screen, constrainedWidth, constrainedHeight);
            refreshWidgets.invoke(screen);
        }
    }

    private boolean resizeRoot(UiRect viewport) throws ReflectiveOperationException {
        if (viewport.width() == viewportWidth && viewport.height() == viewportHeight) return true;
        setSize.invoke(questScreen, viewport.width(), viewport.height());
        viewportWidth = viewport.width();
        viewportHeight = viewport.height();
        return true;
    }

    private void update(Object screen, UiRect viewport, double mouseX, double mouseY, float partialTick)
            throws ReflectiveOperationException {
        lastViewport = viewport;
        if (screen == questScreen) resizeRoot(viewport);
        updateGui.invoke(screen, embeddedX(viewport, mouseX, screen), embeddedY(viewport, mouseY, screen), partialTick);
    }

    private int embeddedX(UiRect viewport, double mouseX, Object screen) throws ReflectiveOperationException {
        return intValue(getX, screen) + (int) mouseX - targetX(viewport, screen);
    }

    private int embeddedY(UiRect viewport, double mouseY, Object screen) throws ReflectiveOperationException {
        return intValue(getY, screen) + (int) mouseY - targetY(viewport, screen);
    }

    private int targetX(UiRect viewport, Object screen) throws ReflectiveOperationException {
        if (screen == questScreen) return viewport.x();
        return viewport.x() + Math.max(0, (viewport.width() - intValue(getWidth, screen)) / 2);
    }

    private int targetY(UiRect viewport, Object screen) throws ReflectiveOperationException {
        if (screen == questScreen) return viewport.y();
        return viewport.y() + Math.max(0, (viewport.height() - intValue(getHeight, screen)) / 2);
    }

    private boolean invokeMouse(UiRect viewport, double mouseX, double mouseY, int button, Method method) {
        if (questScreen == null) return false;
        Object screen = activeScreen();
        try {
            update(screen, viewport, mouseX, mouseY, 0);
            beginDispatch();
            try {
                return (boolean) method.invoke(screen, mouseButtonGet.invoke(null, button));
            } finally {
                endDispatch();
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            logUnavailable(exception);
            return false;
        }
    }

    private Object activeScreen() {
        return screens.peekLast();
    }

    private void beginDispatch() {
        FtbEmbeddedScreenBridge.begin(this);
    }

    private void endDispatch() {
        FtbEmbeddedScreenBridge.end(this);
    }

    private static int intValue(Method method, Object owner) throws ReflectiveOperationException {
        return ((Number) method.invoke(owner)).intValue();
    }

    private static Method method(Class<?> type, String name, int parameterCount) {
        for (Method candidate : type.getMethods()) {
            if (candidate.getName().equals(name) && candidate.getParameterCount() == parameterCount) return candidate;
        }
        throw new IllegalStateException("Missing FTB method " + name);
    }

    private void logUnavailable(Throwable exception) {
        if (!unavailableLogged) {
            unavailableLogged = true;
            RpgMenuFramework.LOGGER.warn("[RPGMF] Embedded FTB Quests host failed", exception);
        }
    }
}
