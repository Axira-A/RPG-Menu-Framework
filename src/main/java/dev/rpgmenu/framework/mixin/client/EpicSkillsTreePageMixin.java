package dev.rpgmenu.framework.mixin.client;

import dev.rpgmenu.framework.client.map.EmbeddedMapScreenBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Translates Epic Skills' local tree scissor into the RPG menu's GUI-logical viewport. */
@Pseudo
@Mixin(targets = "com.yesman.epicskills.client.gui.screen.SkillTreeScreen$TreePage", remap = false)
public abstract class EpicSkillsTreePageMixin {
    @ModifyArgs(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;enableScissor(IIII)V"),
            require = 0,
            remap = false
    )
    private void rpgmf$translateEmbeddedScissor(Args args) {
        if (!EmbeddedMapScreenBridge.isActive()) return;
        int x = EmbeddedMapScreenBridge.viewportGuiX();
        int y = EmbeddedMapScreenBridge.viewportGuiY();
        args.set(0, args.<Integer>get(0) + x);
        args.set(1, args.<Integer>get(1) + y);
        args.set(2, args.<Integer>get(2) + x);
        args.set(3, args.<Integer>get(3) + y);
    }
}
