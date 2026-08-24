# Tabs

Top-level tabs represent RPG concepts, not installed mods. Multiple skill providers belong under one `skills` tab as subpages.

`RpgMenuTab` supports ID, translation key, icon ID, priority, required mod, visibility/enabled predicates, disabled reason, tooltip, badge, logical content factory and ordered subpages. `MenuTabRegistry` supports registration, removal and adding a subpage to an existing semantic tab.

## Data-driven tabs

Place files in `assets/<namespace>/rpg_menu_tabs/*.json`:

```json
{
  "id": "example:factions",
  "title": "tab.example.factions",
  "icon": "example:textures/gui/icons/factions.png",
  "priority": 450,
  "required_mod": "example",
  "action": "placeholder",
  "subpages": [
    {
      "id": "example:reputation",
      "title": "subpage.example.reputation",
      "priority": 100,
      "action": "placeholder"
    }
  ]
}
```

Only registered safe action types are accepted: `placeholder`, `inventory`, and `attributes` in 0.1.0. JSON cannot execute Java, commands, scripts, URLs or system actions. Resource reload removes only tabs previously loaded from JSON; Java registrations remain authoritative on conflicts.

When tabs exceed available width, the built-in screen switches to compact mode and provides a horizontally scrollable window instead of shrinking labels indefinitely.
