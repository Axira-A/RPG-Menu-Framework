package dev.rpgmenu.framework.api.theme;

import net.minecraft.resources.ResourceLocation;
import java.util.Map;

/** Public theme sound schema. Missing event keys are silent or may be resolved by the active theme fallback. */
public record SoundDefinition(Map<String, ResourceLocation> events, Map<String, Float> volumes,
                              Map<String, Float> pitches, float masterVolume, boolean disableUiSounds) {
    public SoundDefinition {
        events = Map.copyOf(events);
        volumes = Map.copyOf(volumes);
        pitches = Map.copyOf(pitches);
        masterVolume = Float.isFinite(masterVolume)
                ? Math.max(0.0F, Math.min(1.0F, masterVolume)) : 0.0F;
    }

    public SoundDefinition(Map<String, ResourceLocation> events) {
        this(events, Map.of(), Map.of(), 1.0F, false);
    }
}
