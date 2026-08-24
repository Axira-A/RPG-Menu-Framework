package dev.rpgmenu.framework.common;

import dev.rpgmenu.framework.RpgMenuFramework;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Native sound events used by the semantic UI sound layer. */
public final class ModSoundEvents {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, RpgMenuFramework.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> RPG_MENU_OPEN = register("rpg_menu_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> RPG_MENU_CLOSE = register("rpg_menu_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> TAB_SWITCH = register("tab_switch");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUBTAB_SWITCH = register("subtab_switch");
    public static final DeferredHolder<SoundEvent, SoundEvent> FOCUS_MOVE = register("focus_move");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_SELECT = register("item_select");
    public static final DeferredHolder<SoundEvent, SoundEvent> EQUIP = register("equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> REPLACE_EQUIPMENT = register("replace_equipment");
    public static final DeferredHolder<SoundEvent, SoundEvent> UNEQUIP = register("unequip");
    public static final DeferredHolder<SoundEvent, SoundEvent> FAVORITE = register("favorite");
    public static final DeferredHolder<SoundEvent, SoundEvent> CONFIRM = register("confirm");
    public static final DeferredHolder<SoundEvent, SoundEvent> CANCEL = register("cancel");
    public static final DeferredHolder<SoundEvent, SoundEvent> ERROR = register("error");
    public static final DeferredHolder<SoundEvent, SoundEvent> MODAL_OPEN = register("modal_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> MODAL_CLOSE = register("modal_close");

    private ModSoundEvents() {}

    public static void register(IEventBus modBus) {
        SOUND_EVENTS.register(modBus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(RpgMenuFramework.id(name)));
    }
}
