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
 * Drives a mob back to its tether anchor when the {@link ModMemoryTypes#RETURNING_HOME}
 * memory is set.  Runs as part of {@link net.minecraft.world.entity.schedule.Activity#NEUTRAL},
 * which takes priority over both FIGHT and IDLE so normal AI is fully suppressed.
 *
 * <p>On arrival the memory is cleared (resuming normal activity), the mob is healed
 * to full health, and invulnerability is removed.  If the mob cannot reach home within
 * 30 seconds it is discarded.
 *
 * @param <T> The concrete HostileMob subclass.
 */
public class ReturnHomeBehaviour<T extends HostileMob<T>> extends ExtendedBehaviour<T> {

    /** Distance² (blocks²) at which the mob is considered home (5 blocks). */
    private static final double HOME_DIST_SQ   = 25.0;
    /** Ticks before a stuck returning mob is discarded (30 seconds). */
    private static final int    RETURN_TIMEOUT = 600;

    private int timeoutTicks = 0;

    public ReturnHomeBehaviour() {
        noTimeout();
    }

    @Override
    protected void start(T entity) {
        BrainUtils.clearMemory(entity, MemoryModuleType.ATTACK_TARGET);
        entity.setTarget(null);
        this.timeoutTicks = 0;
    }

    @Override
    protected void tick(T entity) {
        double distSq = entity.distanceToSqr(Vec3.atCenterOf(entity.getRestrictCenter()));
        if (distSq <= HOME_DIST_SQ) {
            arriveHome(entity);
        } else if (++timeoutTicks >= RETURN_TIMEOUT) {
            BrainUtils.clearMemory(entity, ModMemoryTypes.RETURNING_HOME.get());
            entity.discard();
        } else {
            entity.applyReturnMovement();
        }
    }

    private void arriveHome(T entity) {
        BrainUtils.clearMemory(entity, ModMemoryTypes.RETURNING_HOME.get());
        BrainUtils.clearMemory(entity, MemoryModuleType.HURT_BY_ENTITY);
        BrainUtils.setForgettableMemory(entity, ModMemoryTypes.DEAGGRO_COOLDOWN.get(), true, 200);
        entity.setInvulnerable(false);
        entity.setHealth(entity.getMaxHealth());
    }

    @Override
    protected boolean shouldKeepRunning(T entity) {
        return BrainUtils.hasMemory(entity, ModMemoryTypes.RETURNING_HOME.get());
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return List.of(Pair.of(ModMemoryTypes.RETURNING_HOME.get(), MemoryStatus.VALUE_PRESENT));
    }
}
