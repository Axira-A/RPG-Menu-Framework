package dev.rpgmenu.framework.common.network.payload;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.inventory.InventoryOperation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.UUID;

public record InventoryActionPayload(UUID sessionId, long entryOpaqueId, long requestedAmount,
                                     InventoryOperation operation, long nonce) implements CustomPacketPayload {
    public static final Type<InventoryActionPayload> TYPE = new Type<>(RpgMenuFramework.id("inventory_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InventoryActionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public InventoryActionPayload decode(RegistryFriendlyByteBuf buf) {
            return new InventoryActionPayload(buf.readUUID(), buf.readVarLong(), buf.readVarLong(), safeOperation(buf.readVarInt()), buf.readVarLong());
        }
        @Override public void encode(RegistryFriendlyByteBuf buf, InventoryActionPayload value) {
            buf.writeUUID(value.sessionId);
            buf.writeVarLong(value.entryOpaqueId);
            buf.writeVarLong(value.requestedAmount);
            buf.writeVarInt(value.operation.ordinal());
            buf.writeVarLong(value.nonce);
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    private static InventoryOperation safeOperation(int ordinal) {
        return ordinal >= 0 && ordinal < InventoryOperation.values().length ? InventoryOperation.values()[ordinal] : InventoryOperation.WITHDRAW_ONE;
    }
}
