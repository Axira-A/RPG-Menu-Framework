package dev.rpgmenu.framework.api.equipment;

import net.minecraft.resources.ResourceLocation;

/** Stable identity for one provider-owned equipment slot. Dynamic providers use slotKey plus index. */
public record EquipmentTarget(ResourceLocation providerId, String slotKey, int slotIndex) {
    public static final int MAX_SLOT_KEY_LENGTH = 64;

    public EquipmentTarget {
        if (providerId == null) throw new IllegalArgumentException("providerId");
        if (slotKey == null || slotKey.isBlank() || slotKey.length() > MAX_SLOT_KEY_LENGTH) {
            throw new IllegalArgumentException("invalid slotKey");
        }
        if (slotIndex < 0 || slotIndex > 4_096) throw new IllegalArgumentException("invalid slotIndex");
    }
}
