package dev.rpgmenu.framework.client.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterEquipmentLayoutTest {
    @Test
    void desktopKeepsStableFourPlusFiveMappingInsidePanel() {
        UiRect panel = new UiRect(510, 55, 280, 420);
        CharacterEquipmentLayout layout = CharacterEquipmentLayout.calculate(panel, 3, 8);

        assertEquals(4, layout.mainHandSlots().size());
        assertEquals(5, layout.itemQuickbarSlots().size());
        assertEquals(3, layout.offhandSlots().size());
        layout.mainHandSlots().forEach(slot -> assertInside(panel, slot));
        layout.itemQuickbarSlots().forEach(slot -> assertInside(panel, slot));
        layout.offhandSlots().forEach(slot -> assertInside(panel, slot));
        assertEquals(layout.itemQuickbarSlots().getFirst().y(), layout.itemQuickbarSlots().getLast().y());
    }

    @Test
    void offhandCountComesFromProviderInsteadOfFixedPlaceholders() {
        UiRect panel = new UiRect(0, 0, 300, 430);
        assertEquals(1, CharacterEquipmentLayout.calculate(panel, 1, 0).offhandSlots().size());
        assertEquals(4, CharacterEquipmentLayout.calculate(panel, 4, 0).offhandSlots().size());
    }

    @Test
    void narrowPanelWrapsFiveItemSlotsWithoutLeavingThePanel() {
        CharacterEquipmentLayout layout = CharacterEquipmentLayout.calculate(new UiRect(0, 0, 100, 360), 1, 0);
        assertEquals(5, layout.itemQuickbarSlots().size());
        assertEquals(layout.itemQuickbarSlots().get(0).y(), layout.itemQuickbarSlots().get(2).y());
        assertTrue(layout.itemQuickbarSlots().get(3).y() > layout.itemQuickbarSlots().get(0).y());
    }

    private static void assertInside(UiRect outer, UiRect inner) {
        assertTrue(inner.x() >= outer.x() && inner.y() >= outer.y());
        assertTrue(inner.right() <= outer.right() && inner.bottom() <= outer.bottom());
    }
}
