package dev.rpgmenu.framework.common.network.payload;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.inventory.QuickbarAction;
import dev.rpgmenu.framework.api.inventory.QuickSlotGroup;
import dev.rpgmenu.framework.api.inventory.QuickSlotTarget;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record QuickbarActionPayload(UUID sessionId, long entryOpaqueId, QuickSlotTarget source, QuickSlotTarget target,
                                    QuickbarAction action, long nonce) implements CustomPacketPayload {
    public static final Type<QuickbarActionPayload> TYPE = new Type<>(RpgMenuFramework.id("quickbar_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuickbarActionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public QuickbarActionPayload decode(RegistryFriendlyByteBuf buf) {
            UUID session = buf.readUUID();
            long entry = buf.readVarLong();
            QuickSlotTarget source = buf.readBoolean() ? readTarget(buf) : null;
            QuickSlotTarget target = readTarget(buf);
            int ordinal = buf.readVarInt();
            if (ordinal < 0 || ordinal >= QuickbarAction.values().length) {
                throw new IllegalArgumentException("invalid quick-slot action");
            }
            QuickbarAction action = QuickbarAction.values()[ordinal];
            return new QuickbarActionPayload(session, entry, source, target, action, buf.readVarLong());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, QuickbarActionPayload value) {
            buf.writeUUID(value.sessionId);
            buf.writeVarLong(value.entryOpaqueId);
            buf.writeBoolean(value.source != null);
            if (value.source != null) writeTarget(buf, value.source);
            writeTarget(buf, value.target);
            buf.writeVarInt(value.action.ordinal());
            buf.writeVarLong(value.nonce);
        }
    };

    public QuickbarActionPayload {
        if (target == null) throw new IllegalArgumentException("quick-slot target is required");
    }

    private static QuickSlotTarget readTarget(RegistryFriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        if (ordinal < 0 || ordinal >= QuickSlotGroup.values().length) {
            throw new IllegalArgumentException("invalid quick-slot group");
        }
        return new QuickSlotTarget(QuickSlotGroup.values()[ordinal], buf.readVarInt());
    }

    private static void writeTarget(RegistryFriendlyByteBuf buf, QuickSlotTarget target) {
        buf.writeVarInt(target.group().ordinal());
        buf.writeVarInt(target.index());
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
