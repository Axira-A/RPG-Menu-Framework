package dev.rpgmenu.framework.api.equipment;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record EquipmentSlotView(EquipmentTarget target, Component title, ItemStack stack,
                                boolean enabled, String disabledReasonKey) {
    public EquipmentSlotView { stack = stack.copy(); }

    public ResourceLocation id() {
        return ResourceLocation.fromNamespaceAndPath(target.providerId().getNamespace(),
                target.providerId().getPath() + "/" + target.slotKey() + "/" + target.slotIndex());
    }
}
