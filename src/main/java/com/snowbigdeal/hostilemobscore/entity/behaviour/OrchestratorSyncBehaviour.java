package com.snowbigdeal.hostilemobscore.entity.behaviour;

import com.mojang.datafixers.util.Pair;
import com.snowbigdeal.hostilemobscore.entity.HostileMob;
import com.snowbigdeal.hostilemobscore.entity.ModMemoryTypes;
import com.snowbigdeal.hostilemobscore.orchestrator.AttackOrchestrator;
import com.snowbigdeal.hostilemobscore.orchestrator.MobParty;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.tslat.smartbrainlib.api.core.behaviour.ExtendedBehaviour;
import net.tslat.smartbrainlib.util.BrainUtils;

import java.util.List;

/**
 * Picks up the party's shared target through the brain system so all target
 * acquisition respects {@link ModMemoryTypes#DEAGGRO_COOLDOWN} and other
 * brain guards.
 *
 * <p>Replaces the direct {@code member.setTarget()} propagation that the
 * {@link AttackOrchestrator} used to perform in {@code onMobTargetChanged}.
 * The orchestrator now only stores state; this behaviour drives the actual
 * target-set.
 *
 * <p>Only runs in IDLE (when there is no current attack target) — guarded by
 * {@code ATTACK_TARGET VALUE_ABSENT} in {@link #getMemoryRequirements()}.
 * {@link ModMemoryTypes#DEAGGRO_COOLDOWN} is checked in
 * {@link #checkExtraStartConditions} so a disengaging mob never re-targets
 * from the party automatically.
 *
 * @param <T> The concrete {@link HostileMob} subclass.
 */
public class OrchestratorSyncBehaviour<T extends HostileMob<T>> extends ExtendedBehaviour<T> {

    private static final List<Pair<MemoryModuleType<?>, MemoryStatus>> MEMORY_REQUIREMENTS =
            List.of(Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT));

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return MEMORY_REQUIREMENTS;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, T entity) {
        if (BrainUtils.hasMemory(entity, ModMemoryTypes.DEAGGRO_COOLDOWN.get())) return false;

        LivingEntity sharedTarget = getSharedTarget(entity, level);
        return sharedTarget != null && sharedTarget.isAlive() && !sharedTarget.isRemoved();
    }

    @Override
    protected void start(T entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        LivingEntity sharedTarget = getSharedTarget(entity, level);
        if (sharedTarget == null || !sharedTarget.isAlive()) return;

        BrainUtils.setMemory(entity, MemoryModuleType.ATTACK_TARGET, sharedTarget);
        entity.setTarget(sharedTarget);

        // Seed the deaggro timer so DeaggroBehaviour starts for this mob.
        // Without this, mobs that pick up the party target here (rather than through
        // TetheredTargetBehaviour or a direct hit) would never have HIT_TIMER set
        // and would never deaggro.
        if (!BrainUtils.hasMemory(entity, ModMemoryTypes.HIT_TIMER.get())) {
            BrainUtils.setMemory(entity, ModMemoryTypes.HIT_TIMER.get(), HostileMob.HIT_TIMER_MAX);
        }
    }

    private LivingEntity getSharedTarget(T entity, ServerLevel level) {
        if (entity.getPartyId() == null) return null;
        MobParty party = AttackOrchestrator.get(level).getParty(entity.getPartyId());
        if (party == null) return null;
        return party.getSharedTarget();
    }
}
