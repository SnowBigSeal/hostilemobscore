package com.snowbigdeal.hostilemobscore.entity.slimes.client.sleepyslime;

import com.snowbigdeal.hostilemobscore.entity.ModMemoryTypes;
import com.snowbigdeal.hostilemobscore.orchestrator.IMobAction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.tslat.smartbrainlib.util.BrainUtils;

/**
 * Registers the cone attack as an orchestrator-managed action for {@link SleepySlime}.
 * The orchestrator calls {@link #beginAction} when the slime's turn comes up in the
 * party queue and its {@link ModMemoryTypes#CONE_COOLDOWN} memory has expired.
 */
public class ConeMobAction implements IMobAction {

    public static final String ID = "cone";

    @Override
    public String getId() { return ID; }

    @Override
    public boolean isReady(Mob mob) {
        return !BrainUtils.hasMemory(mob, ModMemoryTypes.CONE_COOLDOWN.get()) && mob.onGround();
    }

    @Override
    public void beginAction(Mob mob, LivingEntity target) {
        BrainUtils.setMemory(mob, ModMemoryTypes.CONE_PENDING.get(), true);
    }

    @Override
    public boolean isComplete(Mob mob) {
        return !BrainUtils.hasMemory(mob, ModMemoryTypes.CONE_PENDING.get());
    }

    @Override
    public int getCooldownTicks(Mob mob) {
        Integer remaining = BrainUtils.getMemory(mob, ModMemoryTypes.CONE_COOLDOWN.get());
        return remaining != null ? remaining : 0;
    }
}
