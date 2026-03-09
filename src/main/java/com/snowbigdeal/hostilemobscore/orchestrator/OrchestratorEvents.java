package com.snowbigdeal.hostilemobscore.orchestrator;

import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = HostileMobsCore.MODID)
public class OrchestratorEvents {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            AttackOrchestrator.get(serverLevel).tick(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Mob mob && event.getLevel() instanceof ServerLevel) {
            AttackOrchestrator.get(event.getLevel()).enrollOnSpawn(mob);
        }
    }

    @SubscribeEvent
    public static void onMobTargetChanged(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && mob.level() instanceof ServerLevel) {
            AttackOrchestrator.get(mob.level()).onMobTargetChanged(mob, event.getNewAboutToBeSetTarget());
        }
    }

    @SubscribeEvent
    public static void onMobDied(LivingDeathEvent event) {
        if (event.getEntity() instanceof Mob mob && mob.level() instanceof ServerLevel) {
            AttackOrchestrator.get(mob.level()).onMobDied(mob);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            AttackOrchestrator.invalidate(serverLevel.dimension());
        }
    }
}
