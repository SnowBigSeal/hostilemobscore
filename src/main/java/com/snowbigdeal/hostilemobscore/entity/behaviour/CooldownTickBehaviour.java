package com.snowbigdeal.hostilemobscore.entity.behaviour;

import com.mojang.datafixers.util.Pair;
import com.snowbigdeal.hostilemobscore.entity.HostileMob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.tslat.smartbrainlib.api.core.behaviour.ExtendedBehaviour;
import net.tslat.smartbrainlib.util.BrainUtils;

import java.util.List;
import java.util.function.Supplier;

/**
 * CORE behaviour that decrements an {@link Integer} brain memory by 1 each tick,
 * clearing it when it reaches zero. Only runs while the mob has an
 * {@link MemoryModuleType#ATTACK_TARGET}, so the cooldown pauses when the mob
 * is not in combat (matching the semantics of the old per-field decrement that
 * checked {@code getTarget() != null}).
 *
 * <p>Usage (in {@code getCoreTasks()}):
 * <pre>
 *   new CooldownTickBehaviour<>(ModMemoryTypes.SLAM_COOLDOWN)
 * </pre>
 *
 * @param <T> The concrete {@link HostileMob} subclass.
 */
public class CooldownTickBehaviour<T extends HostileMob<T>> extends ExtendedBehaviour<T> {

    private final Supplier<MemoryModuleType<Integer>> cooldownMemory;

    public CooldownTickBehaviour(Supplier<MemoryModuleType<Integer>> cooldownMemory) {
        noTimeout();
        this.cooldownMemory = cooldownMemory;
    }

    // getMemoryRequirements() is called from ExtendedBehaviour's constructor before
    // this subclass's fields are assigned, so it must NOT reference cooldownMemory.
    // We enforce the cooldown memory presence in checkExtraStartConditions() instead.
    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return List.of(Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, T entity) {
        return BrainUtils.hasMemory(entity, cooldownMemory.get());
    }

    @Override
    protected boolean shouldKeepRunning(T entity) {
        if (!BrainUtils.hasMemory(entity, MemoryModuleType.ATTACK_TARGET)) return false;
        Integer remaining = BrainUtils.getMemory(entity, cooldownMemory.get());
        return remaining != null && remaining > 0;
    }

    @Override
    protected void tick(ServerLevel level, T entity, long gameTime) {
        Integer remaining = BrainUtils.getMemory(entity, cooldownMemory.get());
        if (remaining == null || remaining <= 1) {
            BrainUtils.clearMemory(entity, cooldownMemory.get());
        } else {
            BrainUtils.setMemory(entity, cooldownMemory.get(), remaining - 1);
        }
    }
}
