package com.snowbigdeal.hostilemobscore.entity.slimes;

import com.snowbigdeal.hostilemobscore.entity.behaviour.ReturnHomeBehaviour;

/**
 * Slime-specific return-home behaviour. Delegates all lifecycle management
 * (invulnerability, healing, target clearing) to {@link ReturnHomeBehaviour}
 * and only overrides movement to drive {@link SlimeMoveControl}.
 */
public class SlimeReturnHomeBehaviour<T extends BaseSlime<T>> extends ReturnHomeBehaviour<T> {

    private static final double RETURN_SPEED = 2.5;

    @Override
    protected void applyReturnMovement(T slime) {
        if (!(slime.getMoveControl() instanceof SlimeMoveControl smc)) return;
        double dx = slime.getRestrictCenter().getX() + 0.5 - slime.getX();
        double dz = slime.getRestrictCenter().getZ() + 0.5 - slime.getZ();
        float yRot = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        smc.setDirection(yRot, true);
        smc.setWantedMovement(RETURN_SPEED);
    }
}
