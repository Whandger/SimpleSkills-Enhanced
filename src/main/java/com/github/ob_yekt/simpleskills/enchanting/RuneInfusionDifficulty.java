package com.github.ob_yekt.simpleskills.enchanting;

/**
 * Shared difficulty formula (server authoritative, client preview).
 *
 * contribution = weight(level, cap) * frontier(level, cap, unlockedLevel)
 *
 * weight:   level on a common 1..10 scale. Enchantments that stop before level X are stretched so
 *           that, e.g., a single-level enchantment (cap 1) counts as ~3.5 instead of 1.
 * frontier: how close the level is to the player's unlocked ceiling. Staying well below the
 *           ceiling is cheap (~0.8x); sitting right at the ceiling is hard (3.0x).
 */
public final class RuneInfusionDifficulty {
    public static final int MIN_SCORE = RuneInfusionMinigame.MIN_DIFFICULTY;
    public static final int MAX_SCORE = RuneInfusionMinigame.MAX_DIFFICULTY;

    /** Weight of the top (and only) level of an enchantment whose cap is 1. */
    private static final double SINGLE_LEVEL_WEIGHT = 3.5;
    /** Weight of the top level of an enchantment whose cap is 10 or more. */
    private static final double FULL_SCALE_WEIGHT = 10.0;
    private static final int FULL_SCALE_LEVELS = 10;

    /** frontier(r) = BASE + GAIN * r^3, with r = level / min(unlocked, cap). */
    private static final double FRONTIER_BASE = 0.8;
    private static final double FRONTIER_GAIN = 2.2;

    private RuneInfusionDifficulty() {}

    /** Level on the shared 1..10 scale (cap 10 -> weight == level). */
    public static double weight(int level, int configuredCap) {
        int cap = Math.max(1, configuredCap);
        int lvl = Math.max(0, Math.min(level, cap));
        double span = (Math.min(cap, FULL_SCALE_LEVELS) - 1) / (double) (FULL_SCALE_LEVELS - 1);
        double top = SINGLE_LEVEL_WEIGHT + (FULL_SCALE_WEIGHT - SINGLE_LEVEL_WEIGHT) * span;
        return top * lvl / cap;
    }

    /** 0.8 (far below the player's ceiling) .. 3.0 (right at the ceiling). */
    public static double frontier(int level, int configuredCap, int unlockedLevel) {
        int effectiveCap = Math.max(1, Math.min(Math.max(1, unlockedLevel), Math.max(1, configuredCap)));
        double ratio = Math.min(1.0, Math.max(0, level) / (double) effectiveCap);
        return FRONTIER_BASE + FRONTIER_GAIN * ratio * ratio * ratio;
    }

    public static double contribution(int level, int configuredCap, int unlockedLevel) {
        if (level <= 0) return 0.0;
        return weight(level, configuredCap) * frontier(level, configuredCap, unlockedLevel);
    }

    public static int toScore(double rawSum) {
        long rounded = Math.round(rawSum);
        return (int) Math.max(MIN_SCORE, Math.min(MAX_SCORE, rounded));
    }
}