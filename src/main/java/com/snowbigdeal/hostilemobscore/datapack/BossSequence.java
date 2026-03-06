package com.snowbigdeal.hostilemobscore.datapack;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * An ordered sequence of telegraph attacks that choreograph a boss fight.
 *
 * <p>Loaded from {@code data/<namespace>/boss_sequences/<name>.json}.
 *
 * <p>JSON example:
 * <pre>{@code
 * {
 *   "loop": false,
 *   "sequence": [
 *     { "attack": "hostilemobscore:angry_slime.slam", "delay_ticks": 0 },
 *     { "attack": "hostilemobscore:angry_slime.slam", "delay_ticks": 60 }
 *   ]
 * }
 * }</pre>
 *
 * @param id       Datapack-derived resource location (namespace:path of the JSON file).
 * @param steps    Ordered list of attack steps.
 * @param loop     Whether to restart from step 0 after the last step completes.
 */
public record BossSequence(ResourceLocation id, List<BossAttackStep> steps, boolean loop) {}
