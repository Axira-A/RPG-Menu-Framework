package dev.rpgmenu.framework.api.map;

import net.minecraft.resources.ResourceLocation;

public record Waypoint(ResourceLocation id, String name, ResourceLocation dimension, double x, double y, double z, boolean enabled) {}
