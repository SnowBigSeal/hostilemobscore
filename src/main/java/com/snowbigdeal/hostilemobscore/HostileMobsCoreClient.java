package com.snowbigdeal.hostilemobscore;

import com.snowbigdeal.hostilemobscore.debug.SlimeDebugRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = HostileMobsCore.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = HostileMobsCore.MODID, value = Dist.CLIENT)
public class HostileMobsCoreClient {
    public HostileMobsCoreClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        HostileMobsCore.LOGGER.info("HELLO FROM CLIENT SETUP");
        HostileMobsCore.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    @SubscribeEvent
    static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("hmcdebug")
                .then(Commands.literal("tether").executes(ctx -> { SlimeDebugRenderer.toggle("tether"); return 1; }))
                .then(Commands.literal("party").executes(ctx -> { SlimeDebugRenderer.toggle("party"); return 1; }))
                .then(Commands.literal("off").executes(ctx -> { SlimeDebugRenderer.ACTIVE_MODES.clear(); return 1; }))
        );
    }

    @SubscribeEvent
    static void onRenderLevel(RenderLevelStageEvent event) {
        SlimeDebugRenderer.onRenderLevel(event);
    }
}
