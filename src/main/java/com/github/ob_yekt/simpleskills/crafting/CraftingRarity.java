package com.github.ob_yekt.simpleskills.crafting;

import java.util.Locale;
import net.minecraft.ChatFormatting;

/** Rarity tiers and the randomized stat/perk ranges available when crafting equipment. */
public enum CraftingRarity {
    // Percentages are stored as fractions: 0.05 = +5%. Stat chances control whether
    // an individual item receives that attribute at all; Epic+ always rolls applicable stats.
    COMMON("common", "Common", 85.0, 40.0, 5.0,
            0.001, 0.050, 0.000, 0.050, 0.020, 0.050, 0.000, 0.010, 0.005, 0.020,
            0.12, 0.12, 0.08, 0.12, 0.08, 0.001, 0.050, 0.001, 0.030,
            0, 1, 1, ChatFormatting.GRAY),
    RARE("rare", "Rare", 12.0, 32.0, 30.0,
            0.030, 0.080, 0.040, 0.110, 0.040, 0.080, 0.005, 0.018, 0.010, 0.030,
            0.35, 0.35, 0.30, 0.35, 0.25, 0.020, 0.070, 0.010, 0.050,
            1, 1, 1, ChatFormatting.BLUE),
    UNIQUE("unique", "Unique", 2.0, 17.0, 30.0,
            0.050, 0.120, 0.080, 0.180, 0.060, 0.100, 0.010, 0.025, 0.020, 0.045,
            0.70, 0.70, 0.65, 0.70, 0.60, 0.050, 0.120, 0.030, 0.080,
            1, 1, 1, ChatFormatting.AQUA),
    EPIC("epic", "Epic", 0.8, 7.5, 20.0,
            0.080, 0.160, 0.150, 0.280, 0.080, 0.120, 0.020, 0.040, 0.030, 0.055,
            1.00, 1.00, 1.00, 1.00, 1.00, 0.080, 0.160, 0.060, 0.120,
            2, 2, 2, ChatFormatting.DARK_PURPLE),
    LEGENDARY("legendary", "Legendary", 0.15, 2.7, 10.0,
            0.120, 0.230, 0.250, 0.380, 0.120, 0.170, 0.035, 0.060, 0.050, 0.080,
            1.00, 1.00, 1.00, 1.00, 1.00, 0.120, 0.220, 0.100, 0.180,
            2, 3, 3, ChatFormatting.GOLD),
    MYTHIC("mythic", "Mythic", 0.04, 0.65, 4.0,
            0.160, 0.320, 0.350, 0.500, 0.170, 0.230, 0.055, 0.090, 0.075, 0.120,
            1.00, 1.00, 1.00, 1.00, 1.00, 0.180, 0.300, 0.140, 0.240,
            3, 4, 3, ChatFormatting.RED),
    TRANSCENDENT("transcendent", "Transcendent", 0.01, 0.15, 1.0,
            0.220, 0.420, 0.450, 0.650, 0.230, 0.300, 0.080, 0.120, 0.100, 0.180,
            1.00, 1.00, 1.00, 1.00, 1.00, 0.250, 0.420, 0.200, 0.350,
            4, 5, 4, ChatFormatting.YELLOW);

    /**
     * Material quality scales only the Transcendent roll; the removed probability returns to Common.
     * Multipliers are relative to Diamond's 1.00% Transcendent chance at Crafting level 99.
     */
    public enum MaterialTier {
        WOOD("Wood / Leather", 0.05),       // 0.05% at level 99
        STONE("Stone", 0.10),               // 0.10% at level 99
        COPPER("Copper", 0.20),             // 0.20% at level 99
        GOLD("Gold", 0.50),                 // 0.50% at level 99
        IRON("Iron", 0.60),                 // 0.60% at level 99
        OTHER("Other / Modded", 0.10),      // Conservative 0.10% fallback
        DIAMOND("Diamond", 1.00),           // 1.00% at level 99
        NETHERITE("Netherite", 1.00);       // 1.00% at level 99

        private final String displayName;
        private final double transcendentMultiplier;

        MaterialTier(String displayName, double transcendentMultiplier) {
            this.displayName = displayName;
            this.transcendentMultiplier = transcendentMultiplier;
        }

        public String displayName() { return displayName; }
        public double transcendentMultiplier() { return transcendentMultiplier; }
    }

    private final String id;
    private final String displayName;
    private final double level1Chance;
    private final double level50Chance;
    private final double level99Chance;
    private final double minDamageBonus;
    private final double maxDamageBonus;
    private final double minDurabilityBonus;
    private final double maxDurabilityBonus;
    private final double minBreakSpeedBonus;
    private final double maxBreakSpeedBonus;
    private final double minCriticalChance;
    private final double maxCriticalChance;
    private final double minAttackSpeedBonus;
    private final double maxAttackSpeedBonus;
    private final double damageStatChance;
    private final double breakSpeedStatChance;
    private final double attackSpeedStatChance;
    private final double armorStatChance;
    private final double toughnessStatChance;
    private final double minArmorBonus;
    private final double maxArmorBonus;
    private final double minToughnessBonus;
    private final double maxToughnessBonus;
    private final int minimumPerks;
    private final int maximumPerks;
    private final int minimumPerkEffectLevel;
    private final ChatFormatting color;

    CraftingRarity(String id, String displayName, double level1Chance, double level50Chance,
                   double level99Chance, double minDamageBonus, double maxDamageBonus,
                   double minDurabilityBonus, double maxDurabilityBonus,
                   double minBreakSpeedBonus, double maxBreakSpeedBonus,
                   double minCriticalChance, double maxCriticalChance,
                   double minAttackSpeedBonus, double maxAttackSpeedBonus,
                   double damageStatChance, double breakSpeedStatChance, double attackSpeedStatChance,
                   double armorStatChance, double toughnessStatChance,
                   double minArmorBonus, double maxArmorBonus,
                   double minToughnessBonus, double maxToughnessBonus,
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
        this.minBreakSpeedBonus = minBreakSpeedBonus;
        this.maxBreakSpeedBonus = maxBreakSpeedBonus;
        this.minCriticalChance = minCriticalChance;
        this.maxCriticalChance = maxCriticalChance;
        this.minAttackSpeedBonus = minAttackSpeedBonus;
        this.maxAttackSpeedBonus = maxAttackSpeedBonus;
        this.damageStatChance = damageStatChance;
        this.breakSpeedStatChance = breakSpeedStatChance;
        this.attackSpeedStatChance = attackSpeedStatChance;
        this.armorStatChance = armorStatChance;
        this.toughnessStatChance = toughnessStatChance;
        this.minArmorBonus = minArmorBonus;
        this.maxArmorBonus = maxArmorBonus;
        this.minToughnessBonus = minToughnessBonus;
        this.maxToughnessBonus = maxToughnessBonus;
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
    public double minBreakSpeedBonus() { return minBreakSpeedBonus; }
    public double maxBreakSpeedBonus() { return maxBreakSpeedBonus; }
    public double breakSpeedStatChance() { return breakSpeedStatChance; }
    public double minCriticalChance() { return minCriticalChance; }
    public double maxCriticalChance() { return maxCriticalChance; }
    public double minAttackSpeedBonus() { return minAttackSpeedBonus; }
    public double maxAttackSpeedBonus() { return maxAttackSpeedBonus; }
    public double damageStatChance() { return damageStatChance; }
    public double attackSpeedStatChance() { return attackSpeedStatChance; }
    public double armorStatChance() { return armorStatChance; }
    public double toughnessStatChance() { return toughnessStatChance; }
    public double minArmorBonus() { return minArmorBonus; }
    public double maxArmorBonus() { return maxArmorBonus; }
    public double minToughnessBonus() { return minToughnessBonus; }
    public double maxToughnessBonus() { return maxToughnessBonus; }
    public int minimumPerks() { return minimumPerks; }
    public int maximumPerks() { return maximumPerks; }
    public int minimumPerkEffectLevel() { return minimumPerkEffectLevel; }
    public ChatFormatting color() { return color; }

    /**
     * Crafting XP reward multiplier for the rolled equipment quality.
     * Higher tiers reward a better result without making a lucky craft worth
     * an excessive amount of progression XP.
     */
    public double craftingXpMultiplier() {
        return switch (this) {
            case COMMON -> 1.00;
            case RARE -> 1.10;
            case UNIQUE -> 1.20;
            case EPIC -> 1.40;
            case LEGENDARY -> 1.70;
            case MYTHIC -> 2.10;
            case TRANSCENDENT -> 2.60;
        };
    }

    /** Interpolates continuously between the three published progression milestones. */
    public double chanceAt(int level) {
        int safeLevel = Math.max(1, Math.min(99, level));
        if (safeLevel <= 50) {
            return level1Chance + (level50Chance - level1Chance) * ((safeLevel - 1) / 49.0);
        }
        return level50Chance + (level99Chance - level50Chance) * ((safeLevel - 50) / 49.0);
    }

    /** Applies the recipe material multiplier only to Transcendent, compensating Common to keep 100%. */
    public double chanceAt(int level, MaterialTier materialTier) {
        MaterialTier tier = materialTier == null ? MaterialTier.OTHER : materialTier;
        if (this != COMMON && this != TRANSCENDENT) return chanceAt(level);

        double baseTranscendentChance = TRANSCENDENT.chanceAt(level);
        double adjustedTranscendentChance = baseTranscendentChance * tier.transcendentMultiplier();
        return this == TRANSCENDENT
                ? adjustedTranscendentChance
                : COMMON.chanceAt(level) + baseTranscendentChance - adjustedTranscendentChance;
    }

    /** Rolls rarity using the crafted recipe's strongest recognized material. */
    public static CraftingRarity roll(int level, double rollPercent, MaterialTier materialTier) {
        double cumulative = 0.0;
        CraftingRarity last = COMMON;
        for (CraftingRarity rarity : values()) {
            last = rarity;
            cumulative += rarity.chanceAt(level, materialTier);
            if (rollPercent < cumulative) return rarity;
        }
        return last;
    }

    /** Backward-compatible baseline roll; diamond is the reference material. */
    public static CraftingRarity roll(int level, double rollPercent) {
        return roll(level, rollPercent, MaterialTier.DIAMOND);
    }

    public static double totalChance(int level) {
        return totalChance(level, MaterialTier.DIAMOND);
    }

    public static double totalChance(int level, MaterialTier materialTier) {
        double total = 0.0;
        for (CraftingRarity rarity : values()) total += rarity.chanceAt(level, materialTier);
        return total;
    }

    public static String describe(int level) {
        int safeLevel = Math.max(1, Math.min(99, level));
        StringBuilder output = new StringBuilder();
        output.append(String.format(Locale.US,
                "Crafting level %d equipment rarity chances (Diamond baseline):%n", safeLevel));
        for (CraftingRarity rarity : values()) {
            output.append(String.format(Locale.US, "  %-14s %9.4f%%%n", rarity.displayName, rarity.chanceAt(safeLevel)));
        }
        output.append(String.format(Locale.US, "  %-14s %9.4f%%%n", "TOTAL", totalChance(safeLevel)));
        output.append(String.format(Locale.US, "%nTranscendent chance by recipe material:%n"));
        for (MaterialTier tier : MaterialTier.values()) {
            double chance = TRANSCENDENT.chanceAt(safeLevel, tier);
            output.append(String.format(Locale.US, "  %-16s %9.4f%%%n", tier.displayName(), chance));
        }
        return output.toString();
    }

    public static CraftingRarity byId(String id) {
        if (id == null) return null;
        String normalized = id.toLowerCase(Locale.ROOT);
        // Accept the common Portuguese spelling as a convenience for in-game testing.
        if (normalized.equals("transdente") || normalized.equals("transcendente")) {
            return TRANSCENDENT;
        }
        for (CraftingRarity rarity : values()) {
            if (rarity.id.equals(normalized)) return rarity;
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
            for (CraftingRarity.MaterialTier tier : CraftingRarity.MaterialTier.values()) {
                double materialTotal = totalChance(level, tier);
                if (!Double.isFinite(materialTotal) || Math.abs(materialTotal - 100.0) > 1.0E-7) {
                    return "Crafting rarity distribution for " + tier.displayName()
                            + " totals " + materialTotal + "% at level " + level;
                }
                for (CraftingRarity rarity : values()) {
                    double materialChance = rarity.chanceAt(level, tier);
                    if (!Double.isFinite(materialChance) || materialChance < 0.0) {
                        return "Invalid " + rarity.id + " chance for " + tier.displayName()
                                + " at level " + level;
                    }
                }
            }
            for (CraftingRarity rarity : values()) {
                double chance = rarity.chanceAt(level);
                if (!Double.isFinite(chance) || chance < 0.0) {
                    return "Invalid " + rarity.id + " chance at level " + level;
                }
                if (rarity.minDamageBonus < 0.0 || rarity.maxDamageBonus < rarity.minDamageBonus
                        || rarity.minDurabilityBonus < 0.0 || rarity.maxDurabilityBonus < rarity.minDurabilityBonus
                        || rarity.minBreakSpeedBonus < 0.0 || rarity.maxBreakSpeedBonus < rarity.minBreakSpeedBonus
                        || rarity.breakSpeedStatChance < 0.0 || rarity.breakSpeedStatChance > 1.0
                        || rarity.minCriticalChance < 0.0 || rarity.maxCriticalChance < rarity.minCriticalChance
                        || rarity.minAttackSpeedBonus < 0.0 || rarity.maxAttackSpeedBonus < rarity.minAttackSpeedBonus
                        || rarity.damageStatChance < 0.0 || rarity.damageStatChance > 1.0
                        || rarity.attackSpeedStatChance < 0.0 || rarity.attackSpeedStatChance > 1.0
                        || rarity.armorStatChance < 0.0 || rarity.armorStatChance > 1.0
                        || rarity.toughnessStatChance < 0.0 || rarity.toughnessStatChance > 1.0
                        || rarity.minArmorBonus < 0.0 || rarity.maxArmorBonus < rarity.minArmorBonus
                        || rarity.minToughnessBonus < 0.0 || rarity.maxToughnessBonus < rarity.minToughnessBonus) {
                    return "Invalid stat range for rarity " + rarity.id;
                }
            }
        }
        return null;
    }
}
