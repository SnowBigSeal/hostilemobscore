package com.snowbigdeal.hostilemobscore.entity.slimes;

import com.snowbigdeal.hostilemobscore.entity.HostileMob;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.api.core.BrainActivityGroup;
import net.tslat.smartbrainlib.api.core.behaviour.FirstApplicableBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.custom.look.LookAtAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.misc.Idle;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.InvalidateAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetPlayerLookTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetRandomLookTarget;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

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
    // Animation
    // -------------------------------------------------------------------------

    private static final int ANIM_MOVEMENT_TRANSITION = 5;

    private static final RawAnimation ANIM_IDLE  = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_HOP   = RawAnimation.begin().thenLoop("hop");
    private static final RawAnimation ANIM_FLOAT = RawAnimation.begin().thenLoop("float");

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
        this.moveControl = new SlimeMoveControl(this);
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
                this.playSound(land, this.getSoundVolume(),
                        (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            }
        }
        wasOnGround = this.onGround();
    }

    @Override
    public void jumpFromGround() {
        super.jumpFromGround();
        if (this.moveControl instanceof SlimeMoveControl smc && smc.isAggressive()) {
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

    /** Returns a random jump delay in ticks (30–45). Used by {@link SlimeMoveControl}. */
    public int getJumpDelay() {
        return this.random.nextInt(JUMP_DELAY_RANGE) + JUMP_DELAY_MIN;
    }

    @Override
    protected float getJumpPower() {
        if (this.moveControl instanceof SlimeMoveControl smc && smc.isAggressive()) {
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
            if (slime.onGround() && slime.isInWater()) {
                return state.setAndContinue(ANIM_FLOAT);
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
                new SlimeMoveBehaviour<>(),
                new SlimeReturnHomeBehaviour<>()
        );
    }

    @Override
    public BrainActivityGroup<T> getIdleTasks() {
        return BrainActivityGroup.idleTasks(
                new FirstApplicableBehaviour<T>(
                        new SlimeTargetBehaviour<>(),
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
                            if (slime.distanceToSqr(target) > followRange * followRange) return true;
                            return !slime.isWithinRestriction();
                        }),
                getAttackBehaviours()
        );
    }

    /**
     * Subclasses return their specific attack priority list, wrapped in a
     * {@link FirstApplicableBehaviour}.
     */
    protected abstract FirstApplicableBehaviour<T> getAttackBehaviours();
}
