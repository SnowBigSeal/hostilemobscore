package com.snowbigdeal.hostilemobscore.entity.behaviour;

import com.mojang.datafixers.util.Pair;
import com.snowbigdeal.hostilemobscore.entity.HostileMob;
import com.snowbigdeal.hostilemobscore.entity.ModMemoryTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.api.core.behaviour.ExtendedBehaviour;
import net.tslat.smartbrainlib.util.BrainUtils;

import java.util.List;

/**
 * Decrements the {@link ModMemoryTypes#HIT_TIMER} brain memory each tick.
 * When the timer reaches zero the mob disengages: its attack target is dropped
 * and, if it has wandered further than {@link #disengageDistSq} from its tether
 * anchor, it begins returning home.
 *
 * <p>The timer is written by {@link HostileMob#hurt} whenever a non-creative
 * player deals damage.  This behaviour only runs while that memory is present,
 * so it is idle until the mob has been in combat.
 *
 * <p>Configurable options (builder pattern per SmartBrainLib convention):
 * <ul>
 *   <li>{@link #hitTimeout(int)} — ticks before disengagement (default 600 = 30 s)</li>
 *   <li>{@link #disengageDistance(double)} — blocks from anchor that trigger a
 *       return-home walk instead of simply dropping the target (default 16 blocks)</li>
 * </ul>
 *
 * @param <T> The concrete {@link HostileMob} subclass.
 */
public class DeaggroBehaviour<T extends HostileMob<T>> extends ExtendedBehaviour<T> {

    private static final List<Pair<MemoryModuleType<?>, MemoryStatus>> MEMORY_REQUIREMENTS =
            List.of(Pair.of(ModMemoryTypes.HIT_TIMER.get(), MemoryStatus.VALUE_PRESENT));

    private int hitTimerMax    = 600;   // 30 seconds
    private double disengageDistSq = 256.0; // 16 blocks

    public DeaggroBehaviour() {
        noTimeout();
    }

    /** Sets the number of ticks without a player hit before the mob disengages. */
    public DeaggroBehaviour<T> hitTimeout(int ticks) {
        this.hitTimerMax = ticks;
        return this;
    }

    /**
     * Sets the distance from the tether anchor (in blocks) beyond which the mob
     * will walk home on disengagement rather than simply dropping its target.
     */
    public DeaggroBehaviour<T> disengageDistance(double blocks) {
        this.disengageDistSq = blocks * blocks;
        return this;
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return MEMORY_REQUIREMENTS;
    }

    @Override
    protected boolean shouldKeepRunning(T entity) {
        return BrainUtils.hasMemory(entity, ModMemoryTypes.HIT_TIMER.get());
    }

    @Override
    protected void tick(T entity) {
        if (entity.getTarget() == null) {
            BrainUtils.clearMemory(entity, ModMemoryTypes.HIT_TIMER.get());
            return;
        }

        Integer remaining = BrainUtils.getMemory(entity, ModMemoryTypes.HIT_TIMER.get());
        if (remaining == null || remaining <= 0) {
            disengage(entity);
            return;
        }

        BrainUtils.setMemory(entity, ModMemoryTypes.HIT_TIMER.get(), remaining - 1);
    }

    private void disengage(T entity) {
        BrainUtils.clearMemory(entity, MemoryModuleType.ATTACK_TARGET);
        BrainUtils.clearMemory(entity, MemoryModuleType.HURT_BY_ENTITY);
        BrainUtils.clearMemory(entity, ModMemoryTypes.HIT_TIMER.get());
        BrainUtils.setForgettableMemory(entity, ModMemoryTypes.DEAGGRO_COOLDOWN.get(), true, 200);
        entity.setTarget(null);

        if (entity.distanceToSqr(Vec3.atCenterOf(entity.getRestrictCenter())) > disengageDistSq) {
            entity.setReturningHome(true);
            entity.setInvulnerable(true);
        }
    }
}
