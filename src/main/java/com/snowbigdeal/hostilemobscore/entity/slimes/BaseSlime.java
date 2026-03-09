package com.snowbigdeal.hostilemobscore.entity.slimes;

import com.snowbigdeal.hostilemobscore.entity.HostileMob;
import com.snowbigdeal.hostilemobscore.entity.ModMemoryTypes;
import com.snowbigdeal.hostilemobscore.entity.behaviour.DeaggroBehaviour;
import com.snowbigdeal.hostilemobscore.entity.behaviour.ReturnHomeBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.api.core.BrainActivityGroup;
import net.tslat.smartbrainlib.api.core.behaviour.FirstApplicableBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.custom.look.LookAtAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.misc.Idle;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.InvalidateAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetPlayerLookTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetRandomLookTarget;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for all slime-type mobs.
 * Provides shared physics (buoyancy, hopping), animations (idle/hop/float),
 * and brain tasks (movement, targeting, return-home).
 *
 * <p>Subclasses must implement {@link #getAttackBehaviours()} to supply their
 * specific attack priority list, and {@link #getTetherRadius()} for their leash range.
 *
 * @param <T> The concrete slime subclass (F-bounded for SmartBrainOwner).
 */
public abstract class BaseSlime<T extends BaseSlime<T>> extends HostileMob<T> {

    // -------------------------------------------------------------------------
    // Jump constants — can be read by subclasses
    // -------------------------------------------------------------------------

    protected static final float JUMP_POWER_AGGRESSIVE = 0.55f;
    protected static final float JUMP_POWER_WANDER     = 0.30f;
    protected static final int   JUMP_DELAY_MIN        = 30;
    protected static final int   JUMP_DELAY_RANGE      = 15;

    // -------------------------------------------------------------------------
    // Mixed-pack companion spawning
    // -------------------------------------------------------------------------

    private static final float MIXED_PACK_CHANCE = 0.30f;
    private static final int   MIXED_PACK_MAX    = 2;
    private static final ThreadLocal<Boolean> SPAWNING_COMPANIONS = ThreadLocal.withInitial(() -> false);

    /**
     * Override to return the companion slime type to occasionally spawn alongside
     * this mob on natural spawns. Return {@code null} for no companion.
     */
    @Nullable
    protected EntityType<? extends BaseSlime<?>> getCompanionType() { return null; }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);

        EntityType<? extends BaseSlime<?>> companionType = getCompanionType();
        if (!level.isClientSide()
                && !SPAWNING_COMPANIONS.get()
                && companionType != null
                && spawnData == null
                && (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION)
                && this.random.nextFloat() < MIXED_PACK_CHANCE) {
            SPAWNING_COMPANIONS.set(true);
            try {
                int count = 1 + this.random.nextInt(MIXED_PACK_MAX);
                for (int i = 0; i < count; i++) {
                    BaseSlime<?> companion = companionType.create(level.getLevel());
                    if (companion == null) continue;
                    int dx = this.random.nextInt(9) - 4;
                    int dz = this.random.nextInt(9) - 4;
                    BlockPos surface = level.getHeightmapPos(
                            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            this.blockPosition().offset(dx, 0, dz));
                    if (!level.getBlockState(surface.below()).isSolid()) continue;
                    companion.moveTo(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5,
                            this.getYRot(), 0f);
                    companion.finalizeSpawn(level, difficulty, spawnType, result);
                    level.addFreshEntity(companion);
                }
            } finally {
                SPAWNING_COMPANIONS.set(false);
            }
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Animation
    // -------------------------------------------------------------------------

    private static final int ANIM_MOVEMENT_TRANSITION = 5;

    private static final RawAnimation ANIM_IDLE  = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_HOP   = RawAnimation.begin().thenLoop("hop");

    // -------------------------------------------------------------------------
    // Sounds — subclasses override to supply their specific sound events
    // -------------------------------------------------------------------------

    /** Sound played when the slime leaves the ground. Return {@code null} for silence. */
    protected SoundEvent getJumpSound() { return null; }

    /** Sound played when the slime lands. Return {@code null} for silence. */
    protected SoundEvent getLandSound() { return null; }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    protected BaseSlime(EntityType<? extends T> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new HoppingMoveControl(this);
    }

    // -------------------------------------------------------------------------
    // Physics
    // -------------------------------------------------------------------------

    private boolean wasOnGround = true;

    /** Apply a strong upward buoyancy force whenever submerged in water. */
    @Override
    public void tick() {
        super.tick();
        if (this.isInWater()) {
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x, motion.y + 0.12, motion.z);
        }
        // Landing sound: detect transition from airborne to on-ground
        if (!wasOnGround && this.onGround()) {
            SoundEvent land = getLandSound();
            if (land != null) {
                playSound(land, this.getSoundVolume(), 1.0F);
            }
        }
        wasOnGround = this.onGround();
    }

    @Override
    public void jumpFromGround() {
        super.jumpFromGround();
        if (this.moveControl instanceof HoppingMoveControl smc && smc.isAggressive()) {
            float yRotRad = this.getYRot() * (float) (Math.PI / 180.0);
            Vec3 current = this.getDeltaMovement();
            double boost = smc.getLeapForce();
            this.setDeltaMovement(
                current.x - Math.sin(yRotRad) * boost,
                current.y,
                current.z + Math.cos(yRotRad) * boost
            );
        }
        SoundEvent jump = getJumpSound();
        if (jump != null) {
            this.playSound(jump, this.getSoundVolume(),
                    (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
        }
    }

    /** Returns a random jump delay in ticks (30–45). Used by {@link HoppingMoveControl}. */
    public int getJumpDelay() {
        return this.random.nextInt(JUMP_DELAY_RANGE) + JUMP_DELAY_MIN;
    }

    @Override
    protected float getJumpPower() {
        if (this.moveControl instanceof HoppingMoveControl smc && smc.isAggressive()) {
            return JUMP_POWER_AGGRESSIVE;
        }
        return JUMP_POWER_WANDER;
    }

    // -------------------------------------------------------------------------
    // Animations — subclasses call super then add their own controllers
    // -------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", ANIM_MOVEMENT_TRANSITION, state -> {
            var slime = state.getAnimatable();
            if (slime.isInWater()) {
                return PlayState.STOP;
            }
            if (state.isMoving() || !slime.onGround()) {
                return state.setAndContinue(ANIM_HOP);
            }
            return state.setAndContinue(ANIM_IDLE);
        }));
    }

    // -------------------------------------------------------------------------
    // Brain / AI
    // -------------------------------------------------------------------------

    @Override
    public BrainActivityGroup<T> getCoreTasks() {
        return BrainActivityGroup.coreTasks(
                new LookAtAttackTarget<>(),
                new HoppingCombatBehaviour<>(),
                new DeaggroBehaviour<>()
        );
    }

    @Override
    public BrainActivityGroup<T> getIdleTasks() {
        return BrainActivityGroup.idleTasks(
                new FirstApplicableBehaviour<T>(
                        new TetheredTargetBehaviour<>(),
                        new SetPlayerLookTarget<>(),
                        new SetRandomLookTarget<>()),
                new Idle<>().runFor(entity -> entity.getRandom().nextInt(30, 60)));
    }

    @Override
    public BrainActivityGroup<T> getFightTasks() {
        return BrainActivityGroup.fightTasks(
                new InvalidateAttackTarget<T>().invalidateIf(
                        (slime, target) -> {
                            if (target instanceof Player player && player.getAbilities().invulnerable) return true;
                            double followRange = slime.getAttributeValue(Attributes.FOLLOW_RANGE);
                            return slime.distanceToSqr(target) > followRange * followRange;
                        }),
                getAttackBehaviours()
        );
    }

    @Override
    public Map<Activity, BrainActivityGroup<? extends T>> getAdditionalTasks() {
        Map<Activity, BrainActivityGroup<? extends T>> tasks = new HashMap<>();
        tasks.put(ModMemoryTypes.ACTIVITY_RETURNING_HOME.get(),
                new BrainActivityGroup<T>(ModMemoryTypes.ACTIVITY_RETURNING_HOME.get())
                        .onlyStartWithMemoryStatus(ModMemoryTypes.RETURNING_HOME.get(), MemoryStatus.VALUE_PRESENT)
                        .behaviours(new ReturnHomeBehaviour<>()));
        return tasks;
    }

    @Override
    public List<Activity> getActivityPriorities() {
        return List.of(ModMemoryTypes.ACTIVITY_RETURNING_HOME.get(), Activity.FIGHT, Activity.IDLE);
    }

    /**
     * Subclasses return their specific attack priority list, wrapped in a
     * {@link FirstApplicableBehaviour}.
     */
    protected abstract FirstApplicableBehaviour<T> getAttackBehaviours();

    @Override
    public void applyReturnMovement() {
        if (!(getMoveControl() instanceof HoppingMoveControl smc)) return;
        double dx = getRestrictCenter().getX() + 0.5 - getX();
        double dz = getRestrictCenter().getZ() + 0.5 - getZ();
        float yRot = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        smc.setDirection(yRot, true);
        smc.setWantedMovement(2.5);
    }
}
