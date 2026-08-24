package dev.rpgmenu.framework.api.inventory;

import dev.rpgmenu.framework.api.equipment.EquipmentTarget;
import java.util.Locale;

/** Bounded server-side inventory query. */
public record InventoryQuery(String search, String category, InventorySort sort, int page, int pageSize,
                             EquipmentTarget equipmentTarget) {
    public static final int MAX_PAGE_SIZE = 256;
    public static final int DEFAULT_PAGE_SIZE = 120;

    public InventoryQuery {
        search = search == null ? "" : search.strip();
        category = category == null || category.isBlank() ? "all" : category.toLowerCase(Locale.ROOT);
        sort = sort == null ? InventorySort.DEFAULT : sort;
        page = Math.max(0, page);
        pageSize = Math.max(1, Math.min(MAX_PAGE_SIZE, pageSize));
    }

    public InventoryQuery(String search, String category, InventorySort sort, int page, int pageSize) {
        this(search, category, sort, page, pageSize, null);
    }
}
