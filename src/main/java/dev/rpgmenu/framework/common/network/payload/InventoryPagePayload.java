package dev.rpgmenu.framework.common.network.payload;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.inventory.InventoryQuery;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record InventoryPagePayload(UUID sessionId, long storageRevision, int page, int pageSize, long totalEntries,
                                   List<Entry> entries) implements CustomPacketPayload {
    public static final Type<InventoryPagePayload> TYPE = new Type<>(RpgMenuFramework.id("inventory_page"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InventoryPagePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public InventoryPagePayload decode(RegistryFriendlyByteBuf buf) {
            UUID session = buf.readUUID();
            long revision = buf.readLong();
            int page = buf.readVarInt();
            int pageSize = buf.readVarInt();
            long total = buf.readVarLong();
            int count = buf.readVarInt();
            if (count < 0 || count > InventoryQuery.MAX_PAGE_SIZE) throw new DecoderException("Inventory page exceeds entry bound");
            List<Entry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                entries.add(new Entry(buf.readVarLong(), ItemStack.STREAM_CODEC.decode(buf), buf.readVarLong(), buf.readVarInt()));
            }
            return new InventoryPagePayload(session, revision, page, pageSize, total, entries);
        }

        @Override public void encode(RegistryFriendlyByteBuf buf, InventoryPagePayload value) {
            buf.writeUUID(value.sessionId);
            buf.writeLong(value.storageRevision);
            buf.writeVarInt(value.page);
            buf.writeVarInt(value.pageSize);
            buf.writeVarLong(value.totalEntries);
            buf.writeVarInt(value.entries.size());
            for (Entry entry : value.entries) {
                buf.writeVarLong(entry.opaqueId);
                ItemStack.STREAM_CODEC.encode(buf, entry.stack);
                buf.writeVarLong(entry.amount);
                buf.writeVarInt(entry.sourceCount);
            }
        }
    };

    public InventoryPagePayload {
        entries = List.copyOf(entries);
        if (entries.size() > InventoryQuery.MAX_PAGE_SIZE) throw new IllegalArgumentException("too many page entries");
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public record Entry(long opaqueId, ItemStack stack, long amount, int sourceCount) {
        public Entry { stack = stack.copyWithCount(1); }
    }
}
