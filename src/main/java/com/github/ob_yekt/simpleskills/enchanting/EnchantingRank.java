package com.github.ob_yekt.simpleskills.enchanting;

/** Ten progression ranks for the Enchanting profession. */
public enum EnchantingRank {
    RUNE_NOVICE("Rune Novice", 1, 10, "§7"),
    RUNE_INITIATE("Rune Initiate", 11, 20, "§e"),
    GLYPH_SCRIBE("Glyph Scribe", 21, 30, "§a"),
    SIGIL_ADEPT("Sigil Adept", 31, 40, "§3"),
    SPELLWEAVER("Spellweaver", 41, 50, "§6"),
    RUNE_BINDER("Rune Binder", 51, 60, "§b"),
    ARCANE_INFUSER("Arcane Infuser", 61, 70, "§9"),
    MASTER_RUNESMITH("Master Runesmith", 71, 80, "§5"),
    GRAND_RUNECaster("Grand Runecaster", 81, 90, "§d"),
    ARCHMAGE_OF_RUNES("Archmage of Runes", 91, 99, "§c");

    private final String displayName;
    private final int minimumLevel;
    private final int maximumLevel;
    private final String color;

    EnchantingRank(String displayName, int minimumLevel, int maximumLevel, String color) {
        this.displayName = displayName;
        this.minimumLevel = minimumLevel;
        this.maximumLevel = maximumLevel;
        this.color = color;
    }

    public String getDisplayName() { return displayName; }
    public int getMinimumLevel() { return minimumLevel; }
    public int getMaximumLevel() { return maximumLevel; }
    public String getColor() { return color; }
    public String getColoredName() { return color + displayName; }

    public static EnchantingRank forLevel(int level) {
        int safeLevel = Math.max(1, Math.min(99, level));
        for (EnchantingRank rank : values()) {
            if (safeLevel >= rank.minimumLevel && safeLevel <= rank.maximumLevel) return rank;
        }
        return RUNE_NOVICE;
    }
}
