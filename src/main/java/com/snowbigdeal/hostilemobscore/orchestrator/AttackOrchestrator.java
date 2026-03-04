package com.snowbigdeal.hostilemobscore.orchestrator;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Per-level singleton that manages all active {@link MobParty} instances.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Form parties when mobs share an aggro target within {@link #PARTY_JOIN_RADIUS}.</li>
 *   <li>Merge parties that converge on the same target.</li>
 *   <li>Pull from each party's attack queue once per {@link #QUEUE_PULL_INTERVAL} ticks.</li>
 *   <li>Disband empty parties and clean up dead members.</li>
 * </ul>
 *
 * <p>Each party maintains a round-robin queue of its members. Every second the orchestrator
 * dequeues the front mob. If that mob's internal cooldown is ready it fires its action;
 * otherwise it silently passes. The mob is then re-enqueued at the back. A no-op entry
 * is randomly appended after a successful attack to add artificial delay between rounds.
 */
public class AttackOrchestrator {

    private static final double PARTY_JOIN_RADIUS   = 24.0;
    /** Ticks between queue pulls — one pull per second at 20 TPS. */
    private static final int    QUEUE_PULL_INTERVAL = 20;
    /** Probability (0–1) that a no-op slot is appended after a successful attack dispatch. */
    private static final double NOOP_CHANCE         = 0.4;

    private static final Map<ResourceKey<Level>, AttackOrchestrator> INSTANCES = new HashMap<>();

    private final Map<UUID, MobParty> parties    = new LinkedHashMap<>();
    private final Map<UUID, UUID>     mobToParty = new HashMap<>();
    private final Random              random     = new Random();

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

    public void onMobTargetChanged(Mob mob, LivingEntity newTarget) {
        if (!(mob instanceof IOrchestrated)) return;

        if (newTarget == null) {
            removeMobFromParty(mob);
            return;
        }

        tryJoinOrCreateParty(mob, newTarget);
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

        for (MobParty party : parties.values()) {
            tickParty(party, toDisband);
        }

        toDisband.forEach(this::disbandParty);
    }

    // -------------------------------------------------------------------------
    // Internal — party formation
    // -------------------------------------------------------------------------

    private void tryJoinOrCreateParty(Mob mob, LivingEntity target) {
        UUID targetId = target.getUUID();

        List<Mob> sameTargetNearby = mob.level().getEntitiesOfClass(
                Mob.class,
                mob.getBoundingBox().inflate(PARTY_JOIN_RADIUS),
                other -> other != mob
                      && other instanceof IOrchestrated
                      && other.getTarget() != null
                      && other.getTarget().getUUID().equals(targetId));

        Set<UUID> involvedPartyIds = new LinkedHashSet<>();
        UUID myPartyId = mobToParty.get(mob.getUUID());
        if (myPartyId != null) involvedPartyIds.add(myPartyId);
        for (Mob nearby : sameTargetNearby) {
            UUID nearbyPartyId = mobToParty.get(nearby.getUUID());
            if (nearbyPartyId != null) involvedPartyIds.add(nearbyPartyId);
        }

        MobParty survivingParty;
        Iterator<UUID> it = involvedPartyIds.iterator();
        if (it.hasNext()) {
            survivingParty = parties.get(it.next());
            while (it.hasNext()) {
                UUID otherId = it.next();
                MobParty other = parties.remove(otherId);
                if (other == null) continue;

                for (Map.Entry<UUID, Mob> entry : other.getMembers().entrySet()) {
                    mobToParty.put(entry.getKey(), survivingParty.getPartyId());
                    if (entry.getValue() instanceof IOrchestrated o) {
                        o.setPartyId(survivingParty.getPartyId());
                    }
                }
                survivingParty.merge(other);
            }
        } else {
            survivingParty = new MobParty(UUID.randomUUID());
            parties.put(survivingParty.getPartyId(), survivingParty);
        }

        survivingParty.setSharedTarget(target);
        enrollMob(mob, survivingParty);
        for (Mob nearby : sameTargetNearby) {
            enrollMob(nearby, survivingParty);
        }
    }

    private void enrollMob(Mob mob, MobParty party) {
        UUID currentPartyId = mobToParty.get(mob.getUUID());
        if (party.getPartyId().equals(currentPartyId)) return;

        if (currentPartyId != null) {
            MobParty old = parties.get(currentPartyId);
            if (old != null) old.removeMember(mob.getUUID());
        }

        party.addMember(mob);
        party.enqueue(mob.getUUID()); // place at the back of the rotation
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

        // Pull one entry from the attack queue every QUEUE_PULL_INTERVAL ticks
        if (party.decrementQueuePullIn() <= 0) {
            party.resetQueuePullIn(QUEUE_PULL_INTERVAL);
            pullFromQueue(party);
        }
    }

    /**
     * Dequeues the front entry. No-ops are discarded (they exist only to add delay).
     * For mob entries: the mob is unconditionally re-enqueued at the back, then dispatched
     * if its action is ready. If it is not ready it silently passes its turn.
     * A no-op is randomly appended after every successful dispatch.
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

            if (random.nextDouble() < NOOP_CHANCE) {
                party.enqueueNoop();
            }
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
