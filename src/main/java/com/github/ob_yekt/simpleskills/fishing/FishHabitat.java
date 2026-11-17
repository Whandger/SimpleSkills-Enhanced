package com.github.ob_yekt.simpleskills.fishing;

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

/** Broad aquatic habitats used by both fishing selection and natural fish spawning. */
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
        if (biome.is(BiomeTags.IS_RIVER)) {
            return RIVER;
        }

        float temperature = biome.value().getBaseTemperature();
        if (biome.is(BiomeTags.IS_OCEAN)) {
            // Vanilla ocean biomes use nearly identical base temperatures. Their registry IDs,
            // not temperature, are the reliable way to distinguish warm, temperate and cold seas.
            String path = biome.unwrapKey()
                    .map(key -> key.identifier().getPath())
                    .orElse("");

            if (path.equals("warm_ocean") || path.equals("deep_warm_ocean")
                    || path.equals("lukewarm_ocean") || path.equals("deep_lukewarm_ocean")) {
                return WARM_OCEAN;
            }
            if (path.equals("cold_ocean") || path.equals("deep_cold_ocean")
                    || path.equals("frozen_ocean") || path.equals("deep_frozen_ocean")) {
                return COLD_OCEAN;
            }
            return TEMPERATE_OCEAN;
        }

        // Swamps, mangrove swamps and jungle waterways are treated as warm freshwater.
        if (temperature >= 0.8F) return WARM_FRESHWATER;
        if (temperature <= 0.3F) return COLD_FRESHWATER;
        return TEMPERATE_FRESHWATER;
    }
}
