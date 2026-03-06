package com.snowbigdeal.hostilemobscore.datapack;

import net.minecraft.resources.ResourceLocation;

/**
 * A single step in a {@link BossSequence}: perform {@code attackId} after
 * waiting {@code delayTicks} ticks from the previous step.
 *
 * <p>JSON example:
 * <pre>{@code { "attack": "hostilemobscore:angry_slime.slam", "delay_ticks": 0 } }</pre>
 */
public record BossAttackStep(ResourceLocation attackId, int delayTicks) {}
