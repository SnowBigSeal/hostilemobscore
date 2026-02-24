package com.snowbigdeal.hostilemobscore.events;

import com.snowbigdeal.hostilemobscore.entity.ModEntities;
import com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime.AngrySlime;
import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = HostileMobsCore.MODID)
public class MobEventBusEvents {

    @SubscribeEvent
    public static void entityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ANGRY_SLIME.get(), AngrySlime.createAttributes().build());
    }
}
