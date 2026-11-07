package com.github.ob_yekt.simpleskills.excavating;

/**
 * The 10 Excavating ranks (spec section 2) and the investigation-time formula (section 3).
 *
 * Each rank has its own colour so it is easy to tell apart in the TAB menu. All ten colour
 * codes are different from each other.
 */
public enum ExcavatingRank {
    NOVICE_DIGGER("Novice Digger", 1, 10, "§7", "Basic loot"),
    SAND_SEEKER("Sand Seeker", 11, 20, "§e", "Gold Nugget, Gold Ingot"),
    TREASURE_SCOUT("Treasure Scout", 21, 30, "§a", "Coal, Glowstone Dust"),
    RELIC_HUNTER("Relic Hunter", 31, 40, "§3", "Copper Ingot, Amethyst Shard"),
    GOLD_PROSPECTOR("Gold Prospector", 41, 50, "§6", "Lapis Lazuli, Prismarine Shard"),
    EXPERT_EXCAVATOR("Expert Excavator", 51, 60, "§b", "Nautilus Shell"),
    TREASURE_TRACKER("Treasure Tracker", 61, 70, "§9", "Enchanted Book"),
    MASTER_PROSPECTOR("Master Prospector", 71, 80, "§5", "Pottery Sherd, Echo Shard, Golden Apple"),
    LEGENDARY_DIGGER("Legendary Digger", 81, 90, "§d", "Heart of the Sea"),
    LEGENDARY_TREASURE_HUNTER("Legendary Treasure Hunter", 91, 99, "§c",
            "Ancient Debris, Netherite Scrap, Netherite Ingot, Totem of Undying, Enchanted Golden Apple");

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 99;

    /** Investigation time at level 1, in seconds. */
    public static final double MAX_TIME_SECONDS = 3.0;
    /** Total time reduction between level 1 and level 99, in seconds (3.0s -> 1.0s). */
    public static final double TIME_REDUCTION_SECONDS = 2.0;

    private final String displayName;
    private final int minLevel;
    private final int maxLevel;
    private final String colorCode;
    private final String unlocks;

    ExcavatingRank(String displayName, int minLevel, int maxLevel, String colorCode, String unlocks) {
        this.displayName = displayName;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.colorCode = colorCode;
        this.unlocks = unlocks;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    /** Legacy formatting code of this rank (e.g. "§e"). */
    public String getColorCode() {
        return colorCode;
    }

    /** Short description of the loot unlocked by this rank. */
    public String getUnlocks() {
        return unlocks;
    }

    /** Rank name wrapped in its colour, ready for chat / tab text. */
    public String getColoredName() {
        return colorCode + displayName;
    }

    /** Returns the rank for a level; levels outside 1..99 are clamped. */
    public static ExcavatingRank forLevel(int level) {
        int clamped = clampLevel(level);
        ExcavatingRank result = NOVICE_DIGGER;
        for (ExcavatingRank rank : values()) {
            if (clamped >= rank.minLevel) {
                result = rank;
            }
        }
        return result;
    }

    public static int clampLevel(int level) {
        return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
    }

    /**
     * Investigation time in seconds: T(L) = 3.0 - 2 * (L - 1) / 98.
     * Level 1 = 3.00s, level 99 = 1.00s.
     */
    public static double investigationSeconds(int level) {
        int clamped = clampLevel(level);
        return MAX_TIME_SECONDS - TIME_REDUCTION_SECONDS * (clamped - MIN_LEVEL) / (MAX_LEVEL - MIN_LEVEL);
    }

    /** Investigation time in server ticks (20 ticks = 1 second), never below 1 tick. */
    public static int investigationTicks(int level) {
        return Math.max(1, (int) Math.round(investigationSeconds(level) * 20.0));
    }
}
