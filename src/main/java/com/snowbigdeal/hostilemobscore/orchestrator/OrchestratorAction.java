package com.snowbigdeal.hostilemobscore.orchestrator;

import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/**
 * Represents a single active action assignment from the orchestrator to a mob.
 * Tracks the action, its target, and the current execution status.
 */
public class OrchestratorAction {

    public enum Status { PENDING, RUNNING, COMPLETE, CANCELLED }

    private final UUID assignmentId;
    private final IMobAction mobAction;
    private final LivingEntity target;
    private Status status;

    public OrchestratorAction(IMobAction mobAction, LivingEntity target) {
        this.assignmentId = UUID.randomUUID();
        this.mobAction    = mobAction;
        this.target       = target;
        this.status       = Status.PENDING;
    }

    public UUID getAssignmentId()    { return assignmentId; }
    public IMobAction getMobAction() { return mobAction; }
    public LivingEntity getTarget()  { return target; }
    public Status getStatus()        { return status; }
    public void setStatus(Status status) { this.status = status; }

    public boolean isTerminal() {
        return status == Status.COMPLETE || status == Status.CANCELLED;
    }
}
