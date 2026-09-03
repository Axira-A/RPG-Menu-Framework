package dev.rpgmenu.framework.common.equipment;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.equipment.EquipmentChangeResult;
import dev.rpgmenu.framework.api.equipment.EquipmentProvider;
import dev.rpgmenu.framework.api.equipment.EquipmentSlotView;
import dev.rpgmenu.framework.api.equipment.EquipmentTarget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Exposes the nine real vanilla hotbar slots to the character-panel layout without duplicating their data. */
public final class HotbarEquipmentProvider implements EquipmentProvider {
    public static final ResourceLocation ID = RpgMenuFramework.id("hotbar");
    public static final String SLOT_KEY = "hotbar";
    public static final int SLOT_COUNT = 9;

    @Override public ResourceLocation id() { return ID; }
    @Override public int priority() { return 990; }

    @Override
    public List<EquipmentSlotView> slots(Player player) {
        List<EquipmentSlotView> result = new ArrayList<>(SLOT_COUNT);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            result.add(new EquipmentSlotView(target(slot),
                    Component.translatable("equipment.rpgmenuframework.hotbar_slot", slot + 1),
                    player.getInventory().getItem(slot), true, ""));
        }
        return List.copyOf(result);
    }

    @Override
    public boolean canEquip(Player player, EquipmentTarget target, ItemStack stack) {
        return resolve(target) >= 0 && !stack.isEmpty() && stack.getCount() <= stack.getMaxStackSize();
    }

    @Override
    public boolean canUnequip(Player player, EquipmentTarget target) {
        int slot = resolve(target);
        return slot >= 0 && !player.getInventory().getItem(slot).isEmpty();
    }

    @Override
    public EquipmentChangeResult replace(ServerPlayer player, EquipmentTarget target,
                                         ItemStack replacement, boolean simulate) {
        int slot = resolve(target);
        if (slot < 0) return EquipmentChangeResult.rejected("message.rpgmenuframework.invalid_equipment_target");
        ItemStack previous = player.getInventory().getItem(slot).copy();
        if (!replacement.isEmpty() && !canEquip(player, target, replacement)) {
            return EquipmentChangeResult.rejected("message.rpgmenuframework.invalid_equipment_item");
        }
        if (!simulate) {
            player.getInventory().setItem(slot, replacement.copy());
            player.getInventory().setChanged();
        }
        return EquipmentChangeResult.accepted(previous);
    }

    @Override
    public boolean rollback(ServerPlayer player, EquipmentTarget target, ItemStack expectedCurrent, ItemStack previous) {
        int slot = resolve(target);
        if (slot < 0 || !sameStack(player.getInventory().getItem(slot), expectedCurrent)) return false;
        player.getInventory().setItem(slot, previous.copy());
        player.getInventory().setChanged();
        return true;
    }

    @Override
    public int backingInventorySlot(Player player, EquipmentTarget target) {
        return resolve(target);
    }

    public static EquipmentTarget target(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) throw new IllegalArgumentException("invalid hotbar slot");
        return new EquipmentTarget(ID, SLOT_KEY, slot);
    }

    public static int resolve(EquipmentTarget target) {
        if (target == null || !ID.equals(target.providerId()) || !SLOT_KEY.equals(target.slotKey())) return -1;
        return target.slotIndex() < SLOT_COUNT ? target.slotIndex() : -1;
    }

    private static boolean sameStack(ItemStack left, ItemStack right) {
        return left.getCount() == right.getCount()
                && (left.isEmpty() && right.isEmpty() || ItemStack.isSameItemSameComponents(left, right));
    }
}
