package com.snowbigdeal.hostilemobscore.orchestrator;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Represents a discrete action a mob can perform, as registered with the orchestrator.
 * The orchestrator calls these methods to query availability and drive execution.
 */
public interface IMobAction {

    /** Unique identifier for this action (e.g. "slam"). */
    String getId();

    /** Returns true if the mob is able to begin this action right now. */
    boolean isReady(Mob mob);

    /** Instructs the mob to begin executing this action toward the given target. */
    void beginAction(Mob mob, LivingEntity target);

    /** Returns true once the action has fully completed (including recovery). */
    boolean isComplete(Mob mob);

    /** Returns the remaining cooldown in ticks (0 = ready). Used by the orchestrator for scheduling. */
    int getCooldownTicks(Mob mob);

    // -------------------------------------------------------------------------
    // Optional coordination hooks — called by the orchestrator before beginAction.
    // Actions that need wave-level positioning override these; others use the no-ops.
    // -------------------------------------------------------------------------

    /**
     * Called when the orchestrator assigns this action to the wave's lead mob.
     * Should compute and apply the lead's execution position to the mob, then
     * return that position so the orchestrator can store it for followers.
     * Return null if this action has no wave coordination.
     */
    default Vec3 computeLeadPosition(Mob mob, LivingEntity target) { return null; }

    /**
     * Called when the orchestrator assigns this action to a follower mob.
     * {@code leadPosition} is the value returned by the lead's {@link #computeLeadPosition}.
     * {@code target} is the shared party target at dispatch time (already moved since wave start).
     * Should compute and apply the follower's execution position to the mob.
     */
    default void applyFollowerOffset(Mob mob, Vec3 leadPosition, LivingEntity target) {}
}
