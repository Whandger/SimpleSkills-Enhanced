package com.github.ob_yekt.simpleskills.enchanting;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns one authoritative minigame per player. No Minecraft networking or client dependencies.
 */
public final class RuneInfusionMinigameController {
    private static final Map<UUID, RuneInfusionMinigame> ACTIVE = new ConcurrentHashMap<>();

    private RuneInfusionMinigameController() {}

    public enum StartResult { STARTED, PLAYER_ALREADY_ACTIVE, INVALID_ARGUMENTS }

    /**
     * Starts a new game for the player. RuneInfusionService already guarantees that a player has a
     * single live session, so any entry still here is stale and is deliberately replaced.
     */
    public static synchronized StartResult start(UUID playerId, UUID sessionId, long serverSeed,
                                                   int difficultyScore) {
        if (playerId == null || sessionId == null || difficultyScore < 1) {
            return StartResult.INVALID_ARGUMENTS;
        }
        int score = Math.min(RuneInfusionMinigame.MAX_DIFFICULTY, difficultyScore);
        ACTIVE.put(playerId, RuneInfusionMinigame.start(sessionId, serverSeed, score));
        return StartResult.STARTED;
    }

    public static RuneInfusionMinigame.Snapshot tickPlayer(UUID playerId) {
        RuneInfusionMinigame game = playerId == null ? null : ACTIVE.get(playerId);
        return game == null ? null : game.tick();
    }

    public static void tickAll() {
        ACTIVE.forEach((playerId, game) -> {
            if (game.snapshot().state() == RuneInfusionMinigame.State.RUNNING) game.tick();
        });
    }

    public static boolean beginRepair(UUID playerId, UUID sessionId, int crackId) {
        RuneInfusionMinigame game = find(playerId, sessionId);
        return game != null && game.beginRepair(sessionId, crackId, 0);
    }

    public static boolean holdRepair(UUID playerId, UUID sessionId, int crackId) {
        RuneInfusionMinigame game = find(playerId, sessionId);
        return game != null && game.holdRepair(sessionId, crackId, 0);
    }

    public static boolean releaseRepair(UUID playerId, UUID sessionId, int crackId) {
        RuneInfusionMinigame game = find(playerId, sessionId);
        return game != null && game.releaseRepair(sessionId, crackId);
    }

    public static RuneInfusionMinigame.Snapshot snapshot(UUID playerId, UUID sessionId) {
        RuneInfusionMinigame game = find(playerId, sessionId);
        return game == null ? null : game.snapshot();
    }

    /** Abort is terminal and never changes or destroys the player's equipment. */
    public static boolean abort(UUID playerId, UUID sessionId) {
        RuneInfusionMinigame game = find(playerId, sessionId);
        if (game == null) return false;
        game.fail();
        return true;
    }

    /** Removes only the matching session, so a stale packet cannot delete a newer attempt. */
    public static boolean remove(UUID playerId, UUID sessionId) {
        if (playerId == null || sessionId == null) return false;
        RuneInfusionMinigame game = ACTIVE.get(playerId);
        return game != null && game.sessionId().equals(sessionId) && ACTIVE.remove(playerId, game);
    }

    public static void removePlayer(UUID playerId) {
        if (playerId == null) return;
        RuneInfusionMinigame game = ACTIVE.remove(playerId);
        if (game != null) game.fail();
    }

    public static int activeCount() { return ACTIVE.size(); }

    public static UUID sessionId(UUID playerId) {
        RuneInfusionMinigame game = playerId == null ? null : ACTIVE.get(playerId);
        return game == null ? null : game.sessionId();
    }

    public static RuneInfusionMinigame.Snapshot snapshot(UUID playerId) {
        RuneInfusionMinigame game = playerId == null ? null : ACTIVE.get(playerId);
        return game == null ? null : game.snapshot();
    }

    private static RuneInfusionMinigame find(UUID playerId, UUID sessionId) {
        if (playerId == null || sessionId == null) return null;
        RuneInfusionMinigame game = ACTIVE.get(playerId);
        return game != null && game.sessionId().equals(sessionId) ? game : null;
    }
}