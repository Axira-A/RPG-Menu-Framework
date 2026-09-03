package dev.rpgmenu.framework.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.rpgmenu.framework.client.compat.epicskills.EpicSkillsEmbeddedView;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Preserves Epic Skills' ability-point UI callback while its tree is embedded. */
@Pseudo
@Mixin(targets = "com.yesman.epicskills.network.EpicSkillsClientBoundPayloadHandler", remap = false)
public interface EpicSkillsPayloadHandlerMixin {
    @ModifyExpressionValue(
            method = "handleSetAbilityPoints",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;"),
            require = 0,
            remap = false
    )
    private static Screen rpgmf$embeddedSkillTreeForAbilitySync(Screen ordinaryScreen) {
        return EpicSkillsEmbeddedView.packetScreen(ordinaryScreen);
    }
}
