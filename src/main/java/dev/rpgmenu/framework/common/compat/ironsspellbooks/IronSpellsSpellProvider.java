package dev.rpgmenu.framework.common.compat.ironsspellbooks;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.spells.SpellEntry;
import dev.rpgmenu.framework.api.spells.SpellProvider;
import dev.rpgmenu.framework.api.spells.SpellSchool;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Converts the player's synchronized SpellSelectionManager data into framework spell entries. */
public final class IronSpellsSpellProvider implements SpellProvider {
    public static final ResourceLocation ID = RpgMenuFramework.id("irons_spellbooks");
    private boolean unavailableLogged;

    @Override public ResourceLocation id() { return ID; }
    @Override public int priority() { return 500; }

    @Override
    public List<SpellEntry> spells(Player player) {
        try {
            Class<?> managerClass = Class.forName("io.redspace.ironsspellbooks.api.magic.SpellSelectionManager");
            Object manager = managerClass.getConstructor(Player.class).newInstance(player);
            List<?> options = (List<?>) managerClass.getMethod("getAllSpells").invoke(manager);
            List<SpellEntry> result = new ArrayList<>(options.size());
            for (Object option : options) result.add(entry(option, player));
            return List.copyOf(result);
        } catch (ReflectiveOperationException | LinkageError exception) {
            logUnavailable(exception);
            return List.of();
        }
    }

    @Override
    public List<SpellSchool> schools(Player player) {
        Map<ResourceLocation, SpellSchool> schools = new LinkedHashMap<>();
        for (SpellEntry spell : spells(player)) schools.putIfAbsent(spell.school(),
                new SpellSchool(spell.school(), "school." + spell.school().getNamespace() + "." + spell.school().getPath(), 0xFFB689FF));
        return List.copyOf(schools.values());
    }

    private static SpellEntry entry(Object option, Player player) throws ReflectiveOperationException {
        Field spellDataField = option.getClass().getField("spellData");
        Object data = spellDataField.get(option);
        Object spell = data.getClass().getMethod("getSpell").invoke(data);
        int level = ((Number) data.getClass().getMethod("getLevel").invoke(data)).intValue();
        ResourceLocation id = (ResourceLocation) spell.getClass().getMethod("getSpellResource").invoke(spell);
        ResourceLocation icon = (ResourceLocation) spell.getClass().getMethod("getSpellIconResource").invoke(spell);
        Object schoolType = spell.getClass().getMethod("getSchoolType").invoke(spell);
        ResourceLocation school = schoolId(schoolType);
        String titleKey = (String) spell.getClass().getMethod("getComponentId").invoke(spell);
        int mana = ((Number) spell.getClass().getMethod("getManaCost", int.class).invoke(spell, level)).intValue();
        int cooldown = ((Number) spell.getClass().getMethod("getSpellCooldown").invoke(spell)).intValue();
        int castTime = ((Number) spell.getClass().getMethod("getEffectiveCastTime", int.class, net.minecraft.world.entity.LivingEntity.class)
                .invoke(spell, level, player)).intValue();
        return new SpellEntry(id, titleKey, titleKey + ".description", icon, school, level, mana, cooldown, castTime);
    }

    private static ResourceLocation schoolId(Object school) {
        try {
            Object value = school.getClass().getMethod("getId").invoke(school);
            if (value instanceof ResourceLocation id) return id;
        } catch (ReflectiveOperationException ignored) {
            // 1.21 SchoolType did not expose a stable id method in every early build; use a deterministic fallback.
        }
        return ResourceLocation.fromNamespaceAndPath("irons_spellbooks", school.toString().toLowerCase(java.util.Locale.ROOT));
    }

    private void logUnavailable(Throwable exception) {
        if (!unavailableLogged) {
            unavailableLogged = true;
            RpgMenuFramework.LOGGER.warn("[RPGMF] Iron's Spells 1.21 spell bridge is unavailable", exception);
        }
    }
}
