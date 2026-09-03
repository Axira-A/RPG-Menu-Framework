package dev.rpgmenu.framework.mixin.client;

import dev.rpgmenu.framework.client.compat.ftbquests.FtbEmbeddedScreenBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures only FTB child-screen transitions made while RPGMF is dispatching embedded quest input/ticks. */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftblibrary.ui.BaseScreen", remap = false)
public abstract class FtbBaseScreenMixin {
    @Inject(method = "openGui", at = @At("HEAD"), cancellable = true, require = 0)
    private void rpgmf$captureEmbeddedOpen(CallbackInfo callback) {
        if (FtbEmbeddedScreenBridge.captureOpen(this)) callback.cancel();
    }

    @Inject(method = "closeGui", at = @At("HEAD"), cancellable = true, require = 0)
    private void rpgmf$captureEmbeddedClose(boolean openPrevScreen, CallbackInfo callback) {
        if (FtbEmbeddedScreenBridge.captureClose(this)) callback.cancel();
    }
}
