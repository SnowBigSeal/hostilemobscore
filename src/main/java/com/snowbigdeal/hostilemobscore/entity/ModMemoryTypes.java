package com.snowbigdeal.hostilemobscore.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

import static com.snowbigdeal.hostilemobscore.HostileMobsCore.MODID;

public final class ModMemoryTypes {

    private static final DeferredRegister<MemoryModuleType<?>> MEMORY_TYPES =
            DeferredRegister.create(BuiltInRegistries.MEMORY_MODULE_TYPE, MODID);

    private static final DeferredRegister<Activity> ACTIVITIES =
            DeferredRegister.create(BuiltInRegistries.ACTIVITY, MODID);

    /** Set to {@code true} when a mob is walking back to its tether anchor. */
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> RETURNING_HOME =
            MEMORY_TYPES.register("returning_home", () -> new MemoryModuleType<>(Optional.empty()));

    /**
     * Ticks remaining before a mob that hasn't been hit by a player disengages.
     * Written to on player hit; decremented each tick by {@link com.snowbigdeal.hostilemobscore.entity.behaviour.DeaggroBehaviour}.
     */
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Integer>> HIT_TIMER =
            MEMORY_TYPES.register("hit_timer", () -> new MemoryModuleType<>(Optional.empty()));

    /**
     * Short-lived flag set after a mob disengages or returns home.
     * While present, {@link com.snowbigdeal.hostilemobscore.entity.slimes.TetheredTargetBehaviour}
     * will not acquire a new target, preventing immediate re-aggro.
     * Stored as a forgettable memory; expires automatically after the cooldown period.
     */
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> DEAGGRO_COOLDOWN =
            MEMORY_TYPES.register("deaggro_cooldown", () -> new MemoryModuleType<>(Optional.empty()));

    /**
     * Ticks remaining on the slam attack cooldown, decremented each combat tick by
     * {@link com.snowbigdeal.hostilemobscore.entity.behaviour.CooldownTickBehaviour}.
     * Absent means the slam is ready. Required-absent by
     * {@link com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime.SlimeSlamAttackBehaviour}.
     */
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Integer>> SLAM_COOLDOWN =
            MEMORY_TYPES.register("slam_cooldown", () -> new MemoryModuleType<>(Optional.empty()));

    /**
     * Set by {@link com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime.SlamMobAction#beginAction}
     * to grant the slam attack. Cleared by
     * {@link com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime.SlimeSlamAttackBehaviour#onStop}.
     * Required-present by the behaviour; absence signals the orchestrator that the action is complete.
     */
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> SLAM_PENDING =
            MEMORY_TYPES.register("slam_pending", () -> new MemoryModuleType<>(Optional.empty()));

    /**
     * Mirrors {@link #SLAM_COOLDOWN} for the cone attack.
     */
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Integer>> CONE_COOLDOWN =
            MEMORY_TYPES.register("cone_cooldown", () -> new MemoryModuleType<>(Optional.empty()));

    /**
     * Mirrors {@link #SLAM_PENDING} for the cone attack.
     */
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> CONE_PENDING =
            MEMORY_TYPES.register("cone_pending", () -> new MemoryModuleType<>(Optional.empty()));

    /** Activity that preempts FIGHT and IDLE while the mob walks home. */
    public static final DeferredHolder<Activity, Activity> ACTIVITY_RETURNING_HOME =
            ACTIVITIES.register("returning_home", () -> new Activity("returning_home"));

    public static void register(IEventBus eventBus) {
        MEMORY_TYPES.register(eventBus);
        ACTIVITIES.register(eventBus);
    }

    private ModMemoryTypes() {}
}
