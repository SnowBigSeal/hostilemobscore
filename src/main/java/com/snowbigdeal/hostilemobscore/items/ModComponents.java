package com.snowbigdeal.hostilemobscore.items;

import com.mojang.serialization.Codec;
import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModComponents {

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, HostileMobsCore.MODID);

    /** Packed RGB color (0xRRGGBB) tinting the slimeball texture. Defaults to white (0xFFFFFF). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SLIMEBALL_COLOR =
            COMPONENTS.register("slimeball_color", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    public static void register(IEventBus eventBus) {
        COMPONENTS.register(eventBus);
    }
}
