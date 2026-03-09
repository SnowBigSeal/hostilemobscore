package com.snowbigdeal.hostilemobscore.entity.slimes;

import com.snowbigdeal.hostilemobscore.entity.ModMemoryTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.TargetOrRetaliate;
import net.tslat.smartbrainlib.util.BrainUtils;
import org.jetbrains.annotations.Nullable;

/**
 * Targeting behaviour for tethered hostile mobs. Extends TargetOrRetaliate and gates
 * all target acquisition (initial + retaliation) through a single predicate so
 * nothing can sneak past: creative/spectator players and targets outside the
 * tether radius are never set or kept.
 */
public class TetheredTargetBehaviour<T extends BaseSlime<T>> extends TargetOrRetaliate<T> {

    public TetheredTargetBehaviour() {
        attackablePredicate(target ->
            target instanceof Player player
            && target.isAlive()
            && !player.getAbilities().invulnerable);
    }

    @Override
    @Nullable
    protected LivingEntity getTarget(T owner, ServerLevel level, @Nullable LivingEntity existingTarget) {
        LivingEntity candidate = super.getTarget(owner, level, existingTarget);
        if (candidate == null) return null;
        if (!owner.isWithinRestriction()) return null;
        return candidate;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, T owner) {
        if (!owner.isWithinRestriction()) return false;
        if (owner.isReturningHome()) return false;
        if (BrainUtils.hasMemory(owner, ModMemoryTypes.DEAGGRO_COOLDOWN.get())) return false;
        return super.checkExtraStartConditions(level, owner);
    }

    @Override
    protected void start(T entity) {
        super.start(entity);
        // Ensure brain memory stays in sync with what we allow
        LivingEntity target = BrainUtils.getTargetOfEntity(entity);
        if (target instanceof Player player && player.getAbilities().invulnerable) {
            BrainUtils.clearMemory(entity, MemoryModuleType.ATTACK_TARGET);
            entity.setTarget(null);
        }
    }
}
