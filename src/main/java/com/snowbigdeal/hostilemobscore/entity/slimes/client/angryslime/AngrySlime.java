package com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime;

import com.snowbigdeal.hostilemobscore.Constants;
import com.snowbigdeal.hostilemobscore.entity.ModEntities;
import com.snowbigdeal.hostilemobscore.entity.slimes.BaseSlime;
import com.snowbigdeal.hostilemobscore.entity.slimes.SlimeMoveControl;
import com.snowbigdeal.hostilemobscore.orchestrator.IMobAction;
import com.snowbigdeal.hostilemobscore.sounds.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.tslat.smartbrainlib.api.core.behaviour.FirstApplicableBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.custom.attack.AnimatableMeleeAttack;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

public class AngrySlime extends BaseSlime<AngrySlime> {

    public int slamCooldown = Constants.seconds(10); // start on cooldown so it can't slam immediately

    // -------------------------------------------------------------------------
    // IOrchestrated — slam-specific orchestrator state
    // -------------------------------------------------------------------------

    private boolean orchestratorSlamPending  = false;
    private boolean orchestratorSlamFinished = false;

    @Override public List<IMobAction> getMobActions() { return List.of(new SlamMobAction()); }

    /** Called by {@link SlamMobAction} when the orchestrator assigns a slam. */
    public void grantOrchestratedSlam() {
        this.orchestratorSlamPending  = true;
        this.orchestratorSlamFinished = false;
    }

    /** Called by {@link SlimeSlamAttackBehaviour} when the slam finishes. */
    public void notifyOrchestratedSlamComplete() {
        this.orchestratorSlamPending  = false;
        this.orchestratorSlamFinished = true;
    }

    public boolean isOrchestratorSlamPending()  { return orchestratorSlamPending; }
    public boolean isOrchestratedSlamFinished() { return orchestratorSlamFinished; }

    public AngrySlime(EntityType<? extends AngrySlime> entityType, Level level) {
        super(entityType, level);
        xpReward = XP_REWARD;
    }

    // -------------------------------------------------------------------------
    // Tether
    // -------------------------------------------------------------------------

    @Override
    protected int getTetherRadius() { return TETHER_RADIUS; }

    @Override
    protected EntityType<? extends BaseSlime<?>> getCompanionType() {
        return ModEntities.SLEEPY_SLIME.get();
    }

    // -------------------------------------------------------------------------
    // Attributes
    // -------------------------------------------------------------------------

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH,              BASE_HEALTH)
                .add(Attributes.JUMP_STRENGTH,           BASE_JUMP_STRENGTH)
                .add(Attributes.MOVEMENT_SPEED,          BASE_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE,            BASE_FOLLOW_RANGE)
                .add(Attributes.ATTACK_DAMAGE,           BASE_ATTACK_DAMAGE)
                .add(Attributes.KNOCKBACK_RESISTANCE,    BASE_KNOCKBACK_RESIST)
                .add(Attributes.SAFE_FALL_DISTANCE,      BASE_SAFE_FALL_DISTANCE)
                .add(Attributes.FALL_DAMAGE_MULTIPLIER,  BASE_FALL_DAMAGE_MULTIPLIER);
    }

    private static final double BASE_HEALTH               = 40.0;
    private static final float  BASE_JUMP_STRENGTH        = 0.42f;
    private static final double BASE_MOVEMENT_SPEED       = 0.25;
    private static final double BASE_FOLLOW_RANGE         = 16.0;
    private static final double BASE_ATTACK_DAMAGE        = 1.0;
    private static final double BASE_KNOCKBACK_RESIST     = 0.6;
    private static final int    XP_REWARD                 = 5;
    private static final int    TETHER_RADIUS             = 64;
    private static final double BASE_SAFE_FALL_DISTANCE   = 20.0;
    private static final double BASE_FALL_DAMAGE_MULTIPLIER = 0.1;

    // -------------------------------------------------------------------------
    // Sounds
    // -------------------------------------------------------------------------

    @Override protected SoundEvent getJumpSound() { return ModSounds.ANGRY_SLIME_JUMP.get(); }
    @Override protected SoundEvent getLandSound() { return ModSounds.ANGRY_SLIME_LAND.get(); }

    @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return SoundEvents.SLIME_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SLIME_DEATH; }

    // -------------------------------------------------------------------------
    // Animation — slam controllers added on top of BaseSlime's movement controller
    // -------------------------------------------------------------------------

    private static final RawAnimation ANIM_SLAM_WINDUP = RawAnimation.begin().thenPlay("slam_windup");
    private static final RawAnimation ANIM_SLAM_IMPACT = RawAnimation.begin().thenPlay("slam_impact");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        controllers.add(new AnimationController<>(this, "slam", 0, state -> PlayState.STOP)
                .triggerableAnim("slam_windup", ANIM_SLAM_WINDUP)
                .triggerableAnim("slam_impact", ANIM_SLAM_IMPACT));
    }

    // -------------------------------------------------------------------------
    // AI
    // -------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        if (this.slamCooldown > 0 && this.getTarget() != null) this.slamCooldown--;
        super.customServerAiStep();
    }

    @Override
    protected FirstApplicableBehaviour<AngrySlime> getAttackBehaviours() {
        return new FirstApplicableBehaviour<>(
                new SlimeSlamAttackBehaviour(),
                new AnimatableMeleeAttack<AngrySlime>(10)
                        .attackInterval(slime -> 60 + slime.getRandom().nextInt(20))
        );
    }
}
