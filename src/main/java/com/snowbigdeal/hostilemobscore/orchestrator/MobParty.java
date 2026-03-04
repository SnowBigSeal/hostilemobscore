package com.snowbigdeal.hostilemobscore.orchestrator;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A group of mobs coordinated by the {@link AttackOrchestrator}.
 * Tracks membership, the shared attack target, per-member action assignments,
 * and a round-robin attack queue that the orchestrator pulls from every second.
 */
public class MobParty {

    /** Sentinel UUID placed in the queue to represent a no-op (artificial delay slot). */
    public static final UUID NOOP_ID = new UUID(0L, 0L);

    private final UUID partyId;
    private final Map<UUID, Mob>                members           = new LinkedHashMap<>();
    private final Map<UUID, OrchestratorAction> activeAssignments = new HashMap<>();
    private final Deque<UUID>                   attackQueue       = new ArrayDeque<>();

    private LivingEntity sharedTarget;
    private int queuePullIn = 0; // ticks until the next queue pull; counts down each tick

    public MobParty(UUID partyId) {
        this.partyId = partyId;
    }

    public UUID getPartyId() { return partyId; }

    public void addMember(Mob mob) {
        members.put(mob.getUUID(), mob);
    }

    public void removeMember(UUID mobId) {
        members.remove(mobId);
        activeAssignments.remove(mobId);
        // Stale queue entries for removed mobs are silently skipped on pull.
    }

    public boolean isEmpty()  { return members.isEmpty(); }
    public boolean isIdle()   { return activeAssignments.isEmpty(); }

    public Map<UUID, Mob> getMembers() { return Collections.unmodifiableMap(members); }

    // -------------------------------------------------------------------------
    // Target
    // -------------------------------------------------------------------------

    public LivingEntity getSharedTarget()              { return sharedTarget; }
    public void         setSharedTarget(LivingEntity t) { this.sharedTarget = t; }

    // -------------------------------------------------------------------------
    // Attack queue
    // -------------------------------------------------------------------------

    public void enqueue(UUID mobId) { attackQueue.addLast(mobId); }
    public void enqueueNoop()       { attackQueue.addLast(NOOP_ID); }

    /** Removes and returns the front entry, or {@code null} if the queue is empty. */
    public UUID pollQueue() { return attackQueue.pollFirst(); }

    // -------------------------------------------------------------------------
    // Queue pull countdown
    // -------------------------------------------------------------------------

    /** Decrements the pull-in counter and returns the new value. */
    public int  decrementQueuePullIn()    { return --queuePullIn; }
    public void resetQueuePullIn(int ticks) { this.queuePullIn = ticks; }

    // -------------------------------------------------------------------------
    // Assignments
    // -------------------------------------------------------------------------

    public OrchestratorAction getActiveAssignment(UUID mobId)                     { return activeAssignments.get(mobId); }
    public void               setActiveAssignment(UUID mobId, OrchestratorAction a) { activeAssignments.put(mobId, a); }
    public void               clearActiveAssignment(UUID mobId)                    { activeAssignments.remove(mobId); }

    /** Absorbs all members, assignments, and queue entries from {@code other} into this party. */
    public void merge(MobParty other) {
        members.putAll(other.members);
        activeAssignments.putAll(other.activeAssignments);
        attackQueue.addAll(other.attackQueue);
    }
}
