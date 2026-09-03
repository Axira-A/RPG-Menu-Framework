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
import dev.rpgmenu.framework.api.spells.SpellEntry;
import dev.rpgmenu.framework.client.compat.ftbquests.FtbQuestEmbeddedView;
import dev.rpgmenu.framework.client.ClientBootstrap;
import dev.rpgmenu.framework.client.FavoriteStore;
import dev.rpgmenu.framework.client.input.EquipmentSelectionContext;
import dev.rpgmenu.framework.client.input.FocusRegion;
import dev.rpgmenu.framework.client.input.InputRouter;
import dev.rpgmenu.framework.client.layout.ResponsiveLayout;
import dev.rpgmenu.framework.client.layout.CharacterEquipmentLayout;
import dev.rpgmenu.framework.client.layout.ContentLayoutMode;
import dev.rpgmenu.framework.client.layout.LayoutOverrides;
import dev.rpgmenu.framework.client.layout.UiRect;
import dev.rpgmenu.framework.client.map.EmbeddedMapHost;
import dev.rpgmenu.framework.client.skills.EmbeddedSkillTreeHost;
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
import dev.rpgmenu.framework.common.network.payload.QuickbarActionPayload;
import dev.rpgmenu.framework.common.util.LongAmounts;
import dev.rpgmenu.framework.common.equipment.HotbarEquipmentProvider;
import dev.rpgmenu.framework.common.equipment.VanillaEquipmentProvider;
import dev.rpgmenu.framework.common.compat.moreoffhandslots.MoreOffhandSlotsEquipmentProvider;
import dev.rpgmenu.framework.api.inventory.QuickbarAction;
import dev.rpgmenu.framework.api.inventory.QuickEquipKind;
import dev.rpgmenu.framework.api.inventory.QuickSlotGroup;
import dev.rpgmenu.framework.api.inventory.QuickSlotTarget;
import dev.rpgmenu.framework.common.inventory.QuickEquipResolver;
import dev.rpgmenu.framework.common.inventory.QuickSlotTargets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
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
    private FtbQuestEmbeddedView embeddedQuestView;
    private EmbeddedMapHost embeddedMapHost;
    private EmbeddedSkillTreeHost embeddedSkillTreeHost;
    private List<TargetSelectorOption> targetSelector = List.of();
    private int targetSelectorIndex;
    private SelectorKind targetSelectorKind = SelectorKind.GENERAL;
    private Component targetSelectorTitle = Component.empty();
    private EquipmentTarget invalidEquipmentTarget;
    private int invalidTargetTicks;
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
        if (invalidTargetTicks > 0 && --invalidTargetTicks == 0) invalidEquipmentTarget = null;
        if (isQuestsTab() && embeddedQuestView != null) embeddedQuestView.tick(questViewport(), currentMouseX(), currentMouseY());
        if (isMapTab() && embeddedMapHost != null) embeddedMapHost.tick();
        if (isSkillsTab() && embeddedSkillTreeHost != null) embeddedSkillTreeHost.tick();
        pollController();
    }

    public InputRouter.State inputState() {
        if (!targetSelector.isEmpty()) return InputRouter.State.MODAL;
        if (isMapTab() && embeddedMapHost != null && embeddedMapHost.hasTextInputFocus()) {
            return InputRouter.State.TEXT_INPUT;
        }
        if (isSkillsTab() && embeddedSkillTreeHost != null && embeddedSkillTreeHost.hasTextInputFocus()) {
            return InputRouter.State.TEXT_INPUT;
        }
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
        if (contentLayoutMode() == ContentLayoutMode.INVENTORY_SPLIT) {
            panel(graphics, layout.leftPanel(), theme.panel(), theme.borderMuted());
            if (layout.rightPanel().width() > 0) panel(graphics, layout.rightPanel(), theme.panel(), theme.borderMuted());
        } else {
            panel(graphics, contentViewport(), theme.panel(), theme.borderMuted());
        }

        if (isInventoryTab()) renderInventory(graphics, mouseX, mouseY, theme);
        else if (activeTab.getPath().equals("attributes")) renderStats(graphics, theme);
        else if ("spells".equals(activeContentMarker())) renderSpells(graphics, theme);
        else if ("skills".equals(activeContentMarker())) renderSkills(graphics, mouseX, mouseY, partialTick, theme);
        else if ("quests".equals(activeContentMarker())) renderQuests(graphics, mouseX, mouseY, partialTick, theme);
        else if ("map".equals(activeContentMarker())) renderMap(graphics, mouseX, mouseY, partialTick, theme);
        else renderUnavailablePage(graphics, theme);

        // Embedded apps render first. The shared navigation chrome always stays above their content.
        renderTopTabs(graphics, mouseX, mouseY, theme);
        renderFooter(graphics, theme);

        // Keep vanilla widgets (notably EditBox and its Font pipeline) above the menu without calling
        // Screen.render() a second time and re-running the post-processing background pass.
        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
        if (targetSelector.isEmpty() && draggedInventory != null) graphics.renderItem(draggedInventory.stack(), mouseX - 8, mouseY - 8);
        if (targetSelector.isEmpty() && draggedEquipment != null) {
            EquipmentSlotView dragged = equipmentViews().stream()
                    .filter(view -> view.target().equals(draggedEquipment)).findFirst().orElse(null);
            if (dragged != null && !dragged.stack().isEmpty()) graphics.renderItem(dragged.stack(), mouseX - 8, mouseY - 8);
        }
        if (!targetSelector.isEmpty()) renderTargetSelector(graphics, mouseX, mouseY, theme);
    }

    private void renderTopTabs(GuiGraphics graphics, int mouseX, int mouseY, ThemeDefinition theme) {
        MenuTabWidget.Layout tabLayout = topTabLayout();
        tabOffset = tabLayout.offset();
        for (MenuTabWidget widget : tabLayout.widgets()) {
            widget.render(graphics, font, mouseX, mouseY, theme, widget.tab().badge(tabContext()));
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
        if (autoEquipAvailable(selected.stack())) {
            int buttonWidth = Math.min(74, Math.max(54, layout.details().width() / 4));
            equipmentButton = new UiRect(layout.details().right() - buttonWidth - 8,
                    layout.details().bottom() - 23, buttonWidth, 16);
            boolean replacing = firstEmptyAutomaticTarget(selected.stack()) == null;
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
        UiRect preview = calculateCharacterEquipmentLayout().preview();
        int size = Math.min(78, Math.max(24, Math.min(preview.width(), preview.height()) / 2));
        InventoryScreen.renderEntityInInventoryFollowsAngle(graphics, preview.x(), preview.y(),
                preview.right(), preview.bottom(), size, 0.0625F, previewYaw, previewPitch, minecraft.player);
    }

    private void renderEquipment(GuiGraphics graphics, int mouseX, int mouseY, ThemeDefinition theme) {
        if (minecraft.player == null || layout.character().width() < 120) {
            renderedEquipmentSlots = List.of();
            return;
        }
        CharacterEquipmentLayout equipmentLayout = calculateCharacterEquipmentLayout();
        List<EquipmentSlotView> slots = equipmentViews();
        List<RenderedEquipmentSlot> rendered = new ArrayList<>();
        ItemStack dragStack = draggedInventory == null ? ItemStack.EMPTY : draggedInventory.stack();
        EquipmentSlotView hovered = null;

        List<IndexedEquipmentSlot> hotbar = indexed(slots, view -> HotbarEquipmentProvider.ID.equals(view.target().providerId()));
        List<IndexedEquipmentSlot> mainHand = hotbar.stream()
                .filter(slot -> slot.view().target().slotIndex() < QuickSlotTargets.MAIN_HAND_COUNT).toList();
        List<IndexedEquipmentSlot> itemBar = hotbar.stream()
                .filter(slot -> slot.view().target().slotIndex() >= QuickSlotTargets.MAIN_HAND_COUNT).toList();
        List<IndexedEquipmentSlot> offhands = indexed(slots, this::isOffhandSlot);
        List<IndexedEquipmentSlot> armor = List.of(
                indexedSlot(slots, VanillaEquipmentProvider.HEAD), indexedSlot(slots, VanillaEquipmentProvider.CHEST),
                indexedSlot(slots, VanillaEquipmentProvider.LEGS), indexedSlot(slots, VanillaEquipmentProvider.FEET));
        List<IndexedEquipmentSlot> sides = indexed(slots, view -> !isQuickbarSlot(view)
                && !isOffhandSlot(view) && !isArmorSlot(view) && !isLegacyMainhandSlot(view));

        UiRect character = layout.character();
        graphics.drawCenteredString(font, Component.translatable("label.rpgmenuframework.main_hand_quickbar"),
                character.x() + character.width() / 4, equipmentLayout.mainTitleY(), theme.textMuted());
        graphics.drawCenteredString(font, Component.translatable("equipment.rpgmenuframework.offhand"),
                character.x() + character.width() * 3 / 4, equipmentLayout.offhandTitleY(), theme.textMuted());
        graphics.drawCenteredString(font, Component.translatable("label.rpgmenuframework.item_quickbar"),
                character.x() + character.width() / 2, equipmentLayout.itemTitleY(), theme.textMuted());

        hovered = renderSlotGroup(graphics, mouseX, mouseY, theme, dragStack, rendered, mainHand,
                equipmentLayout.mainHandSlots(), FocusRegion.MAIN_HAND_QUICKBAR, false, hovered);
        hovered = renderSlotGroup(graphics, mouseX, mouseY, theme, dragStack, rendered, offhands,
                equipmentLayout.offhandSlots(), FocusRegion.OFFHAND_QUICKBAR, false, hovered);
        hovered = renderSlotGroup(graphics, mouseX, mouseY, theme, dragStack, rendered, itemBar,
                equipmentLayout.itemQuickbarSlots(), FocusRegion.ITEM_QUICKBAR, false, hovered);
        hovered = renderSlotGroup(graphics, mouseX, mouseY, theme, dragStack, rendered, armor,
                equipmentLayout.armorSlots(), FocusRegion.EQUIPMENT, true, hovered);

        int visibleSideSlots = equipmentLayout.sideEquipmentSlots().size();
        int totalSideRows = (sides.size() + 1) / 2;
        int visibleSideRows = equipmentLayout.sideRowCapacity();
        equipmentRowOffset = Math.max(0, Math.min(equipmentRowOffset,
                Math.max(0, totalSideRows - visibleSideRows)));
        int firstSide = Math.min(sides.size(), equipmentRowOffset * 2);
        List<IndexedEquipmentSlot> visibleSides = sides.subList(firstSide,
                Math.min(sides.size(), firstSide + visibleSideSlots));
        hovered = renderSlotGroup(graphics, mouseX, mouseY, theme, dragStack, rendered, visibleSides,
                equipmentLayout.sideEquipmentSlots(), FocusRegion.EQUIPMENT, true, hovered);

        renderedEquipmentSlots = List.copyOf(rendered);
        if (totalSideRows > visibleSideRows && visibleSideRows > 0) {
            graphics.drawCenteredString(font, (equipmentRowOffset + 1) + " / "
                            + (totalSideRows - visibleSideRows + 1),
                    character.x() + character.width() / 2, equipmentLayout.itemTitleY() - 10, theme.textMuted());
        }
        if (hovered != null) {
            if (!hovered.stack().isEmpty()) graphics.renderTooltip(font, hovered.stack(), mouseX, mouseY);
            else graphics.renderTooltip(font, hovered.title(), mouseX, mouseY);
        }
    }

    private EquipmentSlotView renderSlotGroup(GuiGraphics graphics, int mouseX, int mouseY, ThemeDefinition theme,
                                               ItemStack dragStack, List<RenderedEquipmentSlot> rendered,
                                               List<IndexedEquipmentSlot> views, List<UiRect> rects,
                                               FocusRegion region, boolean labels,
                                               EquipmentSlotView hovered) {
        int count = Math.min(views.size(), rects.size());
        for (int local = 0; local < count; local++) {
            IndexedEquipmentSlot indexed = views.get(local);
            EquipmentSlotView equipment = indexed.view();
            UiRect slot = rects.get(local);
            rendered.add(new RenderedEquipmentSlot(indexed.index(), equipment, slot, region));
            boolean hover = slot.contains(mouseX, mouseY);
            boolean selectedTarget = equipment.target().equals(selectedEquipmentTarget)
                    || equipmentSelection != null && equipment.target().equals(equipmentSelection.target());
            boolean activeHotbar = HotbarEquipmentProvider.resolve(equipment.target())
                    == minecraft.player.getInventory().selected;
            boolean controllerFocus = focusRegion == region && indexed.index() == focusedEquipmentIndex;
            boolean evaluating = !dragStack.isEmpty();
            boolean validTarget = evaluating && canEquip(equipment.target(), dragStack);
            int fill = selectedTarget || activeHotbar ? 0x665D441B : hover ? theme.slotHover() : theme.slot();
            graphics.fill(slot.x(), slot.y(), slot.right(), slot.bottom(), fill);
            boolean recentlyInvalid = invalidTargetTicks > 0 && equipment.target().equals(invalidEquipmentTarget);
            int borderColor = !equipment.enabled() || recentlyInvalid || evaluating && !validTarget ? theme.danger()
                    : validTarget ? 0xFF55CC66
                    : selectedTarget || activeHotbar || controllerFocus ? theme.accent() : theme.borderMuted();
            border(graphics, slot, borderColor);
            if (!equipment.stack().isEmpty()) {
                int itemX = slot.x() + Math.max(1, (slot.width() - 16) / 2);
                int itemY = slot.y() + Math.max(1, (slot.height() - 16) / 2);
                graphics.renderItem(equipment.stack(), itemX, itemY);
                graphics.renderItemDecorations(font, equipment.stack(), itemX, itemY);
            }
            if (labels) {
                graphics.drawCenteredString(font, ellipsize(equipment.title().getString(), 48),
                        slot.x() + slot.width() / 2, slot.y() - 9, theme.textMuted());
            }
            if (hover) hovered = equipment;
        }
        return hovered;
    }

    private CharacterEquipmentLayout calculateCharacterEquipmentLayout() {
        List<EquipmentSlotView> views = equipmentViews();
        int offhands = (int)views.stream().filter(this::isOffhandSlot).count();
        int sides = (int)views.stream().filter(view -> !isQuickbarSlot(view)
                && !isOffhandSlot(view) && !isArmorSlot(view) && !isLegacyMainhandSlot(view)).count();
        return CharacterEquipmentLayout.calculate(layout.character(), Math.max(1, offhands), sides);
    }

    private static List<IndexedEquipmentSlot> indexed(List<EquipmentSlotView> slots,
                                                       java.util.function.Predicate<EquipmentSlotView> predicate) {
        List<IndexedEquipmentSlot> result = new ArrayList<>();
        for (int index = 0; index < slots.size(); index++) {
            EquipmentSlotView view = slots.get(index);
            if (predicate.test(view)) result.add(new IndexedEquipmentSlot(index, view));
        }
        return List.copyOf(result);
    }

    private static IndexedEquipmentSlot indexedSlot(List<EquipmentSlotView> slots, EquipmentTarget target) {
        for (int index = 0; index < slots.size(); index++) {
            if (slots.get(index).target().equals(target)) return new IndexedEquipmentSlot(index, slots.get(index));
        }
        throw new IllegalStateException("Missing built-in equipment slot " + target);
    }

    private boolean isQuickbarSlot(EquipmentSlotView view) {
        return HotbarEquipmentProvider.ID.equals(view.target().providerId());
    }

    private boolean isOffhandSlot(EquipmentSlotView view) {
        return VanillaEquipmentProvider.OFFHAND.equals(view.target())
                || MoreOffhandSlotsEquipmentProvider.ID.equals(view.target().providerId());
    }

    private boolean isArmorSlot(EquipmentSlotView view) {
        EquipmentTarget target = view.target();
        return VanillaEquipmentProvider.HEAD.equals(target) || VanillaEquipmentProvider.CHEST.equals(target)
                || VanillaEquipmentProvider.LEGS.equals(target) || VanillaEquipmentProvider.FEET.equals(target);
    }

    private boolean isLegacyMainhandSlot(EquipmentSlotView view) {
        return VanillaEquipmentProvider.MAINHAND.equals(view.target());
    }

    private void renderStats(GuiGraphics graphics, ThemeDefinition theme) {
        if (minecraft.player == null) return;
        UiRect content = contentViewport();
        int x = content.x() + 16;
        int y = content.y() + 16;
        for (var provider : RpgMenuApi.get().statProviders().values()) {
            for (StatGroup group : provider.groups(minecraft.player)) {
                graphics.drawString(font, Component.translatable(group.titleKey()), x, y, theme.accent(), false);
                y += 17;
                for (StatEntry entry : group.entries()) {
                    String value = entry.display() == dev.rpgmenu.framework.api.stats.StatDisplay.PERCENT
                            ? String.format(java.util.Locale.ROOT, "%.1f%%", entry.value() * 100.0)
                            : String.format(java.util.Locale.ROOT, "%.2f", entry.value());
                    graphics.drawString(font, Component.translatable(entry.titleKey()), x + 8, y, theme.text(), false);
                    graphics.drawString(font, value, content.right() - 20 - font.width(value), y, theme.text(), false);
                    if (entry.display() == dev.rpgmenu.framework.api.stats.StatDisplay.PROGRESS && entry.max() > entry.min()) {
                        int barX = x + 125;
                        int barW = Math.max(30, content.right() - barX - 80);
                        double ratio = Math.max(0, Math.min(1, (entry.value() - entry.min()) / (entry.max() - entry.min())));
                        graphics.fill(barX, y + 2, barX + barW, y + 7, theme.slot());
                        graphics.fill(barX, y + 2, barX + (int)(barW * ratio), y + 7, theme.accent());
                    }
                    y += 16;
                }
                y += 8;
            }
        }
    }

    private void renderUnavailablePage(GuiGraphics graphics, ThemeDefinition theme) {
        UiRect content = contentViewport();
        graphics.drawCenteredString(font, Component.translatable("message.rpgmenuframework.provider_page"),
                content.x() + content.width() / 2,
                content.y() + content.height() / 2, theme.textMuted());
    }

    private void renderSpells(GuiGraphics graphics, ThemeDefinition theme) {
        if (minecraft.player == null) return;
        UiRect content = contentViewport();
        int x = content.x() + 14;
        int y = content.y() + 14;
        boolean any = false;
        for (var provider : RpgMenuApi.get().spellProviders().values()) {
            List<SpellEntry> spells = provider.spells(minecraft.player);
            for (SpellEntry spell : spells) {
                any = true;
                graphics.blit(spell.icon(), x, y, 0, 0, 16, 16, 16, 16);
                String title = Component.translatable(spell.titleKey()).getString();
                graphics.drawString(font, ellipsize(title, Math.max(48, content.width() - 178)), x + 22, y + 1, theme.text(), false);
                String details = "Lv " + spell.level() + "  •  " + spell.school().getPath() + "  •  "
                        + Math.round(spell.manaCost()) + " mana  •  " + (spell.cooldownTicks() / 20D) + "s";
                graphics.drawString(font, ellipsize(details, Math.max(48, content.width() - 38)), x + 22, y + 11, theme.textMuted(), false);
                y += 23;
                if (y > content.bottom() - 22) return;
            }
        }
        if (!any) graphics.drawCenteredString(font, Component.translatable("message.rpgmenuframework.no_spells"),
                content.x() + content.width() / 2, content.y() + content.height() / 2, theme.textMuted());
    }

    private void renderQuests(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, ThemeDefinition theme) {
        UiRect viewport = questViewport();
        if (embeddedQuestView == null) embeddedQuestView = new FtbQuestEmbeddedView();
        if (!embeddedQuestView.ensure(viewport)) {
            graphics.drawCenteredString(font, Component.translatable("message.rpgmenuframework.ftb_quests_available"),
                    viewport.x() + viewport.width() / 2, viewport.y() + viewport.height() / 2, theme.textMuted());
            return;
        }
        embeddedQuestView.render(graphics, viewport, mouseX, mouseY, partialTick);
    }

    private void renderMap(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, ThemeDefinition theme) {
        UiRect viewport = mapViewport();
        if (embeddedMapHost == null) embeddedMapHost = new EmbeddedMapHost();
        if (!embeddedMapHost.render(graphics, viewport, mouseX, mouseY, partialTick, theme)) {
            graphics.drawCenteredString(font, Component.translatable("message.rpgmenuframework.map_embed_unavailable"),
                    viewport.x() + viewport.width() / 2, viewport.y() + viewport.height() / 2, theme.textMuted());
        }
    }

    private void renderSkills(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, ThemeDefinition theme) {
        UiRect viewport = skillViewport();
        if (embeddedSkillTreeHost == null) embeddedSkillTreeHost = new EmbeddedSkillTreeHost();
        EmbeddedSkillTreeHost.RenderState state = embeddedSkillTreeHost.render(
                graphics, viewport, mouseX, mouseY, partialTick);
        if (state == EmbeddedSkillTreeHost.RenderState.FAILED) {
            graphics.drawCenteredString(font, Component.translatable("message.rpgmenuframework.skill_embed_unavailable"),
                    viewport.x() + viewport.width() / 2, viewport.y() + viewport.height() / 2, theme.textMuted());
        } else if (state == EmbeddedSkillTreeHost.RenderState.UNAVAILABLE) {
            graphics.drawCenteredString(font, Component.translatable("message.rpgmenuframework.provider_page"),
                    viewport.x() + viewport.width() / 2, viewport.y() + viewport.height() / 2, theme.textMuted());
        }
    }

    private void renderFooter(GuiGraphics graphics, ThemeDefinition theme) {
        UiRect footer = layout.footer();
        graphics.fill(footer.x(), footer.y(), footer.right(), footer.bottom(), theme.panelAlt());
        String key;
        if (!isInventoryTab()) {
            key = isQuestsTab() ? "footer.rpgmenuframework.quests"
                    : isMapTab() ? "footer.rpgmenuframework.map"
                    : isSkillsTab() ? "footer.rpgmenuframework.skills"
                    : "spells".equals(activeContentMarker()) ? "footer.rpgmenuframework.spells"
                    : "footer.rpgmenuframework.page";
        } else {
            key = equipmentSelection != null ? "footer.rpgmenuframework.select_equipment"
                    : isEquipmentFocusRegion(focusRegion) ? "footer.rpgmenuframework.equipment"
                    : "footer.rpgmenuframework.keyboard";
        }
        String hints = Component.translatable(key).getString();
        if (statusTicks > 0 && !statusMessage.getString().isBlank()) hints = statusMessage.getString();
        graphics.drawCenteredString(font, ellipsize(hints, footer.width() - 12), footer.x() + footer.width() / 2,
                footer.y() + (footer.height() - 8) / 2, theme.textMuted());
    }

    private void renderTargetSelector(GuiGraphics graphics, int mouseX, int mouseY, ThemeDefinition theme) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 500);
        graphics.fill(0, 0, width, height, 0xA6000000);
        UiRect box = targetSelectorBox();
        panel(graphics, box, theme.panelAlt(), theme.accent());
        graphics.drawCenteredString(font, targetSelectorTitle,
                box.x() + box.width() / 2, box.y() + 8, theme.accent());
        if (targetSelectorKind == SelectorKind.HANDS) {
            graphics.drawString(font, Component.translatable("label.rpgmenuframework.main_hand_quickbar"),
                    box.x() + 10, box.y() + 23, theme.textMuted(), false);
            graphics.drawString(font, Component.translatable("equipment.rpgmenuframework.offhand"),
                    box.x() + 10, box.y() + 73, theme.textMuted(), false);
        }
        TargetSelectorOption hovered = null;
        for (int i = 0; i < targetSelector.size(); i++) {
            UiRect option = targetSelectorOption(box, i);
            boolean focused = i == targetSelectorIndex;
            boolean hover = option.contains(mouseX, mouseY);
            graphics.fill(option.x(), option.y(), option.right(), option.bottom(),
                    focused ? theme.slotSelected() : hover ? theme.slotHover() : theme.slot());
            TargetSelectorOption selector = targetSelector.get(i);
            border(graphics, option, !selector.enabled() ? theme.danger()
                    : focused ? theme.accent() : theme.borderMuted());
            ItemStack stack = selector.view().stack();
            if (!stack.isEmpty()) {
                int itemX = option.x() + (option.width() - 16) / 2;
                int itemY = option.y() + 2;
                graphics.renderItem(stack, itemX, itemY);
                graphics.renderItemDecorations(font, stack, itemX, itemY);
            }
            if (!selector.enabled()) {
                graphics.fill(option.x() + 1, option.y() + 1, option.right() - 1, option.bottom() - 1, 0x88000000);
            }
            String number = Integer.toString(selector.quickTarget() == null
                    ? i + 1 : selector.quickTarget().index() + 1);
            graphics.drawCenteredString(font, number, option.x() + option.width() / 2,
                    option.bottom() + 2, selector.enabled() ? theme.textMuted() : theme.danger());
            if (hover) hovered = selector;
        }
        if (hovered != null) {
            if (!hovered.view().stack().isEmpty()) {
                graphics.renderTooltip(font, hovered.view().stack(), mouseX, mouseY);
            } else {
                graphics.renderTooltip(font, hovered.view().title(), mouseX, mouseY);
            }
        }
        graphics.pose().popPose();
    }

    private UiRect targetSelectorBox() {
        int columns = selectorColumns();
        int boxWidth = Math.min(width - 20, Math.max(112, 20 + columns * 29));
        int rows = Math.max(1, (targetSelector.size() + columns - 1) / columns);
        int boxHeight = targetSelectorKind == SelectorKind.HANDS ? 125 : 42 + rows * 36;
        return new UiRect((width - boxWidth) / 2, (height - boxHeight) / 2, boxWidth, boxHeight);
    }

    private int selectorColumns() {
        return switch (targetSelectorKind) {
            case HANDS -> 4;
            case ITEM_BAR -> 5;
            case GENERAL -> Math.max(1, Math.min(5, targetSelector.size()));
        };
    }

    private UiRect targetSelectorOption(UiRect box, int index) {
        int columns = selectorColumns();
        int row = index / columns;
        int column = index % columns;
        int y = targetSelectorKind == SelectorKind.HANDS ? box.y() + 34 + row * 50 : box.y() + 25 + row * 36;
        return new UiRect(box.x() + 10 + column * 29, y, 24, 24);
    }

    private boolean clickTargetSelector(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        UiRect box = targetSelectorBox();
        for (int i = 0; i < targetSelector.size(); i++) {
            if (targetSelectorOption(box, i).contains(mouseX, mouseY)) {
                targetSelectorIndex = i;
                confirmTargetSelector();
                return true;
            }
        }
        if (!box.contains(mouseX, mouseY)) closeTargetSelector();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!targetSelector.isEmpty()) return clickTargetSelector(mouseX, mouseY, button);
        if (isInventoryTab() && search != null && search.isMouseOver(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (search != null) search.setFocused(false);

        if (isInventoryTab()) {
            RenderedEquipmentSlot equipmentHit = equipmentAt(mouseX, mouseY);
            if (equipmentHit != null) {
                boolean changed = focusRegion != equipmentHit.region()
                        || focusedEquipmentIndex != equipmentHit.index()
                        || !equipmentHit.view().target().equals(selectedEquipmentTarget);
                selectedEquipmentTarget = equipmentHit.view().target();
                focusedEquipmentIndex = equipmentHit.index();
                focusRegion = equipmentHit.region();
                int hotbarSlot = HotbarEquipmentProvider.resolve(equipmentHit.view().target());
                if (hotbarSlot >= 0 && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) selectHotbarSlot(hotbarSlot);
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !equipmentHit.view().stack().isEmpty()) {
                    draggedEquipment = equipmentHit.view().target();
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && !equipmentHit.view().stack().isEmpty()) {
                    QuickSlotTargets.fromEquipmentTarget(equipmentHit.view().target())
                            .ifPresentOrElse(this::sendQuickSlotMoveToInventory,
                                    () -> sendUnequip(equipmentHit.view().target()));
                }
                if (changed && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) UiSoundPlayer.play(UiSoundCue.ITEM_SELECT);
                return true;
            }
            if (equipmentButton.contains(mouseX, mouseY) && selected != null && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                beginDetailEquip();
                return true;
            }
            if (layout.character().contains(mouseX, mouseY)) {
                draggingPreview = true;
                return true;
            }
        }
        MenuTabWidget.Layout tabLayout = topTabLayout();
        tabOffset = tabLayout.offset();
        for (MenuTabWidget widget : tabLayout.widgets()) {
            if (widget.contains(mouseX, mouseY)) {
                if (widget.enabled()) setTab(widget.tab().id());
                return true;
            }
        }
        if (isQuestsTab() && questViewport().contains(mouseX, mouseY) && embeddedQuestView != null) {
            return embeddedQuestView.mouseClicked(questViewport(), mouseX, mouseY, button);
        }
        if (isMapTab() && mapViewport().contains(mouseX, mouseY) && embeddedMapHost != null) {
            return embeddedMapHost.mouseClicked(mouseX, mouseY, button);
        }
        if (isSkillsTab() && skillViewport().contains(mouseX, mouseY) && embeddedSkillTreeHost != null) {
            return embeddedSkillTreeHost.mouseClicked(mouseX, mouseY, button);
        }
        if (isInventoryTab()) {
            var categories = RpgMenuApi.get().itemCategories().values().stream().limit(MAX_VISIBLE_CATEGORIES).toList();
            int widthEach = Math.max(35, layout.subTabs().width() / Math.max(1, categories.size()));
            int x = layout.subTabs().x();
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
        if (!targetSelector.isEmpty()) return true;
        if (isQuestsTab() && embeddedQuestView != null) {
            return embeddedQuestView.mouseDragged(questViewport(), mouseX, mouseY, button, dragX, dragY);
        }
        if (isMapTab() && embeddedMapHost != null) {
            return embeddedMapHost.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        if (isSkillsTab() && embeddedSkillTreeHost != null) {
            return embeddedSkillTreeHost.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
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
        if (!targetSelector.isEmpty()) return true;
        if (isQuestsTab() && embeddedQuestView != null) {
            return embeddedQuestView.mouseReleased(questViewport(), mouseX, mouseY, button);
        }
        if (isMapTab() && embeddedMapHost != null) {
            return embeddedMapHost.mouseReleased(mouseX, mouseY, button);
        }
        if (isSkillsTab() && embeddedSkillTreeHost != null) {
            return embeddedSkillTreeHost.mouseReleased(mouseX, mouseY, button);
        }
        if (draggingPreview) {
            draggingPreview = false;
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggedInventory != null) {
            RenderedEquipmentSlot target = equipmentAt(mouseX, mouseY);
            if (target != null) {
                QuickSlotTarget quickTarget = QuickSlotTargets.fromEquipmentTarget(target.view().target()).orElse(null);
                if (quickTarget != null) sendQuickSlotPlace(draggedInventory, quickTarget);
                else if (canEquip(target.view().target(), draggedInventory.stack())) sendEquip(draggedInventory, target.view().target());
                else showInvalidTarget(target.view().target());
            }
            draggedInventory = null;
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggedEquipment != null) {
            QuickSlotTarget sourceQuick = QuickSlotTargets.fromEquipmentTarget(draggedEquipment).orElse(null);
            RenderedEquipmentSlot target = equipmentAt(mouseX, mouseY);
            QuickSlotTarget targetQuick = target == null ? null
                    : QuickSlotTargets.fromEquipmentTarget(target.view().target()).orElse(null);
            if (sourceQuick != null && targetQuick != null && !sourceQuick.equals(targetQuick)) {
                sendQuickSlotSwap(sourceQuick, targetQuick);
            } else if (target != null && sourceQuick != null && targetQuick == null) {
                showInvalidTarget(target.view().target());
            } else if (layout.grid().contains(mouseX, mouseY)) {
                if (sourceQuick != null) sendQuickSlotMoveToInventory(sourceQuick);
                else sendUnequip(draggedEquipment);
            }
            draggedEquipment = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (isQuestsTab() && questViewport().contains(mouseX, mouseY) && embeddedQuestView != null) {
            embeddedQuestView.mouseMoved(questViewport(), mouseX, mouseY);
        }
        if (isMapTab() && mapViewport().contains(mouseX, mouseY) && embeddedMapHost != null) {
            embeddedMapHost.mouseMoved(mouseX, mouseY);
        }
        if (isSkillsTab() && skillViewport().contains(mouseX, mouseY) && embeddedSkillTreeHost != null) {
            embeddedSkillTreeHost.mouseMoved(mouseX, mouseY);
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!targetSelector.isEmpty()) {
            return true;
        }
        if (layout.topTabs().contains(mouseX, mouseY)) {
            tabOffset = Math.max(0, tabOffset + (scrollY < 0 ? 1 : -1));
            return true;
        }
        if (isQuestsTab() && questViewport().contains(mouseX, mouseY) && embeddedQuestView != null) {
            return embeddedQuestView.mouseScrolled(questViewport(), mouseX, mouseY, scrollX, scrollY);
        }
        if (isMapTab() && mapViewport().contains(mouseX, mouseY) && embeddedMapHost != null) {
            return embeddedMapHost.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (isSkillsTab() && skillViewport().contains(mouseX, mouseY) && embeddedSkillTreeHost != null) {
            return embeddedSkillTreeHost.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        RenderedEquipmentSlot equipmentHover = isInventoryTab() ? equipmentAt(mouseX, mouseY) : null;
        if (equipmentHover != null && HotbarEquipmentProvider.resolve(equipmentHover.view().target()) >= 0) {
            cycleHotbar(scrollY < 0 ? 1 : -1);
            return true;
        }
        if (isInventoryTab() && layout.character().contains(mouseX, mouseY)
                && equipmentHover != null && equipmentHover.region() == FocusRegion.EQUIPMENT) {
            int sideCount = (int)equipmentViews().stream().filter(view -> !isQuickbarSlot(view)
                    && !isOffhandSlot(view) && !isArmorSlot(view) && !isLegacyMainhandSlot(view)).count();
            CharacterEquipmentLayout equipmentLayout = calculateCharacterEquipmentLayout();
            int maxOffset = Math.max(0, (sideCount + 1) / 2 - equipmentLayout.sideRowCapacity());
            if (maxOffset > 0) {
                int next = Math.max(0, Math.min(maxOffset, equipmentRowOffset + (scrollY < 0 ? 1 : -1)));
                if (next != equipmentRowOffset) equipmentRowOffset = next;
                return true;
            }
        }
        if (isInventoryTab() && layout.character().contains(mouseX, mouseY)) {
            cycleHotbar(scrollY < 0 ? 1 : -1);
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
        if (!targetSelector.isEmpty()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) controllerAction(InputAction.BACK);
            else if (keyCode == GLFW.GLFW_KEY_UP) controllerAction(InputAction.UP);
            else if (keyCode == GLFW.GLFW_KEY_DOWN) controllerAction(InputAction.DOWN);
            else if (keyCode == GLFW.GLFW_KEY_LEFT) controllerAction(InputAction.LEFT);
            else if (keyCode == GLFW.GLFW_KEY_RIGHT) controllerAction(InputAction.RIGHT);
            else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                controllerAction(InputAction.CONFIRM);
            }
            return true;
        }
        if (search != null && search.isFocused()) return super.keyPressed(keyCode, scanCode, modifiers);
        if (isInventoryTab() && keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
            selectHotbarSlot(keyCode - GLFW.GLFW_KEY_1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP) { cycleTab(-1); return true; }
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) { cycleTab(1); return true; }
        if (isQuestsTab() && embeddedQuestView != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (embeddedQuestView.handleBack()) return true;
            } else if (embeddedQuestView.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        if (isMapTab() && embeddedMapHost != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (embeddedMapHost.handleBack()) return true;
            } else if (isMovementKey(keyCode, scanCode) && !embeddedMapHost.hasTextInputFocus()) {
                return false;
            } else if (embeddedMapHost.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        if (isSkillsTab() && embeddedSkillTreeHost != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (embeddedSkillTreeHost.handleBack()) return true;
            } else if (isMovementKey(keyCode, scanCode) && !embeddedSkillTreeHost.hasTextInputFocus()) {
                return false;
            } else if (embeddedSkillTreeHost.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
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
        if (keyCode == GLFW.GLFW_KEY_Y && selected != null) { toggleFavorite(); return true; }
        if (keyCode == GLFW.GLFW_KEY_C) { ClientBootstrap.openVanillaInventory(); return true; }
        if (isMovementKey(keyCode, scanCode)) return false;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (!targetSelector.isEmpty()) return true;
        if (isQuestsTab() && embeddedQuestView != null) embeddedQuestView.keyReleased(keyCode, scanCode, modifiers);
        if (isMapTab() && embeddedMapHost != null) {
            if (isMovementKey(keyCode, scanCode) && !embeddedMapHost.hasTextInputFocus()) return false;
            if (embeddedMapHost.keyReleased(keyCode, scanCode, modifiers)) return true;
        }
        if (isSkillsTab() && embeddedSkillTreeHost != null) {
            if (isMovementKey(keyCode, scanCode) && !embeddedSkillTreeHost.hasTextInputFocus()) return false;
            if (embeddedSkillTreeHost.keyReleased(keyCode, scanCode, modifiers)) return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!targetSelector.isEmpty()) return true;
        if (isMapTab() && embeddedMapHost != null && embeddedMapHost.charTyped(codePoint, modifiers)) return true;
        if (isSkillsTab() && embeddedSkillTreeHost != null && embeddedSkillTreeHost.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void removed() {
        InputRouter.release(minecraft);
        draggedInventory = null;
        draggedEquipment = null;
        if (embeddedQuestView != null) embeddedQuestView.close();
        if (embeddedMapHost != null) embeddedMapHost.close();
        if (embeddedSkillTreeHost != null) embeddedSkillTreeHost.close();
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
            if (isDirection(action)) {
                moveSelectorFocus(action);
            } else if (action == InputAction.CONFIRM) {
                confirmTargetSelector();
            } else if (action == InputAction.BACK) {
                closeTargetSelector();
            }
            return;
        }
        if (action == InputAction.PREVIOUS_TAB) { cycleTab(-1); return; }
        if (action == InputAction.NEXT_TAB) { cycleTab(1); return; }
        if (isQuestsTab() && action == InputAction.BACK) {
            if (embeddedQuestView == null || !embeddedQuestView.handleBack()) onClose();
            return;
        }
        if (isMapTab()) {
            if (action == InputAction.BACK) {
                if (embeddedMapHost == null || !embeddedMapHost.handleBack()) onClose();
            } else if (embeddedMapHost != null) {
                embeddedMapHost.controllerAction(action);
            }
            return;
        }
        if (isSkillsTab()) {
            if (action == InputAction.BACK) {
                if (embeddedSkillTreeHost == null || !embeddedSkillTreeHost.handleBack()) onClose();
            } else if (embeddedSkillTreeHost != null) {
                embeddedSkillTreeHost.controllerAction(action);
            }
            return;
        }
        if (action == InputAction.FAVORITE && selected != null) { toggleFavorite(); return; }

        if (equipmentSelection != null) {
            if (action == InputAction.BACK) {
                cancelEquipmentSelection();
            } else if (action == InputAction.CONFIRM) {
                InventoryPagePayload.Entry entry = focusedEntry();
                if (entry != null) sendEntryToTarget(entry, equipmentSelection.target());
            } else if (isDirection(action)) {
                moveInventoryFocus(action);
            }
            return;
        }

        if (isEquipmentFocusRegion(focusRegion)) {
            if (action == InputAction.CONFIRM) {
                EquipmentSlotView slot = equipmentView(focusedEquipmentIndex);
                if (slot != null) beginEquipmentSelection(focusedEquipmentIndex);
            } else if (action == InputAction.BACK) {
                EquipmentSlotView slot = equipmentView(focusedEquipmentIndex);
                if (slot != null && !slot.stack().isEmpty()) {
                    QuickSlotTargets.fromEquipmentTarget(slot.target())
                            .ifPresentOrElse(this::sendQuickSlotMoveToInventory, () -> sendUnequip(slot.target()));
                }
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
                    equipSelectedFromInventory();
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
            if (equipmentSelection == null && action == InputAction.RIGHT) focusNearestEquipmentFromInventory();
            playInventoryFocusChange(previousRegion, previousIndex);
            return;
        }
        int columns = Math.max(1, layout.gridColumns(ThemeManager.INSTANCE.current().slotSize()));
        if (equipmentSelection == null && action == InputAction.RIGHT
                && (focusedInventoryIndex % columns == columns - 1
                || focusedInventoryIndex + 1 >= count)
                && !renderedEquipmentSlots.isEmpty()) {
            focusNearestEquipmentFromInventory();
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
        RenderedEquipmentSlot origin = renderedEquipmentSlots.stream()
                .filter(slot -> slot.index() == focusedEquipmentIndex).findFirst().orElse(null);
        if (origin == null) {
            if (!renderedEquipmentSlots.isEmpty()) focusEquipment(renderedEquipmentSlots.getFirst());
            return;
        }
        RenderedEquipmentSlot nearest = nearestInDirection(origin, action);
        if (nearest != null) {
            focusEquipment(nearest);
        } else if (action == InputAction.LEFT) {
            focusRegion = FocusRegion.INVENTORY;
            int columns = Math.max(1, layout.gridColumns(ThemeManager.INSTANCE.current().slotSize()));
            int entryCount = page == null ? 0 : page.entries().size();
            if (entryCount > 0) focusedInventoryIndex = Math.min(entryCount - 1,
                    focusedInventoryIndex / columns * columns + columns - 1);
        }
        playEquipmentFocusChange(previousRegion, previousIndex);
    }

    private void beginEquipmentSelection(int index) {
        EquipmentSlotView view = equipmentView(index);
        if (view == null || !view.enabled()) {
            showError("message.rpgmenuframework.invalid_equipment_target");
            return;
        }
        selectedEquipmentTarget = view.target();
        equipmentSelection = new EquipmentSelectionContext(view.target(), index, focusRegion);
        focusRegion = FocusRegion.INVENTORY;
        focusedInventoryIndex = 0;
        UiSoundPlayer.play(UiSoundCue.CONFIRM);
        requestPage(0);
    }

    private void cancelEquipmentSelection() {
        if (equipmentSelection == null) return;
        focusedEquipmentIndex = equipmentSelection.originEquipmentIndex();
        selectedEquipmentTarget = equipmentSelection.target();
        FocusRegion originRegion = equipmentSelection.originRegion();
        equipmentSelection = null;
        focusRegion = originRegion;
        UiSoundPlayer.play(UiSoundCue.CANCEL);
        requestPage(0);
    }

    private void beginDetailEquip() {
        if (selected == null) return;
        List<EquipmentSlotView> specialized = specializedTargetsFor(selected.stack());
        if (!specialized.isEmpty()) {
            EquipmentSlotView empty = specialized.stream().filter(view -> view.stack().isEmpty()).findFirst().orElse(null);
            if (empty != null || specialized.size() == 1) {
                EquipmentSlotView direct = empty != null ? empty : specialized.getFirst();
                UiSoundPlayer.play(UiSoundCue.CONFIRM);
                sendEquip(selected, direct.target());
            } else {
                openTargetSelector(specialized.stream()
                                .map(view -> new TargetSelectorOption(null, view, canEquip(view.target(), selected.stack())))
                                .toList(), SelectorKind.GENERAL,
                        Component.translatable("title.rpgmenuframework.select_equipment_target"));
            }
            return;
        }

        QuickEquipKind kind = QuickEquipResolver.classify(minecraft.player, selected.stack());
        List<TargetSelectorOption> group = quickOptions(kind.defaultGroup(), selected.stack(), true);
        TargetSelectorOption empty = group.stream()
                .filter(TargetSelectorOption::enabled).filter(option -> option.view().stack().isEmpty())
                .findFirst().orElse(null);
        if (empty != null) {
            UiSoundPlayer.play(UiSoundCue.CONFIRM);
            sendQuickSlotPlace(selected, empty.quickTarget());
        } else if (!group.isEmpty()) {
            if (kind.defaultGroup() == QuickSlotGroup.ITEM_BAR) {
                openTargetSelector(quickOptions(QuickSlotGroup.ITEM_BAR, selected.stack(), true),
                        SelectorKind.ITEM_BAR,
                        Component.translatable("title.rpgmenuframework.select_item_bar_target"));
            } else {
                List<TargetSelectorOption> hands = new ArrayList<>();
                hands.addAll(quickOptions(QuickSlotGroup.MAIN_HAND, selected.stack(), true));
                hands.addAll(quickOptions(QuickSlotGroup.OFF_HAND, selected.stack(), true));
                openTargetSelector(hands, SelectorKind.HANDS,
                        Component.translatable("title.rpgmenuframework.select_equipment_target"));
            }
        } else {
            showError("message.rpgmenuframework.invalid_equipment_item");
        }
    }

    private void equipSelectedFromInventory() {
        if (selected == null) return;
        if (equipmentSelection != null) {
            sendEntryToTarget(selected, equipmentSelection.target());
            return;
        }
        beginDetailEquip();
    }

    private void sendEntryToTarget(InventoryPagePayload.Entry entry, EquipmentTarget target) {
        QuickSlotTarget quickTarget = QuickSlotTargets.fromEquipmentTarget(target).orElse(null);
        if (quickTarget != null) sendQuickSlotPlace(entry, quickTarget);
        else sendEquip(entry, target);
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

    private void sendQuickSlotPlace(InventoryPagePayload.Entry entry, QuickSlotTarget target) {
        EquipmentTarget equipmentTarget = QuickSlotTargets.equipmentTarget(target);
        if (entry == null || equipmentTarget == null || minecraft.player == null
                || !QuickEquipResolver.canPlace(minecraft.player, entry.stack(), target, false)
                || minecraft.getConnection() == null) {
            showInvalidTarget(equipmentTarget);
            return;
        }
        selectedEquipmentTarget = equipmentTarget;
        pendingEquipmentNonce = nextEquipmentNonce++;
        PacketDistributor.sendToServer(new QuickbarActionPayload(sessionId, entry.opaqueId(), null, target,
                QuickbarAction.PLACE_ENTRY, pendingEquipmentNonce));
    }

    private void sendQuickSlotSwap(QuickSlotTarget source, QuickSlotTarget target) {
        EquipmentTarget targetEquipment = QuickSlotTargets.equipmentTarget(target);
        EquipmentSlotView sourceView = quickSlotView(source);
        EquipmentSlotView targetView = quickSlotView(target);
        if (targetEquipment == null || sourceView == null || targetView == null || sourceView.stack().isEmpty()
                || minecraft.player == null
                || !QuickEquipResolver.canPlace(minecraft.player, sourceView.stack(), target, false)
                || !targetView.stack().isEmpty()
                && !QuickEquipResolver.canPlace(minecraft.player, targetView.stack(), source, false)
                || minecraft.getConnection() == null) {
            showInvalidTarget(targetEquipment);
            return;
        }
        selectedEquipmentTarget = targetEquipment;
        pendingEquipmentNonce = nextEquipmentNonce++;
        PacketDistributor.sendToServer(new QuickbarActionPayload(sessionId, 0, source, target,
                QuickbarAction.SWAP_SLOTS, pendingEquipmentNonce));
    }

    private void sendQuickSlotMoveToInventory(QuickSlotTarget source) {
        EquipmentTarget equipmentTarget = QuickSlotTargets.equipmentTarget(source);
        if (equipmentTarget == null || quickSlotView(source) == null || minecraft.getConnection() == null) {
            showInvalidTarget(equipmentTarget);
            return;
        }
        selectedEquipmentTarget = equipmentTarget;
        pendingEquipmentNonce = nextEquipmentNonce++;
        PacketDistributor.sendToServer(new QuickbarActionPayload(sessionId, 0, source, source,
                QuickbarAction.MOVE_TO_INVENTORY, pendingEquipmentNonce));
    }

    private void selectHotbarSlot(int slot) {
        if (minecraft.player == null || minecraft.getConnection() == null
                || slot < 0 || slot >= HotbarEquipmentProvider.SLOT_COUNT) return;
        if (minecraft.player.getInventory().selected != slot) {
            minecraft.player.getInventory().selected = slot;
            minecraft.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
            UiSoundPlayer.play(UiSoundCue.ITEM_SELECT);
        }
        selectedEquipmentTarget = HotbarEquipmentProvider.target(slot);
    }

    private void cycleHotbar(int direction) {
        if (minecraft.player == null) return;
        selectHotbarSlot(Math.floorMod(minecraft.player.getInventory().selected + direction,
                HotbarEquipmentProvider.SLOT_COUNT));
    }

    private void acceptEquipmentResult(EquipmentResultPayload result) {
        showStatus(result.messageKey());
        if (result.status() != TransactionResult.Status.SUCCESS) {
            invalidEquipmentTarget = result.target();
            invalidTargetTicks = 12;
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
            FocusRegion originRegion = equipmentSelection.originRegion();
            equipmentSelection = null;
            focusRegion = originRegion;
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

    private void showInvalidTarget(EquipmentTarget target) {
        invalidEquipmentTarget = target;
        invalidTargetTicks = 12;
        showError("message.rpgmenuframework.invalid_equipment_item");
    }

    private void toggleFavorite() {
        if (selected == null) return;
        favorites.toggle(selected.stack());
        UiSoundPlayer.play(UiSoundCue.FAVORITE);
    }

    private void closeTargetSelector() {
        if (targetSelector.isEmpty()) return;
        targetSelector = List.of();
        targetSelectorKind = SelectorKind.GENERAL;
        targetSelectorTitle = Component.empty();
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
                || (isEquipmentFocusRegion(focusRegion) && focusedEquipmentIndex != previousIndex)) {
            UiSoundPlayer.play(UiSoundCue.FOCUS_MOVE);
        }
    }

    private void focusNearestEquipmentFromInventory() {
        if (renderedEquipmentSlots.isEmpty()) return;
        int slotSize = ThemeManager.INSTANCE.current().slotSize();
        int columns = Math.max(1, layout.gridColumns(slotSize));
        int row = Math.max(0, focusedInventoryIndex / columns);
        int targetY = layout.grid().y() + row * slotSize + slotSize / 2;
        RenderedEquipmentSlot nearest = renderedEquipmentSlots.stream()
                .min(java.util.Comparator.comparingLong(slot ->
                        Math.abs((long)slot.rect().y() + slot.rect().height() / 2 - targetY) * 8
                                + slot.rect().x()))
                .orElse(null);
        if (nearest != null) focusEquipment(nearest);
    }

    private void focusEquipment(RenderedEquipmentSlot slot) {
        focusedEquipmentIndex = slot.index();
        selectedEquipmentTarget = slot.view().target();
        focusRegion = slot.region();
    }

    private RenderedEquipmentSlot nearestInDirection(RenderedEquipmentSlot origin, InputAction action) {
        int originX = origin.rect().x() + origin.rect().width() / 2;
        int originY = origin.rect().y() + origin.rect().height() / 2;
        RenderedEquipmentSlot best = null;
        long bestScore = Long.MAX_VALUE;
        for (RenderedEquipmentSlot candidate : renderedEquipmentSlots) {
            if (candidate == origin) continue;
            int dx = candidate.rect().x() + candidate.rect().width() / 2 - originX;
            int dy = candidate.rect().y() + candidate.rect().height() / 2 - originY;
            boolean directionMatch = switch (action) {
                case LEFT -> dx < 0;
                case RIGHT -> dx > 0;
                case UP -> dy < 0;
                case DOWN -> dy > 0;
                default -> false;
            };
            if (!directionMatch) continue;
            int primary = action == InputAction.LEFT || action == InputAction.RIGHT ? Math.abs(dx) : Math.abs(dy);
            int perpendicular = action == InputAction.LEFT || action == InputAction.RIGHT ? Math.abs(dy) : Math.abs(dx);
            long score = (long)primary * primary + (long)perpendicular * perpendicular * 3;
            if (score < bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private static boolean isEquipmentFocusRegion(FocusRegion region) {
        return region == FocusRegion.EQUIPMENT || region == FocusRegion.MAIN_HAND_QUICKBAR
                || region == FocusRegion.OFFHAND_QUICKBAR || region == FocusRegion.ITEM_QUICKBAR;
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
        QuickSlotTarget quickTarget = QuickSlotTargets.fromEquipmentTarget(target).orElse(null);
        if (quickTarget != null) return QuickEquipResolver.canPlace(minecraft.player, stack, quickTarget, false);
        EquipmentProvider provider = RpgMenuApi.get().equipmentProviders().get(target.providerId()).orElse(null);
        return provider != null && provider.canEquip(minecraft.player, target, stack);
    }

    private List<EquipmentSlotView> specializedTargetsFor(ItemStack stack) {
        return equipmentViews().stream()
                .filter(view -> QuickSlotTargets.fromEquipmentTarget(view.target()).isEmpty())
                .filter(view -> !isLegacyMainhandSlot(view))
                .filter(EquipmentSlotView::enabled)
                .filter(view -> canEquip(view.target(), stack))
                .toList();
    }

    private List<TargetSelectorOption> quickOptions(QuickSlotGroup group, ItemStack stack, boolean automatic) {
        List<QuickSlotTarget> targets = new ArrayList<>();
        if (group == QuickSlotGroup.MAIN_HAND) {
            for (int index = 0; index < QuickSlotTargets.MAIN_HAND_COUNT; index++) {
                targets.add(new QuickSlotTarget(group, index));
            }
        } else if (group == QuickSlotGroup.ITEM_BAR) {
            for (int index = 0; index < QuickSlotTargets.ITEM_BAR_COUNT; index++) {
                targets.add(new QuickSlotTarget(group, index));
            }
        } else {
            equipmentViews().stream().map(EquipmentSlotView::target)
                    .map(QuickSlotTargets::fromEquipmentTarget).flatMap(java.util.Optional::stream)
                    .filter(target -> target.group() == QuickSlotGroup.OFF_HAND)
                    .sorted(java.util.Comparator.comparingInt(QuickSlotTarget::index))
                    .forEach(targets::add);
        }
        List<TargetSelectorOption> result = new ArrayList<>(targets.size());
        for (QuickSlotTarget target : targets) {
            EquipmentSlotView view = quickSlotView(target);
            if (view == null) continue;
            result.add(new TargetSelectorOption(target, view,
                    view.enabled() && QuickEquipResolver.canPlace(minecraft.player, stack, target, automatic)));
        }
        return List.copyOf(result);
    }

    private EquipmentSlotView quickSlotView(QuickSlotTarget target) {
        EquipmentTarget equipmentTarget = QuickSlotTargets.equipmentTarget(target);
        if (equipmentTarget == null) return null;
        return equipmentViews().stream().filter(view -> view.target().equals(equipmentTarget)).findFirst().orElse(null);
    }

    private boolean autoEquipAvailable(ItemStack stack) {
        if (minecraft == null || minecraft.player == null || stack.isEmpty()) return false;
        if (!specializedTargetsFor(stack).isEmpty()) return true;
        QuickEquipKind kind = QuickEquipResolver.classify(minecraft.player, stack);
        return !quickOptions(kind.defaultGroup(), stack, true).isEmpty();
    }

    private TargetSelectorOption firstEmptyAutomaticTarget(ItemStack stack) {
        if (minecraft == null || minecraft.player == null || stack.isEmpty()) return null;
        EquipmentSlotView specialized = specializedTargetsFor(stack).stream()
                .filter(view -> view.stack().isEmpty()).findFirst().orElse(null);
        if (specialized != null) return new TargetSelectorOption(null, specialized, true);
        QuickEquipKind kind = QuickEquipResolver.classify(minecraft.player, stack);
        return quickOptions(kind.defaultGroup(), stack, true).stream()
                .filter(TargetSelectorOption::enabled).filter(option -> option.view().stack().isEmpty())
                .findFirst().orElse(null);
    }

    private void openTargetSelector(List<TargetSelectorOption> options, SelectorKind kind, Component title) {
        if (options.isEmpty()) {
            showError("message.rpgmenuframework.invalid_equipment_target");
            return;
        }
        targetSelector = List.copyOf(options);
        targetSelectorKind = kind;
        targetSelectorTitle = title;
        targetSelectorIndex = 0;
        for (int index = 0; index < targetSelector.size(); index++) {
            if (targetSelector.get(index).enabled()) {
                targetSelectorIndex = index;
                break;
            }
        }
        UiSoundPlayer.play(UiSoundCue.MODAL_OPEN);
    }

    private void confirmTargetSelector() {
        if (selected == null || targetSelectorIndex < 0 || targetSelectorIndex >= targetSelector.size()) return;
        TargetSelectorOption option = targetSelector.get(targetSelectorIndex);
        if (!option.enabled()) {
            showInvalidTarget(option.view().target());
            return;
        }
        targetSelector = List.of();
        UiSoundPlayer.play(UiSoundCue.CONFIRM);
        if (option.quickTarget() != null) sendQuickSlotPlace(selected, option.quickTarget());
        else sendEquip(selected, option.view().target());
    }

    private void moveSelectorFocus(InputAction action) {
        if (targetSelector.isEmpty()) return;
        UiRect box = targetSelectorBox();
        UiRect origin = targetSelectorOption(box, targetSelectorIndex);
        int originX = origin.x() + origin.width() / 2;
        int originY = origin.y() + origin.height() / 2;
        int previous = targetSelectorIndex;
        long bestScore = Long.MAX_VALUE;
        for (int index = 0; index < targetSelector.size(); index++) {
            if (index == targetSelectorIndex || !targetSelector.get(index).enabled()) continue;
            UiRect candidate = targetSelectorOption(box, index);
            int dx = candidate.x() + candidate.width() / 2 - originX;
            int dy = candidate.y() + candidate.height() / 2 - originY;
            boolean directionMatch = switch (action) {
                case LEFT -> dx < 0;
                case RIGHT -> dx > 0;
                case UP -> dy < 0;
                case DOWN -> dy > 0;
                default -> false;
            };
            if (!directionMatch) continue;
            int primary = action == InputAction.LEFT || action == InputAction.RIGHT ? Math.abs(dx) : Math.abs(dy);
            int perpendicular = action == InputAction.LEFT || action == InputAction.RIGHT ? Math.abs(dy) : Math.abs(dx);
            long score = (long)primary * primary + (long)perpendicular * perpendicular * 3;
            if (score < bestScore) {
                targetSelectorIndex = index;
                bestScore = score;
            }
        }
        if (targetSelectorIndex != previous) UiSoundPlayer.play(UiSoundCue.FOCUS_MOVE);
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
        renderedEquipmentSlots = List.of();
        equipmentButton = new UiRect(0, 0, 0, 0);
        draggedInventory = null;
        draggedEquipment = null;
        draggingPreview = false;
        search.visible = isInventoryTab();
        search.setFocused(false);
        UiSoundPlayer.play(UiSoundCue.TAB_SWITCH);
        if (isInventoryTab()) requestPage(page == null ? 0 : page.page());
        else if (isQuestsTab() && embeddedQuestView != null) embeddedQuestView.markDirty();
    }

    private boolean isInventoryTab() { return activeTab.getPath().equals("inventory"); }

    private boolean isQuestsTab() { return "quests".equals(activeContentMarker()); }

    private boolean isMapTab() { return "map".equals(activeContentMarker()); }

    private boolean isSkillsTab() { return "skills".equals(activeContentMarker()); }

    /** Used by the bounded Epic Skills transition bridge; does not expose optional-mod classes. */
    public boolean isEmbeddedSkillsActive() { return isSkillsTab(); }

    private ContentLayoutMode contentLayoutMode() {
        return isInventoryTab() ? ContentLayoutMode.INVENTORY_SPLIT : ContentLayoutMode.FULL_CONTENT;
    }

    private UiRect contentViewport() {
        int right = layout.rightPanel().width() > 0 ? layout.rightPanel().right() : layout.leftPanel().right();
        return new UiRect(layout.leftPanel().x(), layout.leftPanel().y(),
                Math.max(1, right - layout.leftPanel().x()), layout.leftPanel().height());
    }

    private UiRect questViewport() {
        return contentViewport();
    }

    private UiRect mapViewport() {
        return questViewport().inset(1);
    }

    private UiRect skillViewport() {
        return questViewport().inset(1);
    }

    private MenuTabWidget.Layout topTabLayout() {
        TabContext context = tabContext();
        return MenuTabWidget.layout(visibleTabs(), font, layout.topTabs(), tabOffset, activeTab,
                tab -> tab.isEnabled(context),
                tab -> focusRegion == FocusRegion.TOP_TABS && tab.id().equals(activeTab));
    }

    private int currentMouseX() {
        return (int) (minecraft.mouseHandler.xpos() * width / minecraft.getWindow().getScreenWidth());
    }

    private int currentMouseY() {
        return (int) (minecraft.mouseHandler.ypos() * height / minecraft.getWindow().getScreenHeight());
    }

    private String activeContentMarker() {
        return RpgMenuApi.get().tabs().get(activeTab).map(tab -> tab.contentFactory().create(tabContext()))
                .filter(String.class::isInstance).map(String.class::cast).orElse("empty");
    }

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

    private record IndexedEquipmentSlot(int index, EquipmentSlotView view) {}
    private record RenderedEquipmentSlot(int index, EquipmentSlotView view, UiRect rect, FocusRegion region) {}
    private record TargetSelectorOption(QuickSlotTarget quickTarget, EquipmentSlotView view, boolean enabled) {}
    private enum SelectorKind { HANDS, ITEM_BAR, GENERAL }
}
