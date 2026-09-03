package dev.rpgmenu.framework.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.rpgmenu.framework.client.map.EmbeddedMapScreenBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Gives Xaero 1.21.1's real GuiMap viewport dimensions only at its logical map calculations. FBO allocation and
 * composition continue to use the real framebuffer dimensions: changing the composite extent moves the baked map
 * (including its mouse highlight) inside Xaero's fractional zoom matrix while map elements are rendered afterwards.
 */
@Pseudo
@Mixin(targets = "xaero.map.gui.GuiMap", remap = false)
public abstract class XaeroGuiMapMixin {
    @Unique private static final String rpgmf$RENDER = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V";
    @Unique private static final String rpgmf$WINDOW_WIDTH = "Lcom/mojang/blaze3d/platform/Window;getWidth()I";
    @Unique private static final String rpgmf$WINDOW_HEIGHT = "Lcom/mojang/blaze3d/platform/Window;getHeight()I";

    /*
     * GuiMap.render 1.45.0 direct Window dimension calls (zero-based):
     * 0 scale multiplier, 1 mouse centre, 2/3 native FBO size, 4 map pose centre, 5/6 visible bounds,
     * 7/8/9 edge fillers, 10 native FBO composite extent, 11 map-element viewport.
     * Calls 2/3/10 deliberately remain untouched.
     */
    @ModifyExpressionValue(
            method = rpgmf$RENDER,
            at = {
                    @At(value = "INVOKE", target = rpgmf$WINDOW_WIDTH, ordinal = 0),
                    @At(value = "INVOKE", target = rpgmf$WINDOW_WIDTH, ordinal = 1),
                    @At(value = "INVOKE", target = rpgmf$WINDOW_WIDTH, ordinal = 4),
                    @At(value = "INVOKE", target = rpgmf$WINDOW_WIDTH, ordinal = 5),
                    @At(value = "INVOKE", target = rpgmf$WINDOW_WIDTH, ordinal = 6),
                    @At(value = "INVOKE", target = rpgmf$WINDOW_WIDTH, ordinal = 7),
                    @At(value = "INVOKE", target = rpgmf$WINDOW_WIDTH, ordinal = 8),
                    @At(value = "INVOKE", target = rpgmf$WINDOW_WIDTH, ordinal = 9),
                    @At(value = "INVOKE", target = rpgmf$WINDOW_WIDTH, ordinal = 11)
            },
            require = 0,
            expect = 9,
            allow = 9,
            remap = false
    )
    private int rpgmf$viewportWidth(int original) {
        if (!EmbeddedMapScreenBridge.isActive()) return original;
        return positiveOrOriginal(EmbeddedMapScreenBridge.viewportFramebufferWidth(), original);
    }

    @ModifyExpressionValue(
            method = rpgmf$RENDER,
            at = {
                    @At(value = "INVOKE", target = rpgmf$WINDOW_HEIGHT, ordinal = 0),
                    @At(value = "INVOKE", target = rpgmf$WINDOW_HEIGHT, ordinal = 1),
                    @At(value = "INVOKE", target = rpgmf$WINDOW_HEIGHT, ordinal = 4),
                    @At(value = "INVOKE", target = rpgmf$WINDOW_HEIGHT, ordinal = 5),
                    @At(value = "INVOKE", target = rpgmf$WINDOW_HEIGHT, ordinal = 6),
                    @At(value = "INVOKE", target = rpgmf$WINDOW_HEIGHT, ordinal = 7),
                    @At(value = "INVOKE", target = rpgmf$WINDOW_HEIGHT, ordinal = 8),
                    @At(value = "INVOKE", target = rpgmf$WINDOW_HEIGHT, ordinal = 9),
                    @At(value = "INVOKE", target = rpgmf$WINDOW_HEIGHT, ordinal = 11)
            },
            require = 0,
            expect = 9,
            allow = 9,
            remap = false
    )
    private int rpgmf$viewportHeight(int original) {
        if (!EmbeddedMapScreenBridge.isActive()) return original;
        return positiveOrOriginal(EmbeddedMapScreenBridge.viewportFramebufferHeight(), original);
    }

    @Unique
    private static int positiveOrOriginal(int requested, int original) {
        return requested > 0 ? requested : original;
    }
}
