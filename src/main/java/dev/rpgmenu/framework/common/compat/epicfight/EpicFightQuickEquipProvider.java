package dev.rpgmenu.framework.common.compat.epicfight;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.inventory.QuickEquipKind;
import dev.rpgmenu.framework.api.inventory.QuickEquipProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

/** Isolated 1.21.1 Epic Fight capability bridge; no Epic Fight class can leak into framework core. */
public final class EpicFightQuickEquipProvider implements QuickEquipProvider {
    private static final ResourceLocation ID = RpgMenuFramework.id("epicfight_quick_equip");
    private final Object capabilityProvider;
    private final Method getCapability;
    private final Class<?> weaponCapability;
    private final Class<?> shieldCapability;

    public EpicFightQuickEquipProvider() {
        try {
            Class<?> providerClass = Class.forName(
                    "yesman.epicfight.world.capabilities.provider.CommonItemCapabilityProvider");
            Field instance = providerClass.getField("INSTANCE");
            capabilityProvider = instance.get(null);
            getCapability = providerClass.getMethod("getCapability", ItemStack.class, Void.class);
            weaponCapability = Class.forName("yesman.epicfight.world.capabilities.item.WeaponCapability");
            shieldCapability = Class.forName("yesman.epicfight.world.capabilities.item.ShieldCapability");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Epic Fight 1.21.1 item capability API is incompatible", exception);
        }
    }

    @Override public ResourceLocation id() { return ID; }
    @Override public int priority() { return 1_100; }

    @Override
    public Optional<QuickEquipKind> classify(Player player, ItemStack stack) {
        try {
            Object capability = getCapability.invoke(capabilityProvider, stack, null);
            if (shieldCapability.isInstance(capability)) return Optional.of(QuickEquipKind.SHIELD);
            if (weaponCapability.isInstance(capability)) return Optional.of(QuickEquipKind.WEAPON);
            return Optional.empty();
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Could not inspect Epic Fight item capability", exception);
        }
    }
}
