# RPG Menu Framework

[English](README.md) | [简体中文](README.zh-CN.md)

RPG Menu Framework is a dual-side Minecraft **1.21.1 / NeoForge 21.1.x / Java 21** mod that provides a responsive, extensible RPG player menu. It is a framework rather than a commercial-game UI clone: the bundled dark-fantasy presentation is original, uses only drawn primitives and Minecraft-rendered items/entities, and contains no third-party game assets.

## Features

- `E` optionally replaces the vanilla inventory screen; `R` is an independent RPG-menu key.
- Dynamic semantic tabs, Java registration API, subpages, priorities, conditions, badges and overflow/compact rendering.
- Server-authoritative inventory query protocol with bounded pages, per-player session IDs, opaque entries and storage revisions.
- Data-component-aware item aggregation (`ItemStack.isSameItemSameComponents`), long counts and safe compact formatting.
- Responsive virtual item Grid, categories, sorting model, `@modid` / normal / `#category` search and debounce.
- Real final player render chain with skin, armor, layers and renderer replacements; draggable preview.
- Vanilla equipment and attributes through providers.
- Context-aware quick equip for vanilla hotbar slots, with optional Epic Fight support.
- Optional integrations for Curios, More Offhand Slots, Iron's Spells 'n Spellbooks, FTB Quests, Xaero's World Map, JourneyMap, Epic Fight and Epic Skills. Tabs are shown only when their backing provider is available.
- Embedded map view with configurable preferred provider and preserved pan/zoom state.
- Non-pausing menu with normal remappable WASD/Space/Shift movement; focused search blocks movement passthrough.
- Per-server favorites stored as UI metadata.
- Resource-reloadable theme colors, `en_us` / `zh_cn`, resource-pack JSON tabs and a developer layout editor.
- Theme-mapped native UI sounds with master/per-event volume and pitch, safe silence overrides and focus-repeat cooldown.
- Vanilla 2×2 crafting and all native slot gestures remain available through `C`, which opens the untouched vanilla inventory.

## Install

1. Install NeoForge `21.1.x` for Minecraft `1.21.1`.
2. Put `rpgmenuframework-0.1.0.jar` in both client and server `mods` directories.
3. Optional integrations are soft dependencies; the framework starts without them. Install a supported integration on the appropriate side to enable its extra tab or equipment support.

Client configuration is generated in `config/rpgmenuframework-client.toml`. Set `replaceVanillaInventory=false` to leave `E` unchanged and use `R` only. Use `preferredMapProvider` to select an installed map provider and `preserveMapView` to retain its center and zoom between menu openings.

### Optional integrations

| Integration | Menu support |
| --- | --- |
| Curios / More Offhand Slots | Additional equipment slots and safe equipment actions when installed. |
| Iron's Spells 'n Spellbooks | Spells tab and spell-related stats. |
| FTB Quests | Quests tab. |
| Xaero's World Map / JourneyMap | Embedded map tab; choose a preferred installed provider in the client config. |
| Epic Fight | Quick-equip integration. |
| Epic Skills | Skills tab when Epic Fight is also present. |

All integrations are optional. Availability is detected at startup; missing or incompatible optional mods do not prevent the framework from loading.

## Developer API

The common entry point is `RpgMenuApi.get()`. Core extension types live under `dev.rpgmenu.framework.api` and do not depend on `Screen`, `GuiGraphics`, `Font`, textures or player renderer classes. See [API.md](docs/API.md), [TABS.md](docs/TABS.md), [THEMING.md](docs/THEMING.md), [ARCHITECTURE.md](docs/ARCHITECTURE.md) and [COMPATIBILITY.md](docs/COMPATIBILITY.md).

### Register a tab

```java
RpgMenuApi.get().tabs().registerTab(RpgMenuTab.builder(
        ResourceLocation.fromNamespaceAndPath("example", "factions"),
        "tab.example.factions")
    .priority(500)
    .content(TabContentFactory.marker("placeholder"))
    .build());
```

### Register a StatProvider

```java
RpgMenuApi.get().statProviders().register(MY_ID, new MyStatProvider());
```

### Register an InventorySource

Implement `InventorySource.query` so filtering, sorting and pagination happen at the source. Never return an unbounded storage snapshot. Mutations are server-only through `extract` / `insert` and the framework transaction coordinator.

## Layout editor

Run `/rpgmenuframework editor` on the client. Drag a component to move it, drag its lower-right handle to resize, press `S` to save/export `config/rpgmenuframework/layout.json`, or `R` to reset.

## Build

```text
./gradlew build
./gradlew runClient
./gradlew runServer
```

The checked-in wrapper targets Gradle 8.14.2. The build baseline is ModDevGradle 2.0.137, NeoForge 21.1.244, Mojang mappings with Parchment 2024.11.17 supplements, and Java 21.

## UI sound attribution

Selected UI sound effects are from **Universal UI/Menu Soundpack** by **Nathan Gibson / Cyrex Studios**, licensed under the [Creative Commons Attribution 4.0 International license](https://creativecommons.org/licenses/by/4.0/). The [original soundpack page](https://cyrex-studios.itch.io/universal-ui-soundpack) and complete attribution are recorded in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Known limits in 0.1.0

- Beyond Dimensions, RarityCore and Controlify have public extension boundaries, but do not ship a dedicated adapter in this release.
- Virtual-grid withdrawal is implemented for external sources that provide safe `extract`/`insert`; vanilla inventory entries are inspect/favorite-only because native operations remain in the vanilla crafting inventory.
- Theme JSON colors, layout overrides and namespaced UI sound mappings are active. Dynamic external PNG/TTF/OGG pack construction is not implemented; sound assets remain standard Minecraft resource-pack assets.
- The layout editor currently edits position and size deltas; anchors, opacity and z-index are represented in the public schema but not all have editor controls yet.

These limits are tracked precisely in [COMPATIBILITY.md](docs/COMPATIBILITY.md).
