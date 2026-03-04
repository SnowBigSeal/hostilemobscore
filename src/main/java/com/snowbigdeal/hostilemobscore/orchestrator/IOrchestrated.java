package com.snowbigdeal.hostilemobscore.orchestrator;

import java.util.List;
import java.util.UUID;

/**
 * Implemented by mobs that participate in the attack orchestrator.
 * Exposes the mob's available actions and its current party/assignment state.
 */
public interface IOrchestrated {

    /** Returns all actions this mob can perform, in priority order. */
    List<IMobAction> getMobActions();

    /** Returns the UUID of the party this mob belongs to, or null if solo. */
    UUID getPartyId();

    /** Sets the party UUID when this mob joins or leaves a party. */
    void setPartyId(UUID partyId);

    /** Returns the currently assigned orchestrator action, or null if none. */
    OrchestratorAction getPendingAction();

    /** Called by the orchestrator to assign or clear an action. */
    void setPendingAction(OrchestratorAction action);
}
