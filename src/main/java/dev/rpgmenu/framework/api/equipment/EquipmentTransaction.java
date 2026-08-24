package dev.rpgmenu.framework.api.equipment;

import java.util.UUID;

/** Client request metadata. The server resolves entryOpaqueId from the player's menu session. */
public record EquipmentTransaction(UUID sessionId, long entryOpaqueId, EquipmentTarget target,
                                   EquipmentAction action, long nonce) {
    public EquipmentTransaction {
        if (sessionId == null || target == null || action == null) throw new IllegalArgumentException("missing transaction field");
        if (action == EquipmentAction.EQUIP && entryOpaqueId <= 0) throw new IllegalArgumentException("equip requires entryOpaqueId");
    }
}
