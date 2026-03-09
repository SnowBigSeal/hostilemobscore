package com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime;

import com.mojang.datafixers.util.Pair;
import com.snowbigdeal.hostilemobscore.Constants;
import com.snowbigdeal.hostilemobscore.attack.AttackSnapshot;
import com.snowbigdeal.hostilemobscore.sounds.ModSounds;
import com.snowbigdeal.hostilemobscore.attack.shape.CircleShape;
import com.snowbigdeal.hostilemobscore.attack.shape.TelegraphAttackShape;
import com.snowbigdeal.hostilemobscore.entity.ModMemoryTypes;
import com.snowbigdeal.hostilemobscore.entity.behaviour.TelegraphAttackBehaviour;
import com.snowbigdeal.hostilemobscore.entity.slimes.HoppingMoveControl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.util.BrainUtils;

import java.util.List;

/**
 * Orchestrated slam attack. Targets a random player in range during wind-up,
 * shows an expanding warning ring, then launches and deals AoE damage + knockback on landing.
 *
 * <p>Extends {@link TelegraphAttackBehaviour}: the windup/packet/recovery lifecycle
 * is handled by the base. This class supplies the slam-specific geometry, movement,
 * and damage.
 */
public class SlimeSlamAttackBehaviour extends TelegraphAttackBehaviour<AngrySlime> {

    // ---- Timing ----
    private static final float  SLAM_RADIUS           = 4f;
    private static final float  SLAM_DAMAGE_MULT      = 5f;
    private static final float  SLAM_KNOCKBACK        = 1.5f;
    private static final int    WINDUP_TICKS          = Constants.seconds(1);
    private static final int    RECOVERY_TICKS_CONST  = Constants.seconds(0.75f);
    private static final int    COOLDOWN_TICKS        = Constants.seconds(10);
    private static final int    COOLDOWN_VARIANCE     = Constants.seconds(10);
    private static final int    FLIGHT_MIN_TICKS      = 3;
    private static final int    BEHAVIOUR_TIMEOUT     = Constants.seconds(7);

    // ---- Physics ----
    /** Arc peak = this fraction of horizontal distance (e.g. 0.75 → 75% of dist). */
    private static final double ARC_HEIGHT_FACTOR  = 0.75;
    private static final double GRAVITY            = 0.08;
    private static final double HORIZONTAL_DRAG    = 0.91; // Minecraft air friction per tick
    private static final double HORIZONTAL_BOOST   = 1.4;  // compensate for real-world friction losses

    /** Minimum vertical clearance required to jump. */
    private static final double JUMP_CLEARANCE = 8.0;

    // ---- Instance state ----
    private Player  slamTarget   = null;
    private Vec3    slamPosition = null;
    private int     flightTicks  = 0;
    private boolean wasAirborne  = false;

    public SlimeSlamAttackBehaviour() {
        runFor(entity -> BEHAVIOUR_TIMEOUT);
    }

    // -------------------------------------------------------------------------
    // TelegraphAttackBehaviour — timing
    // -------------------------------------------------------------------------

    @Override protected int windupTicks()   { return WINDUP_TICKS; }
    @Override protected int recoveryTicks() { return RECOVERY_TICKS_CONST; }

    // -------------------------------------------------------------------------
    // TelegraphAttackBehaviour — start conditions
    // -------------------------------------------------------------------------

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, AngrySlime slime) {
        if (!(slime.getTarget() instanceof Player player)) return false;
        if (!slime.onGround() || slime.isInWater()) return false;
        if (!slime.hasLineOfSight(player)) return false;
        if (!level.noCollision(slime, slime.getBoundingBox().expandTowards(0, JUMP_CLEARANCE, 0))) return false;
        return slime.getNavigation().createPath(player, 1) != null;
    }

    // -------------------------------------------------------------------------
    // TelegraphAttackBehaviour — shape
    // -------------------------------------------------------------------------

    @Override
    protected TelegraphAttackShape buildShape(AngrySlime slime) {
        this.slamTarget  = findSlamTarget(slime);
        this.slamPosition = slamTarget != null
                ? groundPositionBelow(slime, slamTarget.blockPosition())
                : slime.position();
        return new CircleShape(slamPosition, SLAM_RADIUS);
    }

    // -------------------------------------------------------------------------
    // TelegraphAttackBehaviour — lifecycle hooks
    // -------------------------------------------------------------------------

    @Override
    protected void onStart(AngrySlime slime) {
        this.flightTicks = 0;
        this.wasAirborne = false;

        BrainUtils.setForgettableMemory(slime, ModMemoryTypes.SLAM_COOLDOWN.get(), true,
                COOLDOWN_TICKS + slime.getRandom().nextInt(COOLDOWN_VARIANCE));

        if (slime.getMoveControl() instanceof HoppingMoveControl smc) smc.setAttackLock(true);
    }

    @Override
    protected void onWindupTick(AngrySlime slime, int remaining) {
        if (slamTarget != null) faceTarget(slime, slamTarget);
    }

    @Override
    protected void applyLaunch(AngrySlime slime) {
        slime.triggerAnim("slam", "slam_windup");
        slime.playSound(ModSounds.ANGRY_SLIME_JUMP.get(), 1.0F, 1.0F);
        launchTowardSlamPosition(slime);
    }

    @Override
    protected boolean isExecutionComplete(AngrySlime slime) {
        if (!slime.onGround()) wasAirborne = true;
        if (wasAirborne) flightTicks++;
        return wasAirborne && flightTicks > FLIGHT_MIN_TICKS && slime.onGround();
    }

    @Override
    protected void onImpact(AngrySlime slime, AttackSnapshot<Player> snapshot) {
        slime.triggerAnim("slam", "slam_impact");
        slime.playSound(ModSounds.ANGRY_SLIME_SLAM.get(), 1.0F, 1.0F);
        dealDamage(slime, snapshot);
    }

    @Override
    protected boolean shouldAbort(AngrySlime slime) {
        return slamTarget == null || !slamTarget.isAlive();
    }

    @Override
    protected void onStop(AngrySlime slime) {
        if (slime.getMoveControl() instanceof HoppingMoveControl smc) smc.setAttackLock(false);
        BrainUtils.clearMemory(slime, ModMemoryTypes.SLAM_PENDING.get());
        slamTarget   = null;
        slamPosition = null;
        flightTicks  = 0;
        wasAirborne  = false;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void launchTowardSlamPosition(AngrySlime slime) {
        if (slamPosition == null) return;
        double dx = slamPosition.x - slime.getX();
        double dz = slamPosition.z - slime.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        double targetDY  = slamPosition.y - slime.getY();

        // Arc height scales with distance — ensure it always clears the target's height
        double H  = Math.max(targetDY + 1.0, horizDist * ARC_HEIGHT_FACTOR);
        double vy = Math.sqrt(2.0 * GRAVITY * H);

        double T = (vy + Math.sqrt(vy * vy - 2.0 * GRAVITY * targetDY)) / GRAVITY;

        // Horizontal drag: actual dist = vxz * (1 - drag^T) / (1 - drag)
        // Solve for vxz so the slime actually covers horizDist
        double horizSum = (1.0 - Math.pow(HORIZONTAL_DRAG, T)) / (1.0 - HORIZONTAL_DRAG);
        double vxz = horizDist > 0 ? (horizDist / horizSum) * HORIZONTAL_BOOST : 0.0;

        slime.setDeltaMovement(
            horizDist > 0 ? (dx / horizDist) * vxz : 0,
            vy,
            horizDist > 0 ? (dz / horizDist) * vxz : 0
        );
        slime.hasImpulse = true;
    }

    private void faceTarget(AngrySlime slime, Player target) {
        double dx  = target.getX() - slime.getX();
        double dz  = target.getZ() - slime.getZ();
        float yRot = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        slime.setYRot(yRot);
        slime.yHeadRot = yRot;
        slime.yBodyRot = yRot;
    }

    private void dealDamage(AngrySlime slime, AttackSnapshot<Player> snapshot) {
        if (!(slime.level() instanceof ServerLevel)) return;
        float damage = (float) slime.getAttributeValue(Attributes.ATTACK_DAMAGE) * SLAM_DAMAGE_MULT;
        for (Player player : snapshot.targets()) {
            if (!player.isAlive() || player.getAbilities().invulnerable) continue;
            player.hurt(slime.damageSources().mobAttack(slime), damage);
            applyKnockback(slime, player);
        }
    }

    private void applyKnockback(AngrySlime slime, Player player) {
        double dx   = player.getX() - slime.getX();
        double dz   = player.getZ() - slime.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 0) player.knockback(SLAM_KNOCKBACK, -dx / dist, -dz / dist);
    }

    private Player findSlamTarget(AngrySlime slime) {
        double followRange = slime.getAttributeValue(Attributes.FOLLOW_RANGE);
        List<Player> nearby = slime.level().getEntitiesOfClass(Player.class,
                slime.getBoundingBox().inflate(followRange),
                p -> !p.getAbilities().invulnerable && p.isAlive());
        if (nearby.isEmpty()) return slime.getTarget() instanceof Player p ? p : null;
        return nearby.get(slime.getRandom().nextInt(nearby.size()));
    }

    private static Vec3 groundPositionBelow(AngrySlime slime, BlockPos from) {
        BlockPos.MutableBlockPos pos = from.mutable();
        int minY = slime.level().getMinBuildHeight();
        while (pos.getY() > minY) {
            pos.move(Direction.DOWN);
            BlockState state = slime.level().getBlockState(pos);
            if (!state.isAir() && !state.getCollisionShape(slime.level(), pos).isEmpty()) {
                return new Vec3(from.getX() + 0.5, pos.getY() + 1.0, from.getZ() + 0.5);
            }
        }
        return Vec3.atBottomCenterOf(from);
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return List.of(
                Pair.of(ModMemoryTypes.SLAM_COOLDOWN.get(), MemoryStatus.VALUE_ABSENT),
                Pair.of(ModMemoryTypes.SLAM_PENDING.get(),  MemoryStatus.VALUE_PRESENT)
        );
    }
}
