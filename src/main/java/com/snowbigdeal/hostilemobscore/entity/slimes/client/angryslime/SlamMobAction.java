package com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime;

import com.snowbigdeal.hostilemobscore.entity.ModMemoryTypes;
import com.snowbigdeal.hostilemobscore.orchestrator.IMobAction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.tslat.smartbrainlib.util.BrainUtils;

/**
 * Registers the slam attack as an orchestrator-managed action for {@link AngrySlime}.
 * The orchestrator calls {@link #beginAction} when the slime's turn comes up in the party
 * queue and its {@link ModMemoryTypes#SLAM_COOLDOWN} memory has expired.
 */
public class SlamMobAction implements IMobAction {

    public static final String ID = "slam";

    @Override
    public String getId() { return ID; }

    @Override
    public boolean isReady(Mob mob) {
        return !BrainUtils.hasMemory(mob, ModMemoryTypes.SLAM_COOLDOWN.get()) && mob.onGround();
    }

    @Override
    public void beginAction(Mob mob, LivingEntity target) {
        BrainUtils.setMemory(mob, ModMemoryTypes.SLAM_PENDING.get(), true);
    }

    @Override
    public boolean isComplete(Mob mob) {
        return !BrainUtils.hasMemory(mob, ModMemoryTypes.SLAM_PENDING.get());
    }

    @Override
    public int getCooldownTicks(Mob mob) {
        return BrainUtils.hasMemory(mob, ModMemoryTypes.SLAM_COOLDOWN.get()) ? 1 : 0;
    }
}
