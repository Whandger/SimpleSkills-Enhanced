package com.github.ob_yekt.simpleskills.fishing;

import org.jspecify.annotations.Nullable;

/**
 * Level based benefits of the Fishing skill (levels 1-99). Every benefit grows linearly with the
 * level and is shown in the SkillTabMenu through the same numbers used by the fight itself.
 */
public final class FishingProgression {
    public static final int MAX_LEVEL = 99;

    /** Level 99 tires the fish twice as fast as level 1. */
    private static final double FATIGUE_BONUS_AT_MAX = 1.0;
    /** Level 99 reduces the line tension growth by 40%. */
    private static final double TENSION_REDUCTION_AT_MAX = 0.40;
    /** Level 99 recovers the tension twice as fast. */
    private static final double RECOVERY_BONUS_AT_MAX = 1.0;
    /** Level 99 makes the fish approach the bait 2.5x as fast. */
    private static final double ATTRACTION_BONUS_AT_MAX = 1.5;

    private FishingProgression() {}

    /** 0.0 at level 1, 1.0 at level 99. */
    public static double fraction(int level) {
        return Math.max(0.0, Math.min(1.0, (level - 1) / (double) (MAX_LEVEL - 1)));
    }

    /** Fish Fatigue: multiplier on how fast the fish gets tired. */
    public static double fatigueMultiplier(int level) {
        return (1.0 + FATIGUE_BONUS_AT_MAX * fraction(level)) * FishingConfig.fatigueRate();
    }

    /** Line Tension: multiplier on how fast the tension grows (lower is better). */
    public static double tensionMultiplier(int level) {
        return 1.0 - TENSION_REDUCTION_AT_MAX * fraction(level);
    }

    /** Line Recovery: multiplier on how fast the tension drops once the player stops reeling. */
    public static double recoveryMultiplier(int level) {
        return (1.0 + RECOVERY_BONUS_AT_MAX * fraction(level)) * FishingConfig.lineRecoveryRate();
    }

    /**
     * Fish Attraction Speed: multiplier on how fast fish approach the bait. This is the successor of
     * the old "bite wait reduction" and is still controlled by {@code fishing_speed_bonus_enabled}.
     */
    public static double attractionMultiplier(int level, boolean levelBonusEnabled) {
        double bonus = levelBonusEnabled ? ATTRACTION_BONUS_AT_MAX * fraction(level) : 0.0;
        return (1.0 + bonus) * FishingConfig.attractionSpeed();
    }

    // ---- Percentages for the SkillTabMenu ------------------------------------------------------
    public static double fatiguePercent(int level) { return FATIGUE_BONUS_AT_MAX * fraction(level) * 100.0; }
    public static double tensionReductionPercent(int level) { return TENSION_REDUCTION_AT_MAX * fraction(level) * 100.0; }
    public static double recoveryPercent(int level) { return RECOVERY_BONUS_AT_MAX * fraction(level) * 100.0; }
    public static double attractionPercent(int level) { return ATTRACTION_BONUS_AT_MAX * fraction(level) * 100.0; }

    /**
     * Builds the fight parameters for a player of {@code level} fighting an individual fish: the
     * species sets the base strength, the stored weight of this very fish scales it (heavier = stronger,
     * limited to 0.80x .. 1.35x), and the player's Fishing level benefits stay exactly as they were.
     */
    public static FishingFightModel.Params fightParams(int level, FishSpecies species, @Nullable FishAttributes fish) {
        double weight = fish != null ? fish.weightKg() : species.sizeProfile().referenceWeightKg();
        double strength = FishSizeModel.strengthMultiplier(species.sizeProfile(), weight);
        return new FishingFightModel.Params(
                fatigueMultiplier(level),
                tensionMultiplier(level),
                recoveryMultiplier(level),
                FishingConfig.lineMaxTension(),
                species.pullStrength() * strength,
                species.fatigueFactor() / FishSizeModel.fatigueDivisor(strength),
                FishSizeModel.counterEffect(strength));
    }

    /** Fishing XP of a catch from the stored size of the fish (server side, computed once per catch). */
    public static int catchXp(int baseXp, FishSpecies species, @Nullable FishAttributes fish) {
        double weight = fish != null ? fish.weightKg() : species.sizeProfile().referenceWeightKg();
        return FishSizeModel.catchXp(baseXp, species.xpMultiplier(), species.sizeProfile(), weight);
    }
}
