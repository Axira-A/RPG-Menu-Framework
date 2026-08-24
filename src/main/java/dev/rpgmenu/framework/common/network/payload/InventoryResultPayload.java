package dev.rpgmenu.framework.common.network.payload;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.inventory.TransactionResult;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.UUID;

public record InventoryResultPayload(UUID sessionId, long nonce, TransactionResult.Status status, long moved, String messageKey)
        implements CustomPacketPayload {
    public static final Type<InventoryResultPayload> TYPE = new Type<>(RpgMenuFramework.id("inventory_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InventoryResultPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public InventoryResultPayload decode(RegistryFriendlyByteBuf buf) {
            int ordinal = buf.readVarInt();
            TransactionResult.Status status = ordinal >= 0 && ordinal < TransactionResult.Status.values().length
                    ? TransactionResult.Status.values()[ordinal] : TransactionResult.Status.REJECTED;
            return new InventoryResultPayload(buf.readUUID(), buf.readVarLong(), status, buf.readVarLong(), buf.readUtf(128));
        }
        @Override public void encode(RegistryFriendlyByteBuf buf, InventoryResultPayload value) {
            buf.writeVarInt(value.status.ordinal());
            buf.writeUUID(value.sessionId);
            buf.writeVarLong(value.nonce);
            buf.writeVarLong(value.moved);
            buf.writeUtf(value.messageKey, 128);
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
