package com.snowbigdeal.hostilemobscore;

import com.snowbigdeal.hostilemobscore.block.ModBlocks;
import com.snowbigdeal.hostilemobscore.debug.HostileMobDebugRenderer;
import com.snowbigdeal.hostilemobscore.client.TelegraphAttackVfxManager;
import com.snowbigdeal.hostilemobscore.items.ModItems;
import com.snowbigdeal.hostilemobscore.items.SlimeBallColorUtil;
import net.minecraft.commands.Commands;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
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
    }

    @SubscribeEvent
    static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        for (DyeColor dye : DyeColor.values()) {
            int rgb = SlimeBallColorUtil.dyeColorToRgb(dye) | 0xFF000000;
            event.register(
                    (stack, tintIndex) -> tintIndex == 0 ? rgb : 0xFFFFFFFF,
                    ModItems.getSlimeball(dye));
            event.register(
                    (stack, tintIndex) -> tintIndex == 0 ? rgb : 0xFFFFFFFF,
                    ModBlocks.getSlimeBlockItem(dye));
        }
    }

    @SubscribeEvent
    static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        for (DyeColor dye : DyeColor.values()) {
            int rgb = SlimeBallColorUtil.dyeColorToRgb(dye) | 0xFF000000;
            event.register(
                    (state, level, pos, tintIndex) -> tintIndex == 0 ? rgb : 0xFFFFFFFF,
                    ModBlocks.getSlimeBlock(dye));
        }
    }

    @SubscribeEvent
    static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("hmcdebug")
                .then(Commands.literal("tether").executes(ctx -> { HostileMobDebugRenderer.toggle("tether"); return 1; }))
                .then(Commands.literal("party").executes(ctx -> { HostileMobDebugRenderer.toggle("party"); return 1; }))
                .then(Commands.literal("slam-circle").executes(ctx -> {
                    TelegraphAttackVfxManager.debugKeepLastSlamCircle = !TelegraphAttackVfxManager.debugKeepLastSlamCircle;
                    ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                        "Slam circle debug: " + (TelegraphAttackVfxManager.debugKeepLastSlamCircle ? "ON" : "OFF")), false);
                    return 1;
                }))
                .then(Commands.literal("off").executes(ctx -> {
                    HostileMobDebugRenderer.ACTIVE_MODES.clear();
                    TelegraphAttackVfxManager.debugKeepLastSlamCircle = false;
                    return 1;
                }))
        );
    }

    @SubscribeEvent
    static void onRenderLevel(RenderLevelStageEvent event) {
        HostileMobDebugRenderer.onRenderLevel(event);
    }
}
