package com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime;

import com.mojang.datafixers.util.Pair;
import com.snowbigdeal.hostilemobscore.Constants;
import com.snowbigdeal.hostilemobscore.attack.AttackSnapshot;
import com.snowbigdeal.hostilemobscore.network.CircleAoePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tslat.smartbrainlib.api.core.behaviour.ExtendedBehaviour;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Periodic slam attack. Selects the player position that would hit the most
 * other players in the AoE radius, shows an expanding warning ring during
 * wind-up, then launches and deals AoE damage + knockback on landing.
 */
public class SlimeSlamAttackBehaviour extends ExtendedBehaviour<AngrySlime> {

    private static final float  SLAM_RADIUS         = 4f;
    private static final float  SLAM_DAMAGE_MULT    = 5f;
    private static final float  SLAM_KNOCKBACK      = 1.5f;
    private static final int    WINDUP_TICKS        = Constants.seconds(1);
    private static final int    RECOVERY_TICKS      = Constants.seconds(0.75f);
    private static final int    COOLDOWN_TICKS      = Constants.seconds(10);
    private static final int    COOLDOWN_VARIANCE   = Constants.seconds(10); // random extra 0–10 s
    private static final int    FLIGHT_MIN_TICKS    = 3;    // grace period after launch before landing counts
    private static final int    BEHAVIOUR_TIMEOUT   = Constants.seconds(7); // safety cap
    private static final double LAUNCH_H_SCALE      = 0.15; // horizontal speed = dist * scale, capped at 1.0
    private static final double LAUNCH_V_VELOCITY   = 0.65; // upward launch velocity (blocks/tick)
    /** Radius within which other slimes are considered "nearby" for stagger checks. */
    private static final double STAGGER_CHECK_RADIUS = 20.0;
    /** Extra cooldown added per additional nearby slime to spread out slams in groups. */
    private static final int    STAGGER_PENALTY_TICKS = Constants.seconds(4);

    /** Minimum vertical clearance (blocks above head) required to jump — derived from launch physics. */
    private static final double JUMP_CLEARANCE = (LAUNCH_V_VELOCITY * LAUNCH_V_VELOCITY) / (2 * 0.08);

    /** Tracks UUIDs of slimes currently executing a slam — used for stagger gating. */
    private static final Set<UUID> activeSlamming = new HashSet<>();

    /*
     * Particle ring backup — replaced by DamageAoeEntity
     *
     * private static final DustParticleOptions RING_PARTICLE =
     *         new DustParticleOptions(new Vector3f(1.0f, 0.2f, 0.0f), 0.8f);
     * private static final DustParticleOptions RING_EDGE_PARTICLE =
     *         new DustParticleOptions(new Vector3f(1.0f, 0.0f, 0.0f), 1.2f);
     */

    private Player  slamTarget       = null;
    private Vec3    slamPosition     = null; // snapshotted at windup start — does not follow the player
    private AttackSnapshot<Player> slamSnapshot = null; // entities committed at launch time
    private int     windupTimer      = 0;
    private int     recoveryTimer    = 0;
    private int     flightTicks      = 0;  // counts ticks since launch — prevents same-tick land detection
    private boolean launched         = false;
    private boolean wasAirborne      = false;
    private boolean hasLanded        = false;
    private boolean slamComplete     = false;

    public SlimeSlamAttackBehaviour() {
        runFor(entity -> BEHAVIOUR_TIMEOUT);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, AngrySlime slime) {
        if (!(slime.getTarget() instanceof Player player)) return false;
        if (!slime.onGround() || slime.slamCooldown > 0) return false;
        if (slime.isInWater()) return false;
        if (!slime.hasLineOfSight(player)) return false;
        if (!level.noCollision(slime, slime.getBoundingBox().expandTowards(0, JUMP_CLEARANCE, 0))) return false;
        if (slime.getNavigation().createPath(player, 1) == null) return false;

        // If in a party, the orchestrator controls when we slam
        if (slime.getPartyId() != null) {
            return slime.isOrchestratorSlamPending();
        }

        // Solo slime: stagger gate — don't start if any nearby slime is currently slamming
        return level.getEntitiesOfClass(AngrySlime.class,
                        slime.getBoundingBox().inflate(STAGGER_CHECK_RADIUS),
                        other -> other != slime && activeSlamming.contains(other.getUUID()))
                .isEmpty();
    }

    @Override
    protected boolean shouldKeepRunning(AngrySlime slime) {
        return !this.slamComplete;
    }

    @Override
    protected void start(AngrySlime slime) {
        this.slamTarget   = findBestSlamTarget(slime);
        this.windupTimer  = WINDUP_TICKS;
        this.launched     = false;
        this.wasAirborne  = false;
        this.hasLanded    = false;
        this.flightTicks  = 0;
        this.slamComplete = false;

        // Base cooldown + variance
        slime.slamCooldown = COOLDOWN_TICKS + slime.getRandom().nextInt(COOLDOWN_VARIANCE);

        // Cooldown penalty per additional nearby slime
        int nearbyCount = slime.level().getEntitiesOfClass(AngrySlime.class,
                slime.getBoundingBox().inflate(STAGGER_CHECK_RADIUS),
                other -> other != slime).size();
        slime.slamCooldown += nearbyCount * STAGGER_PENALTY_TICKS;

        activeSlamming.add(slime.getUUID());

        this.slamPosition = this.slamTarget != null ? this.slamTarget.position() : null;

        if (slime.getMoveControl() instanceof SlimeMoveControl smc) {
            smc.setSlamLock(true);
        }
        // slam animation is triggered at launch, not here — idle plays during windup naturally
    }

    @Override
    protected void tick(AngrySlime slime) {
        if (this.slamTarget == null || !this.slamTarget.isAlive()) {
            this.slamComplete = true;
            return;
        }

        if (!this.launched) {
            tickWindup(slime);
            return;
        }

        trackAirborneState(slime);

        if (this.wasAirborne && this.flightTicks > FLIGHT_MIN_TICKS && slime.onGround()) {
            stopMovement(slime);
            if (!this.hasLanded) {
                this.hasLanded = true;
                slime.triggerAnim("slam", "slam_impact");
                dealAoeImpact(slime);
                this.recoveryTimer = RECOVERY_TICKS;
            } else if (--this.recoveryTimer <= 0) {
                this.slamComplete = true;
            }
        }
    }

    @Override
    protected void stop(AngrySlime slime) {
        activeSlamming.remove(slime.getUUID());
        if (slime.getMoveControl() instanceof SlimeMoveControl smc) {
            smc.setSlamLock(false);
        }
        slime.notifyOrchestratedSlamComplete();
        this.slamTarget    = null;
        this.slamPosition  = null;
        this.slamSnapshot  = null;
        this.launched      = false;
        this.wasAirborne   = false;
        this.hasLanded     = false;
        this.flightTicks   = 0;
        this.recoveryTimer = 0;
        this.slamComplete  = false;
    }

    // -------------------------------------------------------------------------
    // Wind-up phase
    // -------------------------------------------------------------------------

    private void tickWindup(AngrySlime slime) {
        stopMovement(slime);
        faceTarget(slime, this.slamTarget);

        // Spawn the AOE zone VFX on the first windup tick so the client shows the ring
        if (this.windupTimer == WINDUP_TICKS) {
            sendSlamAoePacket(slime);
        }

        if (--this.windupTimer <= 0) {
            this.slamSnapshot = AttackSnapshot.capture(
                    slime.level(),
                    this.slamPosition,
                    SLAM_RADIUS,
                    Player.class,
                    p -> !p.getAbilities().invulnerable && p.isAlive());
            launchTowardTarget(slime, this.slamTarget);
            slime.triggerAnim("slam", "slam_windup");
            this.wasAirborne = true; // mark airborne at launch; physics confirm next tick
            this.launched = true;
        }
    }

    private void stopMovement(AngrySlime slime) {
        slime.xxa = 0;
        slime.zza = 0;
        slime.setSpeed(0);
    }

    private void faceTarget(AngrySlime slime, Player target) {
        double dx = target.getX() - slime.getX();
        double dz = target.getZ() - slime.getZ();
        float yRot = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        slime.setYRot(yRot);
        slime.yHeadRot = yRot;
        slime.yBodyRot = yRot;
    }

    /** Sends the slam AoE packet to all players on the server so they can show the VFX ring. */
    private void sendSlamAoePacket(AngrySlime slime) {
        if (!(slime.level() instanceof ServerLevel level)) return;
        CircleAoePacket packet = new CircleAoePacket(this.slamPosition, SLAM_RADIUS, WINDUP_TICKS);
        for (ServerPlayer player : level.players()) {
            PacketDistributor.sendToPlayer(player, packet);
        }
    }

    // -------------------------------------------------------------------------
    // Particle ring backup (kept for reference — replaced by DamageAoeEntity)
    // -------------------------------------------------------------------------

    /*
    private static final DustParticleOptions RING_PARTICLE =
            new DustParticleOptions(new Vector3f(1.0f, 0.2f, 0.0f), 0.8f);
    private static final DustParticleOptions RING_EDGE_PARTICLE =
            new DustParticleOptions(new Vector3f(1.0f, 0.0f, 0.0f), 1.2f);

    private void spawnExpandingRing(AngrySlime slime) {
        if (!(slime.level() instanceof ServerLevel level)) return;
        float progress    = (float) (WINDUP_TICKS - this.windupTimer) / WINDUP_TICKS;
        float innerRadius = SLAM_RADIUS * progress;
        Vec3  center      = this.slamPosition;
        int   segments    = 28;
        boolean inFinalPhase = this.windupTimer <= WINDUP_TICKS / 4;
        boolean showBoundary = inFinalPhase ? (this.windupTimer % 2 == 0) : (this.windupTimer % 4 == 0);
        if (showBoundary) spawnRing(level, center, SLAM_RADIUS, segments, RING_EDGE_PARTICLE);
        if (innerRadius >= 0.1f) spawnRing(level, center, innerRadius, segments, RING_PARTICLE);
    }

    private void spawnRing(ServerLevel level, Vec3 center, float radius, int segments, DustParticleOptions particle) {
        for (int i = 0; i < segments; i++) {
            double angle = (2 * Math.PI / segments) * i;
            double x = center.x + radius * Math.cos(angle);
            double z = center.z + radius * Math.sin(angle);
            level.sendParticles(particle, x, center.y + 0.05, z, 1, 0, 0, 0, 0);
        }
    }
    */

    // -------------------------------------------------------------------------
    // Launch + landing phases
    // -------------------------------------------------------------------------

    private void launchTowardTarget(AngrySlime slime, Player target) {
        double dx   = this.slamPosition.x - slime.getX();
        double dz   = this.slamPosition.z - slime.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        double horizontalBoost = dist > 0 ? Math.min(1.0, dist * LAUNCH_H_SCALE) : 0;

        slime.setDeltaMovement(
            dist > 0 ? (dx / dist) * horizontalBoost : 0,
            LAUNCH_V_VELOCITY,
            dist > 0 ? (dz / dist) * horizontalBoost : 0
        );
        slime.hasImpulse = true;
    }

    private void trackAirborneState(AngrySlime slime) {
        if (!slime.onGround()) {
            this.wasAirborne = true;
        }
        if (this.wasAirborne) {
            this.flightTicks++;
        }
    }

    private void dealAoeImpact(AngrySlime slime) {
        if (!(slime.level() instanceof ServerLevel)) return;
        if (this.slamSnapshot == null) return;

        float damage = (float) slime.getAttributeValue(Attributes.ATTACK_DAMAGE) * SLAM_DAMAGE_MULT;

        for (Player player : this.slamSnapshot.targets()) {
            if (!player.isAlive() || player.getAbilities().invulnerable) continue;
            player.hurt(slime.damageSources().mobAttack(slime), damage);
            applyKnockback(slime, player);
        }
    }

    private void applyKnockback(AngrySlime slime, Player player) {
        double dx   = player.getX() - slime.getX();
        double dz   = player.getZ() - slime.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 0) {
            player.knockback(SLAM_KNOCKBACK, -dx / dist, -dz / dist);
        }
    }

    // -------------------------------------------------------------------------
    // Target selection — pick position that hits the most players
    // -------------------------------------------------------------------------

    private Player findBestSlamTarget(AngrySlime slime) {
        double followRange = slime.getAttributeValue(Attributes.FOLLOW_RANGE);

        List<Player> nearby = slime.level().getEntitiesOfClass(Player.class,
                slime.getBoundingBox().inflate(followRange),
                p -> !p.getAbilities().invulnerable && p.isAlive());

        if (nearby.isEmpty()) {
            // Fall back to the current attack target — they were in range when the slam triggered
            return slime.getTarget() instanceof Player p ? p : null;
        }

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

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return List.of();
    }
}
