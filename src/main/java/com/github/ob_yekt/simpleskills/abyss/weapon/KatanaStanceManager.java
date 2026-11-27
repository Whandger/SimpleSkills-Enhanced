package com.github.ob_yekt.simpleskills.abyss.weapon;

import com.github.ob_yekt.simpleskills.abyss.AbyssalKatanaItems;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-authoritative stance and 3-hit combo state machine for the Abyssal Katana.
 *
 * <p>Timing contract (20 TPS):
 * <ul>
 *   <li>{@code DRAWING}: 25 ticks (1.25s) -> transitions to {@code UNSHEATHED} (combat guard).</li>
 *   <li>{@code UNSHEATHED}: after 40 ticks (2.0s) without attacking -> transitions to {@code LOWER_GUARD}.</li>
 *   <li>{@code LOWER_GUARD}: 7 ticks (0.35s) transition animation + 100 ticks (5.0s) hold = 107 ticks
 *       total before automatically transitioning to {@code SHEATHING}.</li>
 *   <li>{@code SHEATHING}: 27 ticks (1.35s) -> returns to {@code SHEATHED}.</li>
 *   <li>Combo chain: {@code ATTACK_1} (10 ticks / 0.46s) -> {@code ATTACK_2} (10 ticks / 0.50s)
 *       -> {@code ATTACK_3} (14 ticks / 0.66s) -> {@code UNSHEATHED}.</li>
 * </ul>
 */
public final class KatanaStanceManager {
    public static final int SHEATHED = 0;
    public static final int DRAWING = 1;
    public static final int UNSHEATHED = 2;
    public static final int SHEATHING = 3;
    public static final int LOWER_GUARD = 4;
    public static final int ATTACK_1 = 5;
    public static final int ATTACK_2 = 6;
    public static final int ATTACK_3 = 7;

    public static final int DRAW_TICKS = 25;
    public static final int DRAW_EARLY_ATTACK_TICKS = 14;
    public static final int IDLE_BEFORE_LOWER_GUARD_TICKS = 40;
    public static final int LOWER_GUARD_TRANSITION_TICKS = 7;
    public static final int LOWER_GUARD_HOLD_BEFORE_SHEATH_TICKS = 100;
    public static final int LOWER_GUARD_TOTAL_TICKS =
            LOWER_GUARD_TRANSITION_TICKS + LOWER_GUARD_HOLD_BEFORE_SHEATH_TICKS;
    public static final int SHEATH_TICKS = 27;

    public static final int ATTACK_1_TICKS = 10;
    public static final int ATTACK_2_TICKS = 10;
    public static final int ATTACK_3_TICKS = 14;
    public static final int ATTACK_1_CHAIN_TICKS = 7;
    public static final int ATTACK_2_CHAIN_TICKS = 8;
    public static final int ATTACK_3_CHAIN_TICKS = 11;
    public static final int COMBO_WINDOW_TICKS = 28;

    private static final Map<UUID, Entry> STATES = new ConcurrentHashMap<>();
    // One monotonic clock for the whole server; per-dimension gameTime can jump on dimension changes.
    private static long serverTick;

    private KatanaStanceManager() {}

    public static boolean isAttackStance(int stance) {
        return stance == ATTACK_1 || stance == ATTACK_2 || stance == ATTACK_3;
    }

    public static int attackStanceForStep(int step) {
        return switch (step) {
            case 1 -> ATTACK_1;
            case 2 -> ATTACK_2;
            case 3 -> ATTACK_3;
            default -> UNSHEATHED;
        };
    }

    public static int comboStepForStance(int stance) {
        return switch (stance) {
            case ATTACK_1 -> 1;
            case ATTACK_2 -> 2;
            case ATTACK_3 -> 3;
            default -> 0;
        };
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.serverboundPlay().register(KatanaStanceRequestPayload.TYPE,
                KatanaStanceRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(KatanaStancePayload.TYPE, KatanaStancePayload.CODEC);
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(KatanaStanceRequestPayload.TYPE, (payload, context) ->
                context.server().execute(() -> handleAttack(context.player(), payload.action(), context.server())));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverTick = 0L;
            STATES.clear();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> STATES.clear());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            serverTick++;
            tick(server);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer joining = handler.getPlayer();
            if (joining != null) {
                server.execute(() -> {
                    for (Map.Entry<UUID, Entry> state : STATES.entrySet()) {
                        ServerPlayer active = server.getPlayerList().getPlayer(state.getKey());
                        if (active != null) send(joining, active.getUUID(), state.getValue(), serverTick);
                    }
                });
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID disconnectedId = handler.getPlayer().getUUID();
            STATES.remove(disconnectedId);
            server.execute(() -> broadcastSheathed(server, disconnectedId));
        });
    }

    private static void handleAttack(ServerPlayer player, int action, MinecraftServer server) {
        if (action != KatanaStanceRequestPayload.ATTACK || player == null || !player.isAlive()
                || player.isSpectator() || !AbyssalKatanaItems.isKatana(player.getMainHandItem().getItem())) {
            return;
        }

        UUID id = player.getUUID();
        long now = serverTick;
        Entry entry = STATES.computeIfAbsent(id, ignored -> new Entry(SHEATHED, now, now, now));

        if (entry.stance == SHEATHED || entry.stance == SHEATHING) {
            entry.stance = DRAWING;
            entry.stateStartedTick = now;
            entry.lastAttackTick = now;
            entry.combatReadyTick = now;
            entry.comboStep = 0;
            entry.queuedAttack = false;
            broadcast(server, id, entry, now);
            return;
        }

        if (entry.stance == DRAWING) {
            long elapsed = now - entry.stateStartedTick;
            entry.lastAttackTick = now;
            if (elapsed >= DRAW_EARLY_ATTACK_TICKS) {
                startAttack(entry, 1, now);
            } else {
                entry.queuedAttack = true;
            }
            broadcast(server, id, entry, now);
            return;
        }

        if (isAttackStance(entry.stance)) {
            long elapsed = now - entry.stateStartedTick;
            entry.lastAttackTick = now;
            if (entry.stance == ATTACK_1) {
                if (elapsed >= ATTACK_1_CHAIN_TICKS) {
                    startAttack(entry, 2, now);
                } else {
                    entry.queuedAttack = true;
                }
            } else if (entry.stance == ATTACK_2) {
                if (elapsed >= ATTACK_2_CHAIN_TICKS) {
                    startAttack(entry, 3, now);
                } else {
                    entry.queuedAttack = true;
                }
            } else {
                if (elapsed >= ATTACK_3_CHAIN_TICKS) {
                    startAttack(entry, 1, now);
                } else if (elapsed >= 8) {
                    entry.queuedAttack = true;
                }
            }
            broadcast(server, id, entry, now);
            return;
        }

        // UNSHEATHED or LOWER_GUARD: enter the appropriate step in the 1 -> 2 -> 3 combo.
        int nextStep = (entry.stance == UNSHEATHED && entry.comboStep > 0
                && (now - entry.lastAttackTick) <= COMBO_WINDOW_TICKS)
                ? ((entry.comboStep % 3) + 1)
                : 1;
        startAttack(entry, nextStep, now);
        broadcast(server, id, entry, now);
    }

    private static void startAttack(Entry entry, int step, long now) {
        entry.comboStep = step;
        entry.stance = attackStanceForStep(step);
        entry.stateStartedTick = now;
        entry.lastAttackTick = now;
        entry.combatReadyTick = now;
        entry.queuedAttack = false;
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            Entry entry = STATES.get(id);
            if (entry == null) continue;

            if (!player.isAlive() || !AbyssalKatanaItems.isKatana(player.getMainHandItem().getItem())) {
                if (STATES.remove(id) != null) broadcastSheathed(server, id);
                continue;
            }

            long now = serverTick;
            long elapsed = now - entry.stateStartedTick;
            switch (entry.stance) {
                case DRAWING -> {
                    if (elapsed >= DRAW_TICKS) {
                        if (entry.queuedAttack) {
                            startAttack(entry, 1, now);
                        } else {
                            entry.stance = UNSHEATHED;
                            entry.stateStartedTick = now;
                            entry.combatReadyTick = now;
                            entry.comboStep = 0;
                        }
                        broadcast(server, id, entry, now);
                    }
                }
                case ATTACK_1 -> {
                    if (entry.queuedAttack && elapsed >= ATTACK_1_CHAIN_TICKS) {
                        startAttack(entry, 2, now);
                        broadcast(server, id, entry, now);
                    } else if (elapsed >= ATTACK_1_TICKS) {
                        entry.stance = UNSHEATHED;
                        entry.stateStartedTick = now;
                        entry.combatReadyTick = now;
                        entry.queuedAttack = false;
                        broadcast(server, id, entry, now);
                    }
                }
                case ATTACK_2 -> {
                    if (entry.queuedAttack && elapsed >= ATTACK_2_CHAIN_TICKS) {
                        startAttack(entry, 3, now);
                        broadcast(server, id, entry, now);
                    } else if (elapsed >= ATTACK_2_TICKS) {
                        entry.stance = UNSHEATHED;
                        entry.stateStartedTick = now;
                        entry.combatReadyTick = now;
                        entry.queuedAttack = false;
                        broadcast(server, id, entry, now);
                    }
                }
                case ATTACK_3 -> {
                    if (entry.queuedAttack && elapsed >= ATTACK_3_CHAIN_TICKS) {
                        startAttack(entry, 1, now);
                        broadcast(server, id, entry, now);
                    } else if (elapsed >= ATTACK_3_TICKS) {
                        entry.stance = UNSHEATHED;
                        entry.stateStartedTick = now;
                        entry.combatReadyTick = now;
                        entry.comboStep = 0;
                        entry.queuedAttack = false;
                        broadcast(server, id, entry, now);
                    }
                }
                case UNSHEATHED -> {
                    long idleReference = Math.max(entry.lastAttackTick, entry.combatReadyTick);
                    if (now - idleReference >= IDLE_BEFORE_LOWER_GUARD_TICKS) {
                        entry.stance = LOWER_GUARD;
                        entry.stateStartedTick = now;
                        entry.comboStep = 0;
                        entry.queuedAttack = false;
                        broadcast(server, id, entry, now);
                    }
                }
                case LOWER_GUARD -> {
                    // 0.35s (7 ticks) transition to lower_guard + 5.0s (100 ticks) hold = 107 ticks.
                    if (elapsed >= LOWER_GUARD_TOTAL_TICKS) {
                        entry.stance = SHEATHING;
                        entry.stateStartedTick = now;
                        entry.comboStep = 0;
                        entry.queuedAttack = false;
                        broadcast(server, id, entry, now);
                    }
                }
                case SHEATHING -> {
                    if (elapsed >= SHEATH_TICKS) {
                        STATES.remove(id);
                        broadcastSheathed(server, id);
                    }
                }
                default -> STATES.remove(id);
            }
        }

        STATES.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
    }

    private static void broadcast(MinecraftServer server, UUID playerId, Entry entry, long now) {
        for (ServerPlayer recipient : server.getPlayerList().getPlayers()) {
            send(recipient, playerId, entry, now);
        }
    }

    private static void send(ServerPlayer recipient, UUID playerId, Entry entry, long now) {
        if (!ServerPlayNetworking.canSend(recipient, KatanaStancePayload.TYPE)) return;
        int elapsed = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, now - entry.stateStartedTick));
        int sinceAttack = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, now - entry.lastAttackTick));
        ServerPlayNetworking.send(recipient, new KatanaStancePayload(
                playerId.toString(), entry.stance, elapsed, sinceAttack));
    }

    private static void broadcastSheathed(MinecraftServer server, UUID playerId) {
        KatanaStancePayload payload = new KatanaStancePayload(playerId.toString(), SHEATHED, 0, 0);
        for (ServerPlayer recipient : server.getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(recipient, KatanaStancePayload.TYPE)) {
                ServerPlayNetworking.send(recipient, payload);
            }
        }
    }

    private static final class Entry {
        private int stance;
        private long stateStartedTick;
        private long lastAttackTick;
        private long combatReadyTick;
        private int comboStep;
        private boolean queuedAttack;

        private Entry(int stance, long stateStartedTick, long lastAttackTick, long combatReadyTick) {
            this.stance = stance;
            this.stateStartedTick = stateStartedTick;
            this.lastAttackTick = lastAttackTick;
            this.combatReadyTick = combatReadyTick;
        }
    }
}
