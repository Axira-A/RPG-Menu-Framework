package dev.rpgmenu.framework.api.theme;

import java.util.Map;

public record LayoutDefinition(Map<String, Constraint> components) {
    public LayoutDefinition { components = Map.copyOf(components); }
    public record Constraint(String anchor, int marginX, int marginY, int minWidth, int minHeight, int zIndex) {}
}
