package com.snowbigdeal.hostilemobscore.choreography;

import com.snowbigdeal.hostilemobscore.entity.behaviour.TelegraphAttackBehaviour;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry mapping a {@link ResourceLocation} attack ID to a factory that creates
 * a fresh {@link TelegraphAttackBehaviour} instance.
 *
 * <p>Mobs register their attacks during mod init:
 * <pre>{@code
 * AttackRegistry.register(
 *     ResourceLocation.fromNamespaceAndPath(MODID, "angry_slime.slam"),
 *     SlimeSlamAttackBehaviour::new
 * );
 * }</pre>
 *
 * <p>The choreography system uses this registry to instantiate attacks referenced
 * by ID inside a boss sequence datapack file.
 */
public final class AttackRegistry {

    private AttackRegistry() {}

    @SuppressWarnings("rawtypes")
    private static final Map<ResourceLocation, Supplier<TelegraphAttackBehaviour>> REGISTRY =
            new HashMap<>();

    /** Returns an unmodifiable view of all registered attacks. */
    @SuppressWarnings("rawtypes")
    public static Map<ResourceLocation, Supplier<TelegraphAttackBehaviour>> all() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    @SuppressWarnings("rawtypes")
    public static void register(ResourceLocation id,
                                Supplier<TelegraphAttackBehaviour> factory) {
        if (REGISTRY.containsKey(id)) {
            throw new IllegalStateException("Duplicate telegraph attack ID: " + id);
        }
        REGISTRY.put(id, factory);
    }

    /** Returns a new instance for the given ID, or {@code null} if not found. */
    @SuppressWarnings("rawtypes")
    public static TelegraphAttackBehaviour create(ResourceLocation id) {
        Supplier<TelegraphAttackBehaviour> factory = REGISTRY.get(id);
        return factory != null ? factory.get() : null;
    }
}
