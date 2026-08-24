package dev.rpgmenu.framework.client.theme;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.client.sound.UiSoundCue;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Immutable, reload-cached sound configuration for one theme. */
public record UiSoundProfile(boolean disabled, float masterVolume, Map<UiSoundCue, UiSoundSettings> events) {
    public UiSoundProfile {
        masterVolume = clamp(masterVolume, 0.0F, 1.0F);
        events = Map.copyOf(events);
    }

    public static UiSoundProfile darkFantasy() {
        EnumMap<UiSoundCue, UiSoundSettings> events = new EnumMap<>(UiSoundCue.class);
        put(events, UiSoundCue.MENU_OPEN, 0.62F, 0.92F);
        put(events, UiSoundCue.MENU_CLOSE, 0.56F, 0.88F);
        put(events, UiSoundCue.TAB_SWITCH, 0.42F, 0.94F);
        put(events, UiSoundCue.SUBTAB_SWITCH, 0.34F, 1.00F);
        put(events, UiSoundCue.FOCUS_MOVE, 0.20F, 0.92F);
        put(events, UiSoundCue.ITEM_SELECT, 0.38F, 0.96F);
        put(events, UiSoundCue.EQUIP, 0.64F, 0.90F);
        put(events, UiSoundCue.REPLACE_EQUIPMENT, 0.68F, 0.84F);
        put(events, UiSoundCue.UNEQUIP, 0.58F, 0.98F);
        put(events, UiSoundCue.FAVORITE, 0.38F, 1.02F);
        put(events, UiSoundCue.CONFIRM, 0.48F, 0.96F);
        put(events, UiSoundCue.CANCEL, 0.42F, 0.88F);
        put(events, UiSoundCue.ERROR, 0.52F, 0.78F);
        put(events, UiSoundCue.MODAL_OPEN, 0.46F, 0.90F);
        put(events, UiSoundCue.MODAL_CLOSE, 0.40F, 0.86F);
        return new UiSoundProfile(false, 0.78F, events);
    }

    public static UiSoundProfile fromJson(JsonObject root, UiSoundProfile fallback) {
        boolean disabled = GsonHelper.getAsBoolean(root, "disableUiSounds", fallback.disabled());
        float master = root.has("uiSoundVolume")
                ? GsonHelper.getAsFloat(root, "uiSoundVolume", fallback.masterVolume())
                : GsonHelper.getAsFloat(root, "masterUiVolume", fallback.masterVolume());
        EnumMap<UiSoundCue, UiSoundSettings> resolved = new EnumMap<>(UiSoundCue.class);
        resolved.putAll(fallback.events());

        JsonObject sounds = root.has("sounds") && root.get("sounds").isJsonObject()
                ? root.getAsJsonObject("sounds") : null;
        if (sounds != null) {
            for (UiSoundCue cue : UiSoundCue.values()) {
                if (!sounds.has(cue.themeKey())) continue;
                applyOverride(resolved, cue, sounds.get(cue.themeKey()));
            }
        }
        return new UiSoundProfile(disabled, master, resolved);
    }

    public Optional<UiSoundSettings> settings(UiSoundCue cue) {
        return Optional.ofNullable(events.get(cue));
    }

    private static void put(EnumMap<UiSoundCue, UiSoundSettings> events, UiSoundCue cue,
                            float volume, float pitch) {
        events.put(cue, new UiSoundSettings(RpgMenuFramework.id(cue.eventPath()), volume, pitch));
    }

    private static void applyOverride(EnumMap<UiSoundCue, UiSoundSettings> events,
                                      UiSoundCue cue, JsonElement value) {
        if (value == null || value.isJsonNull()) {
            events.remove(cue);
            return;
        }
        UiSoundSettings fallback = events.get(cue);
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            ResourceLocation event = ResourceLocation.tryParse(value.getAsString());
            if (event == null || value.getAsString().isBlank()) events.remove(cue);
            else events.put(cue, new UiSoundSettings(event,
                    fallback == null ? 1.0F : fallback.volume(), fallback == null ? 1.0F : fallback.pitch()));
            return;
        }
        if (!value.isJsonObject()) {
            events.remove(cue);
            return;
        }

        JsonObject object = value.getAsJsonObject();
        if (!GsonHelper.getAsBoolean(object, "enabled", true)) {
            events.remove(cue);
            return;
        }
        String eventText = object.has("event") ? GsonHelper.getAsString(object, "event", "")
                : object.has("sound") ? GsonHelper.getAsString(object, "sound", "")
                : fallback == null ? "" : fallback.event().toString();
        ResourceLocation event = ResourceLocation.tryParse(eventText);
        if (event == null || eventText.isBlank()) {
            events.remove(cue);
            return;
        }
        float volume = GsonHelper.getAsFloat(object, "volume", fallback == null ? 1.0F : fallback.volume());
        float pitch = GsonHelper.getAsFloat(object, "pitch", fallback == null ? 1.0F : fallback.pitch());
        events.put(cue, new UiSoundSettings(event, volume, pitch));
    }

    private static float clamp(float value, float minimum, float maximum) {
        if (!Float.isFinite(value)) return minimum;
        return Math.max(minimum, Math.min(maximum, value));
    }
}
