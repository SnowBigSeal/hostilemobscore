package com.snowbigdeal.hostilemobscore.entity.slimes.client.sleepyslime;

import com.mojang.datafixers.util.Pair;
import com.snowbigdeal.hostilemobscore.Constants;
import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import com.snowbigdeal.hostilemobscore.attack.AttackSnapshot;
import com.snowbigdeal.hostilemobscore.attack.shape.ConeShape;
import com.snowbigdeal.hostilemobscore.attack.shape.TelegraphAttackShape;
import com.snowbigdeal.hostilemobscore.entity.ModMemoryTypes;
import com.snowbigdeal.hostilemobscore.entity.behaviour.TelegraphAttackBehaviour;
import com.snowbigdeal.hostilemobscore.entity.slimes.HoppingMoveControl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.util.BrainUtils;

import java.util.List;

/**
 * Telegraphed cone AoE attack. During wind-up the slime faces its target and a cone
 * indicator is shown. On impact, all players inside the cone receive a sleep cocktail:
 * Blindness, Darkness, and Slowness III.
 */
public class SleepyConeAttackBehaviour extends TelegraphAttackBehaviour<SleepySlime> {

    private static final ResourceLocation KB_RESIST_ID =
            ResourceLocation.fromNamespaceAndPath(HostileMobsCore.MODID, "cone_attack_kb_resist");
    private static final AttributeModifier KB_RESIST_MODIFIER =
            new AttributeModifier(KB_RESIST_ID, 1.0, AttributeModifier.Operation.ADD_VALUE);

    private static final float CONE_LENGTH      = 8f;
    private static final float CONE_HALF_ANGLE  = 45f;    // 90° total opening

    private static final int   WINDUP_TICKS     = Constants.seconds(1);
    private static final int   RECOVERY_TICKS   = 10;
    private static final int   COOLDOWN_TICKS   = Constants.seconds(15);
    private static final int   BEHAVIOUR_TIMEOUT = Constants.seconds(5);

    // Effect durations
    private static final int   BLINDNESS_TICKS  = Constants.seconds(3);
    private static final int   DARKNESS_TICKS   = Constants.seconds(5);
    private static final int   SLOWNESS_TICKS   = Constants.seconds(5);

    public SleepyConeAttackBehaviour() {
        runFor(entity -> BEHAVIOUR_TIMEOUT);
    }

    // -------------------------------------------------------------------------
    // TelegraphAttackBehaviour — timing
    // -------------------------------------------------------------------------

    @Override protected int windupTicks()   { return WINDUP_TICKS; }
    @Override protected int recoveryTicks() { return RECOVERY_TICKS; }

    // -------------------------------------------------------------------------
    // TelegraphAttackBehaviour — start conditions
    // -------------------------------------------------------------------------

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return List.of(
                Pair.of(MemoryModuleType.ATTACK_TARGET,    MemoryStatus.VALUE_PRESENT),
                Pair.of(ModMemoryTypes.CONE_COOLDOWN.get(), MemoryStatus.VALUE_ABSENT),
                Pair.of(ModMemoryTypes.CONE_PENDING.get(),  MemoryStatus.VALUE_PRESENT)
        );
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, SleepySlime slime) {
        if (!(slime.getTarget() instanceof Player player)) return false;
        if (!slime.onGround() || slime.isInWater()) return false;
        if (slime.distanceTo(player) > CONE_LENGTH) return false;
        return slime.hasLineOfSight(player);
    }

    // -------------------------------------------------------------------------
    // TelegraphAttackBehaviour — shape
    // -------------------------------------------------------------------------

    @Override
    protected TelegraphAttackShape buildShape(SleepySlime slime) {
        Vec3 apex = slime.position();
        Vec3 direction = slime.getTarget() != null
                ? slime.getTarget().position().subtract(apex).normalize()
                : new Vec3(Math.sin(-slime.getYRot() * Math.PI / 180.0), 0,
                           Math.cos(-slime.getYRot() * Math.PI / 180.0));
        return new ConeShape(apex, direction, CONE_LENGTH, CONE_HALF_ANGLE);
    }

    // -------------------------------------------------------------------------
    // TelegraphAttackBehaviour — lifecycle hooks
    // -------------------------------------------------------------------------

    @Override
    protected void onStart(SleepySlime slime) {
        BrainUtils.setMemory(slime, ModMemoryTypes.CONE_COOLDOWN.get(), COOLDOWN_TICKS);
        if (slime.getMoveControl() instanceof HoppingMoveControl smc) smc.setAttackLock(true);
        var attr = slime.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attr != null) attr.addOrUpdateTransientModifier(KB_RESIST_MODIFIER);
    }

    @Override
    protected void onWindupTick(SleepySlime slime, int remainingTicks) {
        if (slime.getTarget() != null) {
            slime.getLookControl().setLookAt(slime.getTarget());
        }
    }

    @Override
    protected void onImpact(SleepySlime slime, AttackSnapshot<Player> snapshot) {
        for (Player player : snapshot.targets()) {
            player.hurt(slime.damageSources().mobAttack(slime), 4.0f);
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,   BLINDNESS_TICKS, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS,    DARKNESS_TICKS,  0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_TICKS, 2, false, true));
        }
    }

    @Override
    protected void onStop(SleepySlime slime) {
        if (slime.getMoveControl() instanceof HoppingMoveControl smc) smc.setAttackLock(false);
        var attr = slime.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attr != null) attr.removeModifier(KB_RESIST_ID);
        BrainUtils.clearMemory(slime, ModMemoryTypes.CONE_PENDING.get());
    }
}
