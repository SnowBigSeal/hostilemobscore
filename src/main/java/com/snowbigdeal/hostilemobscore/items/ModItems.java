package com.snowbigdeal.hostilemobscore.items;

import com.snowbigdeal.hostilemobscore.entity.ModEntities;
import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, HostileMobsCore.MODID);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    public static final DeferredHolder<Item, DeferredSpawnEggItem> ANGRY_SLIME_SPAWN_EGG = ITEMS.register("angry_slime_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ANGRY_SLIME, 0xB02E26, 0x7A1C1C, new Item.Properties()));

    public static final DeferredHolder<Item, DeferredSpawnEggItem> SLEEPY_SLIME_SPAWN_EGG = ITEMS.register("sleepy_slime_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.SLEEPY_SLIME, 0x87CEEB, 0x6A5ACD, new Item.Properties()));

    public static final Map<DyeColor, DeferredHolder<Item, ColoredSlimeballItem>> SLIMEBALLS = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor dye : DyeColor.values()) {
            DyeColor captured = dye;
            SLIMEBALLS.put(dye, ITEMS.register(dye.getName() + "_slimeball",
                    () -> new ColoredSlimeballItem(captured, new Item.Properties())));
        }
    }

    /** Convenience: get the slimeball item for a specific DyeColor. */
    public static ColoredSlimeballItem getSlimeball(DyeColor dye) {
        return SLIMEBALLS.get(dye).get();
    }
}
