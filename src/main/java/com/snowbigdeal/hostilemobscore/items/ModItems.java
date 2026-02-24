package com.snowbigdeal.hostilemobscore.items;

import com.snowbigdeal.hostilemobscore.entity.ModEntities;
import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, HostileMobsCore.MODID);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    public static final DeferredHolder<Item, DeferredSpawnEggItem> ANGRY_SLIME_SPAWN_EGG = ITEMS.register("angry_slime_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ANGRY_SLIME, 0xFF0000, 0x00FF00, new Item.Properties()));

}
