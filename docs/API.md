# Developer API

Use `RpgMenuApi.get()` from common code. Provider registries are ordered by descending `Prioritized.priority()` and then stable ID. Duplicate IDs throw immediately; integrations should use their own namespace.

Registration can be direct during mod setup or event-style by listening on `NeoForge.EVENT_BUS` for:

- `RegisterRpgMenuTabsEvent`
- `RegisterInventorySourcesEvent`
- `RegisterItemCategoriesEvent`
- `RegisterStatProvidersEvent`

## InventorySource

`query(ServerPlayer, InventoryQuery)` is always server-side. A large source must apply query/category/sort/page before constructing entries. `InventoryQuery.MAX_PAGE_SIZE` is 256.

```java
public final class ExampleSource implements InventorySource {
    public ResourceLocation id() { return EXAMPLE_ID; }

    public InventoryPage query(ServerPlayer player, InventoryQuery query) {
        // Validate access, query only the requested page, and return opaque source keys.
        return new InventoryPage(entries, totalTypes, revision, query.page(), query.pageSize());
    }

    public ItemStack extract(ServerPlayer player, String opaqueKey, long amount, boolean simulate) {
        // Resolve opaqueKey again against authoritative storage; never trust a client ItemStack.
    }

    public long insert(ServerPlayer player, ItemStack stack, long amount, boolean simulate) {
        // Return the accepted long amount.
    }
}
```

Each `UnifiedItemEntry` carries source contributions. Do not create a source key from only an item registry ID. The underlying source must distinguish all data components.

## ItemCategoryProvider

Categories can use item tags, predicates or an external API. Multiple categories may match an item. Mark one low-priority category as `fallback()` when needed.

## StatProvider

Return grouped read-only `StatEntry` values. Mutation is deliberately absent from the base interface; an integration that adds stat points must define an explicit server action and permission checks.

## EquipmentProvider and RarityProvider

Equipment providers return dynamic slots with enabled/disabled state. The built-in screen lays out however many providers expose. Rarity providers return optional presentation style and must not modify the stack.

## Client boundary

The common API uses `Component`, `ResourceLocation`, `Player`, `ServerPlayer` and `ItemStack`, but never imports `Screen`, `GuiGraphics`, `Font`, `TextureManager` or renderer classes. Client rendering extensions should live in a client source/package and be bootstrapped only on `Dist.CLIENT`.
