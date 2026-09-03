package dev.rpgmenu.framework.client.input;

import dev.rpgmenu.framework.api.equipment.EquipmentTarget;

public record EquipmentSelectionContext(EquipmentTarget target, int originEquipmentIndex, FocusRegion originRegion) {
    public EquipmentSelectionContext {
        if (target == null || originEquipmentIndex < 0 || originRegion == null) {
            throw new IllegalArgumentException("invalid equipment selection context");
        }
    }
}
