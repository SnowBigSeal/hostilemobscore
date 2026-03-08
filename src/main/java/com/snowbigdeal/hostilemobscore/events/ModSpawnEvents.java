package com.snowbigdeal.hostilemobscore.events;

import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import com.snowbigdeal.hostilemobscore.entity.ModEntities;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid = HostileMobsCore.MODID)
public class ModSpawnEvents {

    @SubscribeEvent
    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
            ModEntities.ANGRY_SLIME.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (type, level, spawnType, pos, random) -> {
                if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
                AABB box = AABB.unitCubeFromLowerCorner(pos.getCenter()).inflate(48);
                int nearbyCount = level.getEntitiesOfClass(
                    ModEntities.ANGRY_SLIME.get().create(level.getLevel()).getClass(),
                    box, e -> true
                ).size();
                return nearbyCount < 4;
            },
            RegisterSpawnPlacementsEvent.Operation.OR
        );
    }
}
