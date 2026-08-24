package dev.rpgmenu.framework.api.map;

import net.minecraft.resources.ResourceLocation;

public record MapMarker(ResourceLocation id, String title, ResourceLocation dimension, double x, double y, double z,
                        int color, ResourceLocation icon) {}
