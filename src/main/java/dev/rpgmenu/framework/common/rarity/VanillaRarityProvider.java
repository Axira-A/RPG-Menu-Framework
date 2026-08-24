package dev.rpgmenu.framework.common.rarity;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.rarity.RarityProvider;
import dev.rpgmenu.framework.api.rarity.RarityStyle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;

public final class VanillaRarityProvider implements RarityProvider {
    public static final ResourceLocation ID = RpgMenuFramework.id("vanilla");
    @Override public ResourceLocation id() { return ID; }
    @Override public int priority() { return -10_000; }

    @Override
    public Optional<RarityStyle> style(ItemStack stack) {
        String name = stack.getRarity().name().toLowerCase(java.util.Locale.ROOT);
        int color = switch (name) {
            case "uncommon" -> 0xFFFFFF55;
            case "rare" -> 0xFF55FFFF;
            case "epic" -> 0xFFFF55FF;
            default -> 0xFFFFFFFF;
        };
        int background = switch (name) {
            case "uncommon" -> 0x442F3418;
            case "rare" -> 0x44203A45;
            case "epic" -> 0x44392345;
            default -> 0x221C1E20;
        };
        return Optional.of(new RarityStyle(RpgMenuFramework.id(name), "rarity.minecraft." + name, color, background, color));
    }
}
