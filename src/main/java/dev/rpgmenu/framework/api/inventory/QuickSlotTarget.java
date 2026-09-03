package dev.rpgmenu.framework.api.inventory;

import java.util.Objects;

/** Stable wire identity for a quick slot. Resolution to a real provider target is always server-side. */
public record QuickSlotTarget(QuickSlotGroup group, int index) {
    public static final int MAX_INDEX = 63;

    public QuickSlotTarget {
        Objects.requireNonNull(group, "group");
        if (index < 0 || index > MAX_INDEX) throw new IllegalArgumentException("invalid quick slot index");
    }
}
