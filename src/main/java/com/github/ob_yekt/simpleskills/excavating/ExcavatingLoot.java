package com.github.ob_yekt.simpleskills.excavating;

import java.util.Locale;

/**
 * Excavating loot table.
 *
 * Loot distribution is controlled by rarity budgets.
 * Each rarity receives a percentage of the total loot distribution,
 * and unlocked rewards within that rarity share its budget according
 * to their individual relative weights.
 *
 * The total distribution, including Nothing, is always 100%.
 *
 * If a rarity has no unlocked rewards, its budget remains reserved
 * for Nothing instead of being redistributed to other rarities.
 */
public final class ExcavatingLoot {
    private ExcavatingLoot() {}

    /**
     * Every possible reward.
     *
     * Individual values are relative weights, NOT final percentages.
     *
     * Replacement milestones:
     *  - Dirt:         gone at level 30
     *  - Flint:        gone at level 30
     *  - Dead Bush:    gone at level 40
     *  - Iron Nugget:  gone at level 30
     *  - Gold Nugget:  gone at level 50
     */
    public enum Reward {
        // ------------------------------------------------------------ 5.1 basic loot

        DIRT("Dirt", ExcavatingRarity.COMMON,
                new double[][]{{1, 2}, {10, 1}, {20, 0.2}, {30, 0}}),

        FLINT("Flint", ExcavatingRarity.COMMON,
                new double[][]{{1, 1}, {10, 0.5}, {20, 0.1}, {30, 0}}),

        BONE_MEAL("Bone Meal", ExcavatingRarity.COMMON,
                new double[][]{{1, 10}, {30, 8}, {60, 4}, {99, 1}}),

        DEAD_BUSH("Dead Bush", ExcavatingRarity.COMMON,
                new double[][]{{1, 2}, {20, 1}, {30, 0.25}, {40, 0}}),

        IRON_NUGGET("Iron Nugget", ExcavatingRarity.UNCOMMON,
                new double[][]{{1, 0.05}, {20, 0.05}, {30, 0}}),

        IRON_INGOT("Iron Ingot", ExcavatingRarity.RARE,
                new double[][]{{1, 0.2}, {30, 0.25}, {60, 0.25}, {99, 0.25}}),

        DIAMOND("Diamond", ExcavatingRarity.EPIC,
                new double[][]{{1, 0.05}, {30, 0.0935}, {60, 0.1385}, {99, 0.197}}),

        EMERALD("Emerald", ExcavatingRarity.EPIC,
                new double[][]{{1, 0.02}, {30, 0.0432}, {60, 0.0672}, {99, 0.0992}}),

        REDSTONE("Redstone", ExcavatingRarity.UNCOMMON,
                new double[][]{{1, 0.1}, {30, 0.158}, {60, 0.218}, {99, 0.296}}),

        // ------------------------------------------------------- 5.2 unlocked while progressing

        GOLD_NUGGET("Gold Nugget", ExcavatingRarity.RARE,
                new double[][]{{11, 0.25}, {30, 0.25}, {50, 0}}),

        GOLD_INGOT("Gold Ingot", ExcavatingRarity.RARE,
                new double[][]{{11, 0.05}, {99, 0.5}}),

        COAL("Coal", ExcavatingRarity.COMMON,
                new double[][]{{21, 1}, {99, 0.3}}),

        GLOWSTONE_DUST("Glowstone Dust", ExcavatingRarity.UNCOMMON,
                new double[][]{{21, 0.3}, {99, 0.6}}),

        COPPER_INGOT("Copper Ingot", ExcavatingRarity.UNCOMMON,
                new double[][]{{31, 0.5}, {99, 1.2}}),

        AMETHYST_SHARD("Amethyst Shard", ExcavatingRarity.RARE,
                new double[][]{{31, 0.05}, {99, 0.25}}),

        LAPIS_LAZULI("Lapis Lazuli", ExcavatingRarity.RARE,
                new double[][]{{41, 0.15}, {99, 0.5}}),

        PRISMARINE_SHARD("Prismarine Shard", ExcavatingRarity.RARE,
                new double[][]{{41, 0.15}, {99, 0.4}}),

        NAUTILUS_SHELL("Nautilus Shell", ExcavatingRarity.VERY_RARE,
                new double[][]{{51, 0.02}, {99, 0.08}}),

        ENCHANTED_BOOK("Enchanted Book", ExcavatingRarity.VERY_RARE,
                new double[][]{{61, 0.01}, {99, 0.08}}),

        POTTERY_SHERD("Pottery Sherd", ExcavatingRarity.VERY_RARE,
                new double[][]{{71, 0.01}, {99, 0.1}}),

        ECHO_SHARD("Echo Shard", ExcavatingRarity.EPIC,
                new double[][]{{71, 0.002}, {99, 0.02}}),

        GOLDEN_APPLE("Golden Apple", ExcavatingRarity.VERY_RARE,
                new double[][]{{71, 0.005}, {99, 0.04}}),

        HEART_OF_THE_SEA("Heart of the Sea", ExcavatingRarity.LEGENDARY,
                new double[][]{{81, 0.001}, {99, 0.01}}),

        ANCIENT_DEBRIS("Ancient Debris", ExcavatingRarity.LEGENDARY,
                new double[][]{{91, 0.0005}, {99, 0.005}}),

        NETHERITE_SCRAP("Netherite Scrap", ExcavatingRarity.LEGENDARY,
                new double[][]{{91, 0.0002}, {99, 0.003}}),

        NETHERITE_INGOT("Netherite Ingot", ExcavatingRarity.MYTHIC,
                new double[][]{{91, 0.0001}, {99, 0.001}}),

        TOTEM_OF_UNDYING("Totem of Undying", ExcavatingRarity.MYTHIC,
                new double[][]{{91, 0.00005}, {99, 0.0005}}),

        ENCHANTED_GOLDEN_APPLE("Enchanted Golden Apple", ExcavatingRarity.MYTHIC,
                new double[][]{{91, 0.00001}, {99, 0.0001}});

        private final String displayName;
        private final ExcavatingRarity rarity;
        private final double[][] points;

        Reward(String displayName, ExcavatingRarity rarity, double[][] points) {
            this.displayName = displayName;
            this.rarity = rarity;
            this.points = points;
        }

        public String getDisplayName() {
            return displayName;
        }

        public ExcavatingRarity getRarity() {
            return rarity;
        }

        public int getUnlockLevel() {
            return (int) points[0][0];
        }

        public int getTotalXp() {
            return rarity.getTotalXp();
        }

        /**
         * Returns the individual relative weight at the given level.
         */
        private double baseChanceAt(int level) {
            int clamped = ExcavatingRank.clampLevel(level);

            if (clamped < points[0][0]) {
                return 0.0;
            }

            return interpolate(points, clamped);
        }

        /**
         * Returns the final probability of receiving this reward.
         *
         * Each rarity keeps its own budget.
         * Rewards within that rarity share the budget proportionally
         * according to their relative weights.
         */
        public double chanceAt(int level) {
            double weight = baseChanceAt(level);

            if (weight <= 0.0) {
                return 0.0;
            }

            double rarityWeight = rarityWeightTotal(rarity, level);

            if (rarityWeight <= 0.0) {
                return 0.0;
            }

            double rarityBudget = rarityBudgetAt(rarity, level);

            if (rarityBudget <= 0.0) {
                return 0.0;
            }

            return rarityBudget * weight / rarityWeight;
        }
    }

    private static final Reward[] REWARDS = Reward.values();

    /**
     * Interpolates between level/value key points.
     */
    private static double interpolate(double[][] points, int level) {
        if (level <= points[0][0]) {
            return points[0][1];
        }

        for (int i = 0; i < points.length - 1; i++) {
            double l0 = points[i][0];
            double v0 = points[i][1];

            double l1 = points[i + 1][0];
            double v1 = points[i + 1][1];

            if (level <= l1) {
                return v0 + (v1 - v0) *
                        ((level - l0) / (l1 - l0));
            }
        }

        return points[points.length - 1][1];
    }

    // ------------------------------------------------------------ NOTHING

    /**
     * Probability of finding nothing.
     *
     * Unavailable rarity budgets remain assigned to Nothing.
     * Nothing therefore represents both the explicit empty chance
     * and any rarity budget that cannot currently be distributed.
     *
     * At level 99, all rarity budgets are available and Nothing
     * reaches exactly 0%.
     */
    public static double nothingChance(int level) {
        int clamped = ExcavatingRank.clampLevel(level);

        double availableBudget = 0.0;

        for (ExcavatingRarity rarity : ExcavatingRarity.values()) {
            if (rarityWeightTotal(rarity, clamped) > 0.0) {
                availableBudget += rarityBudgetAt(rarity, clamped);
            }
        }

        return Math.max(0.0, 100.0 - availableBudget);
    }

    // ------------------------------------------------------------ RARITY BUDGETS

    /**
     * Each rarity has its own progression curve.
     *
     * Values are percentages of the total distribution.
     *
     * A rarity receives its budget only when it has at least
     * one unlocked reward with a positive relative weight.
     *
     * The sum of available rarity budgets and Nothing is 100%
     * at every level.
     */

    private static final double[][] COMMON_POINTS = {
        {1, 80.0},
        {30, 38.0},
        {60, 12.0},
        {99, 3.0}
    };

    private static final double[][] UNCOMMON_POINTS = {
        {1, 8.0},
        {30, 25.0},
        {60, 20.0},
        {99, 15.0}
    };

    private static final double[][] RARE_POINTS = {
        {1, 2.0},
        {30, 27.5},
        {60, 55.0},
        {99, 60.0}
    };

    private static final double[][] VERY_RARE_POINTS = {
        {51, 0.1},
        {60, 1.0},
        {99, 10.0}
    };

    private static final double[][] EPIC_POINTS = {
        {1, 0.2},
        {30, 2.5},
        {60, 6.0},
        {99, 10.0}
    };

    private static final double[][] LEGENDARY_POINTS = {
        {81, 0.1},
        {91, 0.5},
        {99, 1.5}
    };

    private static final double[][] MYTHIC_POINTS = {
        {91, 0.05},
        {99, 0.5}
    };

    /**
     * Interpolates a rarity budget.
     *
     * Returns zero before the first configured level.
     */
    private static double interpolateBudget(double[][] points, int level) {
        if (level < points[0][0]) {
            return 0.0;
        }

        return interpolate(points, level);
    }

    /**
     * Returns the target budget for a rarity at a given level.
     */
    private static double rarityBudgetAt(
            ExcavatingRarity rarity,
            int level
    ) {
        int clamped = ExcavatingRank.clampLevel(level);

        switch (rarity) {
            case COMMON:
                return interpolateBudget(COMMON_POINTS, clamped);

            case UNCOMMON:
                return interpolateBudget(UNCOMMON_POINTS, clamped);

            case RARE:
                return interpolateBudget(RARE_POINTS, clamped);

            case VERY_RARE:
                return interpolateBudget(VERY_RARE_POINTS, clamped);

            case EPIC:
                return interpolateBudget(EPIC_POINTS, clamped);

            case LEGENDARY:
                return interpolateBudget(LEGENDARY_POINTS, clamped);

            case MYTHIC:
                return interpolateBudget(MYTHIC_POINTS, clamped);

            default:
                return 0.0;
        }
    }

    /**
     * Sum of the relative weights of all available rewards
     * belonging to a particular rarity.
     */
    private static double rarityWeightTotal(
            ExcavatingRarity rarity,
            int level
    ) {
        double total = 0.0;

        for (Reward reward : REWARDS) {
            if (reward.rarity == rarity) {
                total += reward.baseChanceAt(level);
            }
        }

        return total;
    }

    // ------------------------------------------------------------ DISTRIBUTION

    /**
     * Sum of the final probabilities of all rewards.
     */
    public static double totalChance(int level) {
        double total = 0.0;

        for (Reward reward : REWARDS) {
            total += reward.chanceAt(level);
        }

        return total;
    }

    /**
     * Validates the complete distribution at every level.
     *
     * Checks:
     *  - Every reward chance is finite and nonnegative.
     *  - Every active rarity budget is finite and nonnegative.
     *  - Nothing is finite and within the valid percentage range.
     *  - The sum of rewards and Nothing equals 100%.
     *
     * Returns null when valid.
     */
    public static String validate() {
        for (int level = ExcavatingRank.MIN_LEVEL;
             level <= ExcavatingRank.MAX_LEVEL;
             level++) {

            for (Reward reward : REWARDS) {
                double chance = reward.chanceAt(level);

                if (!Double.isFinite(chance) || chance < 0.0) {
                    return "Invalid chance " + chance +
                            " for " + reward +
                            " at level " + level;
                }
            }

            double activeBudget = 0.0;

            for (ExcavatingRarity rarity : ExcavatingRarity.values()) {
                double weight = rarityWeightTotal(rarity, level);

                if (weight > 0.0) {
                    double budget = rarityBudgetAt(rarity, level);

                    if (!Double.isFinite(budget) || budget < 0.0) {
                        return "Invalid budget " + budget +
                                " for " + rarity +
                                " at level " + level;
                    }

                    activeBudget += budget;
                }
            }

            if (!Double.isFinite(activeBudget) || activeBudget > 100.0 + 1.0E-7) {
                return "Invalid active rarity budget " + activeBudget +
                        "% at level " + level;
            }

            double rewards = totalChance(level);
            double nothing = nothingChance(level);

            if (!Double.isFinite(nothing) || nothing < 0.0 || nothing > 100.0) {
                return "Invalid Nothing chance " + nothing +
                        "% at level " + level;
            }

            double total = rewards + nothing;

            if (Math.abs(total - 100.0) > 1.0E-7) {
                return "Distribution total " + total +
                        "% instead of 100% at level " + level;
            }
        }

        return null;
    }

    // ------------------------------------------------------------ ROLL

    /**
     * Picks one reward or null for Nothing.
     *
     * @param level Excavating level when the investigation completes.
     * @param roll uniform random value in [0, 1).
     */
    public static Reward roll(int level, double roll) {
        if (Double.isNaN(roll) || roll < 0.0 || roll >= 1.0) {
            throw new IllegalArgumentException(
                    "Roll must be in the range [0, 1)"
            );
        }

        double target = roll * 100.0;
        double cumulative = 0.0;

        Reward lastAvailableReward = null;

        for (Reward reward : REWARDS) {
            double chance = reward.chanceAt(level);

            if (chance <= 0.0) {
                continue;
            }

            lastAvailableReward = reward;
            cumulative += chance;

            if (target < cumulative) {
                return reward;
            }
        }

        // Prevent floating-point precision from creating
        // an accidental Nothing result when Nothing is zero.
        if (nothingChance(level) <= 1.0E-9) {
            return lastAvailableReward;
        }

        return null;
    }

    // ------------------------------------------------------------ DESCRIPTION

    /**
     * Human-readable loot distribution.
     * Used by /simpleskills excavating odds.
     */
    public static String describe(int level) {
        int clamped = ExcavatingRank.clampLevel(level);

        StringBuilder sb = new StringBuilder();

        sb.append(String.format(
                Locale.US,
                "Excavating level %d (%s):%n",
                clamped,
                ExcavatingRank.forLevel(clamped).getDisplayName()
        ));

        for (Reward reward : REWARDS) {
            double chance = reward.chanceAt(clamped);

            if (chance > 0.0) {
                sb.append(String.format(
                        Locale.US,
                        "  %-22s %-10s %.6f%%%n",
                        reward.getDisplayName(),
                        reward.getRarity().getDisplayName(),
                        chance
                ));
            }
        }

        sb.append(String.format(
                Locale.US,
                "  %-22s %-10s %.6f%%%n",
                "Nothing",
                "-",
                nothingChance(clamped)
        ));

        sb.append(String.format(
                Locale.US,
                "  %-22s %-10s %.6f%%%n",
                "TOTAL",
                "-",
                totalChance(clamped) + nothingChance(clamped)
        ));

        return sb.toString();
    }
}