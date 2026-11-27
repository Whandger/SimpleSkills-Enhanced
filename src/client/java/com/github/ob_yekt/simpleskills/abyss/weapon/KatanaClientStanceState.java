package com.github.ob_yekt.simpleskills.abyss.weapon;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Client-side synchronized stance & 3-hit combo state machine with immediate local attack
 * prediction, smooth pose cross-fading, and horizontal walk-cycle blending.
 */
public final class KatanaClientStanceState {
    private static final long TICK_NANOS = 50_000_000L;
    private static final long DRAW_NANOS = 1_250_000_000L;
    private static final long DRAW_EARLY_ATTACK_NANOS = 700_000_000L;
    private static final long IDLE_TO_LOWER_GUARD_NANOS = 2_000_000_000L;
    private static final long LOWER_GUARD_TRANSITION_NANOS = 350_000_000L;
    private static final long LOWER_GUARD_HOLD_NANOS = 5_000_000_000L;
    private static final long LOWER_GUARD_TOTAL_NANOS =
            LOWER_GUARD_TRANSITION_NANOS + LOWER_GUARD_HOLD_NANOS;
    private static final long SHEATH_NANOS = 1_350_000_000L;

    private static final long ATTACK_1_NANOS = 460_000_000L;
    private static final long ATTACK_2_NANOS = 500_000_000L;
    private static final long ATTACK_3_NANOS = 660_000_000L;
    private static final long ATTACK_1_CHAIN_NANOS = 350_000_000L;
    private static final long ATTACK_2_CHAIN_NANOS = 400_000_000L;
    private static final long ATTACK_3_CHAIN_NANOS = 550_000_000L;
    private static final long COMBO_WINDOW_NANOS = 1_400_000_000L;

    private static final long ATTACK_REENGAGE_BLEND_NANOS = 90_000_000L;
    private static final long RECOVERY_BLEND_NANOS = 160_000_000L;
    private static final long SHEATH_BLEND_NANOS = 150_000_000L;
    private static final long NETWORK_BLEND_NANOS = 100_000_000L;

    private static final Map<UUID, Entry> STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, WalkTracker> WALK_TRACKERS = new ConcurrentHashMap<>();

    private KatanaClientStanceState() {}

    public static void accept(KatanaStancePayload payload) {
        try {
            UUID id = UUID.fromString(payload.playerUuid());
            long now = System.nanoTime();
            int stance = validStance(payload.stance());
            if (stance == KatanaStanceManager.SHEATHED) {
                STATES.remove(id);
                return;
            }
            long elapsed = Math.max(0, payload.elapsedTicks()) * TICK_NANOS;
            long sinceAttack = Math.max(0, payload.sinceAttackTicks()) * TICK_NANOS;
            long incomingStart = now - elapsed;

            STATES.compute(id, (ignored, current) -> {
                if (current != null) {
                    advance(current, now);
                    // Preserve smooth local prediction if the server confirms the exact same state.
                    if (current.stance == stance
                            && Math.abs(current.stateStartedNanos - incomingStart) < 250_000_000L) {
                        current.lastAttackNanos = Math.max(current.lastAttackNanos, now - sinceAttack);
                        return current;
                    }
                    String prevClip = KatanaAnimationLibrary.clipFor(current.stance);
                    float prevSec = Math.max(0.0F, (now - current.stateStartedNanos) / 1_000_000_000.0F);
                    Entry updated = new Entry(stance, incomingStart, now - sinceAttack, incomingStart);
                    updated.comboStep = KatanaStanceManager.comboStepForStance(stance);
                    updated.setBlend(prevClip, prevSec, now, NETWORK_BLEND_NANOS);
                    return updated;
                }
                Entry created = new Entry(stance, incomingStart, now - sinceAttack, incomingStart);
                created.comboStep = KatanaStanceManager.comboStepForStance(stance);
                return created;
            });
        } catch (IllegalArgumentException ignored) {
            // Ignore malformed network identities; never let a bad packet break rendering.
        }
    }

    /** Called immediately on the local attack edge, before the server round-trip completes. */
    public static void predictAttack(UUID id) {
        long now = System.nanoTime();
        STATES.compute(id, (ignored, current) -> {
            if (current == null) {
                return new Entry(KatanaStanceManager.DRAWING, now, now, now);
            }

            advance(current, now);
            String prevClip = KatanaAnimationLibrary.clipFor(current.stance);
            float prevSec = Math.max(0.0F, (now - current.stateStartedNanos) / 1_000_000_000.0F);

            if (current.stance == KatanaStanceManager.SHEATHED
                    || current.stance == KatanaStanceManager.SHEATHING) {
                Entry next = new Entry(KatanaStanceManager.DRAWING, now, now, now);
                next.setBlend(prevClip, prevSec, now, NETWORK_BLEND_NANOS);
                return next;
            }

            if (current.stance == KatanaStanceManager.DRAWING) {
                long elapsed = now - current.stateStartedNanos;
                current.lastAttackNanos = now;
                if (elapsed >= DRAW_EARLY_ATTACK_NANOS) {
                    startAttack(current, 1, now, prevClip, prevSec, ATTACK_REENGAGE_BLEND_NANOS);
                } else {
                    current.queuedAttack = true;
                }
                return current;
            }

            if (KatanaStanceManager.isAttackStance(current.stance)) {
                long elapsed = now - current.stateStartedNanos;
                current.lastAttackNanos = now;
                if (current.stance == KatanaStanceManager.ATTACK_1) {
                    if (elapsed >= ATTACK_1_CHAIN_NANOS) {
                        startAttack(current, 2, now, prevClip, prevSec, 65_000_000L);
                    } else {
                        current.queuedAttack = true;
                    }
                } else if (current.stance == KatanaStanceManager.ATTACK_2) {
                    if (elapsed >= ATTACK_2_CHAIN_NANOS) {
                        startAttack(current, 3, now, prevClip, prevSec, 65_000_000L);
                    } else {
                        current.queuedAttack = true;
                    }
                } else {
                    if (elapsed >= ATTACK_3_CHAIN_NANOS) {
                        startAttack(current, 1, now, prevClip, prevSec, 75_000_000L);
                    } else if (elapsed >= 380_000_000L) {
                        current.queuedAttack = true;
                    }
                }
                return current;
            }

            // UNSHEATHED or LOWER_GUARD: transition cleanly into the next combo attack.
            int nextStep = (current.stance == KatanaStanceManager.UNSHEATHED && current.comboStep > 0
                    && (now - current.lastAttackNanos) <= COMBO_WINDOW_NANOS)
                    ? ((current.comboStep % 3) + 1)
                    : 1;
            startAttack(current, nextStep, now, prevClip, prevSec, ATTACK_REENGAGE_BLEND_NANOS);
            return current;
        });
    }

    public static Snapshot get(UUID id) {
        long now = System.nanoTime();
        Entry entry = STATES.get(id);
        if (entry == null) {
            return new Snapshot(KatanaStanceManager.SHEATHED,
                    (float) ((now / 1_000_000_000.0) % 3.2));
        }

        advance(entry, now);
        if (entry.stance == KatanaStanceManager.SHEATHED) {
            STATES.remove(id, entry);
            return new Snapshot(KatanaStanceManager.SHEATHED,
                    (float) ((now / 1_000_000_000.0) % 3.2));
        }
        return new Snapshot(entry.stance, Math.max(0.0F, (now - entry.stateStartedNanos) / 1_000_000_000.0F));
    }

    /** Resolves the full blended render pose (stance + cross-fade + smooth walk cycle) for a player. */
    public static RenderPose resolveRenderPose(Player player) {
        long now = System.nanoTime();
        UUID id = player.getUUID();
        Entry entry = STATES.get(id);

        if (entry == null) {
            WalkTracker walk = WALK_TRACKERS.computeIfAbsent(id, ignored -> new WalkTracker(now));
            walk.update(player, now, false);
            return new RenderPose(
                    KatanaStanceManager.SHEATHED,
                    KatanaAnimationLibrary.clipFor(KatanaStanceManager.SHEATHED),
                    (float) ((now / 1_000_000_000.0) % 3.2),
                    0.0F,
                    null,
                    0.0F,
                    1.0F,
                    0.0F,
                    0.0F);
        }

        advance(entry, now);
        if (entry.stance == KatanaStanceManager.SHEATHED) {
            STATES.remove(id, entry);
            WalkTracker walk = WALK_TRACKERS.computeIfAbsent(id, ignored -> new WalkTracker(now));
            walk.update(player, now, false);
            return new RenderPose(
                    KatanaStanceManager.SHEATHED,
                    KatanaAnimationLibrary.clipFor(KatanaStanceManager.SHEATHED),
                    (float) ((now / 1_000_000_000.0) % 3.2),
                    0.0F,
                    null,
                    0.0F,
                    1.0F,
                    0.0F,
                    0.0F);
        }

        int stance = entry.stance;
        String clip = KatanaAnimationLibrary.clipFor(stance);
        float elapsedSeconds = Math.max(0.0F, (now - entry.stateStartedNanos) / 1_000_000_000.0F);

        float blendWeight = 1.0F;
        String blendFromClip = null;
        float blendFromSeconds = 0.0F;
        if (entry.blendFromClip != null && entry.blendDurationNanos > 0L) {
            long blendElapsed = now - entry.blendStartedNanos;
            if (blendElapsed < entry.blendDurationNanos) {
                float raw = Math.max(0.0F, Math.min(1.0F, (float) blendElapsed / (float) entry.blendDurationNanos));
                blendWeight = raw * raw * (3.0F - 2.0F * raw);
                blendFromClip = entry.blendFromClip;
                blendFromSeconds = entry.blendFromSeconds;
            } else {
                entry.blendFromClip = null;
            }
        }

        boolean walkEligible = stance == KatanaStanceManager.UNSHEATHED
                || stance == KatanaStanceManager.LOWER_GUARD;
        WalkTracker walk = WALK_TRACKERS.computeIfAbsent(id, ignored -> new WalkTracker(now));
        walk.update(player, now, walkEligible);

        return new RenderPose(
                stance,
                clip,
                elapsedSeconds,
                elapsedSeconds,
                blendFromClip,
                blendFromSeconds,
                blendWeight,
                walk.walkBlend,
                walk.walkPhaseSeconds);
    }

    public static void clear() {
        STATES.clear();
        WALK_TRACKERS.clear();
    }

    private static void startAttack(Entry entry, int step, long now,
                                    String prevClip, float prevSec, long blendNanos) {
        entry.comboStep = step;
        entry.stance = KatanaStanceManager.attackStanceForStep(step);
        entry.stateStartedNanos = now;
        entry.lastAttackNanos = now;
        entry.combatReadyNanos = now;
        entry.queuedAttack = false;
        entry.setBlend(prevClip, prevSec, now, blendNanos);
    }

    private static void advance(Entry entry, long now) {
        // Loop at most a few state transitions if a frame stalled.
        for (int guard = 0; guard < 6; guard++) {
            long elapsed = now - entry.stateStartedNanos;
            switch (entry.stance) {
                case KatanaStanceManager.DRAWING -> {
                    if (elapsed < DRAW_NANOS) return;
                    long boundary = entry.stateStartedNanos + DRAW_NANOS;
                    if (entry.queuedAttack) {
                        startAttack(entry, 1, boundary,
                                KatanaAnimationLibrary.clipFor(KatanaStanceManager.DRAWING),
                                DRAW_NANOS / 1_000_000_000.0F, 60_000_000L);
                    } else {
                        entry.stance = KatanaStanceManager.UNSHEATHED;
                        entry.stateStartedNanos = boundary;
                        entry.combatReadyNanos = boundary;
                        entry.comboStep = 0;
                    }
                }
                case KatanaStanceManager.ATTACK_1 -> {
                    if (entry.queuedAttack && elapsed >= ATTACK_1_CHAIN_NANOS) {
                        long boundary = entry.stateStartedNanos + ATTACK_1_CHAIN_NANOS;
                        startAttack(entry, 2, boundary,
                                KatanaAnimationLibrary.clipFor(KatanaStanceManager.ATTACK_1),
                                ATTACK_1_CHAIN_NANOS / 1_000_000_000.0F, 65_000_000L);
                    } else if (elapsed >= ATTACK_1_NANOS) {
                        long boundary = entry.stateStartedNanos + ATTACK_1_NANOS;
                        entry.setBlend(KatanaAnimationLibrary.clipFor(KatanaStanceManager.ATTACK_1),
                                ATTACK_1_NANOS / 1_000_000_000.0F, boundary, RECOVERY_BLEND_NANOS);
                        entry.stance = KatanaStanceManager.UNSHEATHED;
                        entry.stateStartedNanos = boundary;
                        entry.combatReadyNanos = boundary;
                        entry.queuedAttack = false;
                    } else {
                        return;
                    }
                }
                case KatanaStanceManager.ATTACK_2 -> {
                    if (entry.queuedAttack && elapsed >= ATTACK_2_CHAIN_NANOS) {
                        long boundary = entry.stateStartedNanos + ATTACK_2_CHAIN_NANOS;
                        startAttack(entry, 3, boundary,
                                KatanaAnimationLibrary.clipFor(KatanaStanceManager.ATTACK_2),
                                ATTACK_2_CHAIN_NANOS / 1_000_000_000.0F, 65_000_000L);
                    } else if (elapsed >= ATTACK_2_NANOS) {
                        long boundary = entry.stateStartedNanos + ATTACK_2_NANOS;
                        entry.setBlend(KatanaAnimationLibrary.clipFor(KatanaStanceManager.ATTACK_2),
                                ATTACK_2_NANOS / 1_000_000_000.0F, boundary, RECOVERY_BLEND_NANOS);
                        entry.stance = KatanaStanceManager.UNSHEATHED;
                        entry.stateStartedNanos = boundary;
                        entry.combatReadyNanos = boundary;
                        entry.queuedAttack = false;
                    } else {
                        return;
                    }
                }
                case KatanaStanceManager.ATTACK_3 -> {
                    if (entry.queuedAttack && elapsed >= ATTACK_3_CHAIN_NANOS) {
                        long boundary = entry.stateStartedNanos + ATTACK_3_CHAIN_NANOS;
                        startAttack(entry, 1, boundary,
                                KatanaAnimationLibrary.clipFor(KatanaStanceManager.ATTACK_3),
                                ATTACK_3_CHAIN_NANOS / 1_000_000_000.0F, 75_000_000L);
                    } else if (elapsed >= ATTACK_3_NANOS) {
                        long boundary = entry.stateStartedNanos + ATTACK_3_NANOS;
                        entry.stance = KatanaStanceManager.UNSHEATHED;
                        entry.stateStartedNanos = boundary;
                        entry.combatReadyNanos = boundary;
                        entry.comboStep = 0;
                        entry.queuedAttack = false;
                    } else {
                        return;
                    }
                }
                case KatanaStanceManager.UNSHEATHED -> {
                    long idleReference = Math.max(entry.lastAttackNanos, entry.combatReadyNanos);
                    if (now - idleReference < IDLE_TO_LOWER_GUARD_NANOS) return;
                    long boundary = idleReference + IDLE_TO_LOWER_GUARD_NANOS;
                    entry.stance = KatanaStanceManager.LOWER_GUARD;
                    entry.stateStartedNanos = boundary;
                    entry.comboStep = 0;
                    entry.queuedAttack = false;
                }
                case KatanaStanceManager.LOWER_GUARD -> {
                    // 0.35s transition + 5.0s relaxed hold = 5.35s before sheathing.
                    if (elapsed < LOWER_GUARD_TOTAL_NANOS) return;
                    long boundary = entry.stateStartedNanos + LOWER_GUARD_TOTAL_NANOS;
                    entry.setBlend(KatanaAnimationLibrary.clipFor(KatanaStanceManager.LOWER_GUARD),
                            LOWER_GUARD_TRANSITION_NANOS / 1_000_000_000.0F, boundary, SHEATH_BLEND_NANOS);
                    entry.stance = KatanaStanceManager.SHEATHING;
                    entry.stateStartedNanos = boundary;
                    entry.comboStep = 0;
                    entry.queuedAttack = false;
                }
                case KatanaStanceManager.SHEATHING -> {
                    if (elapsed < SHEATH_NANOS) return;
                    entry.stance = KatanaStanceManager.SHEATHED;
                    entry.stateStartedNanos += SHEATH_NANOS;
                    return;
                }
                default -> {
                    entry.stance = KatanaStanceManager.SHEATHED;
                    return;
                }
            }
        }
    }

    private static int validStance(int stance) {
        return stance >= KatanaStanceManager.SHEATHED && stance <= KatanaStanceManager.ATTACK_3
                ? stance : KatanaStanceManager.SHEATHED;
    }

    private static final class Entry {
        private int stance;
        private long stateStartedNanos;
        private long lastAttackNanos;
        private long combatReadyNanos;
        private int comboStep;
        private boolean queuedAttack;
        private String blendFromClip;
        private float blendFromSeconds;
        private long blendStartedNanos;
        private long blendDurationNanos;

        private Entry(int stance, long stateStartedNanos, long lastAttackNanos, long combatReadyNanos) {
            this.stance = stance;
            this.stateStartedNanos = stateStartedNanos;
            this.lastAttackNanos = lastAttackNanos;
            this.combatReadyNanos = combatReadyNanos;
        }

        private void setBlend(String fromClip, float fromSeconds, long startedNanos, long durationNanos) {
            this.blendFromClip = fromClip;
            this.blendFromSeconds = fromSeconds;
            this.blendStartedNanos = startedNanos;
            this.blendDurationNanos = durationNanos;
        }
    }

    private static final class WalkTracker {
        private long lastUpdateNanos;
        private float walkBlend;
        private float walkPhaseSeconds;

        private WalkTracker(long now) {
            this.lastUpdateNanos = now;
        }

        private void update(Player player, long now, boolean eligible) {
            long deltaNanos = now - lastUpdateNanos;
            if (deltaNanos <= 0L) return;
            lastUpdateNanos = now;

            float dt = Math.min(0.05F, deltaNanos / 1_000_000_000.0F);
            double dx = player.getX() - player.xo;
            double dz = player.getZ() - player.zo;
            double stepDist = Math.hypot(dx, dz);
            double velDist = player.getDeltaMovement().horizontalDistance();
            double speed = Math.max(stepDist, velDist);

            float target = 0.0F;
            if (eligible && speed > 0.008D) {
                target = Mth.clamp((float) ((speed - 0.008D) / 0.11D), 0.35F, 1.0F);
            }

            float alpha = 1.0F - (float) Math.exp(-dt / 0.12F);
            walkBlend += (target - walkBlend) * alpha;
            if (walkBlend < 0.004F && target == 0.0F) {
                walkBlend = 0.0F;
            }

            if (walkBlend > 0.004F) {
                float rate = target > 0.0F ? (0.85F + 0.45F * target) : 0.65F;
                walkPhaseSeconds = (walkPhaseSeconds + dt * rate) % 0.8F;
            } else {
                walkPhaseSeconds = 0.0F;
            }
        }
    }

    public record Snapshot(int stance, float elapsedSeconds) {}

    public record RenderPose(
            int stance,
            String clip,
            float elapsedSeconds,
            float stanceSeconds,
            String blendFromClip,
            float blendFromSeconds,
            float blendWeight,
            float walkWeight,
            float walkPhaseSeconds
    ) {
        public static RenderPose simple(int stance, String clip, float elapsedSeconds) {
            return new RenderPose(stance, clip, elapsedSeconds, elapsedSeconds,
                    null, 0.0F, 1.0F, 0.0F, 0.0F);
        }
    }
}
