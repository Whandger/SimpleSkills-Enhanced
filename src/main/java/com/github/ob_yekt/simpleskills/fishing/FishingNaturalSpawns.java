package com.github.ob_yekt.simpleskills.fishing;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.Heightmap;

/** Adds the custom catchable fish to the actual biome natural-spawn tables. */
public final class FishingNaturalSpawns {
    private static boolean registered;

    private FishingNaturalSpawns() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // Match vanilla fish placement checks. Fabric's object-builder module widens this
        // registration method for mods, so the same IN_WATER rules as vanilla fish can be used.
        registerWaterPlacement(FishingEntities.TROUT, 2);
        registerWaterPlacement(FishingEntities.BASS, 2);
        registerWaterPlacement(FishingEntities.CATFISH, 4);
        registerWaterPlacement(FishingEntities.PIKE, 3);
        registerWaterPlacement(FishingEntities.CARP, 2);
        registerWaterPlacement(FishingEntities.TUNA, 8);
        registerWaterPlacement(FishingEntities.SWORDFISH, 10);
        registerWaterPlacement(FishingEntities.MAHI_MAHI, 5);
        registerWaterPlacement(FishingEntities.REEF_SHARK, 8);
        registerWaterPlacement(FishingEntities.GREAT_WHITE_SHARK, 12);
        registerWaterPlacement(FishingEntities.WHALE_SHARK, 16);

        Identifier modifierId = Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "natural_fish_spawns");
        BiomeModifications.create(modifierId).add(
                ModificationPhase.ADDITIONS,
                context -> context.canGenerateIn(LevelStem.OVERWORLD),
                (selection, context) -> addSpeciesForBiome(selection, context)
        );

        Simpleskills.LOGGER.info("Registered natural biome spawning for {} custom fish species", FishSpecies.all().size());
    }

    private static <T extends WaterAnimal> void registerWaterPlacement(EntityType<T> type, int minimumWaterDepth) {
        SpawnPlacements.register(type, SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (entityType, level, reason, pos, random) ->
                        WaterAnimal.checkSurfaceWaterAnimalSpawnRules(entityType, level, reason, pos, random)
                                && hasWaterColumn(level, pos, minimumWaterDepth));
    }

    /** Prevents deep-water species from being selected in shallow river mouths or coastal pools. */
    private static boolean hasWaterColumn(LevelAccessor level, BlockPos start, int requiredDepth) {
        BlockPos.MutableBlockPos cursor = start.mutable();
        for (int depth = 0; depth < requiredDepth; depth++) {
            if (!level.getFluidState(cursor).is(FluidTags.WATER)) return false;
            cursor.move(Direction.DOWN);
        }
        return true;
    }

    private static void addSpeciesForBiome(
            net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext selection,
            net.fabricmc.fabric.api.biome.v1.BiomeModificationContext context
    ) {
        if (!FishingConfig.isNaturalSpawningEnabled()) return;

        FishHabitat habitat = FishHabitat.fromBiome(selection.getBiomeHolder());
        for (FishSpecies species : FishSpecies.all()) {
            if (!species.isAmbientSpawnable()) continue;

            FishSpawnProfile profile = species.spawnProfile();
            double habitatWeight = profile.habitatWeights().getOrDefault(habitat, 0.0);
            double configuredWeight = habitatWeight * FishingConfig.spawnWeightMultiplier(species);
            if (configuredWeight <= 0.0) continue;

            // Profiles are normalized around 1.0. A base weight of 16 makes common custom fish
            // competitive with vanilla cod/salmon while preserving rarity within each habitat.
            int weight = (int) Math.max(1L, Math.min(100L, Math.round(configuredWeight * 16.0)));
            context.getMobSpawnSettings().addSpawn(
                    species.entityType().getCategory(),
                    new MobSpawnSettings.SpawnerData(
                            species.entityType(),
                            UniformInt.of(profile.minSchoolSize(), profile.maxSchoolSize())
                    ),
                    weight
            );
        }
    }
}
