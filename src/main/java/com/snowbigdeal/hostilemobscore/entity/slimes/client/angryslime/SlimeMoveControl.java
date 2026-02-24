package com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

/**
 * MoveControl that drives slime hopping movement.
 * Logic lives here (not in the brain behaviour) so it runs last each tick
 * and isn't overwritten by the default pathfinding MoveControl.
 *
 * Brain behaviour (SlimeMoveBehaviour) simply calls setDirection() and
 * setWantedMovement() each tick, mirroring how vanilla goals drive vanilla SlimeMoveControl.
 */
public class SlimeMoveControl extends MoveControl {

    private float yRot;
    private int jumpDelay;
    private final AngrySlime slime;
    private boolean isAggressive;

    // Strafe state
    private boolean isStrafeMode;
    private float strafeSign  = 1;  // direction for the NEXT jump (-1 = left, 1 = right)
    private float flightSign  = 1;  // direction locked in for the CURRENT flight

    // Leap force set each tick by SlimeMoveBehaviour based on distance
    private float leapForce = 0.5f;

    private static final double STRAFE_LATERAL_BOOST = 0.45;
    private static final float  ROTATION_SPEED       = 90.0f;

    // When true, slam behaviour controls physics directly — hop logic is suppressed
    private boolean slamLock = false;

    public SlimeMoveControl(AngrySlime slime) {
        super(slime);
        this.slime = slime;
        this.yRot = slime.getYRot();
    }

    /** Normal forward-hop toward a direction. Clears strafe mode. */
    public void setDirection(float yRot, boolean aggressive) {
        this.yRot = yRot;
        this.isAggressive = aggressive;
        this.isStrafeMode = false;
    }

    /** Evasive strafe mode: face facingYRot but hop left/right, alternating each jump. */
    public void setStrafe(float facingYRot) {
        this.yRot = facingYRot;
        this.isAggressive = false;
        this.isStrafeMode = true;
    }

    public boolean isAggressive() {
        return this.isAggressive;
    }

    public void setLeapForce(float force) {
        this.leapForce = force;
    }

    public float getLeapForce() {
        return this.leapForce;
    }

    public void setSlamLock(boolean locked) {
        this.slamLock = locked;
    }

    public void setWantedMovement(double speed) {
        this.speedModifier = speed;
        this.operation = Operation.MOVE_TO;
    }

    @Override
    public void tick() {
        faceTargetDirection();

        if (this.slamLock) return; // slam behaviour owns physics during this window

        if (this.operation != Operation.MOVE_TO) {
            clearMovementInput();
            return;
        }

        this.operation = Operation.WAIT;
        float speed = currentSpeed();

        if (this.mob.onGround()) {
            tickOnGround(speed);
        } else {
            tickAirborne(speed);
        }
    }

    private void faceTargetDirection() {
        this.mob.setYRot(this.rotlerp(this.mob.getYRot(), this.yRot, ROTATION_SPEED));
        this.mob.yHeadRot = this.mob.getYRot();
        this.mob.yBodyRot = this.mob.getYRot();
    }

    private void clearMovementInput() {
        this.mob.setZza(0.0F);
    }

    private float currentSpeed() {
        return (float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
    }

    private void tickOnGround(float speed) {
        if (this.jumpDelay-- <= 0) {
            prepareAndExecuteJump(speed);
        } else {
            stopAllMovement();
        }
    }

    private void stopAllMovement() {
        this.slime.xxa = 0.0F;
        this.slime.zza = 0.0F;
        this.mob.setSpeed(0.0F);
    }

    private void prepareAndExecuteJump(float speed) {
        this.jumpDelay = this.slime.getJumpDelay();
        if (this.isAggressive) {
            this.jumpDelay /= 2;
        }

        if (this.isStrafeMode) {
            launchStrafeJump(speed);
        } else {
            launchForwardJump(speed);
        }

        this.slime.getJumpControl().jump();
    }

    private void launchForwardJump(float speed) {
        this.mob.setSpeed(speed);
    }

    private void launchStrafeJump(float speed) {
        this.flightSign = this.strafeSign;
        this.strafeSign = this.slime.getRandom().nextBoolean() ? 1 : -1;

        this.mob.setSpeed(speed);
        this.slime.zza = 0.0F;
        this.slime.xxa = this.flightSign * speed;

        applyLateralImpulse();
    }

    private void applyLateralImpulse() {
        float yRotRad = this.yRot * (float) (Math.PI / 180.0);
        Vec3 vel = this.slime.getDeltaMovement();
        double boost = STRAFE_LATERAL_BOOST;
        this.slime.setDeltaMovement(
            vel.x + this.flightSign * Math.cos(yRotRad) * boost,
            vel.y,
            vel.z + this.flightSign * Math.sin(yRotRad) * boost
        );
    }

    private void tickAirborne(float speed) {
        this.mob.setSpeed(speed);
        if (this.isStrafeMode) {
            maintainLateralVelocity(speed);
        }
    }

    private void maintainLateralVelocity(float speed) {
        this.slime.zza = 0.0F;
        this.slime.xxa = this.flightSign * speed;
    }
}
