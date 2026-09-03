package dev.rpgmenu.framework.common.gametest;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.common.equipment.VanillaEquipmentProvider;
import dev.rpgmenu.framework.common.equipment.HotbarEquipmentProvider;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import dev.rpgmenu.framework.api.inventory.QuickEquipKind;
import dev.rpgmenu.framework.api.inventory.QuickSlotGroup;
import dev.rpgmenu.framework.api.inventory.QuickSlotTarget;
import dev.rpgmenu.framework.common.inventory.QuickEquipResolver;
import dev.rpgmenu.framework.common.inventory.QuickSlotTargets;
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

    @GameTest(templateNamespace = "minecraft", template = "bastion/mobs/empty")
    @SuppressWarnings("removal")
    public static void hotbarTargetsUseTheNineRealInventorySlots(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        HotbarEquipmentProvider provider = new HotbarEquipmentProvider();
        helper.assertTrue(provider.slots(player).size() == 9, "hotbar provider must expose exactly nine slots");
        for (int slot = 0; slot < 9; slot++) {
            helper.assertTrue(provider.backingInventorySlot(player, HotbarEquipmentProvider.target(slot)) == slot,
                    "hotbar target must retain its vanilla slot index");
        }
        for (int index = 0; index < 4; index++) {
            helper.assertTrue(HotbarEquipmentProvider.resolve(QuickSlotTargets.equipmentTarget(
                    new QuickSlotTarget(QuickSlotGroup.MAIN_HAND, index))) == index,
                    "main-hand quick group must map to hotbar 0..3");
        }
        for (int index = 0; index < 5; index++) {
            helper.assertTrue(HotbarEquipmentProvider.resolve(QuickSlotTargets.equipmentTarget(
                    new QuickSlotTarget(QuickSlotGroup.ITEM_BAR, index))) == index + 4,
                    "item quickbar must map to hotbar 4..8");
        }
        helper.assertTrue(QuickSlotTargets.equipmentTarget(new QuickSlotTarget(QuickSlotGroup.OFF_HAND, 0))
                        .equals(VanillaEquipmentProvider.OFFHAND),
                "offhand 1 must map to vanilla offhand");
        for (int index = 1; index < 4; index++) {
            var offhand = QuickSlotTargets.equipmentTarget(new QuickSlotTarget(QuickSlotGroup.OFF_HAND, index));
            helper.assertTrue(offhand.providerId().equals(QuickSlotTargets.MORE_OFFHAND_PROVIDER)
                            && offhand.slotIndex() == index - 1,
                    "offhand 2..4 must preserve the MoreOffhandSlots handler index");
        }
        ItemStack stack = new ItemStack(Items.STONE, 32);
        var changed = provider.replace(player, HotbarEquipmentProvider.target(7), stack, false);
        helper.assertTrue(changed.accepted() && player.getInventory().getItem(7).getCount() == 32,
                "hotbar replacement must mutate the real vanilla inventory slot");
        helper.assertTrue(provider.rollback(player, HotbarEquipmentProvider.target(7), stack, changed.previous()),
                "hotbar replacement must support compare-and-restore rollback");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "bastion/mobs/empty")
    @SuppressWarnings("removal")
    public static void quickEquipRoutesAndItemBarRestrictions(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(QuickEquipResolver.classify(player, new ItemStack(Items.IRON_SWORD)) == QuickEquipKind.WEAPON,
                "sword must auto-route to the main-hand group");
        helper.assertTrue(QuickEquipResolver.classify(player, new ItemStack(Items.IRON_PICKAXE)) == QuickEquipKind.TOOL,
                "tool must auto-route to the main-hand group");
        helper.assertTrue(QuickEquipResolver.classify(player, new ItemStack(Items.SHIELD)) == QuickEquipKind.SHIELD,
                "shield must auto-route to the offhand group");
        helper.assertFalse(QuickEquipResolver.canPlace(player, new ItemStack(Items.IRON_SWORD),
                        new QuickSlotTarget(QuickSlotGroup.ITEM_BAR, 0), false),
                "weapons must be rejected by the item bar even for exact drops");
        helper.assertFalse(QuickEquipResolver.canPlace(player, new ItemStack(Items.SHIELD),
                        new QuickSlotTarget(QuickSlotGroup.ITEM_BAR, 0), false),
                "shields must be rejected by the item bar even for exact drops");
        helper.assertTrue(QuickEquipResolver.canPlace(player, new ItemStack(Items.IRON_PICKAXE),
                        new QuickSlotTarget(QuickSlotGroup.ITEM_BAR, 0), false),
                "tools remain legal in the item bar for exact drops");
        helper.succeed();
    }
}
