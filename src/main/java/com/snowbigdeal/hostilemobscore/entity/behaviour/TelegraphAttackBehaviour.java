package com.snowbigdeal.hostilemobscore.entity.behaviour;

import com.snowbigdeal.hostilemobscore.attack.AttackSnapshot;
import com.snowbigdeal.hostilemobscore.attack.shape.TelegraphAttackShape;
import com.snowbigdeal.hostilemobscore.network.TelegraphAttackPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tslat.smartbrainlib.api.core.behaviour.ExtendedBehaviour;

/**
 * Abstract base for all telegraph-style attacks: a ground indicator (tell) is shown
 * during a windup phase, then an attack is executed, followed by a recovery phase.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li><b>Start</b> — {@code buildShape()} constructs the geometry; packet is sent to
 *       all nearby clients; {@code onStart()} called for subclass init.</li>
 *   <li><b>Windup</b> — entity stops moving each tick; {@code onWindupTick()} hook allows
 *       facing the target or other per-tick windup logic.</li>
 *   <li><b>Execute</b> — snapshot committed; {@code applyLaunch()} called (no-op for
 *       stationary attacks); base waits for {@code isExecutionComplete()} to return
 *       {@code true}.</li>
 *   <li><b>Impact</b> — {@code onImpact()} called exactly once with the committed
 *       snapshot; recovery countdown begins.</li>
 *   <li><b>Recovery</b> — {@code onRecoveryTick()} hook; entity stops moving.</li>
 *   <li><b>Stop</b> — {@code onStop()} for subclass cleanup.</li>
 * </ol>
 *
 * <h3>Implementing a new attack</h3>
 * <ul>
 *   <li>Extend this class with your mob type: {@code class MyAttack extends TelegraphAttackBehaviour<MyMob>}</li>
 *   <li>Implement {@code buildShape()} and {@code onImpact()}.</li>
 *   <li>Override {@code applyLaunch()} and {@code isExecutionComplete()} for movement attacks.</li>
 * </ul>
 *
 * @param <T> mob type
 */
public abstract class TelegraphAttackBehaviour<T extends Mob>
        extends ExtendedBehaviour<T> {

    /** The shape built at the start of this attack. Available from {@code onStart()} onward. */
    protected TelegraphAttackShape activeShape = null;

    private AttackSnapshot<Player> snapshot          = null;
    private int                    windupRemaining   = 0;
    private int                    recoveryRemaining = 0;
    private boolean                executionDone     = false;
    private boolean                behaviourDone     = false;

    // -------------------------------------------------------------------------
    // Abstract
    // -------------------------------------------------------------------------

    /** Build the attack shape at the start of the windup. */
    protected abstract TelegraphAttackShape buildShape(T entity);

    /** Called exactly once when execution completes (landing for movement attacks, immediately for stationary). */
    protected abstract void onImpact(T entity, AttackSnapshot<Player> snapshot);

    // -------------------------------------------------------------------------
    // Overridable — timing
    // -------------------------------------------------------------------------

    protected int windupTicks()   { return 20; } // 1 second
    protected int recoveryTicks() { return 20; } // 1 second

    // -------------------------------------------------------------------------
    // Overridable — hooks
    // -------------------------------------------------------------------------

    /** Called during each windup tick after movement is stopped. */
    protected void onWindupTick(T entity, int remainingTicks) {}

    /** Called when the windup ends. Default is a no-op (stationary attack). Override to add movement. */
    protected void applyLaunch(T entity) {}

    /**
     * Returns {@code true} when the execution phase is complete.
     * Default returns {@code true} immediately (instant/stationary attack).
     * Movement attacks override this to detect landing.
     */
    protected boolean isExecutionComplete(T entity) { return true; }

    /** Called each recovery tick. */
    protected void onRecoveryTick(T entity, int remainingTicks) {}

    /** Called before subclass-specific initialisation in {@code start()}. */
    protected void onStart(T entity) {}

    /** Called in {@code stop()} before state is cleared. */
    protected void onStop(T entity) {}

    /**
     * Override to return {@code true} when the attack should be aborted mid-flight.
     * (e.g. target has died or become invalid.)
     */
    protected boolean shouldAbort(T entity) { return false; }

    // -------------------------------------------------------------------------
    // Lifecycle — ExtendedBehaviour
    // -------------------------------------------------------------------------

    @Override
    protected void start(T entity) {
        this.activeShape       = buildShape(entity);
        this.windupRemaining   = windupTicks();
        this.recoveryRemaining = 0;
        this.executionDone     = false;
        this.behaviourDone     = false;
        this.snapshot          = null;
        sendPacket(entity, activeShape, windupRemaining);
        onStart(entity);
    }

    @Override
    protected boolean shouldKeepRunning(T entity) {
        return !behaviourDone;
    }

    @Override
    protected void tick(T entity) {
        if (shouldAbort(entity)) {
            behaviourDone = true;
            return;
        }

        if (snapshot == null) {
            // ---- Windup phase ----
            stopMovement(entity);
            onWindupTick(entity, windupRemaining);
            if (--windupRemaining <= 0) {
                snapshot = captureSnapshot(entity);
                applyLaunch(entity);
            }
        } else if (!executionDone) {
            // ---- Execution phase ----
            if (isExecutionComplete(entity)) {
                onImpact(entity, snapshot);
                executionDone     = true;
                recoveryRemaining = recoveryTicks();
            }
        } else {
            // ---- Recovery phase ----
            stopMovement(entity);
            onRecoveryTick(entity, recoveryRemaining);
            if (--recoveryRemaining <= 0) behaviourDone = true;
        }
    }

    @Override
    protected void stop(T entity) {
        onStop(entity);
        activeShape = null;
        snapshot    = null;
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    /** Stops the entity's movement inputs this tick. */
    protected void stopMovement(T entity) {
        entity.xxa = 0;
        entity.zza = 0;
        entity.setSpeed(0);
    }

    private void sendPacket(T entity, TelegraphAttackShape shape, int lifetime) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        TelegraphAttackPacket packet = new TelegraphAttackPacket(shape, lifetime);
        for (ServerPlayer player : level.players()) {
            PacketDistributor.sendToPlayer(player, packet);
        }
    }

    private AttackSnapshot<Player> captureSnapshot(T entity) {
        return AttackSnapshot.capture(entity.level(), activeShape, Player.class,
                p -> !p.getAbilities().invulnerable && p.isAlive());
    }
}
