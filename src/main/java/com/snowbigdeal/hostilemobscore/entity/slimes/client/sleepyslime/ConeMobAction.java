package com.snowbigdeal.hostilemobscore.entity.slimes.client.sleepyslime;

import com.snowbigdeal.hostilemobscore.orchestrator.IMobAction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Registers the cone attack as an orchestrator-managed action for {@link SleepySlime}.
 * The orchestrator calls {@link #beginAction} when the slime's turn comes up in the
 * party queue and its cone cooldown has reached zero.
 */
public class ConeMobAction implements IMobAction {

    public static final String ID = "cone";

    @Override
    public String getId() { return ID; }

    @Override
    public boolean isReady(Mob mob) {
        if (!(mob instanceof SleepySlime slime)) return false;
        return slime.coneCooldown == 0 && slime.onGround();
    }

    @Override
    public void beginAction(Mob mob, LivingEntity target) {
        if (mob instanceof SleepySlime slime) {
            slime.grantOrchestratedCone();
        }
    }

    @Override
    public boolean isComplete(Mob mob) {
        if (!(mob instanceof SleepySlime slime)) return false;
        return slime.isOrchestratedConeFinished();
    }

    @Override
    public int getCooldownTicks(Mob mob) {
        if (!(mob instanceof SleepySlime slime)) return 0;
        return slime.coneCooldown;
    }
}
