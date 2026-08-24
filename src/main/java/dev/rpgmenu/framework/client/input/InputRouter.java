package dev.rpgmenu.framework.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.rpgmenu.framework.client.screen.RpgMenuScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Routes only normal movement mappings through a non-pausing menu; no packets are fabricated. */
public final class InputRouter {
    public enum State { WORLD, UI, TEXT_INPUT, MODAL, CONTROLLER }
    private static volatile State state = State.UI;
    private static final Map<Integer, Integer> SCANCODE_TO_KEY = new ConcurrentHashMap<>();
    private static boolean routing;

    private InputRouter() {}
    public static State state() { return state; }

    public static void tick(Minecraft minecraft) {
        if (!(minecraft.screen instanceof RpgMenuScreen screen)) {
            release(minecraft);
            state = State.UI;
            return;
        }
        state = screen.inputState();
        boolean allow = allowsWorldInput(state, minecraft.isWindowActive(), minecraft.player != null);
        routing = true;
        update(minecraft, minecraft.options.keyUp, allow);
        update(minecraft, minecraft.options.keyDown, allow);
        update(minecraft, minecraft.options.keyLeft, allow);
        update(minecraft, minecraft.options.keyRight, allow);
        update(minecraft, minecraft.options.keyJump, allow);
        update(minecraft, minecraft.options.keyShift, allow);
    }

    private static void update(Minecraft minecraft, KeyMapping mapping, boolean allow) {
        boolean down = allow && physicallyDown(minecraft, mapping.getKey());
        mapping.setDown(down);
    }

    public static void release(Minecraft minecraft) {
        if (!routing || minecraft == null) return;
        minecraft.options.keyUp.setDown(false);
        minecraft.options.keyDown.setDown(false);
        minecraft.options.keyLeft.setDown(false);
        minecraft.options.keyRight.setDown(false);
        minecraft.options.keyJump.setDown(false);
        minecraft.options.keyShift.setDown(false);
        routing = false;
    }

    static boolean allowsWorldInput(State inputState, boolean windowActive, boolean playerPresent) {
        return inputState == State.WORLD && windowActive && playerPresent;
    }

    private static boolean physicallyDown(Minecraft minecraft, InputConstants.Key key) {
        long window = minecraft.getWindow().getWindow();
        return switch (key.getType()) {
            case KEYSYM -> key.getValue() != InputConstants.UNKNOWN.getValue()
                    && InputConstants.isKeyDown(window, key.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
            case SCANCODE -> {
                int keyCode = SCANCODE_TO_KEY.computeIfAbsent(key.getValue(), InputRouter::keyCodeForScanCode);
                yield keyCode != GLFW.GLFW_KEY_UNKNOWN && InputConstants.isKeyDown(window, keyCode);
            }
        };
    }

    private static int keyCodeForScanCode(int scanCode) {
        for (int keyCode = GLFW.GLFW_KEY_SPACE; keyCode <= GLFW.GLFW_KEY_LAST; keyCode++) {
            if (GLFW.glfwGetKeyScancode(keyCode) == scanCode) return keyCode;
        }
        return GLFW.GLFW_KEY_UNKNOWN;
    }
}
