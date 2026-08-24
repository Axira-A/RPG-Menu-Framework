package dev.rpgmenu.framework.common;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.RpgMenuApi;
import dev.rpgmenu.framework.api.menu.RpgMenuTab;
import dev.rpgmenu.framework.api.menu.TabContentFactory;
import dev.rpgmenu.framework.common.inventory.PlayerInventorySource;
import dev.rpgmenu.framework.common.inventory.PredicateItemCategory;
import dev.rpgmenu.framework.common.stats.VanillaStatProvider;
import dev.rpgmenu.framework.common.equipment.VanillaEquipmentProvider;
import dev.rpgmenu.framework.common.rarity.VanillaRarityProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.core.component.DataComponents;
import java.util.Set;
import java.util.function.Predicate;

public final class BuiltInContent {
    private BuiltInContent() {}

    public static void register() {
        RpgMenuApi api = RpgMenuApi.get();
        api.tabs().registerTab(RpgMenuTab.builder(RpgMenuFramework.id("inventory"), "tab.rpgmenuframework.inventory")
                .priority(1_000).content(TabContentFactory.marker("inventory")).build());
        api.tabs().registerTab(RpgMenuTab.builder(RpgMenuFramework.id("attributes"), "tab.rpgmenuframework.attributes")
                .priority(900).content(TabContentFactory.marker("attributes")).build());

        api.inventorySources().register(PlayerInventorySource.ID, new PlayerInventorySource());
        api.statProviders().register(VanillaStatProvider.ID, new VanillaStatProvider());
        api.equipmentProviders().register(VanillaEquipmentProvider.ID, new VanillaEquipmentProvider());
        api.rarityProviders().register(VanillaRarityProvider.ID, new VanillaRarityProvider());

        category("all", 1_000, false, stack -> true, "everything");
        category("weapons", 900, false, stack -> stack.is(ItemTags.SWORDS) || instanceofStack(stack, BowItem.class)
                || instanceofStack(stack, CrossbowItem.class), "weapon", "sword", "bow");
        category("tools", 800, false, stack -> stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES), "tool");
        category("consumables", 700, false, stack -> stack.has(DataComponents.FOOD) || instanceofStack(stack, PotionItem.class), "food", "potion");
        category("blocks", 600, false, stack -> stack.getItem() instanceof BlockItem, "block");
        category("equipment", 500, false, stack -> stack.getItem() instanceof ArmorItem, "armor");
        category("materials", -1_000, true, stack -> true, "material", "misc");
    }

    private static void category(String path, int priority, boolean fallback, Predicate<ItemStack> predicate, String... aliases) {
        var id = RpgMenuFramework.id(path);
        RpgMenuApi.get().itemCategories().register(id, new PredicateItemCategory(
                id, "category.rpgmenuframework." + path, id, priority, fallback, Set.of(aliases), predicate));
    }

    private static boolean instanceofStack(ItemStack stack, Class<?> type) {
        return type.isInstance(stack.getItem());
    }
}
