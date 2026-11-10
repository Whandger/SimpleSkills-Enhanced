
package com.github.ob_yekt.simpleskills.enchanting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RuneInfusionClientState {
    public record Crack(
            int id,
            float x,
            float y,
            int repairProgress,
            int repairRequired,
            int ageTicks,
            boolean repaired
    ) {}

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

    private RuneInfusionClientState() {}

public static void accept(RuneInfusionStatePayload payload) {
    UUID incomingSession = new UUID(
            payload.sessionMost(),
            payload.sessionLeast()
    );

    if (payload.state() == RuneInfusionStatePayload.STOP) {
        State existing = current;

        if (existing != null && existing.sessionId().equals(incomingSession)) {
            current = null;
        }
        return;
    }

    List<Crack> cracks = new ArrayList<>();

    if (!payload.cracks().isEmpty()) {
        for (String row : payload.cracks().split(";")) {
            String[] v = row.split(",", -1);
            if (v.length != 7) continue;

            try {
                cracks.add(new Crack(
                        Integer.parseInt(v[0]),
                        Integer.parseInt(v[1]) / 1000f,
                        Integer.parseInt(v[2]) / 1000f,
                        Integer.parseInt(v[3]),
                        Integer.parseInt(v[4]),
                        Integer.parseInt(v[5]),
                        "1".equals(v[6])
                ));
            } catch (NumberFormatException ignored) {
                // Ignora dados visuais inválidos.
            }
        }
    }

    current = new State(
            incomingSession,
            payload.state(),
            Math.max(0, Math.min(1000, payload.integrity())),
            payload.elapsedTicks(),
            payload.durationTicks(),
            payload.grade(),
            payload.repairedCracks(),
            payload.propagatedCracks(),
            payload.inventorySlot(),
            List.copyOf(cracks)
    );
}


    public static State get() {
        return current;
    }

    public static void reset() {
        current = null;
    }
}
