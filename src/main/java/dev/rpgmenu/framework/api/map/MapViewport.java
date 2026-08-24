package dev.rpgmenu.framework.api.map;

import net.minecraft.resources.ResourceLocation;

public record MapViewport(ResourceLocation dimension, double centerX, double centerZ, double zoom) {}
