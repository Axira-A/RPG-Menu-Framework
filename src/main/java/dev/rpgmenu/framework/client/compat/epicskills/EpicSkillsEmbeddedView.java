package dev.rpgmenu.framework.client.compat.epicskills;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.input.InputAction;
import dev.rpgmenu.framework.api.skills.SkillViewport;
import dev.rpgmenu.framework.client.map.EmbeddedMapScreenBridge;
import dev.rpgmenu.framework.client.map.ScreenTransitionHost;
import dev.rpgmenu.framework.client.screen.RpgMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Hosts Epic Skills 21.3.x's real SkillTreeScreen and child screens without replacing RPGMF's Screen.
 * All optional-mod linkage is reflective and remains confined to this client compat package.
 */
public final class EpicSkillsEmbeddedView implements ScreenTransitionHost {
    private static final String TREE_SCREEN = "com.yesman.epicskills.client.gui.screen.SkillTreeScreen";
    private static final String SKILL_EDITOR = "yesman.epicfight.client.gui.screen.SkillEditScreen";
    private static final String DEFAULT_CATEGORY_TEXTURES =
            TREE_SCREEN + "$TreePage$NodeButton$CategorySlotTextures";
    private static final long RETRY_NANOS = 1_000_000_000L;
    private static volatile EpicSkillsEmbeddedView packetView;

    private final Minecraft minecraft = Minecraft.getInstance();
    private final Deque<Screen> screens = new ArrayDeque<>();
    private Screen root;
    private Screen pendingScreen;
    private boolean transitionPending;
    private boolean rootExitRequested;
    private SkillViewport viewport;
    private int viewportWidth = -1;
    private int viewportHeight = -1;
    private State state = State.NEW;
    private long nextRetry;
    private boolean failureLogged;
    private Method setBackgroundMode;
    private Method navigateTreePage;
    private Method moveViewport;
    private Method scaleUp;
    private Method scaleDown;
    private int transitionDepth;

    public boolean ensure(SkillViewport nextViewport) {
        viewport = nextViewport;
        if (state == State.FAILED) return false;
        if (root != null) {
            resize(nextViewport);
            return true;
        }
        if (state == State.EMPTY) return true;
        if (System.nanoTime() < nextRetry) return true;
        if (minecraft.player == null) return waitForSync();

        try {
            Class<?> capabilities = Class.forName("yesman.epicfight.world.capabilities.EpicFightCapabilities");
            Class<?> patchClass = Class.forName(
                    "yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch");
            Object playerPatch = capabilities.getMethod("getLocalPlayerPatch", LocalPlayer.class)
                    .invoke(null, minecraft.player);
            if (playerPatch == null) return waitForSync();

            Class<?> treeClass = Class.forName(TREE_SCREEN);
            // Epic Skills looks these values up while constructing node widgets. Force its built-in extensible
            // enum to initialize before the first NodeButton is created.
            Object fallbackCategoryTexture = defaultCategoryTexture(treeClass.getClassLoader());
            Constructor<?> constructor = treeClass.getConstructor(patchClass);
            Screen candidate = (Screen) constructor.newInstance(playerPatch);
            if ((boolean) treeClass.getMethod("discarded").invoke(candidate)) {
                state = State.EMPTY;
                RpgMenuFramework.LOGGER.info("[RPGMF] Epic Skills trees available: 0");
                return true;
            }

            setBackgroundMode = treeClass.getMethod("setBackgroundMode", boolean.class);
            navigateTreePage = treeClass.getMethod("navigateTreePage", boolean.class);
            moveViewport = treeClass.getMethod("moveViewport", float.class, float.class);
            scaleUp = treeClass.getMethod("scaleUp");
            scaleDown = treeClass.getMethod("scaleDown");
            root = candidate;
            repairMissingCategoryTextures(treeClass, fallbackCategoryTexture);
            screens.addLast(root);
            viewportWidth = nextViewport.width();
            viewportHeight = nextViewport.height();
            dispatchVoid(() -> root.init(minecraft, viewportWidth, viewportHeight));
            state = State.READY;
            packetView = this;
            RpgMenuFramework.LOGGER.info("[RPGMF] Epic Skills trees available: {}", treeCount(treeClass));
            RpgMenuFramework.LOGGER.info("[RPGMF] Created embedded Epic Skills host: {}x{} GUI pixels",
                    viewportWidth, viewportHeight);
            return true;
        } catch (InvocationTargetException exception) {
            Throwable cause = rootCause(exception);
            if (isUnsynchronized(cause)) return waitForSync();
            return fail(cause);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            if (isUnsynchronized(rootCause(exception))) return waitForSync();
            return fail(exception);
        }
    }

    public void resize(SkillViewport nextViewport) {
        viewport = nextViewport;
        if (root == null) return;
        int width = nextViewport.width();
        int height = nextViewport.height();
        if (width == viewportWidth && height == viewportHeight) return;
        viewportWidth = width;
        viewportHeight = height;
        for (Screen screen : java.util.List.copyOf(screens)) {
            dispatchVoid(() -> screen.resize(minecraft, width, height));
        }
    }

    public void tick() {
        if (root == null || state != State.READY) return;
        try {
            dispatchVoid(active()::tick);
        } catch (LinkageError | RuntimeException exception) {
            fail(exception);
        }
    }

    public boolean render(GuiGraphics graphics, int localMouseX, int localMouseY, float partialTick) {
        if (state == State.EMPTY) {
            drawStatus(graphics, "message.rpgmenuframework.no_skill_trees");
            return true;
        }
        if (state != State.READY || root == null) {
            if (state == State.FAILED) return false;
            drawStatus(graphics, "message.rpgmenuframework.skill_data_pending");
            return true;
        }
        try {
            dispatchVoid(() -> active().render(graphics, localMouseX, localMouseY, partialTick));
            return true;
        } catch (LinkageError | RuntimeException exception) {
            fail(exception);
            return false;
        }
    }

    public void mouseMoved(double x, double y) {
        interactVoid(() -> active().mouseMoved(x, y));
    }

    public boolean mouseClicked(double x, double y, int button) {
        return interact(() -> active().mouseClicked(x, y, button));
    }

    public boolean mouseReleased(double x, double y, int button) {
        return interact(() -> active().mouseReleased(x, y, button));
    }

    public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
        return interact(() -> active().mouseDragged(x, y, button, dragX, dragY));
    }

    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        return interact(() -> active().mouseScrolled(x, y, scrollX, scrollY));
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return interact(() -> active().keyPressed(keyCode, scanCode, modifiers));
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return interact(() -> active().keyReleased(keyCode, scanCode, modifiers));
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return interact(() -> active().charTyped(codePoint, modifiers));
    }

    public boolean controllerAction(InputAction action) {
        if (root == null) return false;
        if (action == InputAction.BACK) return handleBack();
        if (active() == root) {
            try {
                return switch (action) {
                    case PREVIOUS_SUBPAGE -> (boolean) navigateTreePage.invoke(root, false);
                    case NEXT_SUBPAGE -> (boolean) navigateTreePage.invoke(root, true);
                    case UP -> { moveViewport.invoke(root, 0F, -12F); yield true; }
                    case DOWN -> { moveViewport.invoke(root, 0F, 12F); yield true; }
                    case LEFT -> { moveViewport.invoke(root, -12F, 0F); yield true; }
                    case RIGHT -> { moveViewport.invoke(root, 12F, 0F); yield true; }
                    case DETAILS -> { scaleUp.invoke(root); yield true; }
                    case FAVORITE -> { scaleDown.invoke(root); yield true; }
                    case CONFIRM -> keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
                    default -> false;
                };
            } catch (ReflectiveOperationException exception) {
                return fail(exception);
            }
        }
        int key = switch (action) {
            case UP -> GLFW.GLFW_KEY_UP;
            case DOWN -> GLFW.GLFW_KEY_DOWN;
            case LEFT -> GLFW.GLFW_KEY_LEFT;
            case RIGHT -> GLFW.GLFW_KEY_RIGHT;
            case CONFIRM -> GLFW.GLFW_KEY_ENTER;
            default -> GLFW.GLFW_KEY_UNKNOWN;
        };
        return key != GLFW.GLFW_KEY_UNKNOWN && keyPressed(key, 0, 0);
    }

    public boolean hasTextInputFocus() {
        if (!isReady()) return false;
        GuiEventListener focused = active().getFocused();
        if (focused == null) return false;
        String name = focused.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
        return focused instanceof EditBox || name.contains("text") || name.contains("search");
    }

    /** Child detail/modal consumes Back. At the root, Back belongs to RPGMF. */
    public boolean handleBack() {
        if (!isReady() || screens.size() == 1) return false;
        int depth = screens.size();
        dispatchVoid(active()::onClose);
        if (screens.size() == depth) popActive();
        return true;
    }

    public void close() {
        while (!screens.isEmpty()) {
            Screen screen = screens.removeLast();
            try {
                screen.removed();
            } catch (RuntimeException exception) {
                RpgMenuFramework.LOGGER.debug("[RPGMF] Epic Skills embedded screen cleanup failed", exception);
            }
        }
        root = null;
        pendingScreen = null;
        transitionPending = false;
        viewportWidth = -1;
        viewportHeight = -1;
        state = State.NEW;
        transitionDepth = 0;
        if (packetView == this) packetView = null;
    }

    public boolean isReady() { return state == State.READY && root != null; }

    public void markFailed(Throwable exception) { fail(exception); }

    /** Lets Epic Skills' own packet handlers see their embedded active screen without changing Minecraft.currentScreen. */
    public static Screen packetScreen(Screen ordinaryScreen) {
        EpicSkillsEmbeddedView view = packetView;
        return view == null || view.root == null || view.screens.isEmpty() ? ordinaryScreen : view.active();
    }

    /** Captures official Epic Skills packet/UI callbacks which run outside a direct input dispatch. */
    public static boolean captureOutOfBandTransition(Screen nextScreen) {
        EpicSkillsEmbeddedView view = packetView;
        if (view == null || !view.isReady() || !(view.minecraft.screen instanceof RpgMenuScreen menu)) return false;
        if (nextScreen == null && (!menu.isEmbeddedSkillsActive() || view.screens.size() <= 1)) return false;
        if (nextScreen != null && !view.ownsScreen(nextScreen)) return false;
        view.pendingScreen = nextScreen;
        view.transitionPending = true;
        view.finishTransition();
        return true;
    }

    @Override
    public boolean captureScreenTransition(Screen nextScreen) {
        // The admin data editor remains an explicit full-screen escape hatch; player tree/detail flow is embedded.
        if (nextScreen != null && nextScreen.getClass().getName().equals(SKILL_EDITOR)) return false;
        pendingScreen = nextScreen;
        transitionPending = true;
        return true;
    }

    @Override public int viewportGuiX() { return viewport == null ? 0 : viewport.x(); }
    @Override public int viewportGuiY() { return viewport == null ? 0 : viewport.y(); }

    private Screen active() { return screens.getLast(); }

    private boolean ownsScreen(Screen screen) {
        if (screen == root || screens.contains(screen)) return true;
        String name = screen.getClass().getName();
        return !name.equals(SKILL_EDITOR) && (name.startsWith("com.yesman.epicskills.client.gui.screen.")
                || name.equals("yesman.epicfight.client.gui.screen.SlotSelectScreen"));
    }

    private boolean interact(BooleanCall call) {
        if (!isReady()) return false;
        try {
            return dispatch(call);
        } catch (LinkageError | RuntimeException exception) {
            return fail(exception);
        }
    }

    private void interactVoid(VoidCall call) {
        interact(() -> {
            call.run();
            return true;
        });
    }

    private boolean dispatch(BooleanCall call) {
        rootExitRequested = false;
        EmbeddedMapScreenBridge.begin(this);
        try {
            return call.run();
        } finally {
            EmbeddedMapScreenBridge.end(this);
            finishTransition();
        }
    }

    private void dispatchVoid(VoidCall call) {
        dispatch(() -> {
            call.run();
            return true;
        });
    }

    private void finishTransition() {
        if (!transitionPending) return;
        if (++transitionDepth > 8) {
            transitionDepth--;
            fail(new IllegalStateException("Epic Skills produced a recursive embedded screen transition"));
            return;
        }
        Screen next = pendingScreen;
        pendingScreen = null;
        transitionPending = false;
        try {
            if (next == null) {
                if (screens.size() > 1) popActive();
                else rootExitRequested = true;
            } else if (screens.contains(next)) {
                while (screens.size() > 1 && active() != next) popActive();
                clearRootBackgroundIfVisible();
            } else {
                screens.addLast(next);
                try {
                    EmbeddedMapScreenBridge.begin(this);
                    next.init(minecraft, viewportWidth, viewportHeight);
                } catch (RuntimeException exception) {
                    screens.removeLastOccurrence(next);
                    fail(exception);
                } finally {
                    EmbeddedMapScreenBridge.end(this);
                }
            }
        } finally {
            transitionDepth--;
        }
        if (transitionPending && state != State.FAILED) finishTransition();
    }

    private void popActive() {
        if (screens.size() <= 1) return;
        Screen closing = screens.removeLast();
        try {
            closing.removed();
        } catch (RuntimeException exception) {
            RpgMenuFramework.LOGGER.debug("[RPGMF] Epic Skills child cleanup failed", exception);
        }
        clearRootBackgroundIfVisible();
    }

    private void clearRootBackgroundIfVisible() {
        if (root == null || screens.peekLast() != root || setBackgroundMode == null) return;
        try {
            setBackgroundMode.invoke(root, false);
        } catch (ReflectiveOperationException exception) {
            fail(exception);
        }
    }

    private int treeCount(Class<?> treeClass) throws ReflectiveOperationException {
        Field pages = treeClass.getDeclaredField("skillTreePages");
        pages.setAccessible(true);
        return ((Map<?, ?>) pages.get(root)).size();
    }

    private static Object defaultCategoryTexture(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> enumClass = Class.forName(DEFAULT_CATEGORY_TEXTURES, true, loader);
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object value = Enum.valueOf((Class<? extends Enum>) enumClass.asSubclass(Enum.class), "PASSIVE");
        return value;
    }

    private void repairMissingCategoryTextures(Class<?> treeClass, Object fallback) throws ReflectiveOperationException {
        Field pagesField = treeClass.getDeclaredField("skillTreePages");
        pagesField.setAccessible(true);
        int repaired = 0;
        for (Object page : ((Map<?, ?>) pagesField.get(root)).values()) {
            Field nodesField = page.getClass().getDeclaredField("treeNodes");
            nodesField.setAccessible(true);
            for (Object node : ((Map<?, ?>) nodesField.get(page)).values()) {
                Field textureField = node.getClass().getDeclaredField("categoryTexture");
                textureField.setAccessible(true);
                if (textureField.get(node) == null) {
                    textureField.set(node, fallback);
                    repaired++;
                }
            }
        }
        if (repaired > 0) {
            RpgMenuFramework.LOGGER.warn("[RPGMF] Epic Skills supplied {} node(s) without a category slot texture; "
                    + "using its PASSIVE slot frame so the official tree remains usable", repaired);
        }
    }

    private boolean waitForSync() {
        state = State.WAITING;
        nextRetry = System.nanoTime() + RETRY_NANOS;
        return true;
    }

    private boolean fail(Throwable exception) {
        state = State.FAILED;
        if (!failureLogged) {
            failureLogged = true;
            RpgMenuFramework.LOGGER.warn("[RPGMF] EpicSkills compat unavailable: {}", exception.toString(), exception);
        }
        return false;
    }

    private void drawStatus(GuiGraphics graphics, String key) {
        int width = viewport == null ? 1 : viewport.width();
        int height = viewport == null ? 1 : viewport.height();
        graphics.drawCenteredString(minecraft.font, Component.translatable(key), width / 2,
                Math.max(4, height / 2 - 4), 0xFFB9B1A4);
    }

    private static boolean isUnsynchronized(Throwable throwable) {
        return throwable instanceof java.util.NoSuchElementException
                || throwable instanceof IllegalStateException && throwable.getMessage() != null
                && throwable.getMessage().toLowerCase(java.util.Locale.ROOT).contains("attachment");
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable result = throwable;
        while (result.getCause() != null && result.getCause() != result) result = result.getCause();
        return result;
    }

    private enum State { NEW, WAITING, EMPTY, READY, FAILED }
    @FunctionalInterface private interface BooleanCall { boolean run(); }
    @FunctionalInterface private interface VoidCall { void run(); }
}
