package com.github.ob_yekt.simpleskills.enchanting;

/** Progression rules shared by the Enchanting UI, anvil validation and future Rune Enchanting flow. */
public final class EnchantProgression {
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 99;
    public static final int MAX_UNLOCKED_ENCHANTMENT_LEVEL = 10;

    private EnchantProgression() {}

    /**
     * Levels 1-9 unlock I, 10-19 unlock II, ..., 90-99 unlock X.
     * A level below 1 has no unlocked enchantment rank.
     */
    public static int getUnlockedEnchantmentLevel(int professionLevel) {
        if (professionLevel < MIN_LEVEL) return 0;
        int safeLevel = Math.min(MAX_LEVEL, professionLevel);
        return Math.min(MAX_UNLOCKED_ENCHANTMENT_LEVEL, (safeLevel / 10) + 1);
    }

    public static EnchantingRank getRank(int professionLevel) {
        return EnchantingRank.forLevel(professionLevel);
    }

    /** Returns the effective cap after applying both progression and the enchantment-specific cap. */
    public static int getAvailableLevel(int professionLevel, int enchantmentCap) {
        if (enchantmentCap <= 0) return 0;
        return Math.min(getUnlockedEnchantmentLevel(professionLevel), enchantmentCap);
    }
}
