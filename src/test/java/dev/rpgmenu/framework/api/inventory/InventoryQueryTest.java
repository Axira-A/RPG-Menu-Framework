package dev.rpgmenu.framework.api.inventory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryQueryTest {
    @Test void clampsUntrustedPagination() {
        InventoryQuery query = new InventoryQuery(null, null, null, -99, Integer.MAX_VALUE);
        assertEquals("", query.search());
        assertEquals("all", query.category());
        assertEquals(0, query.page());
        assertEquals(InventoryQuery.MAX_PAGE_SIZE, query.pageSize());
        assertEquals(InventorySort.DEFAULT, query.sort());
    }
}
