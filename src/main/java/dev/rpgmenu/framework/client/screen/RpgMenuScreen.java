package dev.rpgmenu.framework.client.screen;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.RpgMenuApi;
import dev.rpgmenu.framework.api.equipment.EquipmentAction;
import dev.rpgmenu.framework.api.equipment.EquipmentProvider;
import dev.rpgmenu.framework.api.equipment.EquipmentSlotView;
import dev.rpgmenu.framework.api.equipment.EquipmentTarget;
import dev.rpgmenu.framework.api.input.InputAction;
import dev.rpgmenu.framework.api.inventory.InventorySort;
import dev.rpgmenu.framework.api.inventory.TransactionResult;
import dev.rpgmenu.framework.api.menu.RpgMenuTab;
import dev.rpgmenu.framework.api.menu.TabContext;
import dev.rpgmenu.framework.api.stats.StatEntry;
import dev.rpgmenu.framework.api.stats.StatGroup;
import dev.rpgmenu.framework.client.ClientBootstrap;
import dev.rpgmenu.framework.client.FavoriteStore;
import dev.rpgmenu.framework.client.input.EquipmentSelectionContext;
import dev.rpgmenu.framework.client.input.FocusRegion;
import dev.rpgmenu.framework.client.input.InputRouter;
import dev.rpgmenu.framework.client.layout.ResponsiveLayout;
import dev.rpgmenu.framework.client.layout.LayoutOverrides;
import dev.rpgmenu.framework.client.layout.UiRect;
import dev.rpgmenu.framework.client.sound.UiSoundCue;
import dev.rpgmenu.framework.client.sound.UiSoundPlayer;
import dev.rpgmenu.framework.client.theme.ThemeDefinition;
import dev.rpgmenu.framework.client.theme.ThemeManager;
import dev.rpgmenu.framework.common.config.FrameworkConfig;
import dev.rpgmenu.framework.common.network.ClientInventoryState;
import dev.rpgmenu.framework.common.network.payload.EquipmentActionPayload;
import dev.rpgmenu.framework.common.network.payload.EquipmentResultPayload;
import dev.rpgmenu.framework.common.network.payload.InventoryPagePayload;
import dev.rpgmenu.framework.common.network.payload.InventoryQueryPayload;
import dev.rpgmenu.framework.common.util.LongAmounts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Responsive, non-pausing top-level menu. The item grid is a virtual server-query view, not thousands of Slots. */
public final class RpgMenuScreen extends Screen {
    private static final int MAX_VISIBLE_CATEGORIES = 8;
    private final UUID sessionId = UUID.randomUUID();
    private final FavoriteStore favorites;
    private ResponsiveLayout layout;
    private EditBox search;
    private ResourceLocation activeTab = RpgMenuFramework.id("inventory");
    private String activeCategory = "all";
    private InventorySort sort = InventorySort.DEFAULT;
    private InventoryPagePayload page;
    private InventoryPagePayload.Entry selected;
    private int pendingSearchTicks;
    private long seenGeneration = -1;
    private float previewYaw;
    private float previewPitch;
    private boolean draggingPreview;
    private int tabOffset;
    private FocusRegion focusRegion = FocusRegion.INVENTORY;
    private EquipmentSelectionContext equipmentSelection;
    private EquipmentTarget selectedEquipmentTarget;
    private int focusedEquipmentIndex;
    private int equipmentRowOffset;
    private int focusedInventoryIndex;
    private List<RenderedEquipmentSlot> renderedEquipmentSlots = List.of();
    private InventoryPagePayload.Entry draggedInventory;
    private EquipmentTarget draggedEquipment;
    private UiRect equipmentButton = new UiRect(0, 0, 0, 0);
    private List<EquipmentSlotView> targetSelector = List.of();
    private int targetSelectorIndex;
    private int targetSelectorOffset;
    private Component statusMessage = Component.empty();
    private int statusTicks;
    private long nextEquipmentNonce = 1;
    private long pendingEquipmentNonce = -1;
    private long seenEquipmentGeneration = -1;
    private boolean openSoundPlayed;
    private boolean closeSoundPlayed;

    public RpgMenuScreen() {
        super(Component.translatable("screen.rpgmenuframework.title"));
        this.favorites = FavoriteStore.open(Minecraft.getInstance());
    }

    @Override
    protected void init() {
        layout = LayoutOverrides.INSTANCE.apply(ResponsiveLayout.calculate(width, height));
        search = new EditBox(font, layout.search().x() + 5, layout.search().y() + 2,
                Math.max(20, layout.search().width() - 10), Math.max(16, layout.search().height() - 4),
                Component.translatable("widget.rpgmenuframework.search"));
        search.setBordered(false);
        search.setMaxLength(128);
        search.setHint(Component.translatable("widget.rpgmenuframework.search_hint"));
        search.setResponder(value -> pendingSearchTicks = FrameworkConfig.SEARCH_DEBOUNCE_TICKS.get());
        addRenderableWidget(search);
        requestPage(0);
        if (!openSoundPlayed) {
            openSoundPlayed = true;
            UiSoundPlayer.play(UiSoundCue.MENU_OPEN);
        }
    }

    @Override
    public void tick() {
        if (pendingSearchTicks > 0 && --pendingSearchTicks == 0) requestPage(0);
        long generation = ClientInventoryState.generation();
        if (generation != seenGeneration) {
            InventoryPagePayload candidate = ClientInventoryState.page();
            if (candidate.sessionId().equals(sessionId)) {
                page = candidate;
                selected = page.entries().stream()
                        .filter(entry -> selected != null && entry.opaqueId() == selected.opaqueId()).findFirst()
                        .orElse(page.entries().isEmpty() ? null : page.entries().getFirst());
            }
            seenGeneration = generation;
        }
        long equipmentGeneration = ClientInventoryState.equipmentGeneration();
        if (equipmentGeneration != seenEquipmentGeneration) {
            EquipmentResultPayload result = ClientInventoryState.equipmentResult();
            if (result != null && result.sessionId().equals(sessionId)) acceptEquipmentResult(result);
            seenEquipmentGeneration = equipmentGeneration;
        }
        if (statusTicks > 0) statusTicks--;
        pollController();
    }

    public InputRouter.State inputState() {
        if (!targetSelector.isEmpty()) return InputRouter.State.MODAL;
        return search != null && search.isFocused() ? InputRouter.State.TEXT_INPUT : InputRouter.State.WORLD;
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Screen.render() invokes renderBackground(), which runs Minecraft's blur pass. The RPG UI must not
        // exist in the main target yet or the blur will process the complete menu instead of only the world.
        renderBackground(graphics, mouseX, mouseY, partialTick);

        ThemeDefinition theme = ThemeManager.INSTANCE.current();
        graphics.fill(0, 0, width, height, theme.backdrop());
        panel(graphics, layout.frame(), theme.panelAlt(), theme.borderMuted());
        renderTopTabs(graphics, mouseX, mouseY, theme);
        panel(graphics, layout.leftPanel(), theme.panel(), theme.borderMuted());
        if (layout.rightPanel().width() > 0) panel(graphics, layout.rightPanel(), theme.panel(), theme.borderMuted());

        if (isInventoryTab()) renderInventory(graphics, mouseX, mouseY, theme);
        else if (activeTab.getPath().equals("attributes")) renderStats(graphics, theme);
        else renderUnavailablePage(graphics, theme);
        renderFooter(graphics, theme);

        // Keep vanilla widgets (notably EditBox and its Font pipeline) above the menu without calling
        // Screen.render() a second time and re-running the post-processing background pass.
        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
        if (!targetSelector.isEmpty()) renderTargetSelector(graphics, mouseX, mouseY, theme);
        if (draggedInventory != null) graphics.renderItem(draggedInventory.stack(), mouseX - 8, mouseY - 8);
        if (draggedEquipment != null) {
            EquipmentSlotView dragged = equipmentViews().stream()
                    .filter(view -> view.target().equals(draggedEquipment)).findFirst().orElse(null);
            if (dragged != null && !dragged.stack().isEmpty()) graphics.renderItem(dragged.stack(), mouseX - 8, mouseY - 8);
        }
    }

    private void renderTopTabs(GuiGraphics graphics, int mouseX, int mouseY, ThemeDefinition theme) {
        List<RpgMenuTab> tabs = visibleTabs();
        int available = Math.max(1, layout.topTabs().width() - 12);
        int normalWidth = 116;
        boolean compact = tabs.size() * normalWidth > available;
        int tabWidth = compact ? 42 : Math.min(normalWidth, Math.max(54, available / Math.max(1, tabs.size())));
        int maxVisible = Math.max(1, available / tabWidth);
        tabOffset = Math.min(tabOffset, Math.max(0, tabs.size() - maxVisible));
        int x = layout.topTabs().x() + 6;
        for (int i = tabOffset; i < tabs.size() && i < tabOffset + maxVisible; i++) {
            RpgMenuTab tab = tabs.get(i);
            UiRect rect = new UiRect(x, layout.topTabs().y() + 3, tabWidth - 2, layout.topTabs().height() - 6);
            boolean active = tab.id().equals(activeTab);
            boolean hover = rect.contains(mouseX, mouseY);
            graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), active ? 0x553A2C18 : hover ? 0x332D261C : 0x11000000);
            if (active) graphics.fill(rect.x(), rect.bottom() - 2, rect.right(), rect.bottom(), theme.accent());
            Component title = Component.translatable(tab.titleKey());
            String text = compact ? title.getString().substring(0, Math.min(1, title.getString().length())) : ellipsize(title.getString(), tabWidth - 12);
            graphics.drawCenteredString(font, text, rect.x() + rect.width() / 2, rect.y() + (rect.height() - 8) / 2,
                    active ? theme.accent() : theme.text());
            if (tab.badge(tabContext()) > 0) graphics.drawString(font, Integer.toString(tab.badge(tabContext())), rect.right() - 9, rect.y() + 2, theme.danger(), true);
            x += tabWidth;
        }
        graphics.drawCenteredString(font, title, layout.topTabs().x() + layout.topTabs().width() / 2,
                Math.max(1, layout.topTabs().y() - 9), theme.textMuted());
    }

    private void renderInventory(GuiGraphics graphics, int mouseX, int mouseY, ThemeDefinition theme) {
        renderCategories(graphics, mouseX, mouseY, theme);
        panel(graphics, layout.search(), theme.panelAlt(), theme.borderMuted());
        renderGrid(graphics, mouseX, mouseY, theme);
        if (layout.details().height() > 0) renderDetails(graphics, mouseX, mouseY, theme);
        if (layout.rightPanel().width() > 0) {
            renderCharacter(graphics, mouseX, mouseY, theme);
            renderEquipment(graphics, mouseX, mouseY, theme);
        }
    }

    private void renderCategories(GuiGraphics graphics, int mouseX, int mouseY, ThemeDefinition theme) {
        var categories = RpgMenuApi.get().itemCategories().values().stream().limit(MAX_VISIBLE_CATEGORIES).toList();
        int widthEach = Math.max(35, layout.subTabs().width() / Math.max(1, categories.size()));
        int x = layout.subTabs().x();
        for (var category : categories) {
            UiRect rect = new UiRect(x, layout.subTabs().y(), Math.min(widthEach - 2, layout.subTabs().right() - x), layout.subTabs().height());
            boolean active = category.id().getPath().equals(activeCategory);
            int fill = active ? 0x553A2C18 : rect.contains(mouseX, mouseY) ? 0x332D261C : theme.panelAlt();
            graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), fill);
            border(graphics, rect, active ? theme.accent() : theme.borderMuted());
            graphics.drawCenteredString(font, ellipsize(Component.translatable(category.titleKey()).getString(), rect.width() - 5),
                    rect.x() + rect.width() / 2, rect.y() + (rect.height() - 8) / 2, active ? theme.accent() : theme.textMuted());
            x += widthEach;
            if (x >= layout.subTabs().right()) break;
        }
    }

    private void renderGrid(GuiGraphics graphics, int mouseX, int mouseY, ThemeDefinition theme) {
        UiRect grid = layout.grid();
        int slotSize = theme.slotSize();
        int columns = layout.gridColumns(slotSize);
        int rows = layout.gridRows(slotSize);
        int capacity = columns * rows;
        List<InventoryPagePayload.Entry> entries = page == null ? List.of() : page.entries();
        for (int index = 0; index < capacity; index++) {
            int x = grid.x() + (index % columns) * slotSize;
            int y = grid.y() + (index / columns) * slotSize;
            UiRect slot = new UiRect(x, y, slotSize - 2, slotSize - 2);
            InventoryPagePayload.Entry entry = index < entries.size() ? entries.get(index) : null;
            boolean hover = slot.contains(mouseX, mouseY);
            boolean active = entry != null && selected != null && entry.opaqueId() == selected.opaqueId();
            boolean controllerFocus = focusRegion == FocusRegion.INVENTORY && index == focusedInventoryIndex;
            int rarityBackground = entry == null ? theme.slot() : RpgMenuApi.get().rarityProviders().values().stream()
                    .map(provider -> provider.style(entry.stack())).flatMap(java.util.Optional::stream)
                    .findFirst().map(style -> style.backgroundColor()).orElse(theme.slot());
            graphics.fill(slot.x(), slot.y(), slot.right(), slot.bottom(), active ? theme.slotSelected() : hover ? theme.slotHover() : rarityBackground);
            border(graphics, slot, active || controllerFocus ? theme.accent() : theme.borderMuted());
            if (entry != null) {
                graphics.renderItem(entry.stack(), x + Math.max(1, (slotSize - 18) / 2), y + Math.max(1, (slotSize - 18) / 2));
                graphics.renderItemDecorations(font, entry.stack(), x + Math.max(1, (slotSize - 18) / 2),
                        y + Math.max(1, (slotSize - 18) / 2), entry.amount() == 1 ? null : LongAmounts.compact(entry.amount()));
                if (favorites.contains(entry.stack())) graphics.drawString(font, "◆", slot.x() + 1, slot.y(), theme.accent(), true);
                if (hover) graphics.renderTooltip(font, entry.stack(), mouseX, mouseY);
            }
        }
        if (page == null) {
            graphics.drawCenteredString(font, Component.translatable("message.rpgmenuframework.loading"), grid.x() + grid.width() / 2,
                    grid.y() + grid.height() / 2, theme.textMuted());
        } else if (entries.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("message.rpgmenuframework.no_items"), grid.x() + grid.width() / 2,
                    grid.y() + grid.height() / 2, theme.textMuted());
        }
        if (page != null) {
            String counter = (page.page() + 1) + " / " + Math.max(1, (page.totalEntries() + page.pageSize() - 1) / page.pageSize());
            graphics.drawString(font, counter, grid.right() - font.width(counter), grid.bottom() - 9, theme.textMuted(), false);
        }
    }

    private void renderDetails(GuiGraphics graphics, int mouseX, int mouseY, ThemeDefinition theme) {
        panel(graphics, layout.details(), theme.panelAlt(), theme.borderMuted());
        equipmentButton = new UiRect(0, 0, 0, 0);
        if (selected == null) return;
        int x = layout.details().x() + 8;
        int y = layout.details().y() + 8;
        graphics.renderItem(selected.stack(), x, y);
        graphics.drawString(font, selected.stack().getHoverName(), x + 23, y, theme.text(), false);
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(selected.stack().getItem());
        graphics.drawString(font, id.toString(), x + 23, y + 12, theme.textMuted(), false);
        graphics.drawString(font, Component.translatable("label.rpgmenuframework.exact_amount", selected.amount()), x + 23, y + 24, theme.accent(), false);
        graphics.drawString(font, Component.translatable("label.rpgmenuframework.sources", selected.sourceCount()), x + 23, y + 36, theme.textMuted(), false);
        List<EquipmentSlotView> targets = targetsFor(selected.stack());
        if (!targets.isEmpty()) {
            int buttonWidth = Math.min(74, Math.max(54, layout.details().width() / 4));
            equipmentButton = new UiRect(layout.details().right() - buttonWidth - 8,
                    layout.details().bottom() - 23, buttonWidth, 16);
            EquipmentSlotView direct = preferredTarget(targets);
            boolean replacing = direct != null && !direct.stack().isEmpty();
            int fill = equipmentButton.contains(mouseX, mouseY) ? theme.slotHover() : theme.slotSelected();
            graphics.fill(equipmentButton.x(), equipmentButton.y(), equipmentButton.right(), equipmentButton.bottom(), fill);
            border(graphics, equipmentButton, theme.accent());
            graphics.drawCenteredString(font, Component.translatable(replacing
                            ? "button.rpgmenuframework.replace" : "button.rpgmenuframework.equip"),
                    equipmentButton.x() + equipmentButton.width() / 2, equipmentButton.y() + 4, theme.accent());
        }
    }

    private void renderCharacter(GuiGraphics graphics, int mouseX, int mouseY, ThemeDefinition theme) {
        UiRect character = layout.character();
        if (character.width() < 60 || minecraft.player == null) return;
        panel(graphics, character, theme.panelAlt(), theme.borderMuted());
        int size = Math.min(78, Math.max(28, Math.min(character.width() / 2, character.height() / 3)));
        InventoryScreen.renderEntityInInventoryFollowsAngle(graphics, character.x() + 12, character.y() + 18,
                character.right() - 12, character.bottom() - 18, size, 0.0625F, previewYaw, previewPitch, minecraft.player);
        graphics.drawCenteredString(font, Component.translatable("label.rpgmenuframework.character"),
                character.x() + character.width() / 2, character.y() + 7, theme.accent());
    }

    private void renderEquipment(GuiGraphics graphics, int mouseX, int mouseY, ThemeDefinition theme) {
        if (minecraft.player == null || layout.character().width() < 120) {
            renderedEquipmentSlots = List.of();
            return;
        }
        List<EquipmentSlotView> slots = equipmentViews();
        List<RenderedEquipmentSlot> rendered = new ArrayList<>();
        ItemStack dragStack = draggedInventory == null ? ItemStack.EMPTY : draggedInventory.stack();
        EquipmentSlotView hovered = null;
        UiRect r = layout.character();
        int visibleRows = equipmentVisibleRows();
        int totalRows = (slots.size() + 1) / 2;
        equipmentRowOffset = Math.max(0, Math.min(equipmentRowOffset, Math.max(0, totalRows - visibleRows)));
        int firstIndex = equipmentRowOffset * 2;
        int lastIndex = Math.min(slots.size(), firstIndex + visibleRows * 2);
        for (int i = firstIndex; i < lastIndex; i++) {
            EquipmentSlotView equipment = slots.get(i);
            boolean left = i % 2 == 0;
            int x = left ? r.x() + 8 : r.right() - 30;
            int y = r.y() + 32 + (i / 2 - equipmentRowOffset) * 36;
            if (y + 22 >= r.bottom()) break;
            UiRect slot = new UiRect(x, y, 22, 22);
            rendered.add(new RenderedEquipmentSlot(i, equipment, slot));
            boolean hover = slot.contains(mouseX, mouseY);
            boolean selectedTarget = equipment.target().equals(selectedEquipmentTarget)
                    || equipmentSelection != null && equipment.target().equals(equipmentSelection.target());
            boolean controllerFocus = focusRegion == FocusRegion.EQUIPMENT && i == focusedEquipmentIndex;
            boolean evaluating = !dragStack.isEmpty();
            boolean validTarget = evaluating && canEquip(equipment.target(), dragStack);
            int fill = selectedTarget ? 0x665D441B : hover ? theme.slotHover() : theme.slot();
            graphics.fill(slot.x(), slot.y(), slot.right(), slot.bottom(), fill);
            int borderColor = !equipment.enabled() || evaluating && !validTarget ? theme.danger()
                    : validTarget ? 0xFF55CC66
                    : selectedTarget || controllerFocus ? theme.accent() : theme.borderMuted();
            border(graphics, slot, borderColor);
            if (!equipment.stack().isEmpty()) graphics.renderItem(equipment.stack(), x + 3, y + 3);
            String label = equipment.title().getString();
            graphics.drawCenteredString(font, ellipsize(label, 42), x + 11, y - 9, theme.textMuted());
            if (hover) hovered = equipment;
        }
        renderedEquipmentSlots = List.copyOf(rendered);
        if (totalRows > visibleRows) {
            graphics.drawCenteredString(font, (equipmentRowOffset + 1) + " / " + (totalRows - visibleRows + 1),
                    r.x() + r.width() / 2, r.bottom() - 11, theme.textMuted());
        }
        if (hovered != null) {
            if (!hovered.stack().isEmpty()) graphics.renderTooltip(font, hovered.stack(), mouseX, mouseY);
            else graphics.renderTooltip(font, hovered.title(), mouseX, mouseY);
        }
    }

    private void renderStats(GuiGraphics graphics, ThemeDefinition theme) {
        if (minecraft.player == null) return;
        int x = layout.leftPanel().x() + 16;
        int y = layout.leftPanel().y() + 16;
        for (var provider : RpgMenuApi.get().statProviders().values()) {
            for (StatGroup group : provider.groups(minecraft.player)) {
                graphics.drawString(font, Component.translatable(group.titleKey()), x, y, theme.accent(), false);
                y += 17;
                for (StatEntry entry : group.entries()) {
                    String value = entry.display() == dev.rpgmenu.framework.api.stats.StatDisplay.PERCENT
                            ? String.format(java.util.Locale.ROOT, "%.1f%%", entry.value() * 100.0)
                            : String.format(java.util.Locale.ROOT, "%.2f", entry.value());
                    graphics.drawString(font, Component.translatable(entry.titleKey()), x + 8, y, theme.text(), false);
                    graphics.drawString(font, value, layout.leftPanel().right() - 20 - font.width(value), y, theme.text(), false);
                    if (entry.display() == dev.rpgmenu.framework.api.stats.StatDisplay.PROGRESS && entry.max() > entry.min()) {
                        int barX = x + 125;
                        int barW = Math.max(30, layout.leftPanel().right() - barX - 80);
                        double ratio = Math.max(0, Math.min(1, (entry.value() - entry.min()) / (entry.max() - entry.min())));
                        graphics.fill(barX, y + 2, barX + barW, y + 7, theme.slot());
                        graphics.fill(barX, y + 2, barX + (int)(barW * ratio), y + 7, theme.accent());
                    }
                    y += 16;
                }
                y += 8;
            }
        }
        if (layout.rightPanel().width() > 0) renderCharacter(graphics, 0, 0, theme);
    }

    private void renderUnavailablePage(GuiGraphics graphics, ThemeDefinition theme) {
        graphics.drawCenteredString(font, Component.translatable("message.rpgmenuframework.provider_page"),
                layout.leftPanel().x() + layout.leftPanel().width() / 2,
                layout.leftPanel().y() + layout.leftPanel().height() / 2, theme.textMuted());
    }

    private void renderFooter(GuiGraphics graphics, ThemeDefinition theme) {
        UiRect footer = layout.footer();
        graphics.fill(footer.x(), footer.y(), footer.right(), footer.bottom(), theme.panelAlt());
        String key = equipmentSelection != null ? "footer.rpgmenuframework.select_equipment"
                : focusRegion == FocusRegion.EQUIPMENT ? "footer.rpgmenuframework.equipment"
                : "footer.rpgmenuframework.keyboard";
        String hints = Component.translatable(key).getString();
        if (statusTicks > 0 && !statusMessage.getString().isBlank()) hints = statusMessage.getString();
        graphics.drawCenteredString(font, ellipsize(hints, footer.width() - 12), footer.x() + footer.width() / 2,
                footer.y() + (footer.height() - 8) / 2, theme.textMuted());
    }

    private void renderTargetSelector(GuiGraphics graphics, int mouseX, int mouseY, ThemeDefinition theme) {
        graphics.fill(0, 0, width, height, 0x99000000);
        UiRect box = targetSelectorBox();
        panel(graphics, box, theme.panelAlt(), theme.accent());
        graphics.drawCenteredString(font, Component.translatable("title.rpgmenuframework.select_equipment_target"),
                box.x() + box.width() / 2, box.y() + 9, theme.accent());
        int visible = selectorVisibleCount();
        targetSelectorOffset = Math.max(0, Math.min(targetSelectorOffset, targetSelector.size() - visible));
        for (int display = 0; display < visible; display++) {
            int i = targetSelectorOffset + display;
            UiRect option = targetSelectorOption(box, display);
            boolean focused = i == targetSelectorIndex;
            boolean hover = option.contains(mouseX, mouseY);
            graphics.fill(option.x(), option.y(), option.right(), option.bottom(),
                    focused ? theme.slotSelected() : hover ? theme.slotHover() : theme.slot());
            border(graphics, option, focused ? theme.accent() : theme.borderMuted());
            EquipmentSlotView view = targetSelector.get(i);
            if (!view.stack().isEmpty()) graphics.renderItem(view.stack(), option.x() + 3, option.y() + 2);
            graphics.drawString(font, ellipsize(view.title().getString(), option.width() - 26),
                    option.x() + 23, option.y() + 6, theme.text(), false);
        }
    }

    private UiRect targetSelectorBox() {
        int boxWidth = Math.min(240, Math.max(140, width - 40));
        int boxHeight = 32 + selectorVisibleCount() * 24;
        return new UiRect((width - boxWidth) / 2, (height - boxHeight) / 2, boxWidth, boxHeight);
    }

    private int selectorVisibleCount() {
        return Math.max(1, Math.min(targetSelector.size(), Math.max(1, (height - 72) / 24)));
    }

    private static UiRect targetSelectorOption(UiRect box, int index) {
        return new UiRect(box.x() + 8, box.y() + 25 + index * 24, box.width() - 16, 21);
    }

    private boolean clickTargetSelector(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        UiRect box = targetSelectorBox();
        for (int display = 0; display < selectorVisibleCount(); display++) {
            int i = targetSelectorOffset + display;
            if (targetSelectorOption(box, display).contains(mouseX, mouseY)) {
                targetSelectorIndex = i;
                if (selected != null) {
                    EquipmentTarget target = targetSelector.get(i).target();
                    targetSelector = List.of();
                    UiSoundPlayer.play(UiSoundCue.CONFIRM);
                    sendEquip(selected, target);
                }
                return true;
            }
        }
        if (!box.contains(mouseX, mouseY)) closeTargetSelector();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!targetSelector.isEmpty()) return clickTargetSelector(mouseX, mouseY, button);
        if (search != null && search.isMouseOver(mouseX, mouseY)) return super.mouseClicked(mouseX, mouseY, button);
        if (search != null) search.setFocused(false);

        RenderedEquipmentSlot equipmentHit = equipmentAt(mouseX, mouseY);
        if (equipmentHit != null) {
            boolean changed = focusRegion != FocusRegion.EQUIPMENT
                    || focusedEquipmentIndex != equipmentHit.index()
                    || !equipmentHit.view().target().equals(selectedEquipmentTarget);
            selectedEquipmentTarget = equipmentHit.view().target();
            focusedEquipmentIndex = equipmentHit.index();
            focusRegion = FocusRegion.EQUIPMENT;
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !equipmentHit.view().stack().isEmpty()) {
                draggedEquipment = equipmentHit.view().target();
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && !equipmentHit.view().stack().isEmpty()) {
                sendUnequip(equipmentHit.view().target());
            }
            if (changed && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) UiSoundPlayer.play(UiSoundCue.ITEM_SELECT);
            return true;
        }
        if (equipmentButton.contains(mouseX, mouseY) && selected != null && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            beginDetailEquip();
            return true;
        }
        if (layout.character().contains(mouseX, mouseY)) { draggingPreview = true; return true; }
        List<RpgMenuTab> tabs = visibleTabs();
        int available = Math.max(1, layout.topTabs().width() - 12);
        int tabWidth = tabs.size() * 116 > available ? 42 : Math.min(116, Math.max(54, available / Math.max(1, tabs.size())));
        int maxVisible = Math.max(1, available / tabWidth);
        int x = layout.topTabs().x() + 6;
        for (int i = tabOffset; i < tabs.size() && i < tabOffset + maxVisible; i++, x += tabWidth) {
            if (new UiRect(x, layout.topTabs().y() + 3, tabWidth - 2, layout.topTabs().height() - 6).contains(mouseX, mouseY)) {
                setTab(tabs.get(i).id()); return true;
            }
        }
        if (isInventoryTab()) {
            var categories = RpgMenuApi.get().itemCategories().values().stream().limit(MAX_VISIBLE_CATEGORIES).toList();
            int widthEach = Math.max(35, layout.subTabs().width() / Math.max(1, categories.size()));
            x = layout.subTabs().x();
            for (var category : categories) {
                UiRect rect = new UiRect(x, layout.subTabs().y(), Math.min(widthEach - 2, layout.subTabs().right() - x), layout.subTabs().height());
                if (rect.contains(mouseX, mouseY)) {
                    String nextCategory = category.id().getPath();
                    if (!nextCategory.equals(activeCategory)) {
                        activeCategory = nextCategory;
                        UiSoundPlayer.play(UiSoundCue.SUBTAB_SWITCH);
                        requestPage(0);
                    }
                    return true;
                }
                x += widthEach;
            }
            int slotSize = ThemeManager.INSTANCE.current().slotSize();
            int columns = layout.gridColumns(slotSize);
            int col = (int)(mouseX - layout.grid().x()) / slotSize;
            int row = (int)(mouseY - layout.grid().y()) / slotSize;
            if (layout.grid().contains(mouseX, mouseY) && col >= 0 && col < columns && row >= 0) {
                int index = row * columns + col;
                if (page != null && index < page.entries().size()) {
                    boolean changed = focusRegion != FocusRegion.INVENTORY || focusedInventoryIndex != index
                            || selected == null || selected.opaqueId() != page.entries().get(index).opaqueId();
                    selected = page.entries().get(index);
                    focusedInventoryIndex = index;
                    focusRegion = FocusRegion.INVENTORY;
                    if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) equipSelectedFromInventory();
                    else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        draggedInventory = selected;
                        if (changed) UiSoundPlayer.play(UiSoundCue.ITEM_SELECT);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingPreview) {
            previewYaw = Math.max(-2.4F, Math.min(2.4F, previewYaw + (float)dragX * 0.025F));
            previewPitch = Math.max(-0.7F, Math.min(0.7F, previewPitch + (float)dragY * 0.015F));
            return true;
        }
        if (draggedInventory != null || draggedEquipment != null) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingPreview) {
            draggingPreview = false;
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggedInventory != null) {
            RenderedEquipmentSlot target = equipmentAt(mouseX, mouseY);
            if (target != null) {
                if (canEquip(target.view().target(), draggedInventory.stack())) sendEquip(draggedInventory, target.view().target());
                else showError("message.rpgmenuframework.invalid_equipment_item");
            }
            draggedInventory = null;
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggedEquipment != null) {
            if (layout.grid().contains(mouseX, mouseY)) sendUnequip(draggedEquipment);
            draggedEquipment = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!targetSelector.isEmpty()) {
            int maxOffset = Math.max(0, targetSelector.size() - selectorVisibleCount());
            targetSelectorOffset = Math.max(0, Math.min(maxOffset,
                    targetSelectorOffset + (scrollY < 0 ? 1 : -1)));
            return true;
        }
        if (layout.topTabs().contains(mouseX, mouseY)) {
            tabOffset = Math.max(0, tabOffset + (scrollY < 0 ? 1 : -1));
            return true;
        }
        if (isInventoryTab() && layout.character().contains(mouseX, mouseY)) {
            int maxOffset = Math.max(0, (equipmentViews().size() + 1) / 2 - equipmentVisibleRows());
            int next = Math.max(0, Math.min(maxOffset, equipmentRowOffset + (scrollY < 0 ? 1 : -1)));
            if (next != equipmentRowOffset) equipmentRowOffset = next;
            return true;
        }
        if (isInventoryTab() && layout.grid().contains(mouseX, mouseY) && page != null) {
            int next = Math.max(0, page.page() + (scrollY < 0 ? 1 : -1));
            long pages = Math.max(1, (page.totalEntries() + page.pageSize() - 1) / page.pageSize());
            next = (int)Math.min(pages - 1, next);
            if (next != page.page()) requestPage(next);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (search != null && search.isFocused()) return super.keyPressed(keyCode, scanCode, modifiers);
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && (!targetSelector.isEmpty() || equipmentSelection != null)) {
            controllerAction(InputAction.BACK);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) { controllerAction(InputAction.UP); return true; }
        if (keyCode == GLFW.GLFW_KEY_DOWN) { controllerAction(InputAction.DOWN); return true; }
        if (keyCode == GLFW.GLFW_KEY_LEFT) { controllerAction(InputAction.LEFT); return true; }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) { controllerAction(InputAction.RIGHT); return true; }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            controllerAction(InputAction.CONFIRM);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP) { cycleTab(-1); return true; }
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) { cycleTab(1); return true; }
        if (keyCode == GLFW.GLFW_KEY_Y && selected != null) { toggleFavorite(); return true; }
        if (keyCode == GLFW.GLFW_KEY_C) { ClientBootstrap.openVanillaInventory(); return true; }
        if (isMovementKey(keyCode, scanCode)) return false;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        InputRouter.release(minecraft);
        draggedInventory = null;
        draggedEquipment = null;
        if (openSoundPlayed && !closeSoundPlayed) {
            closeSoundPlayed = true;
            UiSoundPlayer.play(UiSoundCue.MENU_CLOSE);
        }
        super.removed();
    }

    private boolean isMovementKey(int keyCode, int scanCode) {
        return minecraft.options.keyUp.matches(keyCode, scanCode) || minecraft.options.keyDown.matches(keyCode, scanCode)
                || minecraft.options.keyLeft.matches(keyCode, scanCode) || minecraft.options.keyRight.matches(keyCode, scanCode)
                || minecraft.options.keyJump.matches(keyCode, scanCode) || minecraft.options.keyShift.matches(keyCode, scanCode);
    }

    private void pollController() {
        for (var provider : RpgMenuApi.get().controllerInputProviders().values()) {
            if (!provider.active()) continue;
            for (InputAction action : InputAction.values()) {
                if (provider.consume(action)) {
                    controllerAction(action);
                    return;
                }
            }
        }
    }

    private void controllerAction(InputAction action) {
        if (!targetSelector.isEmpty()) {
            if (action == InputAction.UP || action == InputAction.LEFT) {
                int previous = targetSelectorIndex;
                targetSelectorIndex = Math.floorMod(targetSelectorIndex - 1, targetSelector.size());
                ensureSelectorFocusVisible();
                if (targetSelectorIndex != previous) UiSoundPlayer.play(UiSoundCue.FOCUS_MOVE);
            } else if (action == InputAction.DOWN || action == InputAction.RIGHT) {
                int previous = targetSelectorIndex;
                targetSelectorIndex = Math.floorMod(targetSelectorIndex + 1, targetSelector.size());
                ensureSelectorFocusVisible();
                if (targetSelectorIndex != previous) UiSoundPlayer.play(UiSoundCue.FOCUS_MOVE);
            } else if (action == InputAction.CONFIRM && selected != null) {
                EquipmentTarget target = targetSelector.get(targetSelectorIndex).target();
                targetSelector = List.of();
                UiSoundPlayer.play(UiSoundCue.CONFIRM);
                sendEquip(selected, target);
            } else if (action == InputAction.BACK) {
                closeTargetSelector();
            }
            return;
        }
        if (action == InputAction.PREVIOUS_TAB) { cycleTab(-1); return; }
        if (action == InputAction.NEXT_TAB) { cycleTab(1); return; }
        if (action == InputAction.FAVORITE && selected != null) { toggleFavorite(); return; }

        if (equipmentSelection != null) {
            if (action == InputAction.BACK) {
                cancelEquipmentSelection();
            } else if (action == InputAction.CONFIRM) {
                InventoryPagePayload.Entry entry = focusedEntry();
                if (entry != null) sendEquip(entry, equipmentSelection.target());
            } else if (isDirection(action)) {
                moveInventoryFocus(action);
            }
            return;
        }

        if (focusRegion == FocusRegion.EQUIPMENT) {
            if (action == InputAction.CONFIRM) {
                beginEquipmentSelection(focusedEquipmentIndex);
            } else if (action == InputAction.BACK) {
                EquipmentSlotView slot = equipmentView(focusedEquipmentIndex);
                if (slot != null && !slot.stack().isEmpty()) sendUnequip(slot.target());
            } else if (isDirection(action)) {
                moveEquipmentFocus(action);
            }
            return;
        }

        if (focusRegion == FocusRegion.INVENTORY) {
            if (isDirection(action)) moveInventoryFocus(action);
            else if (action == InputAction.CONFIRM) {
                InventoryPagePayload.Entry entry = focusedEntry();
                if (entry != null) {
                    selected = entry;
                    UiSoundPlayer.play(UiSoundCue.ITEM_SELECT);
                }
            } else if (action == InputAction.BACK) onClose();
        }
    }

    private void moveInventoryFocus(InputAction action) {
        FocusRegion previousRegion = focusRegion;
        int previousIndex = focusedInventoryIndex;
        int count = page == null ? 0 : page.entries().size();
        if (count == 0) {
            focusedInventoryIndex = 0;
            if (equipmentSelection == null && action == InputAction.RIGHT && !equipmentViews().isEmpty()) {
                focusRegion = FocusRegion.EQUIPMENT;
                focusedEquipmentIndex = Math.min(focusedEquipmentIndex, equipmentViews().size() - 1);
                selectedEquipmentTarget = equipmentViews().get(focusedEquipmentIndex).target();
            }
            playInventoryFocusChange(previousRegion, previousIndex);
            return;
        }
        int columns = Math.max(1, layout.gridColumns(ThemeManager.INSTANCE.current().slotSize()));
        if (equipmentSelection == null && action == InputAction.RIGHT
                && (focusedInventoryIndex % columns == columns - 1
                || focusedInventoryIndex + 1 >= count)
                && !equipmentViews().isEmpty()) {
            focusRegion = FocusRegion.EQUIPMENT;
            focusedEquipmentIndex = Math.min(focusedEquipmentIndex, equipmentViews().size() - 1);
            selectedEquipmentTarget = equipmentViews().get(focusedEquipmentIndex).target();
            playInventoryFocusChange(previousRegion, previousIndex);
            return;
        }
        int delta = switch (action) {
            case LEFT -> -1;
            case RIGHT -> 1;
            case UP -> -columns;
            case DOWN -> columns;
            default -> 0;
        };
        focusedInventoryIndex = Math.max(0, Math.min(count - 1, focusedInventoryIndex + delta));
        selected = page.entries().get(focusedInventoryIndex);
        playInventoryFocusChange(previousRegion, previousIndex);
    }

    private void moveEquipmentFocus(InputAction action) {
        FocusRegion previousRegion = focusRegion;
        int previousIndex = focusedEquipmentIndex;
        int count = equipmentViews().size();
        if (count == 0) { focusedEquipmentIndex = 0; return; }
        if (action == InputAction.LEFT && focusedEquipmentIndex % 2 == 0) {
            focusRegion = FocusRegion.INVENTORY;
            int columns = Math.max(1, layout.gridColumns(ThemeManager.INSTANCE.current().slotSize()));
            int entryCount = page == null ? 0 : page.entries().size();
            if (entryCount > 0) focusedInventoryIndex = Math.min(entryCount - 1,
                    focusedInventoryIndex / columns * columns + columns - 1);
            playEquipmentFocusChange(previousRegion, previousIndex);
            return;
        }
        int delta = switch (action) {
            case LEFT -> -1;
            case RIGHT -> 1;
            case UP -> -2;
            case DOWN -> 2;
            default -> 0;
        };
        focusedEquipmentIndex = Math.floorMod(focusedEquipmentIndex + delta, count);
        ensureEquipmentFocusVisible();
        selectedEquipmentTarget = equipmentViews().get(focusedEquipmentIndex).target();
        playEquipmentFocusChange(previousRegion, previousIndex);
    }

    private void beginEquipmentSelection(int index) {
        EquipmentSlotView view = equipmentView(index);
        if (view == null || !view.enabled()) {
            showError("message.rpgmenuframework.invalid_equipment_target");
            return;
        }
        selectedEquipmentTarget = view.target();
        equipmentSelection = new EquipmentSelectionContext(view.target(), index);
        focusRegion = FocusRegion.INVENTORY;
        focusedInventoryIndex = 0;
        UiSoundPlayer.play(UiSoundCue.CONFIRM);
        requestPage(0);
    }

    private void cancelEquipmentSelection() {
        if (equipmentSelection == null) return;
        focusedEquipmentIndex = equipmentSelection.originEquipmentIndex();
        selectedEquipmentTarget = equipmentSelection.target();
        equipmentSelection = null;
        focusRegion = FocusRegion.EQUIPMENT;
        UiSoundPlayer.play(UiSoundCue.CANCEL);
        requestPage(0);
    }

    private void beginDetailEquip() {
        if (selected == null) return;
        List<EquipmentSlotView> targets = targetsFor(selected.stack());
        EquipmentSlotView direct = preferredTarget(targets);
        if (direct != null) {
            UiSoundPlayer.play(UiSoundCue.CONFIRM);
            sendEquip(selected, direct.target());
        } else if (!targets.isEmpty()) {
            targetSelector = targets;
            targetSelectorIndex = 0;
            targetSelectorOffset = 0;
            UiSoundPlayer.play(UiSoundCue.MODAL_OPEN);
        } else {
            showError("message.rpgmenuframework.invalid_equipment_item");
        }
    }

    private void equipSelectedFromInventory() {
        if (selected == null) return;
        if (equipmentSelection != null) {
            sendEquip(selected, equipmentSelection.target());
            return;
        }
        beginDetailEquip();
    }

    private void sendEquip(InventoryPagePayload.Entry entry, EquipmentTarget target) {
        if (entry == null || target == null || !canEquip(target, entry.stack()) || minecraft.getConnection() == null) {
            showError("message.rpgmenuframework.invalid_equipment_item");
            return;
        }
        selectedEquipmentTarget = target;
        pendingEquipmentNonce = nextEquipmentNonce++;
        PacketDistributor.sendToServer(new EquipmentActionPayload(sessionId, entry.opaqueId(), target,
                EquipmentAction.EQUIP, pendingEquipmentNonce));
    }

    private void sendUnequip(EquipmentTarget target) {
        if (target == null) {
            showError("message.rpgmenuframework.invalid_equipment_target");
            return;
        }
        if (minecraft.getConnection() == null) return;
        selectedEquipmentTarget = target;
        pendingEquipmentNonce = nextEquipmentNonce++;
        PacketDistributor.sendToServer(new EquipmentActionPayload(sessionId, 0, target,
                EquipmentAction.UNEQUIP, pendingEquipmentNonce));
    }

    private void acceptEquipmentResult(EquipmentResultPayload result) {
        showStatus(result.messageKey());
        if (result.status() != TransactionResult.Status.SUCCESS) {
            UiSoundPlayer.play(UiSoundCue.ERROR);
            return;
        }
        switch (result.messageKey()) {
            case "message.rpgmenuframework.replaced" -> UiSoundPlayer.play(UiSoundCue.REPLACE_EQUIPMENT);
            case "message.rpgmenuframework.unequipped" -> UiSoundPlayer.play(UiSoundCue.UNEQUIP);
            case "message.rpgmenuframework.equipped" -> UiSoundPlayer.play(UiSoundCue.EQUIP);
            default -> UiSoundPlayer.play(UiSoundCue.CONFIRM);
        }
        if (equipmentSelection != null && result.nonce() == pendingEquipmentNonce
                && equipmentSelection.target().equals(result.target())) {
            focusedEquipmentIndex = equipmentSelection.originEquipmentIndex();
            selectedEquipmentTarget = equipmentSelection.target();
            equipmentSelection = null;
            focusRegion = FocusRegion.EQUIPMENT;
        }
        requestPage(page == null ? 0 : page.page());
    }

    private void showStatus(String messageKey) {
        statusMessage = messageKey == null || messageKey.isBlank() ? Component.empty() : Component.translatable(messageKey);
        statusTicks = 80;
    }

    private void showError(String messageKey) {
        showStatus(messageKey);
        UiSoundPlayer.play(UiSoundCue.ERROR);
    }

    private void toggleFavorite() {
        if (selected == null) return;
        favorites.toggle(selected.stack());
        UiSoundPlayer.play(UiSoundCue.FAVORITE);
    }

    private void closeTargetSelector() {
        if (targetSelector.isEmpty()) return;
        targetSelector = List.of();
        UiSoundPlayer.play(UiSoundCue.MODAL_CLOSE);
    }

    private void playInventoryFocusChange(FocusRegion previousRegion, int previousIndex) {
        if (focusRegion != previousRegion
                || (focusRegion == FocusRegion.INVENTORY && focusedInventoryIndex != previousIndex)) {
            UiSoundPlayer.play(UiSoundCue.FOCUS_MOVE);
        }
    }

    private void playEquipmentFocusChange(FocusRegion previousRegion, int previousIndex) {
        if (focusRegion != previousRegion
                || (focusRegion == FocusRegion.EQUIPMENT && focusedEquipmentIndex != previousIndex)) {
            UiSoundPlayer.play(UiSoundCue.FOCUS_MOVE);
        }
    }

    private List<EquipmentSlotView> equipmentViews() {
        if (minecraft == null || minecraft.player == null) return List.of();
        return RpgMenuApi.get().equipmentProviders().values().stream()
                .flatMap(provider -> provider.slots(minecraft.player).stream()).toList();
    }

    private EquipmentSlotView equipmentView(int index) {
        List<EquipmentSlotView> views = equipmentViews();
        return index >= 0 && index < views.size() ? views.get(index) : null;
    }

    private boolean canEquip(EquipmentTarget target, ItemStack stack) {
        if (minecraft == null || minecraft.player == null || target == null || stack.isEmpty()) return false;
        EquipmentProvider provider = RpgMenuApi.get().equipmentProviders().get(target.providerId()).orElse(null);
        return provider != null && provider.canEquip(minecraft.player, target, stack);
    }

    private List<EquipmentSlotView> targetsFor(ItemStack stack) {
        List<EquipmentSlotView> targets = equipmentViews().stream()
                .filter(EquipmentSlotView::enabled)
                .filter(view -> canEquip(view.target(), stack))
                .collect(Collectors.toCollection(ArrayList::new));
        boolean hasSpecializedTarget = targets.stream().anyMatch(view ->
                !view.target().providerId().equals(RpgMenuFramework.id("vanilla"))
                        || !view.target().slotKey().equals("mainhand"));
        if (hasSpecializedTarget) {
            targets.removeIf(view -> view.target().providerId().equals(RpgMenuFramework.id("vanilla"))
                    && view.target().slotKey().equals("mainhand"));
        }
        return List.copyOf(targets);
    }

    private EquipmentSlotView preferredTarget(List<EquipmentSlotView> targets) {
        if (selectedEquipmentTarget != null) {
            for (EquipmentSlotView view : targets) if (view.target().equals(selectedEquipmentTarget)) return view;
        }
        return targets.size() == 1 ? targets.getFirst() : null;
    }

    private InventoryPagePayload.Entry focusedEntry() {
        if (page == null || focusedInventoryIndex < 0 || focusedInventoryIndex >= page.entries().size()) return null;
        return page.entries().get(focusedInventoryIndex);
    }

    private RenderedEquipmentSlot equipmentAt(double mouseX, double mouseY) {
        for (RenderedEquipmentSlot rendered : renderedEquipmentSlots) {
            if (rendered.rect().contains(mouseX, mouseY)) return rendered;
        }
        return null;
    }

    private static boolean isDirection(InputAction action) {
        return action == InputAction.UP || action == InputAction.DOWN
                || action == InputAction.LEFT || action == InputAction.RIGHT;
    }

    private int equipmentVisibleRows() {
        if (layout == null) return 1;
        int available = layout.character().height() - 55;
        return Math.max(1, available < 0 ? 1 : available / 36 + 1);
    }

    private void ensureEquipmentFocusVisible() {
        int row = focusedEquipmentIndex / 2;
        int visibleRows = equipmentVisibleRows();
        if (row < equipmentRowOffset) equipmentRowOffset = row;
        else if (row >= equipmentRowOffset + visibleRows) equipmentRowOffset = row - visibleRows + 1;
    }

    private void ensureSelectorFocusVisible() {
        int visible = selectorVisibleCount();
        if (targetSelectorIndex < targetSelectorOffset) targetSelectorOffset = targetSelectorIndex;
        else if (targetSelectorIndex >= targetSelectorOffset + visible) {
            targetSelectorOffset = targetSelectorIndex - visible + 1;
        }
    }

    private void cycleTab(int direction) {
        List<RpgMenuTab> tabs = visibleTabs();
        if (tabs.isEmpty()) return;
        int current = 0;
        for (int i = 0; i < tabs.size(); i++) if (tabs.get(i).id().equals(activeTab)) current = i;
        setTab(tabs.get(Math.floorMod(current + direction, tabs.size())).id());
    }

    private void setTab(ResourceLocation id) {
        if (activeTab.equals(id)) return;
        activeTab = id;
        targetSelector = List.of();
        equipmentSelection = null;
        search.visible = isInventoryTab();
        search.setFocused(false);
        UiSoundPlayer.play(UiSoundCue.TAB_SWITCH);
        if (isInventoryTab()) requestPage(page == null ? 0 : page.page());
    }

    private boolean isInventoryTab() { return activeTab.getPath().equals("inventory"); }

    private void requestPage(int pageIndex) {
        if (minecraft == null || minecraft.getConnection() == null || !isInventoryTab()) return;
        int capacity = Math.min(256, layout.gridColumns(ThemeManager.INSTANCE.current().slotSize())
                * layout.gridRows(ThemeManager.INSTANCE.current().slotSize()));
        EquipmentTarget filter = equipmentSelection == null ? null : equipmentSelection.target();
        PacketDistributor.sendToServer(new InventoryQueryPayload(sessionId, search == null ? "" : search.getValue(),
                activeCategory, sort, pageIndex, Math.max(1, capacity), filter));
    }

    private List<RpgMenuTab> visibleTabs() { return RpgMenuApi.get().tabs().visible(tabContext()); }

    private TabContext tabContext() {
        Set<String> mods = ModList.get().getMods().stream().map(info -> info.getModId()).collect(Collectors.toUnmodifiableSet());
        UUID playerId = minecraft != null && minecraft.player != null ? minecraft.player.getUUID() : new UUID(0, 0);
        return new TabContext(playerId, mods, false);
    }

    private static void panel(GuiGraphics graphics, UiRect rect, int fill, int border) {
        if (rect.width() <= 0 || rect.height() <= 0) return;
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), fill);
        border(graphics, rect, border);
    }

    private static void border(GuiGraphics graphics, UiRect rect, int color) {
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, color);
        graphics.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), color);
        graphics.fill(rect.x(), rect.y(), rect.x() + 1, rect.bottom(), color);
        graphics.fill(rect.right() - 1, rect.y(), rect.right(), rect.bottom(), color);
    }

    private String ellipsize(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) return value;
        String suffix = "…";
        int length = value.length();
        while (length > 0 && font.width(value.substring(0, length) + suffix) > maxWidth) length--;
        return value.substring(0, length) + suffix;
    }

    private record RenderedEquipmentSlot(int index, EquipmentSlotView view, UiRect rect) {}
}
