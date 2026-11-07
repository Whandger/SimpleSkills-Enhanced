package com.github.ob_yekt.simpleskills.excavating;

/**
 * Rarity tiers of Excavating rewards (spec section 6).
 *
 * Every completed investigation grants {@link #BASE_XP} XP, even when nothing is found.
 * Finding an item adds the tier bonus on top of that. The XP is NOT multiplied by the
 * player's level.
 */
public enum ExcavatingRarity {
    COMMON("Common", 100, "§f"),
    UNCOMMON("Uncommon", 150, "§a"),
    RARE("Rare", 200, "§9"),
    VERY_RARE("Very Rare", 300, "§5"),
    EPIC("Epic", 500, "§d"),
    LEGENDARY("Legendary", 800, "§6"),
    MYTHIC("Mythic", 1500, "§c");

    /** XP granted for every completed investigation (also when the result is Nothing). */
    public static final int BASE_XP = 40;

    private final String displayName;
    private final int bonusXp;
    private final String colorCode;

    ExcavatingRarity(String displayName, int bonusXp, String colorCode) {
        this.displayName = displayName;
        this.bonusXp = bonusXp;
        this.colorCode = colorCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Additional XP granted on top of {@link #BASE_XP} when an item of this rarity is found. */
    public int getBonusXp() {
        return bonusXp;
    }

    /** Total XP for an investigation that found an item of this rarity (base + bonus). */
    public int getTotalXp() {
        return BASE_XP + bonusXp;
    }

    /** Legacy formatting code (e.g. "§d") used for chat / tab text. */
    public String getColorCode() {
        return colorCode;
    }
}
