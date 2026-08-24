package dev.rpgmenu.framework.common.inventory;

import dev.rpgmenu.framework.api.RpgMenuApi;
import dev.rpgmenu.framework.api.inventory.InventoryPage;
import dev.rpgmenu.framework.api.inventory.InventoryQuery;
import dev.rpgmenu.framework.api.inventory.InventorySource;
import dev.rpgmenu.framework.api.inventory.ItemIdentity;
import dev.rpgmenu.framework.api.inventory.SourceContribution;
import dev.rpgmenu.framework.api.inventory.UnifiedItemEntry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Coordinates bounded source queries. Sources remain responsible for large-storage pagination. */
public final class UnifiedInventoryService {
    public static final UnifiedInventoryService INSTANCE = new UnifiedInventoryService();

    private UnifiedInventoryService() {}

    public InventoryPage query(ServerPlayer player, InventoryQuery query) {
        List<InventorySource> available = RpgMenuApi.get().inventorySources().values().stream()
                .filter(source -> source.available(player)).toList();
        if (available.isEmpty()) return InventoryPage.empty(query, 0);
        if (available.size() == 1) return available.getFirst().query(player, query);

        Map<ItemIdentity, MutableEntry> merged = new LinkedHashMap<>();
        long total = 0;
        long revision = 1;
        for (InventorySource source : available) {
            InventoryPage sourcePage = source.query(player, query);
            total = saturatedAdd(total, sourcePage.totalEntries());
            revision = 31 * revision + sourcePage.storageRevision();
            for (UnifiedItemEntry entry : sourcePage.entries()) {
                MutableEntry target = merged.computeIfAbsent(ItemIdentity.of(entry.displayStack()),
                        ignored -> new MutableEntry(entry.displayStack()));
                target.amount = saturatedAdd(target.amount, entry.amount());
                target.sources.addAll(entry.sources());
                target.acquiredOrder = Math.max(target.acquiredOrder, entry.acquiredOrder());
            }
        }
        List<UnifiedItemEntry> entries = merged.values().stream().map(MutableEntry::freeze).toList();
        return new InventoryPage(entries, Math.max(entries.size(), total), revision, query.page(), query.pageSize());
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }

    private static final class MutableEntry {
        private final ItemStack stack;
        private long amount;
        private long acquiredOrder;
        private final List<SourceContribution> sources = new ArrayList<>();
        private MutableEntry(ItemStack stack) { this.stack = stack.copyWithCount(1); }
        private UnifiedItemEntry freeze() { return new UnifiedItemEntry(stack, amount, sources, acquiredOrder); }
    }
}
