package com.github.ob_yekt.simpleskills.fishing;

import java.util.function.DoubleSupplier;

/**
 * Minecraft-free maths of the individual fish size: how long / heavy a fish is, and how that size
 * changes the fight strength and the Fishing XP.
 *
 * <p>Weight follows the biological length-weight relation {@code W = a * L^b} (b is about 3), so a
 * longer fish is always heavier "on average"; only a small individual variation (about +-7%) is added.
 * Length is skewed towards the smaller sizes, which makes really big fish rare.</p>
 */
public final class FishSizeModel {
    private FishSizeModel() {}

    /**
     * Size profile of a species.
     *
     * @param minLengthCm shortest possible fish
     * @param maxLengthCm longest possible fish
     * @param weightCoefficient {@code a} of {@code W[kg] = a * L[cm]^b}
     * @param weightExponent {@code b} of the relation
     */
    public record Profile(double minLengthCm, double maxLengthCm, double weightCoefficient, double weightExponent) {
        public Profile {
            if (minLengthCm <= 0 || maxLengthCm < minLengthCm || weightCoefficient <= 0 || weightExponent <= 0) {
                throw new IllegalArgumentException("Invalid fish size profile");
            }
        }

        /** Weight a fish of exactly this length has (without the individual variation). */
        public double weightForLength(double lengthCm) {
            return weightCoefficient * Math.pow(lengthCm, weightExponent);
        }

        /** Weight of the typical (median) fish of the species: the neutral point of every size scaling. */
        public double referenceWeightKg() {
            return weightForLength(minLengthCm + (maxLengthCm - minLengthCm) * MEDIAN_POSITION);
        }
    }

    /** Fraction of the length range where the median fish sits (0.5^SKEW). */
    static final double SKEW = 1.5;
    static final double MEDIAN_POSITION = Math.pow(0.5, SKEW);
    private static final double VARIATION = 0.07;
    private static final double VARIATION_LIMIT = 0.18;

    /** The two numbers stored on every fish. */
    public record Result(float weightKg, float lengthCm) {}

    /** Rolls the size of a brand new fish. Called exactly once per fish. */
    public static Result generate(Profile profile, DoubleSupplier uniform01, DoubleSupplier gaussian) {
        double u = Math.pow(clamp(uniform01.getAsDouble(), 0.0, 1.0), SKEW);
        double length = profile.minLengthCm() + (profile.maxLengthCm() - profile.minLengthCm()) * u;
        double variation = clamp(gaussian.getAsDouble() * VARIATION, -VARIATION_LIMIT, VARIATION_LIMIT);
        double weight = profile.weightForLength(length) * (1.0 + variation);
        return new Result(
                (float) (Math.round(Math.max(0.01, weight) * 100.0) / 100.0),
                (float) (Math.round(length * 10.0) / 10.0));
    }

    /** Weight relative to the typical fish of the species, limited to 0.25x .. 4x. */
    public static double sizeRatio(Profile profile, double weightKg) {
        return clamp(weightKg / profile.referenceWeightKg(), 0.25, 4.0);
    }

    /**
     * Fight strength multiplier: a typical fish is 1.0, a 4x heavier one 1.35 (limit), a 4x lighter one 0.80.
     * It scales how hard the fish pulls and how well it resists the player's counter-movements.
     */
    public static double strengthMultiplier(Profile profile, double weightKg) {
        double log2 = Math.log(sizeRatio(profile, weightKg)) / Math.log(2.0);
        return clamp(1.0 + 0.22 * log2, 0.80, 1.35);
    }

    /** How much of the camera counter still works against a strong fish (1.0 = all of it). */
    public static double counterEffect(double strengthMultiplier) {
        return clamp(1.0 - 0.35 * (strengthMultiplier - 1.0), 0.80, 1.0);
    }

    /** Big fish tire a little slower: divisor applied to the fatigue gain. */
    public static double fatigueDivisor(double strengthMultiplier) {
        return 1.0 + 0.6 * Math.max(0.0, strengthMultiplier - 1.0);
    }

    /**
     * XP of a catch: base * species multiplier * square root of the size ratio (0.6x .. 2x), never
     * below 1 and never above 3x the base XP. The square root keeps giants from being worth too much.
     */
    public static int catchXp(int baseXp, double speciesXpMultiplier, Profile profile, double weightKg) {
        if (baseXp <= 0) return 0;
        double size = clamp(Math.sqrt(sizeRatio(profile, weightKg)), 0.6, 2.0);
        double xp = baseXp * speciesXpMultiplier * size;
        return (int) Math.max(1, Math.min(Math.round(xp), (long) baseXp * 3));
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }
}
