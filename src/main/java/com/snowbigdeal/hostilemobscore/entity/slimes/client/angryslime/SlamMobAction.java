package com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime;

import com.snowbigdeal.hostilemobscore.orchestrator.IMobAction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Registers the slam attack as an orchestrator-managed action for {@link AngrySlime}.
 * The orchestrator calls {@link #beginAction} when the slime's turn comes up in the party
 * queue and its internal cooldown ({@link AngrySlime#slamCooldown}) has reached zero.
 */
public class SlamMobAction implements IMobAction {

    public static final String ID = "slam";

    @Override
    public String getId() { return ID; }

    @Override
    public boolean isReady(Mob mob) {
        if (!(mob instanceof AngrySlime slime)) return false;
        return slime.slamCooldown == 0 && slime.onGround();
    }

    @Override
    public void beginAction(Mob mob, LivingEntity target) {
        if (mob instanceof AngrySlime slime) {
            slime.grantOrchestratedSlam();
        }
    }

    @Override
    public boolean isComplete(Mob mob) {
        if (!(mob instanceof AngrySlime slime)) return false;
        return slime.isOrchestratedSlamFinished();
    }

    @Override
    public int getCooldownTicks(Mob mob) {
        if (!(mob instanceof AngrySlime slime)) return 0;
        return slime.slamCooldown;
    }
}
