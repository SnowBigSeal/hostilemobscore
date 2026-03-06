package com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime;

import com.mojang.datafixers.util.Pair;
import com.snowbigdeal.hostilemobscore.Constants;
import com.snowbigdeal.hostilemobscore.attack.AttackSnapshot;
import com.snowbigdeal.hostilemobscore.attack.shape.CircleShape;
import com.snowbigdeal.hostilemobscore.attack.shape.TelegraphAttackShape;
import com.snowbigdeal.hostilemobscore.entity.behaviour.TelegraphAttackBehaviour;
import com.snowbigdeal.hostilemobscore.entity.slimes.SlimeMoveControl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Periodic slam attack. Selects the player position that would hit the most
 * other players in the AoE radius, shows an expanding warning ring during
 * wind-up, then launches and deals AoE damage + knockback on landing.
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
    private static final double LAUNCH_H_SCALE  = 0.15;
    private static final double LAUNCH_V_VELOCITY = 0.65;

    /** Minimum vertical clearance required to jump. */
    private static final double JUMP_CLEARANCE = (LAUNCH_V_VELOCITY * LAUNCH_V_VELOCITY) / (2 * 0.08);

    private static final double STAGGER_CHECK_RADIUS  = 20.0;
    private static final int    STAGGER_PENALTY_TICKS = Constants.seconds(4);

    /** UUIDs of slimes currently executing a slam — stagger gating. */
    private static final Set<UUID> activeSlamming = new HashSet<>();

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
        if (!slime.onGround() || slime.slamCooldown > 0) return false;
        if (slime.isInWater()) return false;
        if (!slime.hasLineOfSight(player)) return false;
        if (!level.noCollision(slime, slime.getBoundingBox().expandTowards(0, JUMP_CLEARANCE, 0))) return false;
        if (slime.getNavigation().createPath(player, 1) == null) return false;

        if (slime.getPartyId() != null) return slime.isOrchestratorSlamPending();

        return level.getEntitiesOfClass(AngrySlime.class,
                slime.getBoundingBox().inflate(STAGGER_CHECK_RADIUS),
                other -> other != slime && activeSlamming.contains(other.getUUID()))
                .isEmpty();
    }

    // -------------------------------------------------------------------------
    // TelegraphAttackBehaviour — shape
    // -------------------------------------------------------------------------

    @Override
    protected TelegraphAttackShape buildShape(AngrySlime slime) {
        this.slamTarget  = findBestSlamTarget(slime);
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

        slime.slamCooldown = COOLDOWN_TICKS + slime.getRandom().nextInt(COOLDOWN_VARIANCE);
        int nearbyCount = slime.level().getEntitiesOfClass(AngrySlime.class,
                slime.getBoundingBox().inflate(STAGGER_CHECK_RADIUS),
                other -> other != slime).size();
        slime.slamCooldown += nearbyCount * STAGGER_PENALTY_TICKS;

        activeSlamming.add(slime.getUUID());

        if (slime.getMoveControl() instanceof SlimeMoveControl smc) smc.setSlamLock(true);
    }

    @Override
    protected void onWindupTick(AngrySlime slime, int remaining) {
        if (slamTarget != null) faceTarget(slime, slamTarget);
    }

    @Override
    protected void applyLaunch(AngrySlime slime) {
        slime.triggerAnim("slam", "slam_windup");
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
        dealDamage(slime, snapshot);
    }

    @Override
    protected boolean shouldAbort(AngrySlime slime) {
        return slamTarget == null || !slamTarget.isAlive();
    }

    @Override
    protected void onStop(AngrySlime slime) {
        activeSlamming.remove(slime.getUUID());
        if (slime.getMoveControl() instanceof SlimeMoveControl smc) smc.setSlamLock(false);
        slime.notifyOrchestratedSlamComplete();
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
        double dx   = slamPosition.x - slime.getX();
        double dz   = slamPosition.z - slime.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        double hBoost = dist > 0 ? Math.min(1.0, dist * LAUNCH_H_SCALE) : 0;
        slime.setDeltaMovement(
                dist > 0 ? (dx / dist) * hBoost : 0,
                LAUNCH_V_VELOCITY,
                dist > 0 ? (dz / dist) * hBoost : 0);
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

    private Player findBestSlamTarget(AngrySlime slime) {
        double followRange = slime.getAttributeValue(Attributes.FOLLOW_RANGE);
        List<Player> nearby = slime.level().getEntitiesOfClass(Player.class,
                slime.getBoundingBox().inflate(followRange),
                p -> !p.getAbilities().invulnerable && p.isAlive());
        if (nearby.isEmpty()) return slime.getTarget() instanceof Player p ? p : null;
        return nearby.stream()
                .max(Comparator.comparingInt(p ->
                        countPlayersNear(p.position(), SLAM_RADIUS, nearby)))
                .orElse(null);
    }

    private int countPlayersNear(Vec3 position, float radius, List<Player> players) {
        return (int) players.stream()
                .filter(p -> p.position().distanceTo(position) <= radius)
                .count();
    }

    private static Vec3 groundPositionBelow(AngrySlime slime, BlockPos from) {
        BlockPos.MutableBlockPos pos = from.mutable();
        int minY = slime.level().getMinBuildHeight();
        while (pos.getY() > minY) {
            pos.move(Direction.DOWN);
            if (slime.level().getBlockState(pos).isSolid()) {
                return new Vec3(from.getX() + 0.5, pos.getY() + 1.0, from.getZ() + 0.5);
            }
        }
        return Vec3.atBottomCenterOf(from);
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return List.of();
    }
}
