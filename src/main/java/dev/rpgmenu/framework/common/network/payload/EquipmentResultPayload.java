package dev.rpgmenu.framework.common.network.payload;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.equipment.EquipmentTarget;
import dev.rpgmenu.framework.api.inventory.TransactionResult;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record EquipmentResultPayload(UUID sessionId, long nonce, EquipmentTarget target,
                                     TransactionResult.Status status, long moved, String messageKey)
        implements CustomPacketPayload {
    public static final Type<EquipmentResultPayload> TYPE = new Type<>(RpgMenuFramework.id("equipment_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EquipmentResultPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public EquipmentResultPayload decode(RegistryFriendlyByteBuf buf) {
            UUID session = buf.readUUID();
            long nonce = buf.readVarLong();
            EquipmentTarget target = new EquipmentTarget(buf.readResourceLocation(),
                    buf.readUtf(EquipmentTarget.MAX_SLOT_KEY_LENGTH), buf.readVarInt());
            int ordinal = buf.readVarInt();
            TransactionResult.Status status = ordinal >= 0 && ordinal < TransactionResult.Status.values().length
                    ? TransactionResult.Status.values()[ordinal] : TransactionResult.Status.REJECTED;
            return new EquipmentResultPayload(session, nonce, target, status, buf.readVarLong(), buf.readUtf(128));
        }

        @Override public void encode(RegistryFriendlyByteBuf buf, EquipmentResultPayload value) {
            buf.writeUUID(value.sessionId);
            buf.writeVarLong(value.nonce);
            buf.writeResourceLocation(value.target.providerId());
            buf.writeUtf(value.target.slotKey(), EquipmentTarget.MAX_SLOT_KEY_LENGTH);
            buf.writeVarInt(value.target.slotIndex());
            buf.writeVarInt(value.status.ordinal());
            buf.writeVarLong(value.moved);
            buf.writeUtf(value.messageKey, 128);
        }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
