package com.github.ob_yekt.simpleskills.enchanting;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns one authoritative overload minigame per player. Call tickPlayer/tickAll only from the
 * logical server tick; packet handlers should call the repair methods on the server thread.
 * This class deliberately has no Minecraft networking or client dependencies.
 */
public final class RuneInfusionMinigameController {
    private static final Map<UUID, RuneInfusionMinigame> ACTIVE = new ConcurrentHashMap<>();

    private RuneInfusionMinigameController() {}

    public enum StartResult { STARTED, PLAYER_ALREADY_ACTIVE, INVALID_ARGUMENTS }

    public static synchronized StartResult start(UUID playerId, UUID sessionId, long serverSeed,
                                                   int enchantmentCount, int highestLevel) {
        if (playerId == null || sessionId == null || enchantmentCount < 1 || enchantmentCount > 4
                || highestLevel < 1 || highestLevel > 10) {
            return StartResult.INVALID_ARGUMENTS;
        }
        RuneInfusionMinigame current = ACTIVE.get(playerId);
        if (current != null && current.snapshot().state() == RuneInfusionMinigame.State.RUNNING) {
            return StartResult.PLAYER_ALREADY_ACTIVE;
        }
        ACTIVE.put(playerId, RuneInfusionMinigame.start(sessionId, serverSeed,
                enchantmentCount, highestLevel));
        return StartResult.STARTED;
    }

    /** Advances exactly one authoritative game tick for this player. */
    public static RuneInfusionMinigame.Snapshot tickPlayer(UUID playerId) {
        RuneInfusionMinigame game = playerId == null ? null : ACTIVE.get(playerId);
        return game == null ? null : game.tick();
    }

    /** Advances all active games once; invoke once per server tick, never from a packet handler. */
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

    /** Remove only the matching session, preventing stale packets from deleting a newer attempt. */
    public static boolean remove(UUID playerId, UUID sessionId) {
        if (playerId == null || sessionId == null) return false;
        RuneInfusionMinigame game = ACTIVE.get(playerId);
        return game != null && game.sessionId().equals(sessionId) && ACTIVE.remove(playerId, game);
    }

    /** Remove a disconnected player's session so it cannot leak or be resumed by a stale client. */
    public static void removePlayer(UUID playerId) {
        if (playerId == null) return;
        RuneInfusionMinigame game = ACTIVE.remove(playerId);
        if (game != null) game.fail();
    }

    public static int activeCount() { return ACTIVE.size(); }

    /** Returns the current session id for server-to-client snapshot routing. */
    public static UUID sessionId(UUID playerId) {
        RuneInfusionMinigame game = playerId == null ? null : ACTIVE.get(playerId);
        return game == null ? null : game.sessionId();
    }

    /** Snapshot for the player's current session, or null when no session exists. */
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
