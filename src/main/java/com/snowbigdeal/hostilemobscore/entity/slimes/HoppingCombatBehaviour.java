package com.snowbigdeal.hostilemobscore.entity.slimes;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.tslat.smartbrainlib.api.core.behaviour.ExtendedBehaviour;

import java.util.List;

/**
 * Drives hopping movement by updating {@link HoppingMoveControl} each tick.
 * All jump/speed logic lives in HoppingMoveControl so it executes after the
 * brain tick and can't be overwritten by the default pathfinding MoveControl.
 */
public class HoppingCombatBehaviour<T extends BaseSlime<T>> extends ExtendedBehaviour<T> {

    /** Maximum horizontal impulse applied at full follow range. */
    private static final float  MAX_LEAP_FORCE    = 1.4f;
    /** Distance (blocks) the entity aims to land at — centre of the strafe sweet spot. */
    private static final float  TARGET_DIST       = 4.0f;
    /** Distance (blocks) at which full leap force is used. Beyond this it's capped. */
    private static final float  MAX_DIST          = 14.0f;
    /** Start strafing (circling) when closer than this distance. */
    private static final float  STRAFE_START_DIST = 3.5f;
    /** Stop strafing when farther than this distance. */
    private static final float  STRAFE_STOP_DIST  = 4.5f;
    /** Deal contact damage when closer than this distance. */
    private static final float  MELEE_DIST        = 2.5f;
    private static final double WANDER_SPEED      = 0.6;
    private static final double STRAFE_SPEED      = 0.8;
    private static final double LEAP_SPEED        = 2.5;
    private static final int    WANDER_DIR_MIN    = 40;
    private static final int    WANDER_DIR_RANGE  = 60;

    private float wanderYRot = 0;
    private int directionTimer = 0;
    private boolean isStrafing = false;
    private boolean isInDamageRange = false;

    public HoppingCombatBehaviour() {
        noTimeout();
    }

    @Override
    protected boolean shouldKeepRunning(T slime) {
        return true;
    }

    @Override
    protected void start(T slime) {
        this.wanderYRot = slime.getYRot();
        this.directionTimer = 0;
        this.isStrafing = false;
        this.isInDamageRange = false;
    }

    @Override
    protected void tick(T slime) {
        if (!(slime.getMoveControl() instanceof HoppingMoveControl smc)) return;
        if (slime.isReturningHome()) return;

        if (slime.getTarget() != null) {
            tickCombat(slime, smc);
        } else {
            tickWander(slime, smc);
        }
    }

    private void tickCombat(T slime, HoppingMoveControl smc) {
        float yRot = angleToTarget(slime);
        double distSq = slime.distanceToSqr(slime.getTarget());

        updateStrafeState(distSq);
        checkMagicHit(slime, distSq);

        if (this.isStrafing) {
            applyStrafeMovement(smc, yRot);
        } else {
            applyLeapMovement(smc, yRot, distSq);
        }
    }

    private void tickWander(T slime, HoppingMoveControl smc) {
        updateWanderDirection(slime);
        smc.setDirection(this.wanderYRot, false);
        smc.setWantedMovement(WANDER_SPEED);
    }

    private float angleToTarget(T slime) {
        double dx = slime.getTarget().getX() - slime.getX();
        double dz = slime.getTarget().getZ() - slime.getZ();
        return (float) (Math.toDegrees(Math.atan2(dz, dx))) - 90.0F;
    }

    private void updateStrafeState(double distSq) {
        if (this.isStrafing) {
            this.isStrafing = distSq < STRAFE_STOP_DIST  * STRAFE_STOP_DIST;
        } else {
            this.isStrafing = distSq < STRAFE_START_DIST * STRAFE_START_DIST;
        }
    }

    private void checkMagicHit(T slime, double distSq) {
        boolean wasInDamageRange = this.isInDamageRange;
        this.isInDamageRange = distSq < MELEE_DIST * MELEE_DIST;

        if (!wasInDamageRange && this.isInDamageRange) {
            slime.getTarget().hurt(
                slime.damageSources().mobAttack(slime),
                (float) slime.getAttributeValue(Attributes.ATTACK_DAMAGE)
            );
        }
    }

    private void applyStrafeMovement(HoppingMoveControl smc, float yRot) {
        smc.setStrafe(yRot);
        smc.setWantedMovement(STRAFE_SPEED);
    }

    private void applyLeapMovement(HoppingMoveControl smc, float yRot, double distSq) {
        smc.setLeapForce(calculateLeapForce(distSq));
        smc.setDirection(yRot, true);
        smc.setWantedMovement(LEAP_SPEED);
    }

    private float calculateLeapForce(double distSq) {
        double dist = Math.sqrt(distSq);
        return (float) Math.min(MAX_LEAP_FORCE,
                Math.max(0, (dist - TARGET_DIST) / (MAX_DIST - TARGET_DIST) * MAX_LEAP_FORCE));
    }

    private void updateWanderDirection(T slime) {
        if (--this.directionTimer <= 0) {
            this.directionTimer = WANDER_DIR_MIN + slime.getRandom().nextInt(WANDER_DIR_RANGE);
            this.wanderYRot = slime.getRandom().nextFloat() * 360.0F;
        }
    }

    @Override
    protected void stop(T slime) {
        if (slime.getMoveControl() instanceof HoppingMoveControl smc) {
            smc.setWantedMovement(0.0);
        }
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return List.of();
    }
}
