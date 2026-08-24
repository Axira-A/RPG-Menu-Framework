# Theming and layout

The active theme is selected by `activeTheme` in `rpgmenuframework-client.toml`. Bundled/resource-pack themes live at:

```text
assets/<namespace>/themes/<theme>/theme.json
```

The default file is `assets/rpgmenuframework/themes/dark_fantasy/theme.json`. It controls backdrop, panel, border, accent, text, danger and slot colors plus padding and slot size. Colors use `#AARRGGBB` or `#RRGGBB`.

Theme names are limited to `[a-z0-9_-]{1,64}`. Invalid names, malformed JSON or missing resources fall back to the original `dark_fantasy` definition and log one clear warning. Theme files are loaded only during resource reload, never per frame.

## UI sounds

UI audio uses registered Minecraft `SoundEvent`s and the native UI `SoundManager` path. The top-level `uiSoundVolume` is multiplied by each event's `volume`; Minecraft's Master sound slider is then applied by the normal client sound engine. Set `disableUiSounds` to `true` to silence the complete theme.

The `sounds` object accepts either a namespaced event ID or an object with `event`, `volume` and `pitch`:

```json
{
  "disableUiSounds": false,
  "uiSoundVolume": 0.78,
  "sounds": {
    "menu_open": "example:menu_open",
    "focus": {
      "event": "example:focus",
      "volume": 0.2,
      "pitch": 0.92
    },
    "error": null
  }
}
```

Supported semantic keys are `menu_open`, `menu_close`, `tab_switch`, `subtab_switch`, `focus`, `select`, `equip`, `replace_equipment`, `unequip`, `favorite`, `confirm`, `cancel`, `error`, `modal_open` and `modal_close`. Omitted keys inherit the built-in defaults. `null`, an empty/invalid ID, or `{ "enabled": false }` explicitly selects silence. Resource IDs that are syntactically valid but not registered also fail safely to silence with one log warning. Theme data is cached during resource reload; playback never rereads the JSON from disk.

## Responsive profiles

`ResponsiveLayout` selects `DESKTOP_LARGE`, `DESKTOP`, `COMPACT` or `SMALL` from current scaled width and height. It recalculates constraints rather than uniformly shrinking a 1920×1080 canvas. Grid columns and rows derive from the resulting panel and theme slot size.

## Editor

`/rpgmenuframework editor` edits deltas for `TopTabs`, `SubTabs`, `SearchBar`, `InventoryGrid`, `ItemDetail`, `CharacterPreview`, `StatusPanel` and `FooterHints`. The result is written to `config/rpgmenuframework/layout.json`, sanitized and clamped before use.

## Asset status

Minecraft resource packs can already override normal namespaced assets and the theme JSON. Direct external PNG/TTF/OGG import and dynamic pack construction are intentionally not active in 0.1.0; no ad-hoc OpenGL font or audio decoder is used. Bundled or resource-pack OGG files are resolved exclusively through Minecraft's sound manager and `sounds.json` system.
