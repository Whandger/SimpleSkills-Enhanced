package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Adds Abyss worldgen and assigns deterministic crimson crystal biome regions inside the Abyss layer. */
public final class AbyssWorldgen {
    public static final int MIN_Y = -256;
    public static final int MAX_Y = -80;

    private static final int CRIMSON_GRID_SIZE = 1_400;
    private static final int CRIMSON_CENTER_JITTER = 150;
    private static final int CRIMSON_RADIUS_MIN = 500;
    private static final int CRIMSON_RADIUS_MAX = 620;

    private AbyssWorldgen() {}

    public static boolean isAbyssY(int y) {
        return y >= MIN_Y && y <= MAX_Y;
    }

    public static boolean intersectsAbyss(int minY, int maxY) {
        return maxY >= MIN_Y && minY <= MAX_Y;
    }

    /** The Abyss is an Overworld layer, not a rule for other dimensions. */
    public static boolean isOverworld(LevelAccessor level) {
        if (level instanceof ServerLevel serverLevel) {
            return Level.OVERWORLD.equals(serverLevel.dimension());
        }
        if (level instanceof ServerLevelAccessor serverLevelAccessor) {
            return Level.OVERWORLD.equals(serverLevelAccessor.getLevel().dimension());
        }
        if (level instanceof Level world) {
            return Level.OVERWORLD.equals(world.dimension());
        }
        return false;
    }

    private static ResourceKey<PlacedFeature> placed(String path) {
        return ResourceKey.create(Registries.PLACED_FEATURE,
                Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, path));
    }

    public static final ResourceKey<Biome> CRIMSON_CRYSTAL_BIOME = ResourceKey.create(
            Registries.BIOME, Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "crimson_crystal_caverns"));

    public static final ResourceKey<PlacedFeature> ABYSSAL_DIAMOND = placed("abyssal_diamond_ore");
    public static final ResourceKey<PlacedFeature> ABYSSAL_EMERALD = placed("abyssal_emerald_ore");
    public static final ResourceKey<PlacedFeature> ABYSSAL_RUNE = placed("abyssal_rune_ore");
    public static final ResourceKey<PlacedFeature> TRANSCENDENT = placed("transcendent_ore");
    public static final ResourceKey<PlacedFeature> ABYSSAL_SPELEOTHEMS = placed("abyssal_speleothem_clusters");
    public static final ResourceKey<PlacedFeature> ABYSSAL_MONUMENTAL_SPELEOTHEMS = placed("abyssal_monumental_speleothems");
    public static final ResourceKey<PlacedFeature> ABYSSAL_COLOSSAL_SPELEOTHEMS = placed("abyssal_colossal_speleothems");
    public static final ResourceKey<PlacedFeature> ABYSSAL_DEAD_TREES = placed("abyssal_dead_trees");
    public static final ResourceKey<PlacedFeature> ABYSSAL_ANCIENT_DEAD_TREES = placed("abyssal_ancient_dead_trees");

    public static final ResourceKey<PlacedFeature> CRIMSON_BIOME_PALETTE = placed("crimson_biome_palette");
    public static final ResourceKey<PlacedFeature> CRIMSON_CRYSTAL_HEART = placed("crimson_crystal_heart");
    public static final ResourceKey<PlacedFeature> CRIMSON_CRYSTAL_FLOOR_SPIRES = placed("crimson_crystal_floor_spires");
    public static final ResourceKey<PlacedFeature> CRIMSON_CRYSTAL_CEILING_SPIRES = placed("crimson_crystal_ceiling_spires");
    public static final ResourceKey<PlacedFeature> CRIMSON_CRYSTAL_WALL_SPIRES = placed("crimson_crystal_wall_spires");

    private static boolean registered;

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        // Structure and piece types must be registered before worldgen data is read.
        AbyssalCastleStructure.register();
        AbyssalBridgeStructure.register();

        // The biome painter runs first in underground decoration and changes only the biome palette
        // for quart cells inside the Abyss and inside deterministic crimson regions.
        addDecoration(CRIMSON_BIOME_PALETTE);
        addDecoration(CRIMSON_CRYSTAL_HEART);
        addDecoration(CRIMSON_CRYSTAL_FLOOR_SPIRES);
        addDecoration(CRIMSON_CRYSTAL_CEILING_SPIRES);
        addDecoration(CRIMSON_CRYSTAL_WALL_SPIRES);

        // The Abyss is an endgame layer: only diamond, emerald, rune, and Transcendent ores spawn here.
        add(ABYSSAL_DIAMOND);
        add(ABYSSAL_EMERALD);
        add(ABYSSAL_RUNE);
        add(TRANSCENDENT);
        addDecoration(ABYSSAL_SPELEOTHEMS);
        addDecoration(ABYSSAL_MONUMENTAL_SPELEOTHEMS);
        addDecoration(ABYSSAL_COLOSSAL_SPELEOTHEMS);
        addDecoration(ABYSSAL_DEAD_TREES);
        addDecoration(ABYSSAL_ANCIENT_DEAD_TREES);
        Simpleskills.LOGGER.info("Registered The Abyss worldgen, including the Crimson Crystal Caverns biome and its colossal hollow crystal.");
    }

    private static void add(ResourceKey<PlacedFeature> feature) {
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES, feature);
    }

    private static void addDecoration(ResourceKey<PlacedFeature> feature) {
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_DECORATION, feature);
    }

    /**
     * Returns the deterministic crimson biome region containing this X/Z coordinate, or null.
     * Regions are large, separated patches rather than a noisy checkerboard; the same world seed
     * therefore produces stable borders and a stable central-heart location.
     */
    public static CrimsonCrystalRegion crimsonRegionAt(int x, int z, long worldSeed) {
        int cellX = Math.floorDiv(x, CRIMSON_GRID_SIZE);
        int cellZ = Math.floorDiv(z, CRIMSON_GRID_SIZE);

        CrimsonCrystalRegion best = null;
        long bestDistanceSquared = Long.MAX_VALUE;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                CrimsonCrystalRegion candidate = crimsonRegionForCell(cellX + dx, cellZ + dz, worldSeed);
                if (candidate == null) continue;
                long offsetX = (long) x - candidate.centerX();
                long offsetZ = (long) z - candidate.centerZ();
                long distanceSquared = offsetX * offsetX + offsetZ * offsetZ;
                if (distanceSquared <= (long) candidate.radius() * candidate.radius()
                        && distanceSquared < bestDistanceSquared) {
                    bestDistanceSquared = distanceSquared;
                    best = candidate;
                }
            }
        }
        return best;
    }

    /**
     * Returns the nearest deterministic crimson region to an X/Z position, even when the
     * position is outside the biome. The search is seed-stable and does not load or generate chunks.
     */
    public static CrimsonCrystalRegion nearestCrimsonCrystalRegion(int x, int z, long worldSeed) {
        int cellX = Math.floorDiv(x, CRIMSON_GRID_SIZE);
        int cellZ = Math.floorDiv(z, CRIMSON_GRID_SIZE);
        CrimsonCrystalRegion best = null;
        long bestDistanceSquared = Long.MAX_VALUE;

        // A 13x13 cell window is wide enough to reliably find a region at the 42% cell density.
        for (int dz = -6; dz <= 6; dz++) {
            for (int dx = -6; dx <= 6; dx++) {
                CrimsonCrystalRegion candidate = crimsonRegionForCell(cellX + dx, cellZ + dz, worldSeed);
                if (candidate == null) continue;
                long offsetX = (long) x - candidate.centerX();
                long offsetZ = (long) z - candidate.centerZ();
                long distanceSquared = offsetX * offsetX + offsetZ * offsetZ;
                if (distanceSquared < bestDistanceSquared) {
                    bestDistanceSquared = distanceSquared;
                    best = candidate;
                }
            }
        }
        return best;
    }

    private static CrimsonCrystalRegion crimsonRegionForCell(int regionCellX, int regionCellZ, long worldSeed) {
        long cellHash = mix64(worldSeed
                ^ ((long) regionCellX * 0x9E3779B97F4A7C15L)
                ^ ((long) regionCellZ * 0xC2B2AE3D27D4EB4FL));

        // About two in five grid cells contain a crimson region.
        if (Long.remainderUnsigned(cellHash, 100L) >= 42L) return null;

        long xHash = mix64(cellHash + 0x165667B19E3779F9L);
        long zHash = mix64(cellHash + 0x85EBCA77C2B2AE63L);
        int jitterX = (int) Long.remainderUnsigned(xHash, CRIMSON_CENTER_JITTER * 2L + 1L)
                - CRIMSON_CENTER_JITTER;
        int jitterZ = (int) Long.remainderUnsigned(zHash, CRIMSON_CENTER_JITTER * 2L + 1L)
                - CRIMSON_CENTER_JITTER;

        int centerX = Math.floorDiv(regionCellX * CRIMSON_GRID_SIZE + jitterX, 16) * 16 + 8;
        int centerZ = Math.floorDiv(regionCellZ * CRIMSON_GRID_SIZE + jitterZ, 16) * 16 + 8;
        int radius = CRIMSON_RADIUS_MIN
                + (int) Long.remainderUnsigned(mix64(cellHash + 0x27D4EB2F165667C5L),
                CRIMSON_RADIUS_MAX - CRIMSON_RADIUS_MIN + 1L);
        return new CrimsonCrystalRegion(centerX, centerZ, radius);
    }

    public static boolean isCrimsonCrystalBiome(int x, int z, long worldSeed) {
        return crimsonRegionAt(x, z, worldSeed) != null;
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    public record CrimsonCrystalRegion(int centerX, int centerZ, int radius) {}
}
