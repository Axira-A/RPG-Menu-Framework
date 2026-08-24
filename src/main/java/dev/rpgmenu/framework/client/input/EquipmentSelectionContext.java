package dev.rpgmenu.framework.client.input;

import dev.rpgmenu.framework.api.equipment.EquipmentTarget;

public record EquipmentSelectionContext(EquipmentTarget target, int originEquipmentIndex) {
    public EquipmentSelectionContext {
        if (target == null || originEquipmentIndex < 0) throw new IllegalArgumentException("invalid equipment selection context");
    }
}
