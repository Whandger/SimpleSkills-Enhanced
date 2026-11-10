package com.github.ob_yekt.simpleskills.enchanting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RuneInfusionClientState {
    public static final int RUNNING = 1;
    public static final int SUCCEEDED = 2;
    public static final int FAILED = 3;

    /** ageTicks / growthRequired = crack size (0..1); at 1.0 it damages the enchantment. */
    public record Crack(int id, float x, float y, int ageTicks, int growthRequired, boolean repairing) {}

    public record State(
            UUID sessionId,
            int state,
            int integrity,
            int elapsedTicks,
            int durationTicks,
            int grade,
            int repairedCracks,
            int propagatedCracks,
            int inventorySlot,
            List<Crack> cracks
    ) {}

    private static volatile State current;
    /** Session whose screen is (or was) opened; a screen opens once per session id. */
    private static volatile UUID shownSession;
    private static volatile boolean ending;

    private RuneInfusionClientState() {}

    public static void accept(RuneInfusionStatePayload payload) {
        UUID incoming = new UUID(payload.sessionMost(), payload.sessionLeast());

        if (payload.state() == RuneInfusionStatePayload.STOP) {
            // Keep the final snapshot so the screen can show the result; the screen resets us on close.
            if (incoming.equals(shownSession)) {
                ending = true;
            } else {
                reset();
            }
            return;
        }

        State existing = current;
        if (existing == null || !existing.sessionId().equals(incoming)) {
            ending = false;
        }

        List<Crack> cracks = new ArrayList<>();
        if (!payload.cracks().isEmpty()) {
            for (String row : payload.cracks().split(";")) {
                String[] v = row.split(",", -1);
                if (v.length != 6) continue;
                try {
                    cracks.add(new Crack(
                            Integer.parseInt(v[0]),
                            Integer.parseInt(v[1]) / 1000f,
                            Integer.parseInt(v[2]) / 1000f,
                            Integer.parseInt(v[3]),
                            Math.max(1, Integer.parseInt(v[4])),
                            "1".equals(v[5])));
                } catch (NumberFormatException ignored) {
                    // Ignore malformed visual data.
                }
            }
        }

        current = new State(
                incoming,
                payload.state(),
                Math.max(0, Math.min(1000, payload.integrity())),
                payload.elapsedTicks(),
                payload.durationTicks(),
                payload.grade(),
                payload.repairedCracks(),
                payload.propagatedCracks(),
                payload.inventorySlot(),
                List.copyOf(cracks));
    }

    public static State get() { return current; }

    public static UUID shownSession() { return shownSession; }

    public static void markShown(UUID sessionId) {
        shownSession = sessionId;
        ending = false;
    }

    public static boolean hasScreen() { return shownSession != null; }

    public static boolean isEnding() { return ending; }

    /** Called when the minigame screen is removed. */
    public static void screenClosed() {
        shownSession = null;
        if (ending) {
            current = null;
            ending = false;
        }
        // If the screen was replaced mid-session (not ending) the next snapshot reopens it.
    }

    public static void reset() {
        current = null;
        shownSession = null;
        ending = false;
    }
}
