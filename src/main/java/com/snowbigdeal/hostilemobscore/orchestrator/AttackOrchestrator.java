package com.snowbigdeal.hostilemobscore.orchestrator;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-level singleton that manages all active {@link MobParty} instances.
 *
 * <p>Parties are <em>static</em> — they form at spawn time (via
 * {@link #enrollOnSpawn}) and persist until all members die. Members never
 * leave or join mid-combat; the party roster is fixed at world-join.
 *
 * <p>When any party member aggroes a target, {@link #onMobTargetChanged}
 * stores the new target as the party's shared target. Other members pick it
 * up on the next tick via
 * {@link com.snowbigdeal.hostilemobscore.entity.behaviour.OrchestratorSyncBehaviour},
 * which respects all brain guards (including deaggro cooldown).
 *
 * <p>The attack queue is round-robin: once per {@link #QUEUE_PULL_INTERVAL}
 * ticks the orchestrator dequeues the front member, fires its action if
 * ready, and re-enqueues it at the back. Queue pulls are skipped while the
 * party has no shared target.
 */
public class AttackOrchestrator {

    /** Radius (blocks) used when searching for a party to join at spawn. */
    private static final double SPAWN_JOIN_RADIUS = 12.0;
    /** Ticks between queue pulls — one pull per 2 seconds at 20 TPS. */
    private static final int    QUEUE_PULL_INTERVAL = 40;

    private static final Map<ResourceKey<Level>, AttackOrchestrator> INSTANCES = new HashMap<>();

    private final Map<UUID, MobParty> parties    = new LinkedHashMap<>();
    private final Map<UUID, UUID>     mobToParty = new HashMap<>();

    private AttackOrchestrator() {}

    // -------------------------------------------------------------------------
    // Singleton access
    // -------------------------------------------------------------------------

    public static AttackOrchestrator get(Level level) {
        return INSTANCES.computeIfAbsent(level.dimension(), k -> new AttackOrchestrator());
    }

    public static void invalidate(ResourceKey<Level> key) {
        INSTANCES.remove(key);
    }

    public MobParty getParty(UUID partyId) {
        return parties.get(partyId);
    }

    // -------------------------------------------------------------------------
    // Party lifecycle — called from events
    // -------------------------------------------------------------------------

    /**
     * Called when an {@link IOrchestrated} mob joins the level (spawn or chunk load).
     * Searches for a nearby existing party within {@link #SPAWN_JOIN_RADIUS} blocks and
     * joins it; if none is found, a new solo party is created for this mob.
     */
    public void enrollOnSpawn(Mob mob) {
        if (!(mob instanceof IOrchestrated)) return;
        if (mobToParty.containsKey(mob.getUUID())) return; // already enrolled

        // Find the nearest existing party within range
        List<Mob> nearbyInParty = mob.level().getEntitiesOfClass(
                Mob.class,
                mob.getBoundingBox().inflate(SPAWN_JOIN_RADIUS),
                other -> other != mob
                      && other instanceof IOrchestrated
                      && mobToParty.containsKey(other.getUUID()));

        MobParty party = null;
        for (Mob nearby : nearbyInParty) {
            UUID nearbyPartyId = mobToParty.get(nearby.getUUID());
            if (nearbyPartyId != null) {
                party = parties.get(nearbyPartyId);
                if (party != null) break;
            }
        }

        if (party == null) {
            party = new MobParty(UUID.randomUUID());
            parties.put(party.getPartyId(), party);
        }

        enrollMob(mob, party);
    }

    /**
     * Called when a party member acquires a target.
     * Updates the party's shared target so other members can pick it up via
     * {@link com.snowbigdeal.hostilemobscore.entity.behaviour.OrchestratorSyncBehaviour}.
     * Target-loss (null) is ignored — parties are static and persist between fights.
     */
    public void onMobTargetChanged(Mob mob, LivingEntity newTarget) {
        if (!(mob instanceof IOrchestrated) || newTarget == null) return;

        UUID partyId = mobToParty.get(mob.getUUID());
        if (partyId == null) return;
        MobParty party = parties.get(partyId);
        if (party == null) return;

        party.setSharedTarget(newTarget);
    }

    public void onMobDied(Mob mob) {
        if (!(mob instanceof IOrchestrated)) return;
        removeMobFromParty(mob);
    }

    // -------------------------------------------------------------------------
    // Server tick — called each level tick
    // -------------------------------------------------------------------------

    public void tick(ServerLevel level) {
        List<UUID> toDisband = new ArrayList<>();

        List<MobParty> snapshot = new ArrayList<>(parties.values());
        for (MobParty party : snapshot) {
            tickParty(party, toDisband);
        }

        toDisband.forEach(this::disbandParty);
    }

    // -------------------------------------------------------------------------
    // Internal — enrol
    // -------------------------------------------------------------------------

    private void enrollMob(Mob mob, MobParty party) {
        UUID currentPartyId = mobToParty.get(mob.getUUID());
        if (party.getPartyId().equals(currentPartyId)) return;

        if (currentPartyId != null) {
            MobParty old = parties.get(currentPartyId);
            if (old != null) old.removeMember(mob.getUUID());
        }

        party.addMember(mob);
        party.enqueue(mob.getUUID());
        party.enqueueNoop();
        mobToParty.put(mob.getUUID(), party.getPartyId());
        if (mob instanceof IOrchestrated o) {
            o.setPartyId(party.getPartyId());
        }
    }

    private void removeMobFromParty(Mob mob) {
        UUID partyId = mobToParty.remove(mob.getUUID());
        if (partyId == null) return;

        if (mob instanceof IOrchestrated o) {
            o.setPartyId(null);
            o.setPendingAction(null);
        }

        MobParty party = parties.get(partyId);
        if (party == null) return;

        party.removeMember(mob.getUUID());
        if (party.isEmpty()) {
            parties.remove(partyId);
        }
    }

    // -------------------------------------------------------------------------
    // Internal — tick
    // -------------------------------------------------------------------------

    private void tickParty(MobParty party, List<UUID> toDisband) {
        // Prune dead or removed members
        List<UUID> dead = new ArrayList<>();
        for (Map.Entry<UUID, Mob> entry : party.getMembers().entrySet()) {
            Mob mob = entry.getValue();
            if (!mob.isAlive() || mob.isRemoved()) dead.add(entry.getKey());
        }
        for (UUID id : dead) {
            party.removeMember(id);
            mobToParty.remove(id);
        }

        if (party.isEmpty()) {
            toDisband.add(party.getPartyId());
            return;
        }

        // Clear stale shared target: dead/removed, or no longer engaged by any member
        LivingEntity sharedTarget = party.getSharedTarget();
        if (sharedTarget != null) {
            boolean stale = !sharedTarget.isAlive() || sharedTarget.isRemoved();
            if (!stale) {
                stale = party.getMembers().values().stream()
                        .noneMatch(m -> m.isAlive() && m.getTarget() == sharedTarget);
            }
            if (stale) party.setSharedTarget(null);
        }

        // Poll active assignments for completion
        for (Map.Entry<UUID, Mob> entry : party.getMembers().entrySet()) {
            UUID mobId = entry.getKey();
            Mob  mob   = entry.getValue();
            if (!(mob instanceof IOrchestrated orchestrated)) continue;

            OrchestratorAction active = party.getActiveAssignment(mobId);
            if (active == null) continue;

            if (active.getMobAction().isComplete(mob)) {
                active.setStatus(OrchestratorAction.Status.COMPLETE);
                party.clearActiveAssignment(mobId);
                orchestrated.setPendingAction(null);
            }
        }

        // Skip queue pulls while the party has no target
        if (party.getSharedTarget() == null) return;

        // Pull one entry from the attack queue every QUEUE_PULL_INTERVAL ticks
        if (party.decrementQueuePullIn() <= 0) {
            party.resetQueuePullIn(QUEUE_PULL_INTERVAL);
            pullFromQueue(party);
        }
    }

    /**
     * Dequeues the front entry. No-ops are discarded (they exist only to add delay).
     * For mob entries: the mob is unconditionally re-enqueued at the back, then dispatched
     * if its action is ready. A no-op is always appended after a successful dispatch so
     * turns are separated by at least one dead pull (~1 second).
     */
    private void pullFromQueue(MobParty party) {
        UUID id = party.pollQueue();
        if (id == null || MobParty.NOOP_ID.equals(id)) return;

        Mob mob = party.getMembers().get(id);

        // Re-enqueue before acting so the rotation is always intact
        if (mob != null && mob.isAlive()) {
            party.enqueue(id);
        }

        if (mob == null || !mob.isAlive() || !(mob instanceof IOrchestrated o)) return;

        LivingEntity target = party.getSharedTarget();
        if (target == null || !target.isAlive()) return;

        for (IMobAction action : o.getMobActions()) {
            if (!action.isReady(mob)) continue;

            OrchestratorAction assignment = new OrchestratorAction(action, target);
            assignment.setStatus(OrchestratorAction.Status.RUNNING);
            party.setActiveAssignment(id, assignment);
            o.setPendingAction(assignment);
            action.beginAction(mob, target);
            party.enqueueNoop();
            break;
        }
    }

    private void disbandParty(UUID partyId) {
        MobParty party = parties.remove(partyId);
        if (party == null) return;

        for (Map.Entry<UUID, Mob> entry : party.getMembers().entrySet()) {
            mobToParty.remove(entry.getKey());
            if (entry.getValue() instanceof IOrchestrated o) {
                o.setPartyId(null);
                o.setPendingAction(null);
            }
        }
    }
}
