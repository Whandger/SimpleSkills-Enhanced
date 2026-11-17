package com.github.ob_yekt.simpleskills.fishing;

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

/** Broad aquatic habitats used by both fishing selection and ambient fish spawning. */
public enum FishHabitat {
    RIVER,
    WARM_FRESHWATER,
    TEMPERATE_FRESHWATER,
    COLD_FRESHWATER,
    WARM_OCEAN,
    TEMPERATE_OCEAN,
    COLD_OCEAN;

    public boolean isOcean() {
        return this == WARM_OCEAN || this == TEMPERATE_OCEAN || this == COLD_OCEAN;
    }

    public static FishHabitat fromBiome(Holder<Biome> biome) {
        float temperature = biome.value().getBaseTemperature();
        if (biome.is(BiomeTags.IS_RIVER)) return RIVER;
        if (biome.is(BiomeTags.IS_OCEAN)) {
            if (temperature >= 0.95F) return WARM_OCEAN;
            if (temperature <= 0.3F) return COLD_OCEAN;
            return TEMPERATE_OCEAN;
        }
        if (temperature >= 0.95F) return WARM_FRESHWATER;
        if (temperature <= 0.3F) return COLD_FRESHWATER;
        return TEMPERATE_FRESHWATER;
    }
}
