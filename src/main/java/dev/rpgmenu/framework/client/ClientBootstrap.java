package dev.rpgmenu.framework.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.client.editor.LayoutEditorScreen;
import dev.rpgmenu.framework.client.input.InputRouter;
import dev.rpgmenu.framework.client.screen.RpgMenuScreen;
import dev.rpgmenu.framework.client.theme.ThemeManager;
import dev.rpgmenu.framework.common.config.FrameworkConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/** Loaded only on the physical client. */
public final class ClientBootstrap {
    private static final KeyMapping OPEN_MENU = new KeyMapping("key.rpgmenuframework.open",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.rpgmenuframework");
    private static boolean bypassInventoryReplacement;

    private ClientBootstrap() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientBootstrap::registerKeys);
        modBus.addListener(ClientBootstrap::registerReloadListeners);
        NeoForge.EVENT_BUS.addListener(ClientBootstrap::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(ClientBootstrap::onClientTickPre);
        NeoForge.EVENT_BUS.addListener(ClientBootstrap::onClientTickPost);
        NeoForge.EVENT_BUS.addListener(ClientBootstrap::registerClientCommands);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) { event.register(OPEN_MENU); }
    private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(ThemeManager.INSTANCE);
        event.registerReloadListener(DataDrivenTabLoader.INSTANCE);
    }

    private static void onScreenOpening(ScreenEvent.Opening event) {
        if (bypassInventoryReplacement) {
            bypassInventoryReplacement = false;
            return;
        }
        if (FrameworkConfig.REPLACE_VANILLA_INVENTORY.get() && event.getNewScreen() instanceof InventoryScreen) {
            event.setNewScreen(new RpgMenuScreen());
        }
    }

    private static void onClientTickPre(ClientTickEvent.Pre event) { InputRouter.tick(Minecraft.getInstance()); }

    private static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_MENU.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) minecraft.setScreen(new RpgMenuScreen());
        }
    }

    private static void registerClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rpgmenuframework")
                .executes(context -> { Minecraft.getInstance().setScreen(new RpgMenuScreen()); return 1; })
                .then(Commands.literal("editor")
                        .executes(context -> { Minecraft.getInstance().setScreen(new LayoutEditorScreen()); return 1; })));
    }

    public static void openVanillaInventory() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        bypassInventoryReplacement = true;
        minecraft.setScreen(new InventoryScreen(minecraft.player));
    }
}
