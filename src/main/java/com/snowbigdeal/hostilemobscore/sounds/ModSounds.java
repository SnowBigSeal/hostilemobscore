package com.snowbigdeal.hostilemobscore.sounds;

import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {

    private static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, HostileMobsCore.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> ANGRY_SLIME_JUMP =
            register("angry_slime.jump");

    public static final DeferredHolder<SoundEvent, SoundEvent> ANGRY_SLIME_LAND =
            register("angry_slime.land");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () ->
                SoundEvent.createVariableRangeEvent(
                        ResourceLocation.fromNamespaceAndPath(HostileMobsCore.MODID, name)));
    }

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
