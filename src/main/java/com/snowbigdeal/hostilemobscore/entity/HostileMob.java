package com.snowbigdeal.hostilemobscore.entity;

import com.snowbigdeal.hostilemobscore.entity.ModMemoryTypes;
import com.snowbigdeal.hostilemobscore.orchestrator.IMobAction;
import com.snowbigdeal.hostilemobscore.orchestrator.IOrchestrated;
import com.snowbigdeal.hostilemobscore.orchestrator.OrchestratorAction;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.SmartBrainProvider;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.HurtBySensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyLivingEntitySensor;
import net.tslat.smartbrainlib.util.BrainUtils;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstract base class for all Hostile Mobs Core entities.
 * Provides shared infrastructure that every mob in this mod requires:
 * <ul>
 *   <li>GeckoLib animation boilerplate</li>
 *   <li>SmartBrainLib brain provider</li>
 *   <li>IOrchestrated (party / pending-action) state</li>
 *   <li>Synced tether centre and party ID for client-side debug rendering</li>
 *   <li>Return-home state backed by {@link ModMemoryTypes#RETURNING_HOME} brain memory</li>
 *   <li>Default sensors (NearbyLiving + HurtBy)</li>
 *   <li>finalizeSpawn: sets tether anchor on first spawn</li>
 *   <li>customServerAiStep: drops creative-player targets then ticks brain</li>
 * </ul>
 *
 * @param <T> The concrete subclass (F-bounded for SmartBrainOwner).
 */
public abstract class HostileMob<T extends HostileMob<T>> extends Mob
        implements GeoEntity, SmartBrainOwner<T>, IOrchestrated {

    // -------------------------------------------------------------------------
    // Synced entity data
    // -------------------------------------------------------------------------

    private static final EntityDataAccessor<BlockPos> DATA_TETHER_CENTER =
            SynchedEntityData.defineId(HostileMob.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Optional<UUID>> DATA_PARTY_ID =
            SynchedEntityData.defineId(HostileMob.class, EntityDataSerializers.OPTIONAL_UUID);

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
    // GeckoLib
    // -------------------------------------------------------------------------

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }

    // -------------------------------------------------------------------------
    // SmartBrainLib
    // -------------------------------------------------------------------------

    private Brain.Provider<?> brainProvider;

    @Override
    protected Brain.Provider<?> brainProvider() {
        if (brainProvider == null) brainProvider = new SmartBrainProvider<T>(typedSelf());
        return brainProvider;
    }

    @Override
    public List<? extends ExtendedSensor<? extends T>> getSensors() {
        return List.of(new NearbyLivingEntitySensor<>(), new HurtBySensor<>());
    }

    // -------------------------------------------------------------------------
    // IOrchestrated
    // -------------------------------------------------------------------------

    private UUID partyId = null;
    private OrchestratorAction pendingAction = null;

    @Override public UUID             getPartyId()                    { return partyId; }
    @Override public void             setPartyId(UUID id)             { this.partyId = id; this.entityData.set(DATA_PARTY_ID, Optional.ofNullable(id)); }
    @Override public OrchestratorAction getPendingAction()            { return pendingAction; }
    @Override public void             setPendingAction(OrchestratorAction a) { this.pendingAction = a; }

    // -------------------------------------------------------------------------
    // Return-home constants
    // -------------------------------------------------------------------------

    /** Distance² (blocks²) from anchor at which disengagement triggers a return-home walk. */
    private static final double DISENGAGE_DIST_SQ = 256.0; // 16 blocks
    /** Ticks without a player hit before the mob disengages (30 seconds). */
    private static final int    HIT_TIMER_MAX     = 600;

    // -------------------------------------------------------------------------
    // Return-home state — backed by the RETURNING_HOME brain memory so the
    // SmartBrainLib activity system can gate and run ReturnHomeBehaviour.
    // -------------------------------------------------------------------------

    private int lastPlayerHitTimer = 0;

    public boolean isReturningHome() {
        return BrainUtils.hasMemory(this, ModMemoryTypes.RETURNING_HOME.get());
    }

    public void setReturningHome(boolean v) {
        if (v) {
            BrainUtils.setMemory(this, ModMemoryTypes.RETURNING_HOME.get(), true);
        } else {
            BrainUtils.clearMemory(this, ModMemoryTypes.RETURNING_HOME.get());
        }
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    protected HostileMob(EntityType<? extends T> entityType, Level level) {
        super(entityType, level);
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    private static final String NBT_TETHER_X      = "HMCTetherX";
    private static final String NBT_TETHER_Y      = "HMCTetherY";
    private static final String NBT_TETHER_Z      = "HMCTetherZ";
    private static final String NBT_TETHER_RADIUS = "HMCTetherRadius";

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.hasRestriction()) {
            BlockPos center = this.getRestrictCenter();
            tag.putInt(NBT_TETHER_X, center.getX());
            tag.putInt(NBT_TETHER_Y, center.getY());
            tag.putInt(NBT_TETHER_Z, center.getZ());
            tag.putFloat(NBT_TETHER_RADIUS, this.getRestrictRadius());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(NBT_TETHER_X)) {
            BlockPos center = new BlockPos(
                tag.getInt(NBT_TETHER_X),
                tag.getInt(NBT_TETHER_Y),
                tag.getInt(NBT_TETHER_Z)
            );
            this.restrictTo(center, (int) tag.getFloat(NBT_TETHER_RADIUS));
        }
    }

    /**
     * Sets the tether anchor on first spawn. Subclasses should call
     * {@code super.finalizeSpawn()} and may add additional spawn setup.
     */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        this.restrictTo(this.blockPosition(), getTetherRadius());
        return super.finalizeSpawn(level, difficulty, spawnType, spawnData);
    }

    /**
     * Fallback for entities that have no saved tether data (e.g. spawned before
     * tether persistence was added). Sets the tether at the current position so
     * the mob is always constrained even on its first reload.
     */
    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!this.level().isClientSide() && !this.hasRestriction()) {
            this.restrictTo(this.blockPosition(), getTetherRadius());
        }
    }

    /** The tether radius in blocks for this mob type. */
    protected abstract int getTetherRadius();

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (damaged && source.getEntity() instanceof Player p && !p.getAbilities().invulnerable) {
            lastPlayerHitTimer = HIT_TIMER_MAX;
        }
        return damaged;
    }

    @Override
    protected void customServerAiStep() {
        // Last-hit timer: disengage if a player hasn't damaged this mob in 30 seconds
        if (lastPlayerHitTimer > 0) {
            if (getTarget() == null) {
                lastPlayerHitTimer = 0;
            } else if (--lastPlayerHitTimer == 0) {
                BrainUtils.clearMemory(this, MemoryModuleType.ATTACK_TARGET);
                setTarget(null);
                if (distanceToSqr(Vec3.atCenterOf(getRestrictCenter())) > DISENGAGE_DIST_SQ) {
                    setReturningHome(true);
                    setInvulnerable(true);
                }
            }
        }

        if (this.getTarget() instanceof Player p && p.getAbilities().invulnerable) {
            this.setTarget(null);
        }
        tickBrain(typedSelf());
    }

    /**
     * Called each tick while this mob is returning to its tether anchor.
     * Drive your movement controller toward {@link #getRestrictCenter()} here.
     */
    public abstract void applyReturnMovement();

    /** Type-safe self-reference for SmartBrainOwner. */
    @SuppressWarnings("unchecked")
    protected T typedSelf() { return (T) this; }
}
