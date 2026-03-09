package com.snowbigdeal.hostilemobscore.events;

import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import com.snowbigdeal.hostilemobscore.entity.ModEntities;
import com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime.AngrySlime;
import com.snowbigdeal.hostilemobscore.entity.slimes.client.sleepyslime.SleepySlime;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import java.util.List;

@EventBusSubscriber(modid = HostileMobsCore.MODID)
public class ModSpawnEvents {

    private static final int PARTY_CAP    = 6;
    private static final double PARTY_RADIUS = 48;

    private static int countNearbyModHostiles(ServerLevelAccessor level, Vec3 center) {
        AABB box = AABB.ofSize(center, PARTY_RADIUS * 2, PARTY_RADIUS * 2, PARTY_RADIUS * 2);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, box,
            e -> e instanceof AngrySlime || e instanceof SleepySlime);
        return nearby.size();
    }

    @SubscribeEvent
    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
            ModEntities.ANGRY_SLIME.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (type, level, spawnType, pos, random) -> {
                if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
                return countNearbyModHostiles(level, pos.getCenter()) < PARTY_CAP;
            },
            RegisterSpawnPlacementsEvent.Operation.OR
        );

        event.register(
            ModEntities.SLEEPY_SLIME.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (type, level, spawnType, pos, random) -> {
                if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
                return countNearbyModHostiles(level, pos.getCenter()) < PARTY_CAP;
            },
            RegisterSpawnPlacementsEvent.Operation.OR
        );
    }
}
