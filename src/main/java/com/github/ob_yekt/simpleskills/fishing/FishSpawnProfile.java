package com.github.ob_yekt.simpleskills.fishing;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Data-driven habitat, depth and natural school preferences for one fish species. */
public record FishSpawnProfile(
        Map<FishHabitat, Double> habitatWeights,
        int minDepth,
        int preferredDepth,
        int maxPopulation,
        boolean large,
        int minSchoolSize,
        int maxSchoolSize
) {
    public FishSpawnProfile {
        if (habitatWeights == null || habitatWeights.isEmpty()) {
            throw new IllegalArgumentException("A fish spawn profile needs at least one habitat");
        }
        EnumMap<FishHabitat, Double> copy = new EnumMap<>(FishHabitat.class);
        habitatWeights.forEach((habitat, weight) -> {
            if (habitat == null || weight == null || !Double.isFinite(weight) || weight < 0.0) {
                throw new IllegalArgumentException("Invalid fish habitat weight");
            }
            if (weight > 0.0) copy.put(habitat, weight);
        });
        if (copy.isEmpty()) throw new IllegalArgumentException("All fish habitat weights are zero");
        if (minDepth < 1 || preferredDepth < minDepth || maxPopulation < 1) {
            throw new IllegalArgumentException("Invalid fish depth or population settings");
        }
        if (minSchoolSize < 1 || maxSchoolSize < minSchoolSize || maxSchoolSize > 16) {
            throw new IllegalArgumentException("Invalid fish school size");
        }
        habitatWeights = Collections.unmodifiableMap(copy);
    }

    /** Returns this profile with the minimum and maximum natural group size. */
    public FishSpawnProfile withSchoolSize(int minimum, int maximum) {
        return new FishSpawnProfile(habitatWeights, minDepth, preferredDepth, maxPopulation,
                large, minimum, maximum);
    }

    public double weightFor(FishHabitat habitat, int depth) {
        if (depth < minDepth) return 0.0;
        double weight = habitatWeights.getOrDefault(habitat, 0.0);
        if (weight <= 0.0) return 0.0;
        if (depth < preferredDepth) {
            weight *= 0.70 + 0.30 * ((depth - minDepth + 1.0) / (preferredDepth - minDepth + 1.0));
        }
        return weight;
    }

    public static FishSpawnProfile of(int minDepth, int preferredDepth, int maxPopulation, boolean large,
                                      Object... habitatAndWeights) {
        if (habitatAndWeights.length == 0 || habitatAndWeights.length % 2 != 0) {
            throw new IllegalArgumentException("Expected habitat/weight pairs");
        }
        EnumMap<FishHabitat, Double> weights = new EnumMap<>(FishHabitat.class);
        for (int i = 0; i < habitatAndWeights.length; i += 2) {
            FishHabitat habitat = (FishHabitat) habitatAndWeights[i];
            double weight = ((Number) habitatAndWeights[i + 1]).doubleValue();
            weights.put(habitat, weight);
        }
        return new FishSpawnProfile(weights, minDepth, preferredDepth, maxPopulation, large, 1, 1);
    }
}
