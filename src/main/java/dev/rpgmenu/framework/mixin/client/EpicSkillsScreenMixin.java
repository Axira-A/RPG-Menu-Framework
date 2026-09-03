package dev.rpgmenu.framework.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.rpgmenu.framework.client.map.EmbeddedMapScreenBridge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps the RPGMF content background instead of letting the embedded root replace the whole viewport backdrop. */
@Pseudo
@Mixin(targets = "com.yesman.epicskills.client.gui.screen.SkillTreeScreen", remap = false)
public abstract class EpicSkillsScreenMixin {
    @WrapOperation(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFFFFFF)V",
                    ordinal = 0),
            require = 0,
            remap = false
    )
    private void rpgmf$keepEmbeddedContentBackground(GuiGraphics graphics, ResourceLocation texture,
                                                      int left, int right, int top, int bottom, int blitOffset,
                                                      float minU, float maxU, float minV, float maxV,
                                                      float red, float green, float blue, float alpha,
                                                      Operation<Void> original) {
        if (!EmbeddedMapScreenBridge.isActive()) {
            original.call(graphics, texture, left, right, top, bottom, blitOffset,
                    minU, maxU, minV, maxV, red, green, blue, alpha);
        }
    }
}
