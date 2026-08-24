package dev.rpgmenu.framework.common.gametest;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.common.equipment.VanillaEquipmentProvider;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(RpgMenuFramework.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "bastion/mobs/empty")
    @SuppressWarnings("removal")
    public static void vanillaTargetsAndRollback(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        VanillaEquipmentProvider provider = new VanillaEquipmentProvider();

        helper.assertTrue(provider.canEquip(player, VanillaEquipmentProvider.MAINHAND,
                new ItemStack(Items.IRON_SWORD)), "sword must target main hand");
        helper.assertTrue(provider.canEquip(player, VanillaEquipmentProvider.OFFHAND,
                new ItemStack(Items.SHIELD)), "shield must target offhand");
        helper.assertTrue(provider.canEquip(player, VanillaEquipmentProvider.HEAD,
                new ItemStack(Items.IRON_HELMET)), "helmet must target head");
        helper.assertFalse(provider.canEquip(player, VanillaEquipmentProvider.CHEST,
                new ItemStack(Items.STONE)), "ordinary blocks must not enter armor slots");

        ItemStack iron = new ItemStack(Items.IRON_SWORD);
        ItemStack diamond = new ItemStack(Items.DIAMOND_SWORD);
        helper.assertTrue(provider.replace(player, VanillaEquipmentProvider.MAINHAND, iron, false).accepted(),
                "initial main-hand write must succeed");
        var changed = provider.replace(player, VanillaEquipmentProvider.MAINHAND, diamond, false);
        helper.assertTrue(changed.accepted() && ItemStack.isSameItemSameComponents(changed.previous(), iron),
                "replacement must return the exact previous item");
        helper.assertTrue(provider.rollback(player, VanillaEquipmentProvider.MAINHAND, diamond, changed.previous()),
                "compare-and-restore rollback must succeed");
        helper.assertTrue(ItemStack.isSameItemSameComponents(player.getMainHandItem(), iron),
                "rollback must restore the previous main-hand item");
        helper.succeed();
    }
}
