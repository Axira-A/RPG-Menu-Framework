package dev.rpgmenu.framework.client.theme;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

/** Color/spacing subset of the data-driven theme used by the built-in renderer. */
public record ThemeDefinition(int backdrop, int panel, int panelAlt, int border, int borderMuted,
                              int accent, int accentSoft, int text, int textMuted, int danger,
                              int slot, int slotHover, int slotSelected, int padding, int slotSize,
                              UiSoundProfile sounds) {
    public static ThemeDefinition darkFantasy() {
        return new ThemeDefinition(0xC5101215, 0xE116181B, 0xD10C0E10, 0xFF9B7A45, 0xFF4B4030,
                0xFFD5A94F, 0x554E9BD8, 0xFFE8D9BD, 0xFF9E968A, 0xFFCF4A45,
                0xC31C1E20, 0xDD3B3327, 0xDD55452B, 8, 22, UiSoundProfile.darkFantasy());
    }

    public static ThemeDefinition fromJson(JsonObject json) {
        ThemeDefinition d = darkFantasy();
        return new ThemeDefinition(color(json, "backdrop", d.backdrop), color(json, "panel", d.panel),
                color(json, "panelAlt", d.panelAlt), color(json, "border", d.border), color(json, "borderMuted", d.borderMuted),
                color(json, "accent", d.accent), color(json, "accentSoft", d.accentSoft), color(json, "text", d.text),
                color(json, "textMuted", d.textMuted), color(json, "danger", d.danger), color(json, "slot", d.slot),
                color(json, "slotHover", d.slotHover), color(json, "slotSelected", d.slotSelected),
                Math.max(2, Math.min(24, GsonHelper.getAsInt(json, "padding", d.padding))),
                Math.max(18, Math.min(32, GsonHelper.getAsInt(json, "slotSize", d.slotSize))),
                UiSoundProfile.fromJson(json, d.sounds));
    }

    private static int color(JsonObject json, String key, int fallback) {
        if (!json.has(key)) return fallback;
        String value = GsonHelper.getAsString(json, key);
        try {
            String normalized = value.startsWith("#") ? value.substring(1) : value;
            long parsed = Long.parseUnsignedLong(normalized, 16);
            if (normalized.length() <= 6) parsed |= 0xFF000000L;
            return (int)parsed;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
