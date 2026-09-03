package dev.rpgmenu.framework.client.screen;

import dev.rpgmenu.framework.api.menu.RpgMenuTab;
import dev.rpgmenu.framework.client.layout.UiRect;
import dev.rpgmenu.framework.client.layout.AdaptiveTabWidths;
import dev.rpgmenu.framework.client.theme.ThemeDefinition;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** One shared visual and hit-test component for every semantic top-level tab. */
final class MenuTabWidget {
    private static final int HORIZONTAL_PADDING = 20;
    private static final int MIN_FULL_WIDTH = 38;
    private static final int MIN_COMPACT_WIDTH = 32;

    private final RpgMenuTab tab;
    private final UiRect bounds;
    private final String label;
    private final boolean selected;
    private final boolean enabled;
    private final boolean focused;

    private MenuTabWidget(RpgMenuTab tab, UiRect bounds, String label,
                          boolean selected, boolean enabled, boolean focused) {
        this.tab = tab;
        this.bounds = bounds;
        this.label = label;
        this.selected = selected;
        this.enabled = enabled;
        this.focused = focused;
    }

    static Layout layout(java.util.List<RpgMenuTab> tabs, Font font, UiRect strip, int requestedOffset,
                         net.minecraft.resources.ResourceLocation selectedId,
                         java.util.function.Predicate<RpgMenuTab> enabled,
                         java.util.function.Predicate<RpgMenuTab> focused) {
        if (tabs.isEmpty()) return new Layout(java.util.List.of(), 0);
        int available = Math.max(1, strip.width() - 12);
        int[] required = new int[tabs.size()];
        int totalRequired = 0;
        for (int i = 0; i < tabs.size(); i++) {
            String title = Component.translatable(tabs.get(i).titleKey()).getString();
            required[i] = Math.max(MIN_FULL_WIDTH, font.width(title) + HORIZONTAL_PADDING);
            totalRequired += required[i];
        }

        boolean fullText = totalRequired <= available;
        int offset = fullText ? 0 : Math.max(0, Math.min(requestedOffset,
                Math.max(0, tabs.size() - Math.max(1, available / MIN_COMPACT_WIDTH))));
        int visibleCount = fullText ? tabs.size()
                : Math.min(tabs.size() - offset, Math.max(1, available / MIN_COMPACT_WIDTH));
        int[] widths = new int[visibleCount];
        if (fullText) {
            widths = AdaptiveTabWidths.distribute(available, required);
        } else {
            for (int i = 0; i < visibleCount; i++) {
                widths[i] = available / visibleCount + (i < available % visibleCount ? 1 : 0);
            }
        }

        java.util.List<MenuTabWidget> widgets = new java.util.ArrayList<>(visibleCount);
        int x = strip.x() + 6;
        for (int visibleIndex = 0; visibleIndex < visibleCount; visibleIndex++) {
            int tabIndex = offset + visibleIndex;
            RpgMenuTab tab = tabs.get(tabIndex);
            UiRect bounds = new UiRect(x, strip.y() + 3, Math.max(1, widths[visibleIndex] - 2), strip.height() - 6);
            String title = Component.translatable(tab.titleKey()).getString();
            widgets.add(new MenuTabWidget(tab, bounds, ellipsize(font, title, bounds.width() - 12),
                    tab.id().equals(selectedId), enabled.test(tab), focused.test(tab)));
            x += widths[visibleIndex];
        }
        return new Layout(java.util.List.copyOf(widgets), offset);
    }

    void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, ThemeDefinition theme, int badge) {
        boolean hovered = enabled && bounds.contains(mouseX, mouseY);
        int fill = !enabled ? 0x16000000
                : selected ? 0x553A2C18
                : hovered || focused ? 0x332D261C : 0x11000000;
        graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), fill);
        if (focused && !selected) border(graphics, bounds, theme.accent());
        if (selected) graphics.fill(bounds.x(), bounds.bottom() - 2, bounds.right(), bounds.bottom(), theme.accent());
        int textColor = !enabled ? theme.textMuted() : selected ? theme.accent() : theme.text();
        graphics.drawCenteredString(font, label, bounds.x() + bounds.width() / 2,
                bounds.y() + (bounds.height() - 8) / 2, textColor);
        if (badge > 0) graphics.drawString(font, Integer.toString(badge), bounds.right() - 9,
                bounds.y() + 2, theme.danger(), true);
    }

    boolean contains(double mouseX, double mouseY) { return bounds.contains(mouseX, mouseY); }
    boolean enabled() { return enabled; }
    RpgMenuTab tab() { return tab; }

    private static String ellipsize(Font font, String value, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (font.width(value) <= maxWidth) return value;
        String suffix = "…";
        int length = value.length();
        while (length > 0 && font.width(value.substring(0, length) + suffix) > maxWidth) length--;
        return value.substring(0, length) + suffix;
    }

    private static void border(GuiGraphics graphics, UiRect rect, int color) {
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, color);
        graphics.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), color);
        graphics.fill(rect.x(), rect.y(), rect.x() + 1, rect.bottom(), color);
        graphics.fill(rect.right() - 1, rect.y(), rect.right(), rect.bottom(), color);
    }

    record Layout(java.util.List<MenuTabWidget> widgets, int offset) {}
}
