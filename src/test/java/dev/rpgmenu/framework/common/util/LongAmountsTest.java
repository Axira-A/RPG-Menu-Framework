package dev.rpgmenu.framework.common.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LongAmountsTest {
    @Test void formatsBoundariesWithoutOverflow() {
        assertEquals("999", LongAmounts.compact(999));
        assertEquals("1K", LongAmounts.compact(1_000));
        assertEquals("12.4K", LongAmounts.compact(12_400));
        assertEquals("8.2M", LongAmounts.compact(8_200_000));
        assertEquals("9.2E", LongAmounts.compact(Long.MAX_VALUE));
        assertEquals("-9.2E", LongAmounts.compact(Long.MIN_VALUE));
    }
}
