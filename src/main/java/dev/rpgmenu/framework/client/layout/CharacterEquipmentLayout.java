package dev.rpgmenu.framework.client.layout;

import java.util.ArrayList;
import java.util.List;

/** Pure logical-pixel layout for the inventory character panel. */
public record CharacterEquipmentLayout(List<UiRect> mainHandSlots,
                                       List<UiRect> offhandSlots,
                                       List<UiRect> itemQuickbarSlots,
                                       List<UiRect> armorSlots,
                                       List<UiRect> sideEquipmentSlots,
                                       UiRect preview,
                                       int sideRowCapacity,
                                       int slotSize,
                                       int mainTitleY,
                                       int offhandTitleY,
                                       int itemTitleY) {
    public static final int MAIN_HAND_COUNT = 4;
    public static final int ITEM_BAR_COUNT = 5;

    public CharacterEquipmentLayout {
        mainHandSlots = List.copyOf(mainHandSlots);
        offhandSlots = List.copyOf(offhandSlots);
        itemQuickbarSlots = List.copyOf(itemQuickbarSlots);
        armorSlots = List.copyOf(armorSlots);
        sideEquipmentSlots = List.copyOf(sideEquipmentSlots);
    }

    public static CharacterEquipmentLayout calculate(UiRect panel, int offhandCount, int sideEquipmentCount) {
        int slot = Math.max(18, Math.min(23, panel.width() / 12));
        int gap = Math.max(2, slot / 7);
        int titleY = panel.y() + 9;
        int topY = titleY + 11;

        List<UiRect> main = centeredRow(panel.x() + panel.width() / 4, topY, MAIN_HAND_COUNT, slot, gap);
        List<UiRect> offhand = centeredRow(panel.x() + panel.width() * 3 / 4, topY,
                Math.max(1, offhandCount), slot, gap);

        int usable = Math.max(1, panel.width() - 16);
        int singleRowWidth = ITEM_BAR_COUNT * slot + (ITEM_BAR_COUNT - 1) * gap;
        boolean twoRows = singleRowWidth > usable;
        int itemRows = twoRows ? 2 : 1;
        int itemColumns = twoRows ? 3 : ITEM_BAR_COUNT;
        int itemWidth = itemColumns * slot + (itemColumns - 1) * gap;
        int itemStartY = panel.bottom() - 8 - itemRows * slot - (itemRows - 1) * gap;
        int itemTitleY = itemStartY - 11;
        List<UiRect> items = new ArrayList<>(ITEM_BAR_COUNT);
        int itemStartX = panel.x() + (panel.width() - itemWidth) / 2;
        for (int index = 0; index < ITEM_BAR_COUNT; index++) {
            items.add(new UiRect(itemStartX + index % itemColumns * (slot + gap),
                    itemStartY + index / itemColumns * (slot + gap), slot, slot));
        }

        int sideStartY = topY + slot + 28;
        int sideStep = slot + 14;
        int sideRows = Math.max(2, (itemTitleY - sideStartY - 4) / Math.max(1, sideStep));
        int leftX = panel.x() + 8;
        int rightX = panel.right() - 8 - slot;
        List<UiRect> armor = List.of(
                new UiRect(leftX, sideStartY, slot, slot),
                new UiRect(rightX, sideStartY, slot, slot),
                new UiRect(leftX, sideStartY + sideStep, slot, slot),
                new UiRect(rightX, sideStartY + sideStep, slot, slot));

        int extraRows = Math.max(0, sideRows - 2);
        int visibleSideCount = Math.min(Math.max(0, sideEquipmentCount), extraRows * 2);
        List<UiRect> sides = new ArrayList<>(visibleSideCount);
        for (int index = 0; index < visibleSideCount; index++) {
            int row = index / 2;
            sides.add(new UiRect(index % 2 == 0 ? leftX : rightX,
                    sideStartY + (row + 2) * sideStep, slot, slot));
        }

        int previewLeft = leftX + slot + 8;
        int previewRight = rightX - 8;
        int previewTop = topY + slot + 6;
        int previewBottom = itemTitleY - 4;
        UiRect preview = new UiRect(previewLeft, previewTop, Math.max(1, previewRight - previewLeft),
                Math.max(1, previewBottom - previewTop));
        return new CharacterEquipmentLayout(main, offhand, items, armor, sides, preview,
                extraRows, slot, titleY, titleY, itemTitleY);
    }

    private static List<UiRect> centeredRow(int centerX, int y, int count, int slot, int gap) {
        int width = count * slot + Math.max(0, count - 1) * gap;
        int x = centerX - width / 2;
        List<UiRect> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            result.add(new UiRect(x + index * (slot + gap), y, slot, slot));
        }
        return result;
    }
}
