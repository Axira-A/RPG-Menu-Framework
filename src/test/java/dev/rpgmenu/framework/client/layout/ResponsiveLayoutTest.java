package dev.rpgmenu.framework.client.layout;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponsiveLayoutTest {
    @Test void supportedDesktopResolutionsStayInsideFrame() {
        Stream.of(new int[]{1280, 720}, new int[]{1600, 900}, new int[]{1920, 1080},
                        new int[]{2560, 1440}, new int[]{3440, 1440})
                .forEach(size -> verify(size[0], size[1]));
    }

    @Test void compactScaledWindowsRemainUsable() {
        Stream.of(new int[]{320, 180}, new int[]{427, 240}, new int[]{480, 270},
                        new int[]{640, 360}, new int[]{854, 480})
                .forEach(size -> verify(size[0], size[1]));
    }

    @Test void requestedPhysicalResolutionAndGuiScaleMatrixStaysPixelAligned() {
        Stream.of(new int[]{1280, 720}, new int[]{1920, 1080}, new int[]{2560, 1440})
                .forEach(physical -> {
                    for (int guiScale = 1; guiScale <= 4; guiScale++) {
                        int logicalWidth = divideCeil(physical[0], guiScale);
                        int logicalHeight = divideCeil(physical[1], guiScale);
                        verify(logicalWidth, logicalHeight);
                        verifyIntegerAligned(ResponsiveLayout.calculate(logicalWidth, logicalHeight));
                    }
                });
    }

    @Test void automaticGuiScaleLogicalWindowsRemainUsable() {
        Stream.of(new int[]{1280, 720}, new int[]{1920, 1080}, new int[]{2560, 1440})
                .forEach(physical -> {
                    int guiScale = automaticGuiScale(physical[0], physical[1]);
                    int logicalWidth = divideCeil(physical[0], guiScale);
                    int logicalHeight = divideCeil(physical[1], guiScale);
                    verify(logicalWidth, logicalHeight);
                    verifyIntegerAligned(ResponsiveLayout.calculate(logicalWidth, logicalHeight));
                });
    }

    private static void verify(int width, int height) {
        ResponsiveLayout layout = ResponsiveLayout.calculate(width, height);
        assertTrue(layout.frame().x() >= 0 && layout.frame().y() >= 0);
        assertTrue(layout.frame().right() <= width);
        assertTrue(layout.topTabs().bottom() <= layout.leftPanel().y());
        assertTrue(layout.grid().width() > 0 && layout.grid().height() > 0);
        assertTrue(layout.gridColumns(22) >= 1 && layout.gridRows(22) >= 1);
        assertTrue(layout.footer().bottom() <= layout.frame().bottom());
    }

    private static void verifyIntegerAligned(ResponsiveLayout layout) {
        List<UiRect> rectangles = List.of(layout.frame(), layout.topTabs(), layout.leftPanel(), layout.rightPanel(),
                layout.subTabs(), layout.search(), layout.grid(), layout.details(), layout.character(), layout.status(),
                layout.footer());
        rectangles.forEach(rect -> {
            assertTrue(rect.width() >= 0 && rect.height() >= 0);
            assertTrue(rect.right() == rect.x() + rect.width());
            assertTrue(rect.bottom() == rect.y() + rect.height());
        });
    }

    private static int divideCeil(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }

    private static int automaticGuiScale(int physicalWidth, int physicalHeight) {
        int scale = 1;
        while (physicalWidth / (scale + 1) >= 320 && physicalHeight / (scale + 1) >= 240) {
            scale++;
        }
        return scale;
    }
}
