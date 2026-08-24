package dev.rpgmenu.framework.common.inventory;

import dev.rpgmenu.framework.api.RpgMenuApi;
import dev.rpgmenu.framework.api.inventory.InventoryPage;
import dev.rpgmenu.framework.api.inventory.InventoryQuery;
import dev.rpgmenu.framework.api.inventory.InventorySort;
import dev.rpgmenu.framework.api.inventory.InventorySource;
import dev.rpgmenu.framework.api.inventory.ItemCategoryProvider;
import dev.rpgmenu.framework.api.inventory.ItemIdentity;
import dev.rpgmenu.framework.api.inventory.SourceContribution;
import dev.rpgmenu.framework.api.inventory.UnifiedItemEntry;
import dev.rpgmenu.framework.RpgMenuFramework;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Bounded adapter for the player's small vanilla inventory. */
public final class PlayerInventorySource implements InventorySource {
    public static final ResourceLocation ID = RpgMenuFramework.id("player_inventory");

    @Override public ResourceLocation id() { return ID; }
    @Override public int priority() { return 1_000; }

    @Override
    public InventoryPage query(ServerPlayer player, InventoryQuery query) {
        Inventory inventory = player.getInventory();
        Map<ItemIdentity, MutableEntry> aggregated = new LinkedHashMap<>();
        int mainSlots = Math.min(PlayerInventoryOperations.MAIN_SLOT_COUNT, inventory.getContainerSize());
        for (int slot = 0; slot < mainSlots; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !matches(player, stack, query)) continue;
            ItemIdentity identity = ItemIdentity.of(stack);
            MutableEntry entry = aggregated.computeIfAbsent(identity, ignored -> new MutableEntry(stack.copyWithCount(1)));
            entry.amount = saturatedAdd(entry.amount, stack.getCount());
            entry.sources.add(new SourceContribution(ID, Integer.toString(slot), stack.getCount()));
        }

        List<UnifiedItemEntry> entries = aggregated.values().stream()
                .map(MutableEntry::freeze)
                .sorted(comparator(query.sort()))
                .toList();
        int from = Math.min(entries.size(), query.page() * query.pageSize());
        int to = Math.min(entries.size(), from + query.pageSize());
        return new InventoryPage(entries.subList(from, to), entries.size(), revision(player), query.page(), query.pageSize());
    }

    @Override
    public long revision(ServerPlayer player) {
        long value = 0xcbf29ce484222325L;
        Inventory inventory = player.getInventory();
        int mainSlots = Math.min(PlayerInventoryOperations.MAIN_SLOT_COUNT, inventory.getContainerSize());
        for (int slot = 0; slot < mainSlots; slot++) {
            ItemStack stack = inventory.getItem(slot);
            value ^= ItemStack.hashItemAndComponents(stack);
            value *= 0x100000001b3L;
            value ^= stack.getCount();
        }
        return value;
    }

    @Override
    public ItemStack extract(ServerPlayer player, String opaqueKey, long amount, boolean simulate) {
        int slot = mainSlot(opaqueKey);
        if (slot < 0 || amount <= 0) return ItemStack.EMPTY;
        ItemStack present = player.getInventory().getItem(slot);
        if (present.isEmpty()) return ItemStack.EMPTY;
        int count = (int)Math.min(Math.min(amount, present.getCount()), Integer.MAX_VALUE);
        return simulate ? present.copyWithCount(count) : player.getInventory().removeItem(slot, count);
    }

    @Override
    public long insert(ServerPlayer player, ItemStack stack, long amount, boolean simulate) {
        return PlayerInventoryOperations.insert(player.getInventory(), stack, amount, -1, simulate);
    }

    private static boolean matches(ServerPlayer player, ItemStack stack, InventoryQuery query) {
        if (query.equipmentTarget() != null) {
            var provider = RpgMenuApi.get().equipmentProviders().get(query.equipmentTarget().providerId()).orElse(null);
            if (provider == null || !provider.canEquip(player, query.equipmentTarget(), stack)) return false;
        }
        if (!"all".equals(query.category())) {
            boolean categoryMatch = RpgMenuApi.get().itemCategories().values().stream()
                    .filter(category -> category.id().getPath().equals(query.category()) || category.id().toString().equals(query.category()))
                    .anyMatch(category -> category.matches(stack));
            if (!categoryMatch) return false;
        }
        String search = query.search().toLowerCase(Locale.ROOT);
        if (search.isBlank()) return true;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (search.startsWith("@")) return itemId.getNamespace().contains(search.substring(1));
        if (search.startsWith("#")) {
            String token = search.substring(1);
            return RpgMenuApi.get().itemCategories().values().stream()
                    .anyMatch(category -> (category.id().getPath().contains(token) || category.searchAliases().contains(token)) && category.matches(stack));
        }
        return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(search)
                || itemId.toString().contains(search);
    }

    public static int mainSlot(String opaqueKey) {
        try {
            int slot = Integer.parseInt(opaqueKey);
            return slot >= 0 && slot < PlayerInventoryOperations.MAIN_SLOT_COUNT ? slot : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static Comparator<UnifiedItemEntry> comparator(InventorySort sort) {
        Comparator<UnifiedItemEntry> byId = Comparator.comparing(entry -> BuiltInRegistries.ITEM.getKey(entry.displayStack().getItem()).toString());
        return switch (sort) {
            case NAME -> Comparator.comparing(entry -> entry.displayStack().getHoverName().getString(), String.CASE_INSENSITIVE_ORDER);
            case QUANTITY -> Comparator.comparingLong(UnifiedItemEntry::amount).reversed().thenComparing(byId);
            case MOD_ID -> Comparator.comparing((UnifiedItemEntry entry) -> BuiltInRegistries.ITEM.getKey(entry.displayStack().getItem()).getNamespace()).thenComparing(byId);
            case RECENT -> Comparator.comparingLong(UnifiedItemEntry::acquiredOrder).reversed().thenComparing(byId);
            case RARITY -> Comparator.comparingInt((UnifiedItemEntry entry) -> entry.displayStack().getRarity().ordinal()).reversed().thenComparing(byId);
            default -> byId;
        };
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }

    private static final class MutableEntry {
        private final ItemStack stack;
        private long amount;
        private final List<SourceContribution> sources = new ArrayList<>();
        private MutableEntry(ItemStack stack) { this.stack = stack; }
        private UnifiedItemEntry freeze() { return new UnifiedItemEntry(stack, amount, sources, 0); }
    }
}
