package dev.rpgmenu.framework.client.theme;

import net.minecraft.resources.ResourceLocation;

/** One resolved theme sound entry. */
public record UiSoundSettings(ResourceLocation event, float volume, float pitch) {
    public UiSoundSettings {
        if (event == null) throw new IllegalArgumentException("event cannot be null");
        volume = clamp(volume, 0.0F, 1.0F);
        pitch = clamp(pitch, 0.5F, 2.0F);
    }

    private static float clamp(float value, float minimum, float maximum) {
        if (!Float.isFinite(value)) return minimum;
        return Math.max(minimum, Math.min(maximum, value));
    }
}
