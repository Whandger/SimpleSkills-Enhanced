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
    /** FINAL set of enchantments wanted (existing kept + new/raised). */
    private final List<RuneInfusionRules.Selection> selections;
    /** Existing enchantments the player chose to drop; removed only if the infusion succeeds. */
    private final List<String> removedIds;
    private final int runeCost;
    private final int difficulty;
    private State state;

    private RuneInfusionSession(UUID sessionId, UUID playerId,
                                List<RuneInfusionRules.Selection> selections,
                                List<String> removedIds, int runeCost, int difficulty) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.selections = RuneInfusionRules.immutableCopy(selections);
        this.removedIds = removedIds == null ? List.of() : List.copyOf(removedIds);
        this.runeCost = runeCost;
        this.difficulty = difficulty;
        this.state = State.PREPARING;
    }

    /** Create only after the server has validated the equipment and the full selection. */
    public static RuneInfusionSession create(UUID playerId, List<RuneInfusionRules.Selection> selections,
                                             List<String> removedIds, int difficulty) {
        int cost = RuneInfusionRules.getRuneCost(selections);
        if (cost < 1) throw new IllegalArgumentException("A session requires at least one new or raised enchantment");
        return new RuneInfusionSession(UUID.randomUUID(), playerId, selections, removedIds, cost,
                Math.max(1, difficulty));
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
    public List<String> removedIds() { return removedIds; }
    public int runeCost() { return runeCost; }
    public int difficulty() { return difficulty; }
    public State state() { return state; }
    public boolean isTerminal() {
        return state == State.COMPLETED || state == State.FAILED || state == State.CANCELLED;
    }
}
