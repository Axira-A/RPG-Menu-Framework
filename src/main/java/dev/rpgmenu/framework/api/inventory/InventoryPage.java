package dev.rpgmenu.framework.api.inventory;

import java.util.List;

/** A provider or unified-service result window, never an entire huge storage dump. */
public record InventoryPage(List<UnifiedItemEntry> entries, long totalEntries, long storageRevision, int page, int pageSize) {
    public InventoryPage {
        entries = List.copyOf(entries);
        if (totalEntries < 0) throw new IllegalArgumentException("totalEntries cannot be negative");
    }

    public static InventoryPage empty(InventoryQuery query, long revision) {
        return new InventoryPage(List.of(), 0, revision, query.page(), query.pageSize());
    }
}
