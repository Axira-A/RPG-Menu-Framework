package dev.rpgmenu.framework.api;

import dev.rpgmenu.framework.api.inventory.InventorySource;
import dev.rpgmenu.framework.api.inventory.ItemCategoryProvider;
import dev.rpgmenu.framework.api.inventory.QuickEquipProvider;
import dev.rpgmenu.framework.api.equipment.EquipmentProvider;
import dev.rpgmenu.framework.api.rarity.RarityProvider;
import dev.rpgmenu.framework.api.skills.SkillProvider;
import dev.rpgmenu.framework.api.spells.SpellProvider;
import dev.rpgmenu.framework.api.quests.QuestProvider;
import dev.rpgmenu.framework.api.map.MapProvider;
import dev.rpgmenu.framework.api.character.CharacterPreviewProvider;
import dev.rpgmenu.framework.api.input.ControllerInputProvider;
import dev.rpgmenu.framework.api.input.InputGlyphProvider;
import dev.rpgmenu.framework.api.menu.MenuTabRegistry;
import dev.rpgmenu.framework.api.registry.ProviderRegistry;
import dev.rpgmenu.framework.api.stats.StatProvider;
import java.util.ServiceLoader;

/** Stable common-side entry point for RPG Menu Framework extensions. */
public interface RpgMenuApi {
    /** Returns the framework service without linking any client-only class. */
    static RpgMenuApi get() {
        return Holder.INSTANCE;
    }

    MenuTabRegistry tabs();

    ProviderRegistry<InventorySource> inventorySources();

    ProviderRegistry<ItemCategoryProvider> itemCategories();

    ProviderRegistry<QuickEquipProvider> quickEquipProviders();

    ProviderRegistry<StatProvider> statProviders();

    ProviderRegistry<EquipmentProvider> equipmentProviders();
    ProviderRegistry<RarityProvider> rarityProviders();
    ProviderRegistry<SkillProvider> skillProviders();
    ProviderRegistry<SpellProvider> spellProviders();
    ProviderRegistry<QuestProvider> questProviders();
    ProviderRegistry<MapProvider> mapProviders();
    ProviderRegistry<CharacterPreviewProvider> characterPreviewProviders();
    ProviderRegistry<ControllerInputProvider> controllerInputProviders();
    ProviderRegistry<InputGlyphProvider> inputGlyphProviders();

    final class Holder {
        private static final RpgMenuApi INSTANCE = ServiceLoader.load(RpgMenuApi.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("RPG Menu Framework API service is unavailable"));
    }
}
