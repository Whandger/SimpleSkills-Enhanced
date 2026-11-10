
package com.github.ob_yekt.simpleskills.enchanting;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Server lifecycle, minigame ticking, authoritative completion and state synchronization.
 */
public final class RuneInfusionMinigameRuntime {
    private static long serverTick;

    private RuneInfusionMinigameRuntime() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(
                RuneInfusionInputPayload.TYPE,
                (payload, context) -> context.server().execute(
                        () -> handleInput(context.player().getUUID(), payload)
                )
        );

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            serverTick++;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                UUID playerId = player.getUUID();

                RuneInfusionMinigameController.tickPlayer(playerId);

                RuneInfusionMinigame.Snapshot snapshot =
                        RuneInfusionMinigameController.snapshot(playerId);

                UUID sessionId =
                        RuneInfusionMinigameController.sessionId(playerId);

                if (snapshot == null || sessionId == null) {
                    continue;
                }

                // Capture the equipment slot before success/failure removes its reservation.
                int equipmentSlot =
                        RuneInfusionService.getActiveEquipmentSlot(player);

                if (snapshot.state() == RuneInfusionMinigame.State.SUCCEEDED) {
                    boolean applied =
                            RuneInfusionService.applySuccessfulInfusion(
                                    player,
                                    snapshot.grade()
                            );

                    if (applied) {
                        ServerPlayNetworking.send(
                                player,
                                new RuneInfusionResultPayload(
                                        RuneInfusionResultPayload.SUCCEEDED,
                                        0,
                                        snapshot.grade().ordinal()
                                )
                        );
                    } else {
                        RuneInfusionService.fail(player);

                        ServerPlayNetworking.send(
                                player,
                                new RuneInfusionResultPayload(
                                        RuneInfusionResultPayload.FAILED,
                                        0,
                                        snapshot.grade().ordinal()
                                )
                        );
                    }

                    finish(player, sessionId, snapshot, equipmentSlot);
                    continue;
                }

                if (snapshot.state() == RuneInfusionMinigame.State.FAILED) {
                    RuneInfusionService.fail(player);

                    ServerPlayNetworking.send(
                            player,
                            new RuneInfusionResultPayload(
                                    RuneInfusionResultPayload.FAILED,
                                    0,
                                    snapshot.grade().ordinal()
                            )
                    );

                    finish(player, sessionId, snapshot, equipmentSlot);
                    continue;
                }

                // 10 snapshots per second are sufficient for visual synchronization.
                if ((serverTick & 1L) == 0L) {
                    ServerPlayNetworking.send(
                            player,
                            RuneInfusionStatePayload.from(
                                    sessionId,
                                    snapshot,
                                    equipmentSlot
                            )
                    );
                }
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            RuneInfusionService.fail(handler.player);
            RuneInfusionService.clearTerminalSession(handler.player);
            RuneInfusionMinigameController.removePlayer(
                    handler.player.getUUID()
            );
        });
    }

    /**
     * Sends the final authoritative snapshot and then stops the client display.
     */
    private static void finish(
            ServerPlayer player,
            UUID sessionId,
            RuneInfusionMinigame.Snapshot snapshot,
            int equipmentSlot
    ) {
        ServerPlayNetworking.send(
                player,
                RuneInfusionStatePayload.from(
                        sessionId,
                        snapshot,
                        equipmentSlot
                )
        );

        ServerPlayNetworking.send(
                player,
                new RuneInfusionStatePayload(
                        0L,
                        0L,
                        RuneInfusionStatePayload.STOP,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        -1,
                        ""
                )
        );

        RuneInfusionMinigameController.remove(
                player.getUUID(),
                sessionId
        );

        RuneInfusionService.clearTerminalSession(player);
    }

    /**
     * Handles repair input. All actual repair progress is validated and applied
     * by the server-side minigame controller.
     */
    private static void handleInput(
            UUID playerId,
            RuneInfusionInputPayload payload
    ) {
        if (payload == null
                || payload.crackId() < 0
                || payload.crackId() > 4096) {
            return;
        }

        UUID sessionId = payload.sessionId();

        switch (payload.action()) {
            case RuneInfusionInputPayload.BEGIN ->
                    RuneInfusionMinigameController.beginRepair(
                            playerId,
                            sessionId,
                            payload.crackId()
                    );

            case RuneInfusionInputPayload.HOLD ->
                    RuneInfusionMinigameController.holdRepair(
                            playerId,
                            sessionId,
                            payload.crackId()
                    );

            case RuneInfusionInputPayload.RELEASE ->
                    RuneInfusionMinigameController.releaseRepair(
                            playerId,
                            sessionId,
                            payload.crackId()
                    );

            default -> {
                // Ignore unknown client actions.
            }
        }
    }
}
