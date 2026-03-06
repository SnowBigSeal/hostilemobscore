package com.snowbigdeal.hostilemobscore.entity.behaviour;

import com.mojang.datafixers.util.Pair;
import com.snowbigdeal.hostilemobscore.entity.HostileMob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.tslat.smartbrainlib.api.core.behaviour.ExtendedBehaviour;
import net.tslat.smartbrainlib.util.BrainUtils;

import java.util.List;

/**
 * Generic return-home behaviour for all {@link HostileMob} entities.
 * <p>
 * Activates when the mob strays outside its tether radius. While active:
 * <ul>
 *   <li>The mob becomes invulnerable.</li>
 *   <li>All target acquisition is blocked (target is cleared every tick).</li>
 *   <li>Movement toward home is delegated to {@link #applyReturnMovement}.</li>
 * </ul>
 * Ends when the mob reaches within {@link #HOME_DIST_SQ} blocks of its anchor,
 * at which point it heals to full health and loses invulnerability.
 *
 * @param <T> The concrete {@link HostileMob} subclass.
 */
public abstract class ReturnHomeBehaviour<T extends HostileMob<T>> extends ExtendedBehaviour<T> {

    /** Distance² from anchor at which the mob is considered home (5 blocks). */
    protected static final double HOME_DIST_SQ = 25.0;

    protected ReturnHomeBehaviour() {
        noTimeout();
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, T entity) {
        return !entity.isWithinRestriction();
    }

    @Override
    protected boolean shouldKeepRunning(T entity) {
        return entity.blockPosition().distSqr(entity.getRestrictCenter()) > HOME_DIST_SQ;
    }

    @Override
    protected void start(T entity) {
        entity.setReturningHome(true);
        entity.setInvulnerable(true);
        entity.setTarget(null);
        BrainUtils.clearMemory(entity, MemoryModuleType.ATTACK_TARGET);
    }

    @Override
    protected void tick(T entity) {
        if (entity.getTarget() != null) {
            entity.setTarget(null);
            BrainUtils.clearMemory(entity, MemoryModuleType.ATTACK_TARGET);
        }
        applyReturnMovement(entity);
    }

    @Override
    protected void stop(T entity) {
        entity.setReturningHome(false);
        entity.setHealth(entity.getMaxHealth());
        entity.setInvulnerable(false);
    }

    /**
     * Apply movement toward the tether anchor each tick.
     * Walking mobs can use {@code entity.getNavigation().moveTo(...)}; hopping
     * mobs should drive their custom move control here.
     */
    protected abstract void applyReturnMovement(T entity);

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return List.of();
    }
}
