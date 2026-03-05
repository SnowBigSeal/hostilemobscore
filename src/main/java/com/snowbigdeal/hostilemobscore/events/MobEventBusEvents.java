package com.snowbigdeal.hostilemobscore.events;

import com.snowbigdeal.hostilemobscore.ServerConfig;
import com.snowbigdeal.hostilemobscore.entity.ModEntities;
import com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime.AngrySlime;
import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

@EventBusSubscriber(modid = HostileMobsCore.MODID)
public class MobEventBusEvents {

    @SubscribeEvent
    public static void entityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ANGRY_SLIME.get(), AngrySlime.createAttributes().build());
    }

    @SubscribeEvent
    public static void onMobSpawnCheck(MobSpawnEvent.PositionCheck event) {
        EntityType<?> type = event.getEntity().getType();

        // Only act on vanilla hostile mobs
        if (type.getCategory() != MobCategory.MONSTER) return;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (!id.getNamespace().equals("minecraft")) return;

        if (!ServerConfig.VANILLA_HOSTILE_MOBS_ENABLED.get()) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
            return;
        }

        boolean blacklisted = ServerConfig.VANILLA_HOSTILE_MOB_BLACKLIST.get()
                .stream().anyMatch(entry -> entry.equals(id.toString()));
        if (blacklisted) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }
}
