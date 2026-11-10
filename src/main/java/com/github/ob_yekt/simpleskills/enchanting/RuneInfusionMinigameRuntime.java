package com.github.ob_yekt.simpleskills.enchanting;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Server lifecycle, minigame ticking, authoritative completion and state synchronization.
 * Input packets (BEGIN/HOLD/RELEASE) are handled in RuneInfusionNetworking only.
 */
public final class RuneInfusionMinigameRuntime {
    private static long serverTick;

    private RuneInfusionMinigameRuntime() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            serverTick++;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickPlayer(player);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                dropSession(handler.player, false));

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) dropSession(player, true);
        });
    }

    private static void tickPlayer(ServerPlayer player) {
        UUID playerId = player.getUUID();
        UUID sessionId = RuneInfusionMinigameController.sessionId(playerId);
        if (sessionId == null) return;

        // Captured before success/failure releases the equipment reservation.
        int equipmentSlot = RuneInfusionService.getActiveEquipmentSlot(player);

        if (equipmentSlot < 0) {
            // The reserved stack is gone (moved, dropped, destroyed): end the attempt cleanly.
            RuneInfusionMinigameController.abort(playerId, sessionId);
        } else {
            RuneInfusionMinigameController.tickPlayer(playerId);
        }

        RuneInfusionMinigame.Snapshot snapshot = RuneInfusionMinigameController.snapshot(playerId);
        if (snapshot == null) return;

        if (snapshot.state() == RuneInfusionMinigame.State.SUCCEEDED) {
            boolean applied = RuneInfusionService.applySuccessfulInfusion(player, snapshot.grade());
            if (!applied) RuneInfusionService.fail(player);

            ServerPlayNetworking.send(player, new RuneInfusionResultPayload(
                    applied ? RuneInfusionResultPayload.SUCCEEDED : RuneInfusionResultPayload.FAILED,
                    0, snapshot.grade().ordinal()));
            finish(player, sessionId, snapshot, equipmentSlot);
            return;
        }

        if (snapshot.state() == RuneInfusionMinigame.State.FAILED) {
            RuneInfusionService.fail(player);
            ServerPlayNetworking.send(player, new RuneInfusionResultPayload(
                    RuneInfusionResultPayload.FAILED, 0, snapshot.grade().ordinal()));
            finish(player, sessionId, snapshot, equipmentSlot);
            return;
        }

        // 10 snapshots per second are enough for visual synchronization.
        if ((serverTick & 1L) == 0L) {
            ServerPlayNetworking.send(player,
                    RuneInfusionStatePayload.from(sessionId, snapshot, equipmentSlot));
        }
    }

    /** Sends the final snapshot, then STOP (carrying the real session id), then frees all state. */
    private static void finish(ServerPlayer player, UUID sessionId,
                               RuneInfusionMinigame.Snapshot snapshot, int equipmentSlot) {
        ServerPlayNetworking.send(player, RuneInfusionStatePayload.from(sessionId, snapshot, equipmentSlot));
        ServerPlayNetworking.send(player, RuneInfusionStatePayload.stop(sessionId));

        RuneInfusionMinigameController.remove(player.getUUID(), sessionId);
        RuneInfusionService.clearTerminalSession(player);
    }

    /** Disconnect / death: drop every trace of the session. Runes are not refunded after start. */
    private static void dropSession(ServerPlayer player, boolean notifyClient) {
        UUID playerId = player.getUUID();
        UUID sessionId = RuneInfusionMinigameController.sessionId(playerId);

        RuneInfusionService.fail(player);
        RuneInfusionService.clearTerminalSession(player);
        RuneInfusionMinigameController.removePlayer(playerId);

        if (notifyClient && sessionId != null) {
            ServerPlayNetworking.send(player, RuneInfusionStatePayload.stop(sessionId));
        }
    }
}