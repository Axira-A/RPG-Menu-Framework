package dev.rpgmenu.framework.client.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputRouterTest {
    @Test
    void worldInputRequiresWorldStateActiveWindowAndPlayer() {
        assertTrue(InputRouter.allowsWorldInput(InputRouter.State.WORLD, true, true));
        assertFalse(InputRouter.allowsWorldInput(InputRouter.State.TEXT_INPUT, true, true));
        assertFalse(InputRouter.allowsWorldInput(InputRouter.State.MODAL, true, true));
        assertFalse(InputRouter.allowsWorldInput(InputRouter.State.UI, true, true));
        assertFalse(InputRouter.allowsWorldInput(InputRouter.State.WORLD, false, true));
        assertFalse(InputRouter.allowsWorldInput(InputRouter.State.WORLD, true, false));
    }
}
