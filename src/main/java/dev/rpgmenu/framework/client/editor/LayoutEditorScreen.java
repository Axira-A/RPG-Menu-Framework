package dev.rpgmenu.framework.client.editor;

import dev.rpgmenu.framework.client.layout.LayoutOverrides;
import dev.rpgmenu.framework.client.layout.ResponsiveLayout;
import dev.rpgmenu.framework.client.layout.UiRect;
import dev.rpgmenu.framework.client.theme.ThemeDefinition;
import dev.rpgmenu.framework.client.theme.ThemeManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.LinkedHashMap;
import java.util.Map;

/** Lightweight developer editor for component position and size deltas, persisted to layout.json. */
public final class LayoutEditorScreen extends Screen {
    private ResponsiveLayout base;
    private String selected = "InventoryGrid";
    private boolean resizing;

    public LayoutEditorScreen() { super(Component.translatable("editor.rpgmenuframework.title")); }

    @Override protected void init() { base = ResponsiveLayout.calculate(width, height); }
    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ThemeDefinition theme = ThemeManager.INSTANCE.current();
        graphics.fill(0, 0, width, height, theme.backdrop());
        for (Map.Entry<String, UiRect> entry : components().entrySet()) {
            UiRect rect = LayoutOverrides.INSTANCE.apply(entry.getKey(), entry.getValue());
            int color = entry.getKey().equals(selected) ? theme.accent() : theme.borderMuted();
            graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), 0x4416191C);
            border(graphics, rect, color);
            graphics.drawString(font, entry.getKey(), rect.x() + 3, rect.y() + 3, color, false);
            graphics.fill(rect.right() - 5, rect.bottom() - 5, rect.right(), rect.bottom(), color);
        }
        graphics.drawString(font, Component.translatable("editor.rpgmenuframework.help"), 8, height - 14, theme.text(), false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Map.Entry<String, UiRect> entry : components().entrySet()) {
            UiRect rect = LayoutOverrides.INSTANCE.apply(entry.getKey(), entry.getValue());
            if (rect.contains(mouseX, mouseY)) {
                selected = entry.getKey();
                resizing = mouseX >= rect.right() - 8 && mouseY >= rect.bottom() - 8;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        LayoutOverrides.Delta current = LayoutOverrides.INSTANCE.get(selected);
        LayoutOverrides.INSTANCE.set(selected, resizing
                ? new LayoutOverrides.Delta(current.x(), current.y(), current.width() + (int)dragX, current.height() + (int)dragY)
                : new LayoutOverrides.Delta(current.x() + (int)dragX, current.y() + (int)dragY, current.width(), current.height()));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_R) { LayoutOverrides.INSTANCE.reset(); return true; }
        if (keyCode == GLFW.GLFW_KEY_S) { LayoutOverrides.INSTANCE.save(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public void onClose() { LayoutOverrides.INSTANCE.save(); super.onClose(); }

    private Map<String, UiRect> components() {
        Map<String, UiRect> result = new LinkedHashMap<>();
        result.put("TopTabs", base.topTabs());
        result.put("SubTabs", base.subTabs());
        result.put("SearchBar", base.search());
        result.put("InventoryGrid", base.grid());
        result.put("ItemDetail", base.details());
        result.put("CharacterPreview", base.character());
        result.put("StatusPanel", base.status());
        result.put("FooterHints", base.footer());
        return result;
    }

    private static void border(GuiGraphics graphics, UiRect rect, int color) {
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, color);
        graphics.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), color);
        graphics.fill(rect.x(), rect.y(), rect.x() + 1, rect.bottom(), color);
        graphics.fill(rect.right() - 1, rect.y(), rect.right(), rect.bottom(), color);
    }
}
