package dev.rpgmenu.framework.common.network;

import dev.rpgmenu.framework.api.inventory.TransactionResult;
import dev.rpgmenu.framework.api.equipment.EquipmentAction;
import dev.rpgmenu.framework.api.equipment.EquipmentTransaction;
import dev.rpgmenu.framework.common.equipment.EquipmentTransactionCoordinator;
import dev.rpgmenu.framework.common.inventory.InventoryTransactionCoordinator;
import dev.rpgmenu.framework.common.inventory.MenuSessionManager;
import dev.rpgmenu.framework.common.network.payload.InventoryActionPayload;
import dev.rpgmenu.framework.common.network.payload.InventoryPagePayload;
import dev.rpgmenu.framework.common.network.payload.InventoryQueryPayload;
import dev.rpgmenu.framework.common.network.payload.InventoryResultPayload;
import dev.rpgmenu.framework.common.network.payload.EquipmentActionPayload;
import dev.rpgmenu.framework.common.network.payload.EquipmentResultPayload;
import dev.rpgmenu.framework.common.network.payload.QuickbarActionPayload;
import dev.rpgmenu.framework.common.inventory.QuickbarTransactionCoordinator;
import dev.rpgmenu.framework.common.inventory.QuickSlotTargets;
import dev.rpgmenu.framework.api.equipment.EquipmentTarget;
import dev.rpgmenu.framework.RpgMenuFramework;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkHandler {
    public static final String PROTOCOL_VERSION = "3";
    private NetworkHandler() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(InventoryQueryPayload.TYPE, InventoryQueryPayload.STREAM_CODEC, NetworkHandler::handleQuery);
        registrar.playToClient(InventoryPagePayload.TYPE, InventoryPagePayload.STREAM_CODEC, (payload, context) -> ClientInventoryState.accept(payload));
        registrar.playToServer(InventoryActionPayload.TYPE, InventoryActionPayload.STREAM_CODEC, NetworkHandler::handleAction);
        registrar.playToClient(InventoryResultPayload.TYPE, InventoryResultPayload.STREAM_CODEC, (payload, context) -> ClientInventoryState.accept(payload));
        registrar.playToServer(EquipmentActionPayload.TYPE, EquipmentActionPayload.STREAM_CODEC, NetworkHandler::handleEquipmentAction);
        registrar.playToClient(EquipmentResultPayload.TYPE, EquipmentResultPayload.STREAM_CODEC,
                (payload, context) -> ClientInventoryState.accept(payload));
        registrar.playToServer(QuickbarActionPayload.TYPE, QuickbarActionPayload.STREAM_CODEC,
                NetworkHandler::handleQuickbarAction);
    }

    private static void handleQuery(InventoryQueryPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        context.reply(MenuSessionManager.INSTANCE.query(player, payload.sessionId(), payload.query()));
    }

    private static void handleAction(InventoryActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        TransactionResult result = MenuSessionManager.INSTANCE.resolve(player, payload.sessionId(), payload.entryOpaqueId())
                .map(access -> InventoryTransactionCoordinator.INSTANCE.execute(player, access, payload.operation(), payload.requestedAmount(), payload.nonce()))
                .orElseGet(() -> new TransactionResult(TransactionResult.Status.STALE, 0, "message.rpgmenuframework.stale_session"));
        context.reply(new InventoryResultPayload(payload.sessionId(), payload.nonce(), result.status(), result.moved(), result.messageKey()));
    }

    private static void handleEquipmentAction(EquipmentActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        EquipmentTransaction transaction = payload.transaction();
        TransactionResult result;
        var replay = MenuSessionManager.INSTANCE.replay(player, transaction.sessionId(), transaction.nonce());
        if (replay.isPresent()) {
            result = replay.get();
        } else if (!MenuSessionManager.INSTANCE.active(player, transaction.sessionId())) {
            result = new TransactionResult(TransactionResult.Status.STALE, 0, "message.rpgmenuframework.stale_session");
        } else if (transaction.action() == EquipmentAction.EQUIP) {
            result = MenuSessionManager.INSTANCE.resolve(player, transaction.sessionId(), transaction.entryOpaqueId())
                    .map(access -> EquipmentTransactionCoordinator.INSTANCE.execute(player, transaction, access))
                    .orElseGet(() -> new TransactionResult(TransactionResult.Status.STALE, 0,
                            "message.rpgmenuframework.stale_session"));
        } else {
            result = EquipmentTransactionCoordinator.INSTANCE.execute(player, transaction, null);
        }
        context.reply(new EquipmentResultPayload(transaction.sessionId(), transaction.nonce(), transaction.target(),
                result.status(), result.moved(), result.messageKey()));
    }

    private static void handleQuickbarAction(QuickbarActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        TransactionResult result;
        if (!MenuSessionManager.INSTANCE.active(player, payload.sessionId())) {
            result = new TransactionResult(TransactionResult.Status.STALE, 0,
                    "message.rpgmenuframework.stale_session");
        } else {
            MenuSessionManager.SessionAccess access = payload.action()
                    == dev.rpgmenu.framework.api.inventory.QuickbarAction.PLACE_ENTRY
                    ? MenuSessionManager.INSTANCE.resolve(player, payload.sessionId(), payload.entryOpaqueId()).orElse(null)
                    : null;
            result = QuickbarTransactionCoordinator.INSTANCE.execute(player, payload.sessionId(),
                    payload.entryOpaqueId(), payload.nonce(), payload.action(), payload.source(), payload.target(), access);
        }
        var responseQuickTarget = payload.action() == dev.rpgmenu.framework.api.inventory.QuickbarAction.MOVE_TO_INVENTORY
                ? payload.source() : payload.target();
        EquipmentTarget responseTarget = QuickSlotTargets.equipmentTarget(responseQuickTarget);
        if (responseTarget == null) responseTarget = new EquipmentTarget(RpgMenuFramework.id("invalid"), "invalid", 0);
        context.reply(new EquipmentResultPayload(payload.sessionId(), payload.nonce(),
                responseTarget,
                result.status(), result.moved(), result.messageKey()));
    }
}
