package com.github.ob_yekt.simpleskills.fishing;

import java.util.random.RandomGenerator;

/**
 * Pure (Minecraft-free) state machine of the fish fight: what the fish is doing, how tired it is,
 * how tense the line is and how far the catch has progressed.
 *
 * <p>The model only knows about <em>inputs per tick</em> (is the player reeling, how well is the
 * player countering the pull with the camera) and about the level-based {@link Params}. All the
 * world interaction (moving the fish, sending packets, rewards) lives in {@link FishingManager}.</p>
 *
 * <p>One call to {@link #tick} equals one server tick (1/20 s).</p>
 */
public final class FishingFightModel {

    /** What the fish is doing right now. {@code dir} is the side from the player's point of view. */
    public enum Move {
        LEFT(-1), PAUSE(0), RIGHT(1);

        public final int dir;

        Move(int dir) {
            this.dir = dir;
        }

        public static Move fromOrdinal(int ordinal) {
            Move[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : PAUSE;
        }
    }

    public enum Outcome { ONGOING, CAUGHT, LINE_BROKEN }

    /** Level / species / config dependent knobs. All multipliers are 1.0 for "vanilla balance". */
    public record Params(double fatigueMult, double tensionMult, double recoveryMult,
                         double maxTension, double speciesPull, double speciesFatigue,
                         double counterEffect) {
        public static final Params DEFAULT = new Params(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0);
    }

    // ---- Tuning (per tick) -------------------------------------------------------------------
    static final double FATIGUE_PULL = 0.0045;      // fatigue gained while the fish pulls
    static final double FATIGUE_COUNTER_BONUS = 0.8; // extra fatigue (fraction) when the pull is countered
    static final double FATIGUE_RECOVER = 0.0012;   // fatigue lost while resting and NOT being reeled

    static final double PULL_TENSION = 0.018;       // tension/tick of an unanswered pull of a fresh fish
    static final double COUNTER_TENSION_CUT = 0.80; // how much of the pull a perfect counter absorbs
    static final double REEL_TENSION_PULL = 0.026;  // reeling while the fish pulls
    static final double REEL_TENSION_REST = 0.011;  // reeling while the fish rests
    static final double TENSION_DECAY = 0.009;      // relief while NOT reeling
    static final double JERK_CHANCE = 0.03;         // per pulling tick: sudden tug of the fish
    static final double JERK_SIZE = 0.08;           // tension added by a tug of a fresh fish

    static final double REEL_PROGRESS = 0.0080;     // progress/tick while reeling a resting fish
    static final double REEL_PROGRESS_PULL = 0.35;  // fraction of that while the fish pulls
    static final double PROGRESS_LOSS = 0.0015;     // line the fish takes back while pulling un-reeled

    // ---- State -------------------------------------------------------------------------------
    private final Params params;
    private final RandomGenerator random;

    private Move move = Move.PAUSE;
    private int moveTicksLeft;
    private double fatigue;
    private double tension;
    private double progress;
    private boolean everMoved;

    public FishingFightModel(Params params, RandomGenerator random) {
        this.params = params;
        this.random = random;
        pickNextMove();
        // The very first move is always a pull so the fight starts with a clear direction.
        if (move == Move.PAUSE) {
            move = random.nextBoolean() ? Move.LEFT : Move.RIGHT;
            moveTicksLeft = 25 + random.nextInt(20);
        }
    }

    public Move move() { return move; }
    public double fatigue() { return fatigue; }
    /** Raw tension, breaks the line when it reaches {@link Params#maxTension()}. */
    public double tension() { return tension; }
    /** Tension as a 0..1 ratio of the break limit (what the HUD shows). */
    public double tensionRatio() { return Math.min(1.0, tension / params.maxTension()); }
    public double progress() { return progress; }
    public boolean isPulling() { return move != Move.PAUSE; }
    /** 1.0 = fresh fish, 0.25 = exhausted. */
    public double resistance() { return 1.0 - 0.75 * fatigue; }

    /**
     * Advances the fight by one tick.
     *
     * @param reeling whether the player is currently reeling in the line
     * @param counter 0..1 how well the player is countering the current pull with the camera
     */
    public Outcome tick(boolean reeling, double counter) {
        if (--moveTicksLeft <= 0) {
            pickNextMove();
        }

        final boolean pulling = isPulling();
        final double resistance = resistance();
        // Strong (big) fish absorb part of the camera counter-movement.
        counter = clamp01(counter) * params.counterEffect();

        // ---- fatigue ----
        if (pulling) {
            fatigue += FATIGUE_PULL * params.fatigueMult() * params.speciesFatigue()
                    * (1.0 + FATIGUE_COUNTER_BONUS * counter);
        } else if (!reeling) {
            fatigue -= FATIGUE_RECOVER;
        }
        fatigue = clamp01(fatigue);

        // ---- tension ----
        double delta = 0.0;
        if (pulling) {
            delta += PULL_TENSION * resistance * params.speciesPull() * params.tensionMult()
                    * (1.0 - COUNTER_TENSION_CUT * counter);
        }
        if (reeling) {
            double reel = pulling
                    ? REEL_TENSION_PULL * (0.35 + 0.65 * resistance)
                    : REEL_TENSION_REST * resistance;
            delta += reel * params.tensionMult();
        } else {
            delta -= TENSION_DECAY * params.recoveryMult();
        }
        if (pulling && random.nextDouble() < JERK_CHANCE) {
            // Sudden tug: it is what makes keeping a safety margin worthwhile.
            delta += JERK_SIZE * resistance * params.speciesPull() * params.tensionMult()
                    * (1.0 - 0.5 * counter);
        }
        tension = Math.max(0.0, tension + delta);

        // ---- progress ----
        if (reeling) {
            double speed = REEL_PROGRESS * (pulling ? REEL_PROGRESS_PULL : 1.0) * (0.55 + 0.45 * fatigue);
            progress += speed;
        } else if (pulling) {
            progress -= PROGRESS_LOSS * resistance;
        }
        progress = clamp01(progress);

        if (tension >= params.maxTension()) {
            return Outcome.LINE_BROKEN;
        }
        if (progress >= 1.0) {
            return Outcome.CAUGHT;
        }
        return Outcome.ONGOING;
    }

    private void pickNextMove() {
        // Weighted choice; the fish never pauses twice in a row and tires into longer pauses.
        double r = random.nextDouble();
        Move next;
        if (r < 0.38) {
            next = Move.LEFT;
        } else if (r < 0.76) {
            next = Move.RIGHT;
        } else {
            next = Move.PAUSE;
        }
        if (next == Move.PAUSE && move == Move.PAUSE && everMoved) {
            next = random.nextBoolean() ? Move.LEFT : Move.RIGHT;
        }
        move = next;
        everMoved = true;

        if (next == Move.PAUSE) {
            moveTicksLeft = 20 + random.nextInt(26) + (int) Math.round(fatigue * 30.0);
        } else {
            // Rested fish pull for longer, tired ones in short bursts.
            int base = 22 + random.nextInt(34);
            moveTicksLeft = Math.max(12, (int) Math.round(base * (1.0 - 0.35 * fatigue)));
        }
    }

    /** Camera offset (degrees, relative to the fight's neutral yaw) -> counter strength. */
    public static double counterFromOffset(int fishDir, double yawOffsetDeg) {
        if (fishDir == 0) {
            return 0.0;
        }
        // Fish goes left (-1)  -> player must look to the right (positive yaw offset), and vice versa.
        double opposite = -fishDir * yawOffsetDeg;
        return clamp01((opposite - 4.0) / 14.0);
    }

    private static double clamp01(double v) {
        return v < 0.0 ? 0.0 : Math.min(v, 1.0);
    }
}
