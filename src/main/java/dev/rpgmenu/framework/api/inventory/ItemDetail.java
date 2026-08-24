package dev.rpgmenu.framework.api.inventory;

import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.Map;

public record ItemDetail(List<Component> tooltip, Map<String, Component> fields) {
    public ItemDetail { tooltip = List.copyOf(tooltip); fields = Map.copyOf(fields); }
}
