package com.github.ob_yekt.simpleskills.enchanting;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Server-owned lifecycle for a single Rune Enchanting attempt. */
public final class RuneInfusionSession {
    public enum State {
        PREPARING,
        STARTED,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    private final UUID sessionId;
    private final UUID playerId;
    private final List<RuneInfusionRules.Selection> selections;
    private final int runeCost;
    private State state;

    private RuneInfusionSession(UUID sessionId, UUID playerId,
                                List<RuneInfusionRules.Selection> selections, int runeCost) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.selections = RuneInfusionRules.immutableCopy(selections);
        this.runeCost = runeCost;
        this.state = State.PREPARING;
    }

    /** Create only after the server has validated the equipment and the full selection. */
    public static RuneInfusionSession create(UUID playerId, List<RuneInfusionRules.Selection> selections) {
        int cost = RuneInfusionRules.getRuneCost(selections);
        if (cost < 1) throw new IllegalArgumentException("A session requires 1-4 unique enchantments");
        return new RuneInfusionSession(UUID.randomUUID(), playerId, selections, cost);
    }

    /** Called only after the server has successfully consumed the Rune items. */
    public boolean markStarted() {
        if (state != State.PREPARING) return false;
        state = State.STARTED;
        return true;
    }

    public boolean markInProgress() {
        if (state != State.STARTED) return false;
        state = State.IN_PROGRESS;
        return true;
    }

    public boolean complete() {
        if (state != State.IN_PROGRESS) return false;
        state = State.COMPLETED;
        return true;
    }

    /** A started attempt cannot be cancelled with a refund. */
    public boolean fail() {
        if (state != State.STARTED && state != State.IN_PROGRESS) return false;
        state = State.FAILED;
        return true;
    }

    /** Closing the preparation UI is free; after start it is treated as a failed attempt. */
    public boolean closeOrDisconnect() {
        if (state == State.PREPARING) {
            state = State.CANCELLED;
            return true;
        }
        return fail();
    }

    public UUID sessionId() { return sessionId; }
    public UUID playerId() { return playerId; }
    public List<RuneInfusionRules.Selection> selections() { return selections; }
    public int runeCost() { return runeCost; }
    public State state() { return state; }
    public boolean isTerminal() {
        return state == State.COMPLETED || state == State.FAILED || state == State.CANCELLED;
    }
}
