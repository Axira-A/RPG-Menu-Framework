package dev.rpgmenu.framework.common.inventory;

import dev.rpgmenu.framework.api.equipment.EquipmentTarget;
import dev.rpgmenu.framework.api.inventory.QuickSlotGroup;
import dev.rpgmenu.framework.api.inventory.QuickSlotTarget;
import dev.rpgmenu.framework.common.equipment.HotbarEquipmentProvider;
import dev.rpgmenu.framework.common.equipment.VanillaEquipmentProvider;
import dev.rpgmenu.framework.common.compat.moreoffhandslots.MoreOffhandSlotsEquipmentProvider;

import java.util.Optional;

/** Exact, reversible mapping between semantic quick slots and their real provider-local targets. */
public final class QuickSlotTargets {
    public static final int MAIN_HAND_COUNT = 4;
    public static final int ITEM_BAR_COUNT = 5;
    public static final String MORE_OFFHAND_SLOT_KEY = MoreOffhandSlotsEquipmentProvider.SLOT_KEY;
    public static final net.minecraft.resources.ResourceLocation MORE_OFFHAND_PROVIDER =
            MoreOffhandSlotsEquipmentProvider.ID;

    private QuickSlotTargets() {}

    public static EquipmentTarget equipmentTarget(QuickSlotTarget target) {
        if (target == null) return null;
        return switch (target.group()) {
            case MAIN_HAND -> target.index() < MAIN_HAND_COUNT
                    ? HotbarEquipmentProvider.target(target.index()) : null;
            case ITEM_BAR -> target.index() < ITEM_BAR_COUNT
                    ? HotbarEquipmentProvider.target(MAIN_HAND_COUNT + target.index()) : null;
            case OFF_HAND -> target.index() == 0 ? VanillaEquipmentProvider.OFFHAND
                    : MoreOffhandSlotsEquipmentProvider.target(target.index() - 1);
        };
    }

    public static Optional<QuickSlotTarget> fromEquipmentTarget(EquipmentTarget target) {
        int hotbar = HotbarEquipmentProvider.resolve(target);
        if (hotbar >= 0 && hotbar < MAIN_HAND_COUNT) {
            return Optional.of(new QuickSlotTarget(QuickSlotGroup.MAIN_HAND, hotbar));
        }
        if (hotbar >= MAIN_HAND_COUNT && hotbar < HotbarEquipmentProvider.SLOT_COUNT) {
            return Optional.of(new QuickSlotTarget(QuickSlotGroup.ITEM_BAR, hotbar - MAIN_HAND_COUNT));
        }
        if (VanillaEquipmentProvider.OFFHAND.equals(target)) {
            return Optional.of(new QuickSlotTarget(QuickSlotGroup.OFF_HAND, 0));
        }
        if (target != null && MORE_OFFHAND_PROVIDER.equals(target.providerId())
                && MORE_OFFHAND_SLOT_KEY.equals(target.slotKey())) {
            return Optional.of(new QuickSlotTarget(QuickSlotGroup.OFF_HAND, target.slotIndex() + 1));
        }
        return Optional.empty();
    }
}
