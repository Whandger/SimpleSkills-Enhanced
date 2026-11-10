package com.github.ob_yekt.simpleskills.enchanting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * Authoritative minigame model. Difficulty is the SUM of the target levels of all selected
 * enchantments (1..40). No client-supplied value influences progress.
 */
public final class RuneInfusionMinigame {
    public static final int TICK_RATE = 20;
    public static final int MAX_INTEGRITY = 1000;
    public static final int MIN_DIFFICULTY = 1;
    public static final int MAX_DIFFICULTY = 40;

    private static final int DAMAGE_INTERVAL = 20;
    private static final int HOLD_TIMEOUT_TICKS = 8;
    private static final int IDLE_RESPAWN_TICKS = 40;

    public enum State { RUNNING, SUCCEEDED, FAILED }
    public enum Grade { POOR, FAIR, GOOD, EXCELLENT, PERFECT }

    public record Difficulty(
            int score,
            int growthTicks,
            int spawnIntervalTicks,
            int maxActiveCracks,
            int initialCracks,
            int repairPerTick,
            int crackDamage,
            int durationTicks,
            double branchChance
    ) {
        public Difficulty {
            if (score < 1 || growthTicks < 1 || spawnIntervalTicks < 1 || maxActiveCracks < 1
                    || initialCracks < 1 || repairPerTick < 1 || crackDamage < 1
                    || durationTicks < 1 || branchChance < 0.0 || branchChance > 1.0) {
                throw new IllegalArgumentException("Invalid difficulty parameters");
            }
        }
    }

    /** ageTicks / growthRequired is the crack's size (0..1). At 1.0 the crack damages the enchantment. */
    public record CrackSnapshot(int id, float x, float y, int ageTicks, int growthRequired, boolean repairing) {}

    public record Snapshot(
            State state,
            int integrity,
            int elapsedTicks,
            int durationTicks,
            int repairedCracks,
            int propagatedCracks,
            int activeCracks,
            Grade grade,
            List<CrackSnapshot> cracks
    ) {}

    private static final class Crack {
        final int id;
        final float x;
        final float y;
        int age;
        int damageTimer;
        boolean propagated;

        Crack(int id, float x, float y, int age) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.age = age;
        }
    }

    private final UUID sessionId;
    private final Random random;
    private final Difficulty difficulty;
    private final List<Crack> cracks = new ArrayList<>();

    private State state = State.RUNNING;
    private int integrity = MAX_INTEGRITY;
    private int elapsedTicks;
    private int nextCrackId;
    private int repairedCracks;
    private int propagatedCracks;
    private int totalSpawned;
    private int criticalPropagations;
    private int idleTicks;

    private int repairingCrackId = -1;
    private int lastHoldTick;

    private RuneInfusionMinigame(UUID sessionId, long seed, Difficulty difficulty) {
        this.sessionId = Objects.requireNonNull(sessionId);
        this.random = new Random(seed);
        this.difficulty = Objects.requireNonNull(difficulty);
        for (int i = 0; i < difficulty.initialCracks(); i++) {
            spawnCrack(Float.NaN, Float.NaN);
        }
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** difficultyScore = sum of the target levels of every selected enchantment. */
    public static Difficulty difficultyFor(int difficultyScore) {
        int d = Math.max(MIN_DIFFICULTY, Math.min(MAX_DIFFICULTY, difficultyScore));
        double t = (d - 1) / (double) (MAX_DIFFICULTY - 1);

        int growth = (int) Math.round(lerp(340, 130, t));
        int spawn = (int) Math.round(lerp(220, 70, t));
        int maxActive = Math.min(5, 1 + (d + 1) / 7);
        int initial = d >= 25 ? 3 : d >= 10 ? 2 : 1;
        int repair = d <= 12 ? 4 : d <= 26 ? 3 : 2;
        int damage = (int) Math.round(lerp(25, 60, t));
        int duration = 20 * (int) Math.round(lerp(26, 50, t));
        double branch = lerp(0.0, 0.30, t);

        return new Difficulty(d, growth, spawn, Math.min(maxActive, Math.max(initial, maxActive)),
                Math.min(initial, maxActive), repair, damage, duration, branch);
    }

    public static RuneInfusionMinigame start(UUID sessionId, long serverSeed, int difficultyScore) {
        return new RuneInfusionMinigame(sessionId, serverSeed, difficultyFor(difficultyScore));
    }

    public synchronized Snapshot tick() {
        if (state != State.RUNNING) return snapshot();

        elapsedTicks++;

        // The player must keep refreshing the hold; a lost RELEASE packet cannot repair forever.
        if (repairingCrackId >= 0 && elapsedTicks - lastHoldTick > HOLD_TIMEOUT_TICKS) {
            repairingCrackId = -1;
        }

        for (Crack crack : new ArrayList<>(cracks)) {
            if (crack.id == repairingCrackId) {
                crack.damageTimer = 0;
                repair(crack);
                continue;
            }

            if (crack.age < difficulty.growthTicks()) {
                crack.age++;
                if (crack.age >= difficulty.growthTicks() && !crack.propagated) {
                    crack.propagated = true;
                    propagatedCracks++;
                    criticalPropagations++;
                    if (random.nextDouble() < difficulty.branchChance()) {
                        spawnBranch(crack.x, crack.y);
                    }
                }
            } else {
                crack.damageTimer++;
                if (crack.damageTimer >= DAMAGE_INTERVAL) {
                    crack.damageTimer = 0;
                    integrity = Math.max(0, integrity - difficulty.crackDamage());
                }
            }
        }

        if (cracks.isEmpty()) {
            if (++idleTicks >= Math.min(IDLE_RESPAWN_TICKS, difficulty.spawnIntervalTicks())) {
                idleTicks = 0;
                spawnCrack(Float.NaN, Float.NaN);
            }
        } else {
            idleTicks = 0;
            if (elapsedTicks % difficulty.spawnIntervalTicks() == 0) {
                spawnCrack(Float.NaN, Float.NaN);
            }
        }

        if (integrity <= 0) {
            state = State.FAILED;
            repairingCrackId = -1;
        } else if (elapsedTicks >= difficulty.durationTicks()) {
            state = State.SUCCEEDED;
            repairingCrackId = -1;
        }

        return snapshot();
    }

    public synchronized boolean beginRepair(UUID suppliedSession, int crackId, int clientTick) {
        if (state != State.RUNNING || !sessionId.equals(suppliedSession)) return false;
        Crack crack = findCrack(crackId);
        if (crack == null) return false;

        // No instant progress: repair is applied exclusively by the server tick.
        repairingCrackId = crackId;
        lastHoldTick = elapsedTicks;
        crack.damageTimer = 0;
        return true;
    }

    public synchronized boolean holdRepair(UUID suppliedSession, int crackId, int clientTick) {
        if (state != State.RUNNING || !sessionId.equals(suppliedSession)) return false;
        Crack crack = findCrack(crackId);
        if (crack == null) {
            if (repairingCrackId == crackId) repairingCrackId = -1;
            return false;
        }
        // Recovers gracefully if the server-side hold timed out but the client is still holding.
        repairingCrackId = crackId;
        lastHoldTick = elapsedTicks;
        return true;
    }

    public synchronized boolean releaseRepair(UUID suppliedSession, int crackId) {
        if (!sessionId.equals(suppliedSession) || repairingCrackId != crackId) return false;
        repairingCrackId = -1;
        return true;
    }

    public synchronized void fail() {
        if (state == State.RUNNING) state = State.FAILED;
        repairingCrackId = -1;
    }

    private void repair(Crack crack) {
        crack.age = Math.max(0, crack.age - difficulty.repairPerTick());
        if (crack.age == 0) {
            cracks.remove(crack);
            repairedCracks++;
            if (repairingCrackId == crack.id) repairingCrackId = -1;
        }
    }

    public synchronized Grade grade() {
        if (state != State.SUCCEEDED) return Grade.POOR;

        double integrityRatio = integrity / (double) MAX_INTEGRITY;
        double repairRatio = totalSpawned == 0 ? 1.0 : repairedCracks / (double) totalSpawned;

        if (integrityRatio >= 0.98 && repairRatio >= 0.95 && criticalPropagations == 0) return Grade.PERFECT;
        if (integrityRatio >= 0.85 && repairRatio >= 0.80) return Grade.EXCELLENT;
        if (integrityRatio >= 0.65 && repairRatio >= 0.60) return Grade.GOOD;
        if (integrityRatio >= 0.40 && repairRatio >= 0.35) return Grade.FAIR;
        return Grade.POOR;
    }

    public static int bonusChancePercent(Grade grade) {
        return switch (grade) {
            case POOR -> 0;
            case FAIR -> 1;
            case GOOD -> 3;
            case EXCELLENT -> 6;
            case PERFECT -> 10;
        };
    }

    public static boolean rollBonus(Random serverRandom, Grade grade) {
        return Objects.requireNonNull(serverRandom).nextInt(100) < bonusChancePercent(grade);
    }

    public synchronized Snapshot snapshot() {
        List<CrackSnapshot> visible = cracks.stream()
                .sorted(Comparator.comparingInt(c -> c.id))
                .map(c -> new CrackSnapshot(c.id, c.x, c.y, c.age, difficulty.growthTicks(),
                        c.id == repairingCrackId))
                .toList();

        return new Snapshot(state, integrity, elapsedTicks, difficulty.durationTicks(),
                repairedCracks, propagatedCracks, cracks.size(), grade(), visible);
    }

    public UUID sessionId() { return sessionId; }
    public Difficulty difficulty() { return difficulty; }

    private Crack findCrack(int id) {
        for (Crack crack : cracks) {
            if (crack.id == id) return crack;
        }
        return null;
    }

    private void spawnBranch(float parentX, float parentY) {
        if (cracks.size() >= difficulty.maxActiveCracks()) return;
        float x = clamp(parentX + (random.nextFloat() - 0.5f) * 0.30f);
        float y = clamp(parentY + (random.nextFloat() - 0.5f) * 0.30f);
        addCrack(x, y);
    }

    private void spawnCrack(float parentX, float parentY) {
        if (cracks.size() >= difficulty.maxActiveCracks()) return;

        float x = 0.5f;
        float y = 0.5f;
        // Try to keep new cracks apart so they remain individually readable.
        for (int attempt = 0; attempt < 10; attempt++) {
            x = 0.14f + random.nextFloat() * 0.72f;
            y = 0.14f + random.nextFloat() * 0.72f;
            if (farFromOthers(x, y)) break;
        }
        addCrack(x, y);
    }

    private boolean farFromOthers(float x, float y) {
        for (Crack c : cracks) {
            float dx = c.x - x;
            float dy = c.y - y;
            if (dx * dx + dy * dy < 0.09f) return false;
        }
        return true;
    }

    private void addCrack(float x, float y) {
        int startAge = Math.max(1, difficulty.growthTicks() / 10);
        cracks.add(new Crack(nextCrackId++, x, y, startAge));
        totalSpawned++;
    }

    private static float clamp(float value) {
        return Math.max(0.08f, Math.min(0.92f, value));
    }
}