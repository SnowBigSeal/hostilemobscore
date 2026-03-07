package com.snowbigdeal.hostilemobscore;

import com.snowbigdeal.hostilemobscore.choreography.AttackRegistry;
import com.snowbigdeal.hostilemobscore.client.TelegraphAttackVfxManager;
import com.snowbigdeal.hostilemobscore.datapack.BossSequenceLoader;
import com.snowbigdeal.hostilemobscore.entity.ModEntities;
import com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime.AngrySlimeRenderer;
import com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime.SlimeSlamAttackBehaviour;
import com.snowbigdeal.hostilemobscore.items.ModItems;
import com.snowbigdeal.hostilemobscore.network.TelegraphAttackPacket;
import com.snowbigdeal.hostilemobscore.sounds.ModSounds;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.neoforge.event.AddReloadListenerEvent;
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
        NeoForge.EVENT_BUS.addListener(HostileMobsCore::onAddReloadListeners);

        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModSounds.register(modEventBus);

        modEventBus.addListener(this::addCreative);
        modContainer.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Register attack IDs for the choreography system
        AttackRegistry.register(
                ResourceLocation.fromNamespaceAndPath(MODID, "angry_slime.slam"),
                SlimeSlamAttackBehaviour::new);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TelegraphAttackPacket.TYPE, TelegraphAttackPacket.STREAM_CODEC,
                (packet, ctx) -> ctx.enqueueWork(() ->
                        TelegraphAttackVfxManager.spawn(packet.shape(), packet.lifetimeTicks())));
    }

    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new BossSequenceLoader());
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
