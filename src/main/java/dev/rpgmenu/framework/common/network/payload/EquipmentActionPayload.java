package dev.rpgmenu.framework.common.network.payload;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.equipment.EquipmentAction;
import dev.rpgmenu.framework.api.equipment.EquipmentTarget;
import dev.rpgmenu.framework.api.equipment.EquipmentTransaction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record EquipmentActionPayload(UUID sessionId, long entryOpaqueId, EquipmentTarget target,
                                     EquipmentAction action, long nonce) implements CustomPacketPayload {
    public static final Type<EquipmentActionPayload> TYPE = new Type<>(RpgMenuFramework.id("equipment_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EquipmentActionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public EquipmentActionPayload decode(RegistryFriendlyByteBuf buf) {
            UUID session = buf.readUUID();
            long entry = buf.readVarLong();
            EquipmentTarget target = new EquipmentTarget(buf.readResourceLocation(),
                    buf.readUtf(EquipmentTarget.MAX_SLOT_KEY_LENGTH), buf.readVarInt());
            int ordinal = buf.readVarInt();
            EquipmentAction action = ordinal >= 0 && ordinal < EquipmentAction.values().length
                    ? EquipmentAction.values()[ordinal] : EquipmentAction.UNEQUIP;
            return new EquipmentActionPayload(session, entry, target, action, buf.readVarLong());
        }

        @Override public void encode(RegistryFriendlyByteBuf buf, EquipmentActionPayload value) {
            buf.writeUUID(value.sessionId);
            buf.writeVarLong(value.entryOpaqueId);
            buf.writeResourceLocation(value.target.providerId());
            buf.writeUtf(value.target.slotKey(), EquipmentTarget.MAX_SLOT_KEY_LENGTH);
            buf.writeVarInt(value.target.slotIndex());
            buf.writeVarInt(value.action.ordinal());
            buf.writeVarLong(value.nonce);
        }
    };

    public EquipmentTransaction transaction() {
        return new EquipmentTransaction(sessionId, entryOpaqueId, target, action, nonce);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
