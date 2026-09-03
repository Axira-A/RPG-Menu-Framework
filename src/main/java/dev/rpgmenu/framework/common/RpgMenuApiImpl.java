package dev.rpgmenu.framework.common;

import dev.rpgmenu.framework.api.RpgMenuApi;
import dev.rpgmenu.framework.api.inventory.InventorySource;
import dev.rpgmenu.framework.api.inventory.ItemCategoryProvider;
import dev.rpgmenu.framework.api.inventory.QuickEquipProvider;
import dev.rpgmenu.framework.api.menu.MenuTabRegistry;
import dev.rpgmenu.framework.api.registry.ProviderRegistry;
import dev.rpgmenu.framework.api.stats.StatProvider;
import dev.rpgmenu.framework.api.equipment.EquipmentProvider;
import dev.rpgmenu.framework.api.rarity.RarityProvider;
import dev.rpgmenu.framework.api.skills.SkillProvider;
import dev.rpgmenu.framework.api.spells.SpellProvider;
import dev.rpgmenu.framework.api.quests.QuestProvider;
import dev.rpgmenu.framework.api.map.MapProvider;
import dev.rpgmenu.framework.api.character.CharacterPreviewProvider;
import dev.rpgmenu.framework.api.input.ControllerInputProvider;
import dev.rpgmenu.framework.api.input.InputGlyphProvider;
import dev.rpgmenu.framework.common.menu.MenuTabRegistryImpl;
import dev.rpgmenu.framework.common.registry.CopyOnWriteProviderRegistry;

public final class RpgMenuApiImpl implements RpgMenuApi {
    private static final MenuTabRegistry TABS = new MenuTabRegistryImpl();
    private static final ProviderRegistry<InventorySource> SOURCES = new CopyOnWriteProviderRegistry<>();
    private static final ProviderRegistry<ItemCategoryProvider> CATEGORIES = new CopyOnWriteProviderRegistry<>();
    private static final ProviderRegistry<QuickEquipProvider> QUICK_EQUIP = new CopyOnWriteProviderRegistry<>();
    private static final ProviderRegistry<StatProvider> STATS = new CopyOnWriteProviderRegistry<>();
    private static final ProviderRegistry<EquipmentProvider> EQUIPMENT = new CopyOnWriteProviderRegistry<>();
    private static final ProviderRegistry<RarityProvider> RARITY = new CopyOnWriteProviderRegistry<>();
    private static final ProviderRegistry<SkillProvider> SKILLS = new CopyOnWriteProviderRegistry<>();
    private static final ProviderRegistry<SpellProvider> SPELLS = new CopyOnWriteProviderRegistry<>();
    private static final ProviderRegistry<QuestProvider> QUESTS = new CopyOnWriteProviderRegistry<>();
    private static final ProviderRegistry<MapProvider> MAPS = new CopyOnWriteProviderRegistry<>();
    private static final ProviderRegistry<CharacterPreviewProvider> PREVIEWS = new CopyOnWriteProviderRegistry<>();
    private static final ProviderRegistry<ControllerInputProvider> CONTROLLERS = new CopyOnWriteProviderRegistry<>();
    private static final ProviderRegistry<InputGlyphProvider> GLYPHS = new CopyOnWriteProviderRegistry<>();

    @Override public MenuTabRegistry tabs() { return TABS; }
    @Override public ProviderRegistry<InventorySource> inventorySources() { return SOURCES; }
    @Override public ProviderRegistry<ItemCategoryProvider> itemCategories() { return CATEGORIES; }
    @Override public ProviderRegistry<QuickEquipProvider> quickEquipProviders() { return QUICK_EQUIP; }
    @Override public ProviderRegistry<StatProvider> statProviders() { return STATS; }
    @Override public ProviderRegistry<EquipmentProvider> equipmentProviders() { return EQUIPMENT; }
    @Override public ProviderRegistry<RarityProvider> rarityProviders() { return RARITY; }
    @Override public ProviderRegistry<SkillProvider> skillProviders() { return SKILLS; }
    @Override public ProviderRegistry<SpellProvider> spellProviders() { return SPELLS; }
    @Override public ProviderRegistry<QuestProvider> questProviders() { return QUESTS; }
    @Override public ProviderRegistry<MapProvider> mapProviders() { return MAPS; }
    @Override public ProviderRegistry<CharacterPreviewProvider> characterPreviewProviders() { return PREVIEWS; }
    @Override public ProviderRegistry<ControllerInputProvider> controllerInputProviders() { return CONTROLLERS; }
    @Override public ProviderRegistry<InputGlyphProvider> inputGlyphProviders() { return GLYPHS; }
}
