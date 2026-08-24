package dev.rpgmenu.framework.common.stats;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.stats.StatDisplay;
import dev.rpgmenu.framework.api.stats.StatEntry;
import dev.rpgmenu.framework.api.stats.StatGroup;
import dev.rpgmenu.framework.api.stats.StatProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import java.util.List;

public final class VanillaStatProvider implements StatProvider {
    public static final ResourceLocation ID = RpgMenuFramework.id("vanilla");
    @Override public ResourceLocation id() { return ID; }
    @Override public int priority() { return 1_000; }

    @Override
    public List<StatGroup> groups(Player player) {
        return List.of(new StatGroup(RpgMenuFramework.id("core"), "stat_group.rpgmenuframework.core", 1_000, List.of(
                entry("health", player.getHealth(), 0, player.getMaxHealth(), StatDisplay.PROGRESS),
                entry("armor", player.getAttributeValue(Attributes.ARMOR), 0, 30, StatDisplay.NUMBER),
                entry("attack", player.getAttributeValue(Attributes.ATTACK_DAMAGE), 0, 0, StatDisplay.NUMBER),
                entry("attack_speed", player.getAttributeValue(Attributes.ATTACK_SPEED), 0, 0, StatDisplay.NUMBER),
                entry("movement_speed", player.getAttributeValue(Attributes.MOVEMENT_SPEED) * 100, 0, 0, StatDisplay.NUMBER),
                entry("luck", player.getAttributeValue(Attributes.LUCK), 0, 0, StatDisplay.NUMBER)
        )));
    }

    private static StatEntry entry(String path, double value, double min, double max, StatDisplay display) {
        ResourceLocation id = RpgMenuFramework.id(path);
        return new StatEntry(id, "stat.rpgmenuframework." + path, value, min, max, display, id, "");
    }
}
