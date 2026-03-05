package com.snowbigdeal.hostilemobscore;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client config — values that only the client needs; never sent to a server.
 * Loaded from: config/hostilemobscore-client.toml
 */
@EventBusSubscriber(modid = HostileMobsCore.MODID, value = Dist.CLIENT)
public class ClientConfig {

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Hostile Mobs Core — Client Config").push("general");
        // Add client config values here
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    @SubscribeEvent
    static void onLoad(ModConfigEvent.Loading event) {
        if (!event.getConfig().getSpec().equals(SPEC)) return;
        HostileMobsCore.LOGGER.debug("Loaded hostilemobscore client config");
    }

    @SubscribeEvent
    static void onReload(ModConfigEvent.Reloading event) {
        if (!event.getConfig().getSpec().equals(SPEC)) return;
        HostileMobsCore.LOGGER.debug("Reloaded hostilemobscore client config");
    }
}
