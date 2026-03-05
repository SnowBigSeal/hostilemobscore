package com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import com.snowbigdeal.hostilemobscore.Constants;
import com.snowbigdeal.hostilemobscore.orchestrator.IMobAction;
import com.snowbigdeal.hostilemobscore.orchestrator.IOrchestrated;
import com.snowbigdeal.hostilemobscore.orchestrator.OrchestratorAction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import org.jetbrains.annotations.Nullable;
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
import java.util.Optional;
import java.util.UUID;


public class AngrySlime extends Mob implements GeoEntity, SmartBrainOwner<AngrySlime>, IOrchestrated {

    // -------------------------------------------------------------------------
    // Synced entity data (accessible client-side for debug rendering)
    // -------------------------------------------------------------------------

    private static final EntityDataAccessor<BlockPos> DATA_TETHER_CENTER =
            SynchedEntityData.defineId(AngrySlime.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Optional<UUID>> DATA_PARTY_ID =
            SynchedEntityData.defineId(AngrySlime.class, EntityDataSerializers.OPTIONAL_UUID);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TETHER_CENTER, BlockPos.ZERO);
        builder.define(DATA_PARTY_ID, Optional.empty());
    }

    @Override
    public void restrictTo(BlockPos pos, int distance) {
        super.restrictTo(pos, distance);
        this.entityData.set(DATA_TETHER_CENTER, pos);
    }

    public BlockPos getSyncedTetherCenter() { return this.entityData.get(DATA_TETHER_CENTER); }
    public Optional<UUID> getSyncedPartyId() { return this.entityData.get(DATA_PARTY_ID); }

    // -------------------------------------------------------------------------

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private Brain.Provider<?> BRAIN_PROVIDER;
    public int slamCooldown = Constants.seconds(10); // start on cooldown so it can't slam immediately

    private UUID partyId = null;
    private OrchestratorAction pendingAction = null;

    private boolean orchestratorSlamPending  = false;
    private boolean orchestratorSlamFinished = false;

    @Override public List<IMobAction> getMobActions()        { return List.of(new SlamMobAction()); }
    @Override public UUID             getPartyId()           { return partyId; }
    @Override public void             setPartyId(UUID id)    { this.partyId = id; this.entityData.set(DATA_PARTY_ID, Optional.ofNullable(id)); }
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

    @Override
    public boolean canStandOnFluid(FluidState fluid) {
        return fluid.is(FluidTags.WATER);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        if (this.isUnderWater()) {
            BlockPos surface = findWaterSurface(level);
            if (surface != null) this.moveTo(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5);
        }
        this.restrictTo(this.blockPosition(), TETHER_RADIUS);
        return super.finalizeSpawn(level, difficulty, spawnType, spawnData);
    }

    private BlockPos findWaterSurface(ServerLevelAccessor level) {
        BlockPos.MutableBlockPos pos = this.blockPosition().mutable();
        while (pos.getY() < level.getMaxBuildHeight()) {
            if (!level.getFluidState(pos).is(FluidTags.WATER)) return pos;
            pos.move(0, 1, 0);
        }
        return null;
    }

    public static AttributeSupplier.Builder createAttributes(){
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,           BASE_HEALTH)
                .add(Attributes.JUMP_STRENGTH,        BASE_JUMP_STRENGTH)
                .add(Attributes.MOVEMENT_SPEED,       BASE_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE,         BASE_FOLLOW_RANGE)
                .add(Attributes.ATTACK_DAMAGE,        BASE_ATTACK_DAMAGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, BASE_KNOCKBACK_RESIST)
                .add(Attributes.SAFE_FALL_DISTANCE,   BASE_SAFE_FALL_DISTANCE)
                .add(Attributes.FALL_DAMAGE_MULTIPLIER, BASE_FALL_DAMAGE_MULTIPLIER);
    }

    // -------------------------------------------------------------------------
    // Attributes
    // -------------------------------------------------------------------------

    private static final double BASE_HEALTH           = 40.0;
    private static final float  BASE_JUMP_STRENGTH    = 0.42f;
    private static final double BASE_MOVEMENT_SPEED   = 0.25;
    private static final double BASE_FOLLOW_RANGE     = 16.0;
    private static final double BASE_ATTACK_DAMAGE    = 1.0;
    private static final double BASE_KNOCKBACK_RESIST      = 0.6;
    private static final int    XP_REWARD                  = 5;
    private static final int    TETHER_RADIUS              = 32;
    private static final double BASE_SAFE_FALL_DISTANCE    = 20.0; // blocks before fall damage starts
    private static final double BASE_FALL_DAMAGE_MULTIPLIER = 0.1; // 90% fall damage reduction

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
    private static final RawAnimation ANIM_FLOAT        = RawAnimation.begin().thenLoop("float");
    private static final RawAnimation ANIM_SLAM_WINDUP  = RawAnimation.begin().thenPlay("slam_windup");
    private static final RawAnimation ANIM_SLAM_IMPACT  = RawAnimation.begin().thenPlay("slam_impact");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Movement controller: float on water, hop when airborne/moving, idle when still
        controllers.add(new AnimationController<>(this, "movement", ANIM_MOVEMENT_TRANSITION, state -> {
            AngrySlime slime = state.getAnimatable();
            if (slime.onGround() && slime.canStandOnFluid(slime.level().getFluidState(slime.blockPosition().below()))) {
                return state.setAndContinue(ANIM_FLOAT);
            }
            if (state.isMoving() || !slime.onGround()) {
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

    private boolean wasOutsideRestriction = false;
    private boolean returningHome = false;

    public boolean isReturningHome() { return returningHome; }
    public void setReturningHome(boolean value) { this.returningHome = value; }

    @Override
    protected void customServerAiStep() {
        if (this.getTarget() instanceof Player p && p.getAbilities().invulnerable) this.setTarget(null);
        if (this.slamCooldown > 0 && this.getTarget() != null) this.slamCooldown--;
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
                new SlimeMoveBehaviour(),
                new SlimeReturnHomeBehaviour()
        );
    }

    @Override
    public BrainActivityGroup<AngrySlime> getIdleTasks() {
        return BrainActivityGroup.idleTasks(
                new FirstApplicableBehaviour<AngrySlime>(
                        new SlimeTargetBehaviour(),
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
                            if (slime.distanceToSqr(target) > followRange * followRange) return true;
                            return !slime.isWithinRestriction();
                        }),
                new FirstApplicableBehaviour<AngrySlime>(
                        new SlimeSlamAttackBehaviour(),
                        new AnimatableMeleeAttack<AngrySlime>(10)
                                .attackInterval(slime -> 60 + slime.getRandom().nextInt(20))
                )
        );
    }
}
