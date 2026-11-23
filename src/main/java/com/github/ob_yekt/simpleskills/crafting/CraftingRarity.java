package com.github.ob_yekt.simpleskills.crafting;

import java.util.Locale;
import net.minecraft.ChatFormatting;

/** Rarity tiers and the randomized stat/perk ranges available when crafting equipment. */
public enum CraftingRarity {
    // Stat values are fractions: 0.05 = +5%. Common damage intentionally starts at +0.1%.
    COMMON("common", "Common", 85.0, 40.0, 5.0,
            0.001, 0.050, 0.000, 0.050, 0.000, 0.010, 0.005, 0.020,
            0, 1, 1, ChatFormatting.GRAY),
    RARE("rare", "Rare", 12.0, 32.0, 30.0,
            0.030, 0.080, 0.040, 0.110, 0.005, 0.018, 0.010, 0.030,
            1, 1, 1, ChatFormatting.BLUE),
    UNIQUE("unique", "Unique", 2.0, 17.0, 30.0,
            0.050, 0.120, 0.080, 0.180, 0.010, 0.025, 0.020, 0.045,
            1, 1, 1, ChatFormatting.AQUA),
    EPIC("epic", "Epic", 0.8, 7.5, 20.0,
            0.080, 0.160, 0.150, 0.280, 0.020, 0.040, 0.030, 0.055,
            2, 2, 2, ChatFormatting.DARK_PURPLE),
    LEGENDARY("legendary", "Legendary", 0.15, 2.7, 10.0,
            0.120, 0.230, 0.250, 0.380, 0.035, 0.060, 0.050, 0.080,
            2, 3, 3, ChatFormatting.GOLD),
    MYTHIC("mythic", "Mythic", 0.04, 0.65, 4.0,
            0.160, 0.320, 0.350, 0.500, 0.055, 0.090, 0.075, 0.120,
            3, 4, 3, ChatFormatting.RED),
    TRANSCENDENT("transcendent", "Transcendent", 0.01, 0.15, 1.0,
            0.220, 0.420, 0.450, 0.650, 0.080, 0.120, 0.100, 0.180,
            4, 5, 4, ChatFormatting.YELLOW);

    private final String id;
    private final String displayName;
    private final double level1Chance;
    private final double level50Chance;
    private final double level99Chance;
    private final double minDamageBonus;
    private final double maxDamageBonus;
    private final double minDurabilityBonus;
    private final double maxDurabilityBonus;
    private final double minCriticalChance;
    private final double maxCriticalChance;
    private final double minAttackSpeedBonus;
    private final double maxAttackSpeedBonus;
    private final int minimumPerks;
    private final int maximumPerks;
    private final int minimumPerkEffectLevel;
    private final ChatFormatting color;

    CraftingRarity(String id, String displayName, double level1Chance, double level50Chance,
                   double level99Chance, double minDamageBonus, double maxDamageBonus,
                   double minDurabilityBonus, double maxDurabilityBonus,
                   double minCriticalChance, double maxCriticalChance,
                   double minAttackSpeedBonus, double maxAttackSpeedBonus,
                   int minimumPerks, int maximumPerks, int minimumPerkEffectLevel,
                   ChatFormatting color) {
        this.id = id;
        this.displayName = displayName;
        this.level1Chance = level1Chance;
        this.level50Chance = level50Chance;
        this.level99Chance = level99Chance;
        this.minDamageBonus = minDamageBonus;
        this.maxDamageBonus = maxDamageBonus;
        this.minDurabilityBonus = minDurabilityBonus;
        this.maxDurabilityBonus = maxDurabilityBonus;
        this.minCriticalChance = minCriticalChance;
        this.maxCriticalChance = maxCriticalChance;
        this.minAttackSpeedBonus = minAttackSpeedBonus;
        this.maxAttackSpeedBonus = maxAttackSpeedBonus;
        this.minimumPerks = minimumPerks;
        this.maximumPerks = maximumPerks;
        this.minimumPerkEffectLevel = minimumPerkEffectLevel;
        this.color = color;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public double minDamageBonus() { return minDamageBonus; }
    public double maxDamageBonus() { return maxDamageBonus; }
    public double minDurabilityBonus() { return minDurabilityBonus; }
    public double maxDurabilityBonus() { return maxDurabilityBonus; }
    public double minCriticalChance() { return minCriticalChance; }
    public double maxCriticalChance() { return maxCriticalChance; }
    public double minAttackSpeedBonus() { return minAttackSpeedBonus; }
    public double maxAttackSpeedBonus() { return maxAttackSpeedBonus; }
    public int minimumPerks() { return minimumPerks; }
    public int maximumPerks() { return maximumPerks; }
    public int minimumPerkEffectLevel() { return minimumPerkEffectLevel; }
    public ChatFormatting color() { return color; }

    /** Interpolates continuously between the three published progression milestones. */
    public double chanceAt(int level) {
        int safeLevel = Math.max(1, Math.min(99, level));
        if (safeLevel <= 50) {
            return level1Chance + (level50Chance - level1Chance) * ((safeLevel - 1) / 49.0);
        }
        return level50Chance + (level99Chance - level50Chance) * ((safeLevel - 50) / 49.0);
    }

    public static CraftingRarity roll(int level, double rollPercent) {
        double cumulative = 0.0;
        CraftingRarity last = COMMON;
        for (CraftingRarity rarity : values()) {
            last = rarity;
            cumulative += rarity.chanceAt(level);
            if (rollPercent < cumulative) return rarity;
        }
        return last;
    }

    public static double totalChance(int level) {
        double total = 0.0;
        for (CraftingRarity rarity : values()) total += rarity.chanceAt(level);
        return total;
    }

    public static String describe(int level) {
        int safeLevel = Math.max(1, Math.min(99, level));
        StringBuilder output = new StringBuilder();
        output.append(String.format(Locale.US, "Crafting level %d equipment rarity chances:%n", safeLevel));
        for (CraftingRarity rarity : values()) {
            output.append(String.format(Locale.US, "  %-14s %9.4f%%%n", rarity.displayName, rarity.chanceAt(safeLevel)));
        }
        output.append(String.format(Locale.US, "  %-14s %9.4f%%%n", "TOTAL", totalChance(safeLevel)));
        return output.toString();
    }

    public static CraftingRarity byId(String id) {
        if (id == null) return null;
        for (CraftingRarity rarity : values()) {
            if (rarity.id.equals(id.toLowerCase(Locale.ROOT))) return rarity;
        }
        return null;
    }

    /** Validates every level, not just the three configured anchor points. */
    public static String validateDistribution() {
        for (int level = 1; level <= 99; level++) {
            double total = totalChance(level);
            if (!Double.isFinite(total) || Math.abs(total - 100.0) > 1.0E-7) {
                return "Crafting rarity distribution totals " + total + "% at level " + level;
            }
            for (CraftingRarity rarity : values()) {
                double chance = rarity.chanceAt(level);
                if (!Double.isFinite(chance) || chance < 0.0) {
                    return "Invalid " + rarity.id + " chance at level " + level;
                }
                if (rarity.minDamageBonus < 0.0 || rarity.maxDamageBonus < rarity.minDamageBonus
                        || rarity.minDurabilityBonus < 0.0 || rarity.maxDurabilityBonus < rarity.minDurabilityBonus
                        || rarity.minCriticalChance < 0.0 || rarity.maxCriticalChance < rarity.minCriticalChance
                        || rarity.minAttackSpeedBonus < 0.0 || rarity.maxAttackSpeedBonus < rarity.minAttackSpeedBonus) {
                    return "Invalid stat range for rarity " + rarity.id;
                }
            }
        }
        return null;
    }
}
