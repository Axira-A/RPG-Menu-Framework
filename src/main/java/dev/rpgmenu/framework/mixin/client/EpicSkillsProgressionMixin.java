package dev.rpgmenu.framework.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.rpgmenu.framework.client.compat.epicskills.EpicSkillsEmbeddedView;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Routes node-unlock responses to the embedded SkillInfoScreen so its official follow-up flow still runs. */
@Pseudo
@Mixin(targets = "com.yesman.epicskills.neoforge.attachment.SkillTreeProgression", remap = false)
public abstract class EpicSkillsProgressionMixin {
    @ModifyExpressionValue(
            method = {
                    "processSyncPacket(Lcom/yesman/epicskills/network/client/ClientBoundUnlockNode;)V",
                    "processSyncPacket(Lcom/yesman/epicskills/network/client/ClientBoundUnlockAchievedNode;)V"
            },
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;"),
            require = 0,
            remap = false
    )
    private Screen rpgmf$embeddedSkillInfoForUnlockSync(Screen ordinaryScreen) {
        return EpicSkillsEmbeddedView.packetScreen(ordinaryScreen);
    }
}
