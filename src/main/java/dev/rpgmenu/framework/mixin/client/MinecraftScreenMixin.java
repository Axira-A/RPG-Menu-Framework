package dev.rpgmenu.framework.mixin.client;

import dev.rpgmenu.framework.client.map.EmbeddedMapScreenBridge;
import dev.rpgmenu.framework.client.compat.epicskills.EpicSkillsEmbeddedView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cancels setScreen only during a bounded embedded-map callback; all ordinary screen transitions are untouched. */
@Mixin(Minecraft.class)
public abstract class MinecraftScreenMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void rpgmf$captureEmbeddedMapChild(Screen nextScreen, CallbackInfo callback) {
        if (EmbeddedMapScreenBridge.capture(nextScreen)
                || EpicSkillsEmbeddedView.captureOutOfBandTransition(nextScreen)) callback.cancel();
    }
}
