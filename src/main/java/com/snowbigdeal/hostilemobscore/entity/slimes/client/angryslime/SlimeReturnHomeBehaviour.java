package com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime;

import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.tslat.smartbrainlib.api.core.behaviour.ExtendedBehaviour;
import net.tslat.smartbrainlib.util.BrainUtils;

import java.util.List;

/**
 * Drives the slime back to its tether anchor when it has strayed outside the
 * tether radius. While active the slime is invulnerable and all target
 * acquisition is blocked. When the slime reaches within
 * {@link #HOME_DIST_SQ} blocks of the anchor it heals to full and the
 * behaviour ends.
 */
public class SlimeReturnHomeBehaviour extends ExtendedBehaviour<AngrySlime> {

    /** Distance² from anchor at which the slime is considered "home". */
    private static final double HOME_DIST_SQ = 25.0; // 5 blocks²

    private static final double RETURN_SPEED = 2.5;

    public SlimeReturnHomeBehaviour() {
        noTimeout();
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, AngrySlime slime) {
        return !slime.isWithinRestriction();
    }

    @Override
    protected boolean shouldKeepRunning(AngrySlime slime) {
        return slime.blockPosition().distSqr(slime.getRestrictCenter()) > HOME_DIST_SQ;
    }

    @Override
    protected void start(AngrySlime slime) {
        slime.setReturningHome(true);
        slime.setInvulnerable(true);
        slime.setTarget(null);
        BrainUtils.clearMemory(slime, MemoryModuleType.ATTACK_TARGET);
    }

    @Override
    protected void tick(AngrySlime slime) {
        // Keep target cleared while returning
        if (slime.getTarget() != null) {
            slime.setTarget(null);
            BrainUtils.clearMemory(slime, MemoryModuleType.ATTACK_TARGET);
        }

        if (!(slime.getMoveControl() instanceof SlimeMoveControl smc)) return;

        double dx = slime.getRestrictCenter().getX() + 0.5 - slime.getX();
        double dz = slime.getRestrictCenter().getZ() + 0.5 - slime.getZ();
        float yRot = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        smc.setDirection(yRot, true);
        smc.setWantedMovement(RETURN_SPEED);
    }

    @Override
    protected void stop(AngrySlime slime) {
        slime.setReturningHome(false);
        slime.setHealth(slime.getMaxHealth());
        slime.setInvulnerable(false);
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return List.of();
    }
}
