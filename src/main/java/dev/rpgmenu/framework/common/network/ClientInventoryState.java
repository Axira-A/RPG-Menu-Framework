package dev.rpgmenu.framework.common.network;

import dev.rpgmenu.framework.common.network.payload.InventoryPagePayload;
import dev.rpgmenu.framework.common.network.payload.InventoryResultPayload;
import dev.rpgmenu.framework.common.network.payload.EquipmentResultPayload;
import java.util.List;
import java.util.UUID;

/** Client-visible immutable packet state with no client-only type in its signature. */
public final class ClientInventoryState {
    private static volatile InventoryPagePayload page = new InventoryPagePayload(new UUID(0, 0), 0, 0, 1, 0, List.of());
    private static volatile InventoryResultPayload result;
    private static volatile EquipmentResultPayload equipmentResult;
    private static volatile long generation;
    private static volatile long equipmentGeneration;

    private ClientInventoryState() {}
    public static void accept(InventoryPagePayload value) { page = value; generation++; }
    public static void accept(InventoryResultPayload value) { result = value; generation++; }
    public static void accept(EquipmentResultPayload value) { equipmentResult = value; equipmentGeneration++; }
    public static InventoryPagePayload page() { return page; }
    public static InventoryResultPayload result() { return result; }
    public static long generation() { return generation; }
    public static EquipmentResultPayload equipmentResult() { return equipmentResult; }
    public static long equipmentGeneration() { return equipmentGeneration; }
}
