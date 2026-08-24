package dev.rpgmenu.framework.common.equipment;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.equipment.EquipmentProvider;
import dev.rpgmenu.framework.api.equipment.EquipmentChangeResult;
import dev.rpgmenu.framework.api.equipment.EquipmentSlotView;
import dev.rpgmenu.framework.api.equipment.EquipmentTarget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import java.util.List;
import java.util.Map;

public final class VanillaEquipmentProvider implements EquipmentProvider {
    public static final ResourceLocation ID = RpgMenuFramework.id("vanilla");
    public static final EquipmentTarget MAINHAND = target("mainhand");
    public static final EquipmentTarget OFFHAND = target("offhand");
    public static final EquipmentTarget HEAD = target("head");
    public static final EquipmentTarget CHEST = target("chest");
    public static final EquipmentTarget LEGS = target("legs");
    public static final EquipmentTarget FEET = target("feet");
    private static final Map<String, EquipmentSlot> SLOTS = Map.of(
            "mainhand", EquipmentSlot.MAINHAND, "offhand", EquipmentSlot.OFFHAND,
            "head", EquipmentSlot.HEAD, "chest", EquipmentSlot.CHEST,
            "legs", EquipmentSlot.LEGS, "feet", EquipmentSlot.FEET);
    @Override public ResourceLocation id() { return ID; }
    @Override public int priority() { return 1_000; }

    @Override
    public List<EquipmentSlotView> slots(Player player) {
        return List.of(view(player, MAINHAND), view(player, OFFHAND), view(player, HEAD),
                view(player, CHEST), view(player, LEGS), view(player, FEET));
    }

    @Override
    public boolean canEquip(Player player, EquipmentTarget target, ItemStack stack) {
        EquipmentSlot slot = resolve(target);
        return slot != null && !stack.isEmpty() && player.getEquipmentSlotForItem(stack) == slot;
    }

    @Override
    public boolean canUnequip(Player player, EquipmentTarget target) {
        EquipmentSlot slot = resolve(target);
        if (slot == null) return false;
        ItemStack present = player.getItemBySlot(slot);
        return !present.isEmpty() && (player.getAbilities().instabuild
                || !EnchantmentHelper.has(present, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE));
    }

    @Override
    public EquipmentChangeResult replace(ServerPlayer player, EquipmentTarget target, ItemStack replacement, boolean simulate) {
        EquipmentSlot slot = resolve(target);
        if (slot == null) return EquipmentChangeResult.rejected("message.rpgmenuframework.invalid_equipment_target");
        ItemStack previous = player.getItemBySlot(slot).copy();
        if (!replacement.isEmpty() && !canEquip(player, target, replacement)) {
            return EquipmentChangeResult.rejected("message.rpgmenuframework.invalid_equipment_item");
        }
        if (replacement.isEmpty() && !previous.isEmpty() && !canUnequip(player, target)) {
            return EquipmentChangeResult.rejected("message.rpgmenuframework.equipment_locked");
        }
        if (!simulate) player.setItemSlot(slot, replacement.copy());
        return EquipmentChangeResult.accepted(previous);
    }

    @Override
    public boolean rollback(ServerPlayer player, EquipmentTarget target, ItemStack expectedCurrent, ItemStack previous) {
        EquipmentSlot slot = resolve(target);
        if (slot == null || !sameStack(player.getItemBySlot(slot), expectedCurrent)) return false;
        player.setItemSlot(slot, previous.copy());
        return true;
    }

    @Override
    public int backingInventorySlot(Player player, EquipmentTarget target) {
        return MAINHAND.equals(target) ? player.getInventory().selected : -1;
    }

    private static EquipmentSlotView view(Player player, EquipmentTarget target) {
        String path = target.slotKey();
        return new EquipmentSlotView(target, Component.translatable("equipment.rpgmenuframework." + path),
                player.getItemBySlot(SLOTS.get(path)), true, "");
    }

    private static EquipmentSlot resolve(EquipmentTarget target) {
        if (target == null || !ID.equals(target.providerId()) || target.slotIndex() != 0) return null;
        return SLOTS.get(target.slotKey());
    }

    private static EquipmentTarget target(String path) {
        return new EquipmentTarget(ID, path, 0);
    }

    private static boolean sameStack(ItemStack left, ItemStack right) {
        return left.getCount() == right.getCount()
                && (left.isEmpty() && right.isEmpty() || ItemStack.isSameItemSameComponents(left, right));
    }
}
