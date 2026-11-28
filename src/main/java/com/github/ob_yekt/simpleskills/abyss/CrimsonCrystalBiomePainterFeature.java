package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Paints the generated chunk's biome palette with the custom Crimson Crystal biome only in the
 * selected X/Z regions and only across the vertical Abyss layer. This deliberately preserves the
 * chunk's original biome holders everywhere else.
 */
public record CrimsonCrystalBiomePainterFeature() implements Feature {
    public static final MapCodec<CrimsonCrystalBiomePainterFeature> CODEC =
            MapCodec.unit(new CrimsonCrystalBiomePainterFeature());

    public static void registerType() {
        Registry.register(BuiltInRegistries.FEATURE_TYPE,
                Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "crimson_crystal_biome_painter"), CODEC);
    }

    @Override
    public MapCodec<? extends Feature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, net.minecraft.core.BlockPos origin) {
        if (!AbyssWorldgen.isOverworld(level) || !AbyssWorldgen.isAbyssY(origin.getY())) {
            return false;
        }

        ChunkAccess chunk = level.getChunk(origin.getX() >> 4, origin.getZ() >> 4);
        var biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        Holder<Biome> crimsonBiome = biomeRegistry.getOrThrow(AbyssWorldgen.CRIMSON_CRYSTAL_BIOME);

        ChunkPos chunkPos = chunk.getPos();
        int minQuartX = chunkPos.getMinBlockX() >> 2;
        int minQuartZ = chunkPos.getMinBlockZ() >> 2;
        int minQuartY = chunk.getMinY() >> 2;
        int quartHeight = chunk.getHeight() >> 2;

        @SuppressWarnings("unchecked")
        Holder<Biome>[][][] originalBiomes = (Holder<Biome>[][][]) new Holder<?>[quartHeight][4][4];

        for (int quartY = 0; quartY < quartHeight; quartY++) {
            for (int quartZ = 0; quartZ < 4; quartZ++) {
                for (int quartX = 0; quartX < 4; quartX++) {
                    originalBiomes[quartY][quartZ][quartX] = chunk.getNoiseBiome(
                            minQuartX + quartX, minQuartY + quartY, minQuartZ + quartZ);
                }
            }
        }

        long seed = level.getSeed();
        chunk.fillBiomesFromNoise((quartX, quartY, quartZ) -> {
            int localX = quartX - minQuartX;
            int localY = quartY - minQuartY;
            int localZ = quartZ - minQuartZ;
            if (localX < 0 || localX >= 4 || localZ < 0 || localZ >= 4
                    || localY < 0 || localY >= quartHeight) {
                return chunk.getNoiseBiome(quartX, quartY, quartZ);
            }

            int blockX = quartX * 4 + 2;
            int blockY = quartY * 4 + 2;
            int blockZ = quartZ * 4 + 2;
            if (AbyssWorldgen.isAbyssY(blockY)
                    && AbyssWorldgen.isCrimsonCrystalBiome(blockX, blockZ, seed)) {
                return crimsonBiome;
            }
            return originalBiomes[localY][localZ][localX];
        });
        return true;
    }
}
