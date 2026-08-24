package dev.rpgmenu.framework.common.compat.curios;

import dev.rpgmenu.framework.api.equipment.EquipmentTarget;

import java.util.Optional;

/** Type-safe Curios identity. Display names are never used for transactions. */
public record CuriosEquipmentTarget(String slotIdentifier, int slotIndex) {
    public CuriosEquipmentTarget {
        if (slotIdentifier == null || slotIdentifier.isBlank()
                || slotIdentifier.length() > EquipmentTarget.MAX_SLOT_KEY_LENGTH) {
            throw new IllegalArgumentException("invalid Curios slot identifier");
        }
        if (slotIndex < 0 || slotIndex > 4_096) throw new IllegalArgumentException("invalid Curios slot index");
    }

    public EquipmentTarget asEquipmentTarget() {
        return new EquipmentTarget(CuriosEquipmentProvider.ID, slotIdentifier, slotIndex);
    }

    public static Optional<CuriosEquipmentTarget> from(EquipmentTarget target) {
        if (target == null || !CuriosEquipmentProvider.ID.equals(target.providerId())) return Optional.empty();
        return Optional.of(new CuriosEquipmentTarget(target.slotKey(), target.slotIndex()));
    }
}
