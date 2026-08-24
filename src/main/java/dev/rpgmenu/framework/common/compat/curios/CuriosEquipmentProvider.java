package dev.rpgmenu.framework.common.compat.curios;

import dev.rpgmenu.framework.api.equipment.EquipmentChangeResult;
import dev.rpgmenu.framework.api.equipment.EquipmentProvider;
import dev.rpgmenu.framework.api.equipment.EquipmentSlotView;
import dev.rpgmenu.framework.api.equipment.EquipmentTarget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Optional Curios 9.x adapter. This class is loaded only after the curios mod-id check succeeds. */
public final class CuriosEquipmentProvider implements EquipmentProvider {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("curios", "equipment");

    @Override public ResourceLocation id() { return ID; }
    @Override public int priority() { return 900; }

    @Override
    public List<EquipmentSlotView> slots(Player player) {
        return CuriosApi.getCuriosInventory(player).map(handler -> {
            List<EquipmentSlotView> result = new ArrayList<>();
            handler.getCurios().entrySet().stream()
                    .sorted(Comparator.<Map.Entry<String, ICurioStacksHandler>>comparingInt(
                            entry -> CuriosApi.getSlot(entry.getKey(), player.level())
                            .map(type -> type.getOrder()).orElse(Integer.MAX_VALUE))
                            .thenComparing(java.util.Map.Entry::getKey))
                    .forEach(entry -> appendSlots(player, entry.getKey(), entry.getValue(), result));
            return List.copyOf(result);
        }).orElseGet(List::of);
    }

    @Override
    public boolean canEquip(Player player, EquipmentTarget target, ItemStack stack) {
        return !stack.isEmpty() && resolve(player, target, true)
                .map(slot -> slot.handler().isItemValid(slot.target().slotIndex(), stack))
                .orElse(false);
    }

    @Override
    public boolean canUnequip(Player player, EquipmentTarget target) {
        return resolve(player, target, true).map(CuriosEquipmentProvider::canFullyExtract).orElse(false);
    }

    @Override
    public EquipmentChangeResult replace(ServerPlayer player, EquipmentTarget target, ItemStack replacement, boolean simulate) {
        return resolve(player, target, true)
                .map(slot -> replace(slot, replacement, simulate))
                .orElseGet(() -> EquipmentChangeResult.rejected("message.rpgmenuframework.invalid_equipment_target"));
    }

    @Override
    public boolean rollback(ServerPlayer player, EquipmentTarget target, ItemStack expectedCurrent, ItemStack previous) {
        return resolve(player, target, false)
                .map(slot -> {
                    ItemStack current = slot.handler().getStackInSlot(slot.target().slotIndex());
                    if (!sameStack(current, expectedCurrent)) return false;
                    // Rollback is the only deliberate restriction bypass. The coordinator calls it in the
                    // same server tick and only after the compare above proves this transaction still owns the slot.
                    slot.handler().setStackInSlot(slot.target().slotIndex(), previous.copy());
                    return true;
                }).orElse(false);
    }

    private static EquipmentChangeResult replace(ResolvedSlot slot, ItemStack replacement, boolean simulate) {
        int index = slot.target().slotIndex();
        IDynamicStackHandler handler = slot.handler();
        ItemStack previous = handler.getStackInSlot(index).copy();
        if (!replacement.isEmpty() && !handler.isItemValid(index, replacement)) {
            return EquipmentChangeResult.rejected("message.rpgmenuframework.invalid_equipment_item");
        }

        if (simulate) {
            if (!previous.isEmpty() && !canFullyExtract(slot)) {
                return EquipmentChangeResult.rejected("message.rpgmenuframework.curio_cannot_unequip");
            }
            return EquipmentChangeResult.accepted(previous);
        }

        ItemStack extracted = ItemStack.EMPTY;
        if (!previous.isEmpty()) {
            // Curios 9.5.1 builds the real SlotContext internally here and applies CurioCanUnequipEvent,
            // ICurio#canUnequip, and PREVENT_ARMOR_CHANGE before mutating the handler.
            extracted = handler.extractItem(index, previous.getCount(), false);
            if (!sameStack(extracted, previous)) {
                if (!extracted.isEmpty()) handler.setStackInSlot(index, previous.copy());
                return EquipmentChangeResult.rejected("message.rpgmenuframework.curio_cannot_unequip");
            }
        }

        if (!replacement.isEmpty()) {
            ItemStack remainder = handler.insertItem(index, replacement, false);
            if (!remainder.isEmpty()) {
                // The item was already removed from its InventorySource by the coordinator. Restore this
                // provider-local change before returning rejection so the outer transaction can restore it.
                handler.setStackInSlot(index, previous.copy());
                return EquipmentChangeResult.rejected("message.rpgmenuframework.invalid_equipment_item");
            }
        }
        return EquipmentChangeResult.accepted(extracted.isEmpty() ? previous : extracted);
    }

    private static void appendSlots(Player player, String identifier, ICurioStacksHandler stacks,
                                    List<EquipmentSlotView> output) {
        if (!stacks.isVisible()) return;
        for (int index = 0; index < stacks.getStacks().getSlots(); index++) {
            EquipmentTarget target = new CuriosEquipmentTarget(identifier, index).asEquipmentTarget();
            boolean active = handlerActive(player, target);
            output.add(new EquipmentSlotView(target, dynamicTitle(identifier, index),
                    stacks.getStacks().getStackInSlot(index), active,
                    active ? "" : "message.rpgmenuframework.equipment_slot_disabled"));
        }
    }

    private static boolean handlerActive(Player player, EquipmentTarget target) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.isSlotActive(target.slotKey(), target.slotIndex())).orElse(false);
    }

    private static Component dynamicTitle(String identifier, int index) {
        String readable = identifier.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Component.translatableWithFallback("curios.identifier." + identifier, readable)
                .copy().append(" " + (index + 1));
    }

    private static boolean canFullyExtract(ResolvedSlot slot) {
        int index = slot.target().slotIndex();
        ItemStack current = slot.handler().getStackInSlot(index);
        if (current.isEmpty()) return false;
        ItemStack simulated = slot.handler().extractItem(index, current.getCount(), true);
        return sameStack(simulated, current);
    }

    private static Optional<ResolvedSlot> resolve(Player player, EquipmentTarget target, boolean requireActive) {
        return CuriosEquipmentTarget.from(target).flatMap(curiosTarget ->
                CuriosApi.getCuriosInventory(player).flatMap(inventory ->
                        inventory.getStacksHandler(curiosTarget.slotIdentifier()).flatMap(stacks -> {
                            IDynamicStackHandler handler = stacks.getStacks();
                            int currentSlotCount = handler.getSlots();
                            if (curiosTarget.slotIndex() >= currentSlotCount) return Optional.empty();
                            if (requireActive && !inventory.isSlotActive(
                                    curiosTarget.slotIdentifier(), curiosTarget.slotIndex())) return Optional.empty();
                            return Optional.of(new ResolvedSlot(curiosTarget, handler));
                        })));
    }

    private static boolean sameStack(ItemStack left, ItemStack right) {
        return left.getCount() == right.getCount()
                && (left.isEmpty() && right.isEmpty() || ItemStack.isSameItemSameComponents(left, right));
    }

    private record ResolvedSlot(CuriosEquipmentTarget target, IDynamicStackHandler handler) {}
}
