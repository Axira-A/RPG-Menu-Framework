package dev.rpgmenu.framework.mixin.client;

import dev.rpgmenu.framework.client.map.EmbeddedMapScreenBridge;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplies Xaero's raw-mouse path with viewport-local physical pixels while embedded. */
@Pseudo
@Mixin(targets = "xaero.map.misc.Misc", remap = false)
public abstract class XaeroMiscMixin {
    @Inject(method = "getMouseX", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void rpgmf$virtualMouseX(Minecraft minecraft, boolean raw,
                                            CallbackInfoReturnable<Double> callback) {
        double mouseX = EmbeddedMapScreenBridge.virtualMouseX();
        if (Double.isFinite(mouseX)) callback.setReturnValue(mouseX);
    }

    @Inject(method = "getMouseY", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void rpgmf$virtualMouseY(Minecraft minecraft, boolean raw,
                                            CallbackInfoReturnable<Double> callback) {
        double mouseY = EmbeddedMapScreenBridge.virtualMouseY();
        if (Double.isFinite(mouseY)) callback.setReturnValue(mouseY);
    }
}
