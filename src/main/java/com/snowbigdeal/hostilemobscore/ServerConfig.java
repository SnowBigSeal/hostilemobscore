package com.snowbigdeal.hostilemobscore;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Server config — values that only the server needs; not visible to clients.
 * Loaded from: saves/<world>/serverconfig/hostilemobscore-server.toml
 */
@EventBusSubscriber(modid = HostileMobsCore.MODID)
public class ServerConfig {

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // -------------------------------------------------------------------------
    // Vanilla mob spawning
    // -------------------------------------------------------------------------

    /** When false, all vanilla hostile mobs are prevented from spawning. */
    public static ModConfigSpec.BooleanValue VANILLA_HOSTILE_MOBS_ENABLED;

    /**
     * Resource locations of vanilla hostile mobs that should not spawn even when
     * vanilla mobs are otherwise enabled. Example: {@code minecraft:creeper}
     */
    public static ModConfigSpec.ConfigValue<List<? extends String>> VANILLA_HOSTILE_MOB_BLACKLIST;

    public static ModConfigSpec.IntValue MAX_PARTY_SIZE;

    static {
        BUILDER.comment("Hostile Mobs Core — Server Config");

        BUILDER.comment("Vanilla mob spawning controls").push("vanilla_mobs");
        VANILLA_HOSTILE_MOBS_ENABLED = BUILDER
                .comment("Allow vanilla hostile mobs to spawn. Set to false to disable all vanilla hostiles.")
                .define("enableVanillaHostileMobs", true);

        VANILLA_HOSTILE_MOB_BLACKLIST = BUILDER
                .comment(
                    "List of vanilla hostile mob resource locations to block from spawning.",
                    "Only has effect when enableVanillaHostileMobs is true.",
                    "Example: [\"minecraft:creeper\", \"minecraft:skeleton\"]"
                )
                .defineListAllowEmpty("vanillaHostileMobBlacklist", List.of(), ServerConfig::isValidResourceLocation);

        BUILDER.pop();

        BUILDER.comment("Attack orchestrator controls").push("orchestrator");

        MAX_PARTY_SIZE = BUILDER
                .comment(
                    "Maximum number of mobs that can belong to a single party.",
                    "New mobs that spawn when all nearby parties are full will form their own party.",
                    "Range: 1-16"
                )
                .defineInRange("maxPartySize", 6, 1, 16);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    @SubscribeEvent
    static void onLoad(ModConfigEvent.Loading event) {
        if (!event.getConfig().getSpec().equals(SPEC)) return;
        HostileMobsCore.LOGGER.debug("Loaded hostilemobscore server config");
    }

    @SubscribeEvent
    static void onReload(ModConfigEvent.Reloading event) {
        if (!event.getConfig().getSpec().equals(SPEC)) return;
        HostileMobsCore.LOGGER.debug("Reloaded hostilemobscore server config");
    }

    private static boolean isValidResourceLocation(Object obj) {
        return obj instanceof String s && s.contains(":");
    }
}
