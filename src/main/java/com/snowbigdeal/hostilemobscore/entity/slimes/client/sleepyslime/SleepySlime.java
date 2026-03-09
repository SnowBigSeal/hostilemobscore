package com.snowbigdeal.hostilemobscore.entity.slimes.client.sleepyslime;

import com.snowbigdeal.hostilemobscore.entity.ModEntities;
import com.snowbigdeal.hostilemobscore.entity.slimes.BaseSlime;
import com.snowbigdeal.hostilemobscore.orchestrator.IMobAction;
import net.minecraft.sounds.SoundEvents;
import java.util.List;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.tslat.smartbrainlib.api.core.BrainActivityGroup;
import net.tslat.smartbrainlib.api.core.behaviour.FirstApplicableBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.custom.attack.AnimatableMeleeAttack;
import net.tslat.smartbrainlib.api.core.behaviour.custom.misc.Idle;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.InvalidateAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetRandomLookTarget;

public class SleepySlime extends BaseSlime<SleepySlime> {

    // -------------------------------------------------------------------------
    // Attributes
    // -------------------------------------------------------------------------

    private static final double BASE_HEALTH               = 30.0;
    private static final float  BASE_JUMP_STRENGTH        = 0.42f;
    private static final double BASE_MOVEMENT_SPEED       = 0.18;
    private static final double BASE_FOLLOW_RANGE         = 16.0;
    private static final double BASE_ATTACK_DAMAGE        = 2.0;
    private static final double BASE_KNOCKBACK_RESIST     = 0.4;
    private static final double BASE_SAFE_FALL_DISTANCE   = 20.0;
    private static final double BASE_FALL_DAMAGE_MULTIPLIER = 0.1;
    private static final int    XP_REWARD                 = 3;
    private static final int    TETHER_RADIUS             = 48;

    public int coneCooldown = 0;

    private boolean orchestratorConePending  = false;
    private boolean orchestratorConeFinished = false;

    public void grantOrchestratedCone() {
        this.orchestratorConePending  = true;
        this.orchestratorConeFinished = false;
    }

    public void notifyOrchestratedConeComplete() {
        this.orchestratorConePending  = false;
        this.orchestratorConeFinished = true;
    }

    public boolean isConeAttackPending()     { return orchestratorConePending; }
    public boolean isOrchestratedConeFinished() { return orchestratorConeFinished; }

    public SleepySlime(EntityType<? extends SleepySlime> entityType, Level level) {
        super(entityType, level);
        xpReward = XP_REWARD;
    }

    @Override
    protected int getTetherRadius() { return TETHER_RADIUS; }

    @Override
    protected EntityType<? extends BaseSlime<?>> getCompanionType() {
        return ModEntities.ANGRY_SLIME.get();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH,             BASE_HEALTH)
                .add(Attributes.JUMP_STRENGTH,          BASE_JUMP_STRENGTH)
                .add(Attributes.MOVEMENT_SPEED,         BASE_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE,           BASE_FOLLOW_RANGE)
                .add(Attributes.ATTACK_DAMAGE,          BASE_ATTACK_DAMAGE)
                .add(Attributes.KNOCKBACK_RESISTANCE,   BASE_KNOCKBACK_RESIST)
                .add(Attributes.SAFE_FALL_DISTANCE,     BASE_SAFE_FALL_DISTANCE)
                .add(Attributes.FALL_DAMAGE_MULTIPLIER, BASE_FALL_DAMAGE_MULTIPLIER);
    }

    @Override
    public List<IMobAction> getMobActions() { return List.of(new ConeMobAction()); }

    // -------------------------------------------------------------------------
    // Cooldown tick-down
    // -------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (coneCooldown > 0 && this.getTarget() != null) coneCooldown--;
    }

    // -------------------------------------------------------------------------
    // Sounds
    // -------------------------------------------------------------------------

    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) { return SoundEvents.SLIME_HURT; }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return SoundEvents.SLIME_DEATH; }

    // -------------------------------------------------------------------------
    // Passive-until-attacked: set attack target when hurt by a player
    // -------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (damaged && source.getEntity() instanceof Player player
                && !player.getAbilities().invulnerable) {
            getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, player);
            setTarget(player);
        }
        return damaged;
    }

    // -------------------------------------------------------------------------
    // Brain / AI — override idle tasks to remove proactive targeting
    // -------------------------------------------------------------------------

    @Override
    public BrainActivityGroup<SleepySlime> getIdleTasks() {
        return BrainActivityGroup.idleTasks(
                new FirstApplicableBehaviour<SleepySlime>(
                        new SetRandomLookTarget<>()),
                new Idle<>().runFor(entity -> entity.getRandom().nextInt(40, 80)));
    }

    @Override
    public BrainActivityGroup<SleepySlime> getFightTasks() {
        return BrainActivityGroup.fightTasks(
                new InvalidateAttackTarget<SleepySlime>().invalidateIf(
                        (slime, target) -> {
                            if (slime.isConeAttackPending()) return false;
                            if (target instanceof Player player && player.getAbilities().invulnerable) return true;
                            double followRange = slime.getAttributeValue(Attributes.FOLLOW_RANGE);
                            if (slime.distanceToSqr(target) > followRange * followRange) return true;
                            return !slime.isWithinRestriction();
                        }),
                getAttackBehaviours()
        );
    }

    @Override
    protected FirstApplicableBehaviour<SleepySlime> getAttackBehaviours() {
        return new FirstApplicableBehaviour<>(
                new SleepyConeAttackBehaviour(),
                new AnimatableMeleeAttack<SleepySlime>(10)
                        .attackInterval(slime -> 60 + slime.getRandom().nextInt(20))
        );
    }
}

