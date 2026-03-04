package com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import com.snowbigdeal.hostilemobscore.Constants;
import com.snowbigdeal.hostilemobscore.orchestrator.IMobAction;
import com.snowbigdeal.hostilemobscore.orchestrator.IOrchestrated;
import com.snowbigdeal.hostilemobscore.orchestrator.OrchestratorAction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.BrainActivityGroup;
import net.tslat.smartbrainlib.api.core.SmartBrainProvider;
import net.tslat.smartbrainlib.api.core.behaviour.FirstApplicableBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.custom.attack.AnimatableMeleeAttack;
import net.tslat.smartbrainlib.api.core.behaviour.custom.look.LookAtAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.misc.Idle;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.InvalidateAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetPlayerLookTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetRandomLookTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.TargetOrRetaliate;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.HurtBySensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyLivingEntitySensor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;


public class AngrySlime extends Mob implements GeoEntity, SmartBrainOwner<AngrySlime>, IOrchestrated {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private Brain.Provider<?> BRAIN_PROVIDER;
    public int slamCooldown = Constants.seconds(10); // start on cooldown so it can't slam immediately

    // -------------------------------------------------------------------------
    // IOrchestrated state
    // -------------------------------------------------------------------------

    private UUID partyId = null;
    private OrchestratorAction pendingAction = null;

    private boolean orchestratorSlamPending  = false;
    private boolean orchestratorSlamFinished = false;

    @Override public List<IMobAction> getMobActions()        { return List.of(new SlamMobAction()); }
    @Override public UUID             getPartyId()           { return partyId; }
    @Override public void             setPartyId(UUID id)    { this.partyId = id; }
    @Override public OrchestratorAction getPendingAction()   { return pendingAction; }
    @Override public void setPendingAction(OrchestratorAction a) { this.pendingAction = a; }

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

    public AngrySlime(EntityType<? extends AngrySlime> entityType , Level level) {
        super(entityType, level);
        xpReward = XP_REWARD;
        this.moveControl = new SlimeMoveControl(this);
    }

    public static AttributeSupplier.Builder createAttributes(){
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,           BASE_HEALTH)
                .add(Attributes.JUMP_STRENGTH,        BASE_JUMP_STRENGTH)
                .add(Attributes.MOVEMENT_SPEED,       BASE_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE,         BASE_FOLLOW_RANGE)
                .add(Attributes.ATTACK_DAMAGE,        BASE_ATTACK_DAMAGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, BASE_KNOCKBACK_RESIST);
    }

    // -------------------------------------------------------------------------
    // Attributes
    // -------------------------------------------------------------------------

    private static final double BASE_HEALTH           = 40.0;
    private static final float  BASE_JUMP_STRENGTH    = 0.42f;
    private static final double BASE_MOVEMENT_SPEED   = 0.25;
    private static final double BASE_FOLLOW_RANGE     = 16.0;
    private static final double BASE_ATTACK_DAMAGE    = 1.0;
    private static final double BASE_KNOCKBACK_RESIST = 0.6;
    private static final int    XP_REWARD             = 5;

    // -------------------------------------------------------------------------
    // Jump
    // -------------------------------------------------------------------------

    private static final float JUMP_POWER_AGGRESSIVE = 0.55f;
    private static final float JUMP_POWER_WANDER     = 0.30f;
    private static final int   JUMP_DELAY_MIN        = 30;
    private static final int   JUMP_DELAY_RANGE      = 15;

    // -------------------------------------------------------------------------
    // Animation
    // -------------------------------------------------------------------------

    private static final int ANIM_MOVEMENT_TRANSITION = 5;

    private static final RawAnimation ANIM_IDLE         = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_HOP          = RawAnimation.begin().thenLoop("hop");
    private static final RawAnimation ANIM_SLAM_WINDUP  = RawAnimation.begin().thenPlay("slam_windup");
    private static final RawAnimation ANIM_SLAM_IMPACT  = RawAnimation.begin().thenPlay("slam_impact");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Movement controller: hop when in the air or actively moving, idle when still
        controllers.add(new AnimationController<>(this, "movement", ANIM_MOVEMENT_TRANSITION, state -> {
            if (state.isMoving() || !state.getAnimatable().onGround()) {
                return state.setAndContinue(ANIM_HOP);
            }
            return state.setAndContinue(ANIM_IDLE);
        }));
        controllers.add(new AnimationController<>(this, "attack", 0, state -> PlayState.STOP));
        controllers.add(new AnimationController<>(this, "slam", 0, state -> PlayState.STOP)
                .triggerableAnim("slam_windup", ANIM_SLAM_WINDUP)
                .triggerableAnim("slam_impact", ANIM_SLAM_IMPACT));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    protected Brain.Provider<?> brainProvider() {
        if (BRAIN_PROVIDER == null) {
            BRAIN_PROVIDER = new SmartBrainProvider<>(this);
        }
        return BRAIN_PROVIDER;
    }

    @Override
    protected void customServerAiStep() {
        if (this.slamCooldown > 0) this.slamCooldown--;
        tickBrain(this);
    }

    @Override
    public List<? extends ExtendedSensor<? extends AngrySlime>> getSensors() {
        return ObjectArrayList.of(
                new NearbyLivingEntitySensor<>(), // This tracks nearby entities
                new HurtBySensor<>()                // This tracks the last damage source and attacker
        );
    }

    @Override
    public void jumpFromGround() {
        super.jumpFromGround();
        if (this.moveControl instanceof SlimeMoveControl smc && smc.isAggressive()) {
            // Inject a forward velocity burst so the long jump actually lunges
            float yRotRad = this.getYRot() * (float) (Math.PI / 180.0);
            Vec3 current = this.getDeltaMovement();
            double boost = smc.getLeapForce();
            this.setDeltaMovement(
                current.x - Math.sin(yRotRad) * boost,
                current.y,
                current.z + Math.cos(yRotRad) * boost
            );
        }
    }

    /** Returns a random jump delay in ticks (30-45). Used by SlimeMoveControl. */
    public int getJumpDelay() {
        return this.random.nextInt(JUMP_DELAY_RANGE) + JUMP_DELAY_MIN;
    }

    @Override
    protected float getJumpPower() {
        // Long jump when chasing, short hop when wandering
        if (this.moveControl instanceof SlimeMoveControl smc && smc.isAggressive()) {
            return JUMP_POWER_AGGRESSIVE;
        }
        return JUMP_POWER_WANDER;
    }

    @Override
    public BrainActivityGroup<AngrySlime> getCoreTasks() {
        return BrainActivityGroup.coreTasks(
                new LookAtAttackTarget<>(),
                new SlimeMoveBehaviour()
        );
    }

    @Override
    public BrainActivityGroup<AngrySlime> getIdleTasks() {
        return BrainActivityGroup.idleTasks(
                new FirstApplicableBehaviour<AngrySlime>(
                        new TargetOrRetaliate<AngrySlime>().attackablePredicate(
                        target -> target instanceof Player player
                                  && !player.getAbilities().invulnerable),
                        new SetPlayerLookTarget<>(),
                        new SetRandomLookTarget<>()),
                new Idle<>().runFor(entity -> entity.getRandom().nextInt(30, 60)));
    }

    @Override
    public BrainActivityGroup<AngrySlime> getFightTasks() {
        return BrainActivityGroup.fightTasks(
                new InvalidateAttackTarget<AngrySlime>().invalidateIf(
                        (slime, target) -> {
                            if (target instanceof Player player && player.getAbilities().invulnerable) return true;
                            double followRange = slime.getAttributeValue(Attributes.FOLLOW_RANGE);
                            return slime.distanceToSqr(target) > followRange * followRange;
                        }),
                new FirstApplicableBehaviour<AngrySlime>(
                        new SlimeSlamAttackBehaviour(),
                        new AnimatableMeleeAttack<AngrySlime>(10)
                                .attackInterval(slime -> 60 + slime.getRandom().nextInt(20))
                )
        );
    }
}
