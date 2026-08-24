package dev.rpgmenu.framework;

import com.mojang.logging.LogUtils;
import dev.rpgmenu.framework.api.RpgMenuApi;
import dev.rpgmenu.framework.api.event.RegisterInventorySourcesEvent;
import dev.rpgmenu.framework.api.event.RegisterEquipmentProvidersEvent;
import dev.rpgmenu.framework.api.event.RegisterItemCategoriesEvent;
import dev.rpgmenu.framework.api.event.RegisterRpgMenuTabsEvent;
import dev.rpgmenu.framework.api.event.RegisterStatProvidersEvent;
import dev.rpgmenu.framework.common.BuiltInContent;
import dev.rpgmenu.framework.common.ModSoundEvents;
import dev.rpgmenu.framework.common.config.FrameworkConfig;
import dev.rpgmenu.framework.common.network.NetworkHandler;
import dev.rpgmenu.framework.common.gametest.EquipmentGameTests;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.slf4j.Logger;

@Mod(RpgMenuFramework.MOD_ID)
public final class RpgMenuFramework {
    public static final String MOD_ID = "rpgmenuframework";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RpgMenuFramework(IEventBus modBus, ModContainer container) {
        ModSoundEvents.register(modBus);
        BuiltInContent.register();
        if (ModList.get().isLoaded("curios")) {
            RpgMenuApi.get().equipmentProviders().register(
                    dev.rpgmenu.framework.common.compat.curios.CuriosEquipmentProvider.ID,
                    new dev.rpgmenu.framework.common.compat.curios.CuriosEquipmentProvider());
        }
        container.registerConfig(ModConfig.Type.CLIENT, FrameworkConfig.CLIENT_SPEC);
        modBus.addListener(NetworkHandler::register);
        modBus.addListener(this::registerGameTests);
        modBus.addListener(this::commonSetup);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            dev.rpgmenu.framework.client.ClientBootstrap.init(modBus);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        RpgMenuApi api = RpgMenuApi.get();
        NeoForge.EVENT_BUS.post(new RegisterRpgMenuTabsEvent(api.tabs()));
        NeoForge.EVENT_BUS.post(new RegisterInventorySourcesEvent(api.inventorySources()));
        NeoForge.EVENT_BUS.post(new RegisterItemCategoriesEvent(api.itemCategories()));
        NeoForge.EVENT_BUS.post(new RegisterStatProvidersEvent(api.statProviders()));
        NeoForge.EVENT_BUS.post(new RegisterEquipmentProvidersEvent(api.equipmentProviders()));
    }

    private void registerGameTests(RegisterGameTestsEvent event) {
        event.register(EquipmentGameTests.class);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
