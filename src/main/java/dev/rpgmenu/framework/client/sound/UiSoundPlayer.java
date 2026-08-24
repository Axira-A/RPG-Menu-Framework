package dev.rpgmenu.framework.client.sound;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.client.theme.ThemeManager;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

/** Plays theme-selected sounds through Minecraft's native UI sound pipeline. */
public final class UiSoundPlayer {
    private static final EnumMap<UiSoundCue, Long> LAST_PLAYED = new EnumMap<>(UiSoundCue.class);
    private static final Set<String> WARNED_MISSING_EVENTS = new HashSet<>();

    private UiSoundPlayer() {}

    public static boolean play(UiSoundCue cue) {
        var profile = ThemeManager.INSTANCE.current().sounds();
        if (profile.disabled() || profile.masterVolume() <= 0.0F) return false;
        var configured = profile.settings(cue);
        if (configured.isEmpty() || configured.get().volume() <= 0.0F) return false;

        long now = Util.getMillis();
        long lastPlayed = LAST_PLAYED.getOrDefault(cue, Long.MIN_VALUE / 2);
        if (cue.cooldownMillis() > 0 && now - lastPlayed < cue.cooldownMillis()) return false;

        var settings = configured.get();
        var sound = BuiltInRegistries.SOUND_EVENT.getOptional(settings.event());
        if (sound.isEmpty()) {
            if (WARNED_MISSING_EVENTS.add(settings.event().toString())) {
                RpgMenuFramework.LOGGER.warn("Theme UI sound event {} is not registered; using silence", settings.event());
            }
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound.get(), settings.pitch(),
                settings.volume() * profile.masterVolume()));
        LAST_PLAYED.put(cue, now);
        return true;
    }
}
