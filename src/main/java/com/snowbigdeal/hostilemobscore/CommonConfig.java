package com.snowbigdeal.hostilemobscore;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config — values synced and available on both client and server.
 * Loaded from: config/hostilemobscore-common.toml
 */
@EventBusSubscriber(modid = HostileMobsCore.MODID)
public class CommonConfig {

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Hostile Mobs Core — Common Config").push("general");
        // Add common config values here
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    @SubscribeEvent
    static void onLoad(ModConfigEvent.Loading event) {
        if (!event.getConfig().getSpec().equals(SPEC)) return;
        HostileMobsCore.LOGGER.debug("Loaded hostilemobscore common config");
    }

    @SubscribeEvent
    static void onReload(ModConfigEvent.Reloading event) {
        if (!event.getConfig().getSpec().equals(SPEC)) return;
        HostileMobsCore.LOGGER.debug("Reloaded hostilemobscore common config");
    }
}
