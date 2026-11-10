
package com.github.ob_yekt.simpleskills.enchanting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public final class RuneInfusionMinigame {
    public static final int TICK_RATE = 20;
    public static final int MAX_INTEGRITY = 1000;
    public static final int MAX_ACTIVE_CRACKS = 3;

    private static final int CRACK_GROWTH_TICKS = 240;
    private static final int REPAIR_RATE = 3;
    private static final int DAMAGE_INTERVAL = 20;

    public enum State { RUNNING, SUCCEEDED, FAILED }
    public enum Grade { POOR, FAIR, GOOD, EXCELLENT, PERFECT }

    public record Difficulty(
            int spawnIntervalTicks,
            int repairTicks,
            int propagationTicks,
            int crackDamage,
            int durationTicks,
            double branchChance
    ) {
        public Difficulty {
            if (spawnIntervalTicks < 1 || repairTicks < 1
                    || propagationTicks < 1 || crackDamage < 1
                    || durationTicks < 1 || branchChance < 0.0
                    || branchChance > 1.0) {
                throw new IllegalArgumentException("Invalid difficulty parameters");
            }
        }
    }

    public record CrackSnapshot(
            int id,
            float x,
            float y,
            int repairProgress,
            int repairRequired,
            int ageTicks,
            int growthRequired,
            boolean repaired
    ) {}

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
        boolean repaired;
        boolean propagated;

        Crack(int id, float x, float y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }

    private final UUID sessionId;
    private final Random random;
    private final Difficulty difficulty;
    private final int selectedEnchantments;
    private final int highestEnchantmentLevel;

    private final List<Crack> cracks = new ArrayList<>();

    private State state = State.RUNNING;
    private int integrity = MAX_INTEGRITY;
    private int elapsedTicks;
    private int nextCrackId;
    private int repairedCracks;
    private int propagatedCracks;
    private int totalSpawned;
    private int criticalPropagations;

    // Rachadura que está sendo reparada pelo jogador.
    private int repairingCrackId = -1;

    private RuneInfusionMinigame(
            UUID sessionId,
            long seed,
            int selectedEnchantments,
            int highestEnchantmentLevel,
            Difficulty difficulty
    ) {
        this.sessionId = Objects.requireNonNull(sessionId);
        this.random = new Random(seed);
        this.selectedEnchantments = Math.max(1, Math.min(4, selectedEnchantments));
        this.highestEnchantmentLevel = Math.max(1, Math.min(10, highestEnchantmentLevel));
        this.difficulty = Objects.requireNonNull(difficulty);

        spawnCrack(Float.NaN, Float.NaN);
    }

    public static Difficulty difficultyFor(int highestLevel, int enchantmentCount) {
        int level = Math.max(1, Math.min(10, highestLevel));
        int count = Math.max(1, Math.min(4, enchantmentCount));

        int band = (level - 1) / 2;
        int extra = count - 1;

        return new Difficulty(
                Math.max(120, 170 - band * 5 - extra * 5),
                1,
                CRACK_GROWTH_TICKS,
                35 + band * 5 + extra * 3,
                20 * (38 + band * 3),
                Math.min(0.12, 0.015 + band * 0.01 + extra * 0.005)
        );
    }

    public static RuneInfusionMinigame start(
            UUID sessionId,
            long serverSeed,
            int selectedEnchantments,
            int highestLevel
    ) {
        Difficulty difficulty = difficultyFor(highestLevel, selectedEnchantments);

        return new RuneInfusionMinigame(
                sessionId,
                serverSeed,
                selectedEnchantments,
                highestLevel,
                difficulty
        );
    }

    public synchronized Snapshot tick() {
        if (state != State.RUNNING) return snapshot();

        elapsedTicks++;

        for (Crack crack : new ArrayList<>(cracks)) {
            if (crack.repaired) continue;

            // Enquanto o jogador segura o botão, reduz o tamanho da rachadura.
            if (crack.id == repairingCrackId) {
                doRepair(crack, REPAIR_RATE);
                continue;
            }

            // Crescimento normal da rachadura.
            if (crack.age < CRACK_GROWTH_TICKS) {
                crack.age++;

                if (crack.age >= CRACK_GROWTH_TICKS && !crack.propagated) {
                    crack.propagated = true;
                    propagatedCracks++;
                    criticalPropagations++;

                    if (random.nextDouble() < difficulty.branchChance()) {
                        spawnCrack(crack.x, crack.y);
                    }
                }
            } else {
                // A rachadura permanece em 100% e começa a causar dano.
                crack.damageTimer++;

                if (crack.damageTimer >= DAMAGE_INTERVAL) {
                    crack.damageTimer = 0;
                    integrity = Math.max(0, integrity - difficulty.crackDamage());
                }
            }
        }

        if (elapsedTicks % difficulty.spawnIntervalTicks() == 0) {
            spawnCrack(Float.NaN, Float.NaN);
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

    public synchronized boolean beginRepair(
            UUID suppliedSession,
            int crackId,
            int clientTick
    ) {
        if (state != State.RUNNING || !sessionId.equals(suppliedSession)) {
            return false;
        }

        Crack crack = findCrack(crackId);

        if (crack == null || crack.repaired) {
            if (repairingCrackId == crackId) {
                repairingCrackId = -1;
            }
            return false;
        }

        repairingCrackId = crackId;
        crack.damageTimer = 0;

        // Regride imediatamente o tamanho da rachadura a cada clique.
        doRepair(crack, 30);

        return true;
    }

    public synchronized boolean holdRepair(
            UUID suppliedSession,
            int crackId,
            int clientTick
    ) {
        if (state != State.RUNNING || !sessionId.equals(suppliedSession)) {
            return false;
        }

        Crack crack = findCrack(crackId);

        if (crack == null || crack.repaired) {
            if (repairingCrackId == crackId) {
                repairingCrackId = -1;
            }
            return false;
        }

        repairingCrackId = crackId;
        crack.damageTimer = 0;

        // Regride continuamente enquanto o botão do mouse é mantido pressionado.
        doRepair(crack, REPAIR_RATE);

        return true;
    }

    private void doRepair(Crack crack, int amount) {
        crack.age = Math.max(0, crack.age - amount);

        if (crack.age <= 0 && !crack.repaired) {
            crack.repaired = true;
            repairedCracks++;

            if (repairingCrackId == crack.id) {
                repairingCrackId = -1;
            }
        }
    }

    public synchronized boolean releaseRepair(UUID suppliedSession, int crackId) {
        if (!sessionId.equals(suppliedSession)
                || repairingCrackId != crackId) {
            return false;
        }

        repairingCrackId = -1;
        return true;
    }

    public synchronized void fail() {
        if (state == State.RUNNING) {
            state = State.FAILED;
        }

        repairingCrackId = -1;
    }

    public synchronized Grade grade() {
        if (state != State.SUCCEEDED) return Grade.POOR;

        double integrityRatio = integrity / (double) MAX_INTEGRITY;
        double repairRatio = totalSpawned == 0
                ? 1.0
                : repairedCracks / (double) totalSpawned;

        if (integrityRatio >= 0.98 && repairRatio >= 0.95
                && criticalPropagations == 0) return Grade.PERFECT;

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
        return Objects.requireNonNull(serverRandom).nextInt(100)
                < bonusChancePercent(grade);
    }

    public synchronized Snapshot snapshot() {
        List<CrackSnapshot> visible = cracks.stream()
                .sorted(Comparator.comparingInt(c -> c.id))
                .map(c -> new CrackSnapshot(
                        c.id,
                        c.x,
                        c.y,
                        c.age,
                        CRACK_GROWTH_TICKS,
                        c.age,
                        CRACK_GROWTH_TICKS,
                        c.repaired
                ))
                .toList();

        int active = (int) cracks.stream()
                .filter(c -> !c.repaired)
                .count();

        return new Snapshot(
                state,
                integrity,
                elapsedTicks,
                difficulty.durationTicks(),
                repairedCracks,
                propagatedCracks,
                active,
                grade(),
                visible
        );
    }

    public UUID sessionId() { return sessionId; }
    public Difficulty difficulty() { return difficulty; }
    public int selectedEnchantments() { return selectedEnchantments; }
    public int highestEnchantmentLevel() { return highestEnchantmentLevel; }

    private Crack findCrack(int id) {
        for (Crack crack : cracks) {
            if (crack.id == id) return crack;
        }
        return null;
    }

    private void spawnCrack(float parentX, float parentY) {
        long active = cracks.stream()
                .filter(c -> !c.repaired)
                .count();

        if (active >= MAX_ACTIVE_CRACKS) return;

        float x = Float.isNaN(parentX)
                ? 0.12f + random.nextFloat() * 0.76f
                : clamp(parentX + (random.nextFloat() - 0.5f) * 0.22f);

        float y = Float.isNaN(parentY)
                ? 0.12f + random.nextFloat() * 0.76f
                : clamp(parentY + (random.nextFloat() - 0.5f) * 0.22f);

        Crack crack = new Crack(nextCrackId++, x, y);
        crack.age = 60;

        cracks.add(crack);
        totalSpawned++;
    }

    private static float clamp(float value) {
        return Math.max(0.05f, Math.min(0.95f, value));
    }
}
