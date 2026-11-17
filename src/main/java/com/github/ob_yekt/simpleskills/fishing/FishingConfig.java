package com.github.ob_yekt.simpleskills.fishing;

import com.github.ob_yekt.simpleskills.managers.ConfigManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Cached view of the fishing options stored in {@code config.json}.
 *
 * <p>Every key is optional: a missing or invalid value falls back to its default, and the numeric
 * values are clamped to a sane range so a typo in the file can never freeze or break the server.
 * {@link #reload()} is called at the end of {@link ConfigManager#initialize()}, so the existing
 * {@code /simpleskills reload} command also reloads these options.</p>
 */
public final class FishingConfig {
    // ---- Key names (config.json) ---------------------------------------------------------------
    public static final String FIGHT_ENABLED = "fishing_fight_enabled";
    public static final String DETECTION_RANGE = "fishing_fish_detection_range";
    public static final String ATTRACTION_SPEED = "fishing_fish_attraction_speed";
    public static final String FLEE_RANGE = "fishing_fish_flee_range";
    public static final String HEARING_ENABLED = "fishing_fish_hearing_enabled";
    public static final String VISION_ENABLED = "fishing_fish_vision_enabled";
    public static final String FATIGUE_RATE = "fishing_fish_fatigue_rate";
    public static final String LINE_MAX_TENSION = "fishing_line_max_tension";
    public static final String LINE_RECOVERY_RATE = "fishing_line_recovery_rate";
    public static final String LINE_BREAK_DURABILITY_LOSS = "fishing_line_break_durability_loss";
    public static final String MIN_WATER_VOLUME = "fishing_ecosystem_min_water_volume";
    public static final String MIN_WATER_DEPTH = "fishing_ecosystem_min_water_depth";
    public static final String MIN_FISH_COUNT = "fishing_ecosystem_min_fish_count";
    public static final String SPAWN_ENABLED = "fishing_ecosystem_spawn_enabled";
    public static final String MAX_POPULATION = "fishing_ecosystem_max_population";
    public static final String BITE_WINDOW_TICKS = "fishing_bite_window_ticks";
    public static final String SPECIES_MIN_LEVEL = "fishing_species_min_level";
    public static final String CATCH_SCREEN_ENABLED = "fishing_catch_screen_enabled";
    public static final String SPECIES_SPAWN_WEIGHT = "fishing_species_spawn_weight";
    public static final String SPECIES_MAX_POPULATION = "fishing_species_max_population";
    public static final String NATURAL_SPAWNING_ENABLED = "fishing_ecosystem_natural_spawning_enabled";
    public static final String NATURAL_SPAWN_INTERVAL = "fishing_ecosystem_natural_spawn_interval_ticks";
    public static final String MEGAFAUNA_ENABLED = "fishing_ecosystem_megafauna_enabled";
    public static final String MAX_MEGAFAUNA_PER_AREA = "fishing_ecosystem_max_megafauna_per_area";

    private static volatile boolean fightEnabled = true;
    private static volatile double detectionRange = 12.0;
    private static volatile double attractionSpeed = 1.0;
    private static volatile double fleeRange = 8.0;
    private static volatile boolean hearingEnabled = true;
    private static volatile boolean visionEnabled = true;
    private static volatile double fatigueRate = 1.0;
    private static volatile double lineMaxTension = 1.0;
    private static volatile double lineRecoveryRate = 1.0;
    private static volatile int lineBreakDurabilityLoss = 3;
    private static volatile int minWaterVolume = 40;
    private static volatile int minWaterDepth = 2;
    private static volatile int minFishCount = 3;
    private static volatile boolean spawnEnabled = true;
    private static volatile int maxPopulation = 12;
    private static volatile int biteWindowTicks = 40;
    private static volatile boolean catchScreenEnabled = true;
    private static volatile boolean naturalSpawningEnabled = true;
    private static volatile int naturalSpawnInterval = 900;
    private static volatile boolean megafaunaEnabled = true;
    private static volatile int maxMegafaunaPerArea = 1;
    private static volatile JsonObject speciesMinLevel = new JsonObject();
    private static volatile JsonObject speciesSpawnWeight = new JsonObject();
    private static volatile JsonObject speciesMaxPopulation = new JsonObject();

    private FishingConfig() {}

    /** Adds the default value of every fishing option to {@code json} (used for config.json). */
    public static void addDefaults(JsonObject json) {
        json.addProperty(FIGHT_ENABLED, true);
        json.addProperty(DETECTION_RANGE, 12.0);
        json.addProperty(ATTRACTION_SPEED, 1.0);
        json.addProperty(FLEE_RANGE, 8.0);
        json.addProperty(HEARING_ENABLED, true);
        json.addProperty(VISION_ENABLED, true);
        json.addProperty(FATIGUE_RATE, 1.0);
        json.addProperty(LINE_MAX_TENSION, 1.0);
        json.addProperty(LINE_RECOVERY_RATE, 1.0);
        json.addProperty(LINE_BREAK_DURABILITY_LOSS, 3);
        json.addProperty(MIN_WATER_VOLUME, 40);
        json.addProperty(MIN_WATER_DEPTH, 2);
        json.addProperty(MIN_FISH_COUNT, 3);
        json.addProperty(SPAWN_ENABLED, true);
        json.addProperty(MAX_POPULATION, 12);
        json.addProperty(BITE_WINDOW_TICKS, 40);
        json.addProperty(CATCH_SCREEN_ENABLED, true);
        json.addProperty(NATURAL_SPAWNING_ENABLED, true);
        json.addProperty(NATURAL_SPAWN_INTERVAL, 900);
        json.addProperty(MEGAFAUNA_ENABLED, true);
        json.addProperty(MAX_MEGAFAUNA_PER_AREA, 1);
        JsonObject species = new JsonObject();
        JsonObject weights = new JsonObject();
        JsonObject populations = new JsonObject();
        for (FishSpecies s : FishSpecies.all()) {
            species.addProperty(s.configKey(), s.defaultMinLevel());
            weights.addProperty(s.configKey(), 1.0);
            populations.addProperty(s.configKey(), s.spawnProfile().maxPopulation());
        }
        json.add(SPECIES_MIN_LEVEL, species);
        json.add(SPECIES_SPAWN_WEIGHT, weights);
        json.add(SPECIES_MAX_POPULATION, populations);
    }

    /** Re-reads every option from {@link ConfigManager#getFeatureConfig()}. */
    public static void reload() {
        JsonObject cfg = ConfigManager.getFeatureConfig();
        fightEnabled = bool(cfg, FIGHT_ENABLED, true);
        detectionRange = num(cfg, DETECTION_RANGE, 12.0, 4.0, 32.0);
        attractionSpeed = num(cfg, ATTRACTION_SPEED, 1.0, 0.1, 5.0);
        fleeRange = num(cfg, FLEE_RANGE, 8.0, 2.0, 24.0);
        hearingEnabled = bool(cfg, HEARING_ENABLED, true);
        visionEnabled = bool(cfg, VISION_ENABLED, true);
        fatigueRate = num(cfg, FATIGUE_RATE, 1.0, 0.1, 5.0);
        lineMaxTension = num(cfg, LINE_MAX_TENSION, 1.0, 0.5, 3.0);
        lineRecoveryRate = num(cfg, LINE_RECOVERY_RATE, 1.0, 0.1, 5.0);
        lineBreakDurabilityLoss = (int) num(cfg, LINE_BREAK_DURABILITY_LOSS, 3, 0, 64);
        minWaterVolume = (int) num(cfg, MIN_WATER_VOLUME, 40, 1, 400);
        minWaterDepth = (int) num(cfg, MIN_WATER_DEPTH, 2, 1, 16);
        minFishCount = (int) num(cfg, MIN_FISH_COUNT, 3, 1, 32);
        spawnEnabled = bool(cfg, SPAWN_ENABLED, true);
        maxPopulation = (int) num(cfg, MAX_POPULATION, 12, 1, 64);
        biteWindowTicks = (int) num(cfg, BITE_WINDOW_TICKS, 40, 10, 200);
        catchScreenEnabled = bool(cfg, CATCH_SCREEN_ENABLED, true);
        naturalSpawningEnabled = bool(cfg, NATURAL_SPAWNING_ENABLED, true);
        naturalSpawnInterval = (int) num(cfg, NATURAL_SPAWN_INTERVAL, 900, 200, 7200);
        megafaunaEnabled = bool(cfg, MEGAFAUNA_ENABLED, true);
        maxMegafaunaPerArea = (int) num(cfg, MAX_MEGAFAUNA_PER_AREA, 1, 0, 8);
        JsonElement species = cfg.get(SPECIES_MIN_LEVEL);
        speciesMinLevel = species != null && species.isJsonObject() ? species.getAsJsonObject() : new JsonObject();
        JsonElement weights = cfg.get(SPECIES_SPAWN_WEIGHT);
        speciesSpawnWeight = weights != null && weights.isJsonObject() ? weights.getAsJsonObject() : new JsonObject();
        JsonElement populations = cfg.get(SPECIES_MAX_POPULATION);
        speciesMaxPopulation = populations != null && populations.isJsonObject() ? populations.getAsJsonObject() : new JsonObject();
    }

    public static boolean isFightEnabled() { return fightEnabled; }
    public static double detectionRange() { return detectionRange; }
    public static double attractionSpeed() { return attractionSpeed; }
    public static double fleeRange() { return fleeRange; }
    public static boolean isHearingEnabled() { return hearingEnabled; }
    public static boolean isVisionEnabled() { return visionEnabled; }
    public static double fatigueRate() { return fatigueRate; }
    public static double lineMaxTension() { return lineMaxTension; }
    public static double lineRecoveryRate() { return lineRecoveryRate; }
    public static int lineBreakDurabilityLoss() { return lineBreakDurabilityLoss; }
    public static int minWaterVolume() { return minWaterVolume; }
    public static int minWaterDepth() { return minWaterDepth; }
    public static int minFishCount() { return minFishCount; }
    public static boolean isSpawnEnabled() { return spawnEnabled; }
    public static int maxPopulation() { return Math.max(maxPopulation, minFishCount); }
    public static int biteWindowTicks() { return biteWindowTicks; }
    /** Whether the server sends the catch result screen to players that have the mod. */
    public static boolean isCatchScreenEnabled() { return catchScreenEnabled; }
    public static boolean isNaturalSpawningEnabled() { return naturalSpawningEnabled; }
    public static int naturalSpawnIntervalTicks() { return naturalSpawnInterval; }
    public static boolean isMegafaunaEnabled() { return megafaunaEnabled; }
    public static int maxMegafaunaPerArea() { return maxMegafaunaPerArea; }

    /** Multiplier applied to the species' habitat/depth weight. */
    public static double spawnWeightMultiplier(FishSpecies species) {
        JsonElement value = speciesSpawnWeight.get(species.configKey());
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            return Math.max(0.0, Math.min(10.0, value.getAsDouble()));
        }
        return 1.0;
    }

    /** Configurable local population cap, bounded by the species profile's safe maximum. */
    public static int maxPopulationFor(FishSpecies species) {
        JsonElement value = speciesMaxPopulation.get(species.configKey());
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            return Math.max(0, Math.min(64, value.getAsInt()));
        }
        return species.spawnProfile().maxPopulation();
    }

    /** Minimum Fishing level needed to hook the given species. */
    public static int minLevelFor(FishSpecies species) {
        JsonElement value = speciesMinLevel.get(species.configKey());
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            return Math.max(1, Math.min(99, value.getAsInt()));
        }
        return species.defaultMinLevel();
    }

    private static boolean bool(JsonObject cfg, String key, boolean def) {
        JsonElement e = cfg.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() ? e.getAsBoolean() : def;
    }

    private static double num(JsonObject cfg, String key, double def, double min, double max) {
        JsonElement e = cfg.get(key);
        double v = e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsDouble() : def;
        return Math.max(min, Math.min(max, v));
    }
}
