package dev.rpgmenu.framework.common.network.payload;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.equipment.EquipmentTarget;
import dev.rpgmenu.framework.api.inventory.InventoryQuery;
import dev.rpgmenu.framework.api.inventory.InventorySort;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.Locale;
import java.util.UUID;

public record InventoryQueryPayload(UUID sessionId, String search, String category, InventorySort sort, int page, int pageSize,
                                    EquipmentTarget equipmentTarget)
        implements CustomPacketPayload {
    public static final Type<InventoryQueryPayload> TYPE = new Type<>(RpgMenuFramework.id("inventory_query"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InventoryQueryPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public InventoryQueryPayload decode(RegistryFriendlyByteBuf buf) {
            UUID sessionId = buf.readUUID();
            String search = buf.readUtf(128);
            String category = buf.readUtf(64);
            InventorySort sort = safeSort(buf.readVarInt());
            int page = buf.readVarInt();
            int pageSize = buf.readVarInt();
            EquipmentTarget target = buf.readBoolean()
                    ? new EquipmentTarget(buf.readResourceLocation(), buf.readUtf(EquipmentTarget.MAX_SLOT_KEY_LENGTH), buf.readVarInt())
                    : null;
            return new InventoryQueryPayload(sessionId, search, category, sort, page, pageSize, target);
        }
        @Override public void encode(RegistryFriendlyByteBuf buf, InventoryQueryPayload value) {
            buf.writeUUID(value.sessionId);
            buf.writeUtf(value.search, 128);
            buf.writeUtf(value.category, 64);
            buf.writeVarInt(value.sort.ordinal());
            buf.writeVarInt(value.page);
            buf.writeVarInt(value.pageSize);
            buf.writeBoolean(value.equipmentTarget != null);
            if (value.equipmentTarget != null) {
                buf.writeResourceLocation(value.equipmentTarget.providerId());
                buf.writeUtf(value.equipmentTarget.slotKey(), EquipmentTarget.MAX_SLOT_KEY_LENGTH);
                buf.writeVarInt(value.equipmentTarget.slotIndex());
            }
        }
    };

    public InventoryQueryPayload {
        if (sessionId == null) throw new IllegalArgumentException("sessionId");
        search = search == null ? "" : search.strip();
        category = category == null ? "all" : category.toLowerCase(Locale.ROOT);
        if (search.length() > 128 || category.length() > 64) throw new IllegalArgumentException("query text too long");
        sort = sort == null ? InventorySort.DEFAULT : sort;
    }

    public InventoryQueryPayload(UUID sessionId, String search, String category, InventorySort sort, int page, int pageSize) {
        this(sessionId, search, category, sort, page, pageSize, null);
    }

    public InventoryQuery query() { return new InventoryQuery(search, category, sort, page, pageSize, equipmentTarget); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    private static InventorySort safeSort(int ordinal) {
        return ordinal >= 0 && ordinal < InventorySort.values().length ? InventorySort.values()[ordinal] : InventorySort.DEFAULT;
    }
}
