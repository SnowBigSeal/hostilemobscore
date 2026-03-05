package com.snowbigdeal.hostilemobscore;

import com.snowbigdeal.hostilemobscore.client.SlamVfxManager;
import com.snowbigdeal.hostilemobscore.entity.ModEntities;
import com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime.AngrySlimeRenderer;
import com.snowbigdeal.hostilemobscore.items.ModItems;
import com.snowbigdeal.hostilemobscore.network.CircleAoePacket;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(HostileMobsCore.MODID)
public class HostileMobsCore {
    public static final String MODID = "hostilemobscore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HostileMobsCore(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(HostileMobsCore::registerPayloads);

        NeoForge.EVENT_BUS.register(this);

        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);

        modEventBus.addListener(this::addCreative);
        modContainer.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(CircleAoePacket.TYPE, CircleAoePacket.STREAM_CODEC,
                (packet, ctx) -> ctx.enqueueWork(() ->
                        SlamVfxManager.spawn(packet.center(), packet.radius(), packet.lifetime())));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.ANGRY_SLIME_SPAWN_EGG.get());
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLCommonSetupEvent event) {
            EntityRenderers.register(ModEntities.ANGRY_SLIME.get(), AngrySlimeRenderer::new);
        }
    }
}
