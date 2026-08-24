# Architecture

The common API has no client-only dependencies. The physical client owns rendering, input, local preferences and resource reload. The logical server owns sessions, inventory queries, permissions and mutation.

```text
RpgMenuApi
├─ MenuTabRegistry ── semantic tabs / subpages
├─ InventorySource registry ── PlayerInventorySource / future storage adapters
├─ ItemCategoryProvider registry
├─ StatProvider registry
├─ EquipmentProvider registry
├─ RarityProvider registry
└─ Skill / Spell / Quest / Map / Character / Input registries

Client                                      Server
RpgMenuScreen                               MenuSessionManager
├─ ResponsiveLayout                        ├─ player + session ownership
├─ ThemeManager                            ├─ bounded opaque entry map
├─ virtual Grid        inventory_query ───>├─ UnifiedInventoryService
├─ final player render <── inventory_page ─┤  └─ source-side query/page
├─ FavoriteStore                           └─ InventoryTransactionCoordinator
└─ InputRouter          inventory_action ─>   ├─ nonce replay protection
                       <─ inventory_result    ├─ capacity preflight
                                               └─ remainder rollback
```

## Source tree

```text
dev.rpgmenu.framework
├─ api/{menu,inventory,stats,equipment,skills,spells,quests,map,rarity,character,input,theme,event}
├─ common/{menu,inventory,network,registry,stats,equipment,rarity,config,util}
├─ client/{screen,layout,theme,input,editor}
└─ compat/<modid> (reserved strict classloading boundary)
```

## Inventory invariants

- Item identity is item plus the complete data-component set, never registry ID alone.
- Pages contain at most 256 entries; queries contain at most 128 search characters.
- The client sends only session, opaque entry, amount, operation and nonce for mutation.
- Handlers run on the main thread through NeoForge's default `PayloadRegistrar` handling mode.
- Sessions are owned by the sending player, expire after five idle minutes and are bounded to eight per player.
- Transaction requests are bounded to 4096 units, replay-safe, capacity-checked and return uninserted remainders to the source. If a broken source refuses rollback, the remainder is dropped at the player and an error is logged, preventing silent loss.

## Rendering and compatibility

The preview calls Minecraft's `InventoryScreen.renderEntityInInventoryFollowsAngle`, which reaches the active `EntityRenderDispatcher` and the final player renderer. The framework does not reset player poses, replace player models or import YSM/Epic Fight/Player Animator/Better Combat types. This is intentional compatibility by composition.
