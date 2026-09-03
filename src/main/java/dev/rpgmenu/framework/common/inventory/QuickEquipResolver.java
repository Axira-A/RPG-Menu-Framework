package dev.rpgmenu.framework.common.inventory;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.RpgMenuApi;
import dev.rpgmenu.framework.api.equipment.EquipmentProvider;
import dev.rpgmenu.framework.api.equipment.EquipmentTarget;
import dev.rpgmenu.framework.api.inventory.QuickEquipKind;
import dev.rpgmenu.framework.api.inventory.QuickSlotGroup;
import dev.rpgmenu.framework.api.inventory.QuickSlotTarget;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;

/** One classification and validation path shared by mouse, controller and the authoritative server transaction. */
public final class QuickEquipResolver {
    public static final TagKey<Item> WEAPONS = tag("quick_equip/weapons");
    public static final TagKey<Item> TOOLS = tag("quick_equip/tools");
    public static final TagKey<Item> SHIELDS = tag("quick_equip/shields");
    private static final java.util.Set<net.minecraft.resources.ResourceLocation> FAILED_PROVIDERS =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private QuickEquipResolver() {}

    public static QuickEquipKind classify(Player player, ItemStack stack) {
        if (stack.isEmpty()) return QuickEquipKind.ITEM;
        for (var provider : RpgMenuApi.get().quickEquipProviders().values()) {
            if (FAILED_PROVIDERS.contains(provider.id())) continue;
            try {
                var classified = provider.classify(player, stack);
                if (classified.isPresent()) return classified.get();
            } catch (LinkageError | RuntimeException exception) {
                if (FAILED_PROVIDERS.add(provider.id())) {
                    RpgMenuFramework.LOGGER.warn("Quick-equip provider {} failed; it will be skipped for this session",
                            provider.id(), exception);
                }
            }
        }

        if (stack.is(SHIELDS)) return QuickEquipKind.SHIELD;
        if (stack.is(WEAPONS)) return QuickEquipKind.WEAPON;
        if (stack.is(TOOLS)) return QuickEquipKind.TOOL;

        for (var category : RpgMenuApi.get().itemCategories().values()) {
            String path = category.id().getPath();
            if (("weapons".equals(path) || "weapon".equals(path)) && category.matches(stack)) {
                return QuickEquipKind.WEAPON;
            }
            if (("tools".equals(path) || "tool".equals(path)) && category.matches(stack)) {
                return QuickEquipKind.TOOL;
            }
        }

        if (stack.getItem() instanceof ShieldItem) return QuickEquipKind.SHIELD;
        if (stack.is(ItemTags.SWORDS) || stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem || stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof MaceItem) return QuickEquipKind.WEAPON;
        if (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES)) return QuickEquipKind.TOOL;
        return QuickEquipKind.ITEM;
    }

    public static boolean canPlace(Player player, ItemStack stack, QuickSlotTarget target, boolean automatic) {
        EquipmentTarget equipmentTarget = QuickSlotTargets.equipmentTarget(target);
        if (equipmentTarget == null || stack.isEmpty()) return false;
        QuickEquipKind kind = classify(player, stack);
        if (automatic && kind.defaultGroup() != target.group()) return false;
        if (target.group() == QuickSlotGroup.ITEM_BAR
                && (kind == QuickEquipKind.WEAPON || kind == QuickEquipKind.SHIELD)) return false;
        EquipmentProvider provider = RpgMenuApi.get().equipmentProviders().get(equipmentTarget.providerId()).orElse(null);
        return provider != null && provider.slot(player, equipmentTarget).filter(view -> view.enabled()).isPresent()
                && provider.canEquip(player, equipmentTarget, stack);
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, RpgMenuFramework.id(path));
    }
}
