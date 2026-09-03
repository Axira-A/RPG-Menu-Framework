package dev.rpgmenu.framework.common.compat.ironsspellbooks;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.stats.StatDisplay;
import dev.rpgmenu.framework.api.stats.StatEntry;
import dev.rpgmenu.framework.api.stats.StatGroup;
import dev.rpgmenu.framework.api.stats.StatProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

/** Reads Iron's 1.21 API state without retaining a link when the optional mod is absent. */
public final class IronSpellsStatProvider implements StatProvider {
    public static final ResourceLocation ID = RpgMenuFramework.id("irons_spellbooks");
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "magic");
    private boolean unavailableLogged;

    @Override public ResourceLocation id() { return ID; }
    @Override public int priority() { return 500; }

    @Override
    public List<StatGroup> groups(Player player) {
        try {
            double currentMana = currentMana(player);
            double maxMana = attribute(player, "MAX_MANA");
            return List.of(new StatGroup(RpgMenuFramework.id("magic"), "stat_group.rpgmenuframework.magic", 500, List.of(
                    entry("mana", currentMana, 0, maxMana, StatDisplay.PROGRESS),
                    entry("mana_regen", attribute(player, "MANA_REGEN"), 0, 0, StatDisplay.PERCENT),
                    entry("cooldown_reduction", attribute(player, "COOLDOWN_REDUCTION"), 0, 0, StatDisplay.PERCENT),
                    entry("cast_time_reduction", attribute(player, "CAST_TIME_REDUCTION"), 0, 0, StatDisplay.PERCENT),
                    entry("spell_power", attribute(player, "SPELL_POWER"), 0, 0, StatDisplay.PERCENT)
            )));
        } catch (ReflectiveOperationException | LinkageError exception) {
            logUnavailable(exception);
            return List.of();
        }
    }

    private static double currentMana(Player player) throws ReflectiveOperationException {
        Class<?> magicData = Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
        Method getData = magicData.getMethod("getPlayerMagicData", LivingEntity.class);
        Object data = getData.invoke(null, player);
        return ((Number) magicData.getMethod("getMana").invoke(data)).doubleValue();
    }

    private static double attribute(Player player, String field) throws ReflectiveOperationException {
        Class<?> registry = Class.forName("io.redspace.ironsspellbooks.api.registry.AttributeRegistry");
        Object holder = registry.getField(field).get(null);
        if (holder instanceof Holder<?> attributeHolder) {
            @SuppressWarnings("unchecked") Holder<Attribute> typed = (Holder<Attribute>) attributeHolder;
            return player.getAttributeValue(typed);
        }
        Object value = holder instanceof Supplier<?> supplier ? supplier.get() : holder.getClass().getMethod("value").invoke(holder);
        return player.getAttributeValue(Holder.direct((Attribute) value));
    }

    private static StatEntry entry(String path, double value, double min, double max, StatDisplay display) {
        ResourceLocation id = RpgMenuFramework.id(path);
        return new StatEntry(id, "stat.rpgmenuframework." + path, value, min, max, display, ICON, "");
    }

    private void logUnavailable(Throwable exception) {
        if (!unavailableLogged) {
            unavailableLogged = true;
            RpgMenuFramework.LOGGER.warn("[RPGMF] Iron's Spells 1.21 stat bridge is unavailable", exception);
        }
    }
}
