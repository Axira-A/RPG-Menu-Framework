package dev.rpgmenu.framework.client.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptiveTabWidthsTest {
    @Test
    void sixFullLabelsFillTheStripWithoutShrinkingBelowTheirTextWidths() {
        int[] required = {44, 38, 38, 38, 38, 38};
        int[] result = AdaptiveTabWidths.distribute(610, required);

        assertEquals(610, java.util.Arrays.stream(result).sum());
        for (int i = 0; i < result.length; i++) assertTrue(result[i] >= required[i]);
    }

    @Test
    void remainderDistributionIsIntegerAlignedAndStable() {
        assertArrayEquals(new int[] {34, 34, 33}, AdaptiveTabWidths.distribute(101, new int[] {30, 30, 29}));
    }

    @Test
    void refusesAFullTextLayoutThatDoesNotActuallyFit() {
        assertThrows(IllegalArgumentException.class,
                () -> AdaptiveTabWidths.distribute(80, new int[] {42, 42}));
    }
}
