package dev.rpgmenu.framework.api.equipment;

import net.minecraft.world.item.ItemStack;

/** Result of one provider-local simulated or committed slot replacement. */
public record EquipmentChangeResult(boolean accepted, ItemStack previous, String messageKey) {
    public EquipmentChangeResult {
        previous = previous == null ? ItemStack.EMPTY : previous.copy();
        messageKey = messageKey == null ? "" : messageKey;
    }

    public static EquipmentChangeResult accepted(ItemStack previous) {
        return new EquipmentChangeResult(true, previous, "");
    }

    public static EquipmentChangeResult rejected(String messageKey) {
        return new EquipmentChangeResult(false, ItemStack.EMPTY, messageKey);
    }
}
