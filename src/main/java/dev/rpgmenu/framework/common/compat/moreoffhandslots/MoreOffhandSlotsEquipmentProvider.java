package dev.rpgmenu.framework.common.compat.moreoffhandslots;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.equipment.EquipmentChangeResult;
import dev.rpgmenu.framework.api.equipment.EquipmentProvider;
import dev.rpgmenu.framework.api.equipment.EquipmentSlotView;
import dev.rpgmenu.framework.api.equipment.EquipmentTarget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Soft integration with More Offhand Slots 21.1.x. The optional class is linked by name only; all mutations
 * operate on its real SlotLib-backed ItemStackHandler on the logical server.
 */
public final class MoreOffhandSlotsEquipmentProvider implements EquipmentProvider {
    public static final ResourceLocation ID = RpgMenuFramework.id("moreoffhandslots");
    public static final String SLOT_KEY = "extra_offhand";
    private final Method getOffhandStackHandler;

    public MoreOffhandSlotsEquipmentProvider() {
        try {
            Class<?> api = Class.forName("net.akkynaa.moreoffhandslots.api.OffhandInventory");
            getOffhandStackHandler = api.getMethod("getOffhandStackHandler", Player.class);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("More Offhand Slots API is incompatible", exception);
        }
    }

    @Override public ResourceLocation id() { return ID; }
    @Override public int priority() { return 850; }

    @Override
    public List<EquipmentSlotView> slots(Player player) {
        IItemHandlerModifiable handler = handler(player);
        List<EquipmentSlotView> result = new ArrayList<>(handler.getSlots());
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            result.add(new EquipmentSlotView(target(slot),
                    Component.translatable("equipment.rpgmenuframework.extra_offhand", slot + 2),
                    handler.getStackInSlot(slot), true, ""));
        }
        return List.copyOf(result);
    }

    @Override
    public boolean canEquip(Player player, EquipmentTarget target, ItemStack stack) {
        int slot = resolve(target);
        if (slot < 0 || stack.isEmpty()) return false;
        IItemHandlerModifiable handler = handler(player);
        return slot < handler.getSlots() && handler.isItemValid(slot, stack)
                && stack.getCount() <= handler.getSlotLimit(slot);
    }

    @Override
    public boolean canUnequip(Player player, EquipmentTarget target) {
        int slot = resolve(target);
        if (slot < 0) return false;
        IItemHandlerModifiable handler = handler(player);
        if (slot >= handler.getSlots()) return false;
        return !handler.getStackInSlot(slot).isEmpty();
    }

    @Override
    public EquipmentChangeResult replace(ServerPlayer player, EquipmentTarget target,
                                         ItemStack replacement, boolean simulate) {
        int slot = resolve(target);
        IItemHandlerModifiable handler = handler(player);
        if (slot < 0 || slot >= handler.getSlots()) {
            return EquipmentChangeResult.rejected("message.rpgmenuframework.invalid_equipment_target");
        }
        ItemStack previous = handler.getStackInSlot(slot).copy();
        if (!replacement.isEmpty() && !canEquip(player, target, replacement)) {
            return EquipmentChangeResult.rejected("message.rpgmenuframework.invalid_equipment_item");
        }
        if (replacement.isEmpty() && !previous.isEmpty() && !canUnequip(player, target)) {
            return EquipmentChangeResult.rejected("message.rpgmenuframework.equipment_locked");
        }
        if (!simulate) handler.setStackInSlot(slot, replacement.copy());
        return EquipmentChangeResult.accepted(previous);
    }

    @Override
    public boolean rollback(ServerPlayer player, EquipmentTarget target, ItemStack expectedCurrent, ItemStack previous) {
        int slot = resolve(target);
        IItemHandlerModifiable handler = handler(player);
        if (slot < 0 || slot >= handler.getSlots() || !sameStack(handler.getStackInSlot(slot), expectedCurrent)) {
            return false;
        }
        handler.setStackInSlot(slot, previous.copy());
        return true;
    }

    private IItemHandlerModifiable handler(Player player) {
        try {
            Object result = getOffhandStackHandler.invoke(null, player);
            if (result instanceof IItemHandlerModifiable handler) return handler;
            throw new IllegalStateException("More Offhand Slots returned a non-modifiable handler");
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Could not access More Offhand Slots player inventory", exception);
        }
    }

    public static EquipmentTarget target(int slot) {
        return new EquipmentTarget(ID, SLOT_KEY, slot);
    }

    public static int resolve(EquipmentTarget target) {
        return target != null && ID.equals(target.providerId()) && SLOT_KEY.equals(target.slotKey())
                ? target.slotIndex() : -1;
    }

    private static boolean sameStack(ItemStack left, ItemStack right) {
        return left.getCount() == right.getCount()
                && (left.isEmpty() && right.isEmpty() || ItemStack.isSameItemSameComponents(left, right));
    }
}
