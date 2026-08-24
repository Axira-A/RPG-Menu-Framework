package dev.rpgmenu.framework.api.inventory;

import net.minecraft.network.chat.Component;
import java.util.Map;

public record WeaponInfo(String categoryKey, double attackDamage, double attackSpeed, Map<String, Component> extraStats) {
    public WeaponInfo { extraStats = Map.copyOf(extraStats); }
}
