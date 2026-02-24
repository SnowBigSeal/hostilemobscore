package com.snowbigdeal.hostilemobscore.entity;

import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime.AngrySlime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, HostileMobsCore.MODID);

    public static final Supplier<EntityType<AngrySlime>> ANGRY_SLIME =
            ENTITY_TYPES.register("angry_slime", () -> EntityType.Builder.of(AngrySlime::new, MobCategory.MONSTER)
                    .sized(1.0f, 1.0f).build("angry_slime"));

    public static void register(IEventBus eventBus){
        ENTITY_TYPES.register(eventBus);
    }
}