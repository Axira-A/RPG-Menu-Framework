package dev.rpgmenu.framework.api.input;

import dev.rpgmenu.framework.api.registry.Prioritized;
import net.minecraft.resources.ResourceLocation;

public interface ControllerInputProvider extends Prioritized {
    ResourceLocation id();
    boolean active();
    boolean consume(InputAction action);
}
