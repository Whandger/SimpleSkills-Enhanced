package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Builds thick, irregular, full-block Abyssal formations attached to cave ceilings or floors.
 * Unlike vanilla single-block pillars, each placement forms a broad body with overlapping
 * subsidiary teeth, avoiding long one-block-wide spikes.
 */
public record AbyssalSolidCaveFormationFeature(
        boolean hangsFromCeiling,
        int minLength,
        int maxLength,
        int minRadius,
        int maxRadius,
        int satelliteCount
) implements Feature {

    public static final MapCodec<AbyssalSolidCaveFormationFeature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.fieldOf("hangs_from_ceiling").forGetter(AbyssalSolidCaveFormationFeature::hangsFromCeiling),
            Codec.intRange(6, 24).fieldOf("min_length").forGetter(AbyssalSolidCaveFormationFeature::minLength),
            Codec.intRange(6, 24).fieldOf("max_length").forGetter(AbyssalSolidCaveFormationFeature::maxLength),
            Codec.intRange(2, 6).fieldOf("min_radius").forGetter(AbyssalSolidCaveFormationFeature::minRadius),
            Codec.intRange(2, 6).fieldOf("max_radius").forGetter(AbyssalSolidCaveFormationFeature::maxRadius),
            Codec.intRange(0, 6).fieldOf("satellite_count").forGetter(AbyssalSolidCaveFormationFeature::satelliteCount)
    ).apply(instance, AbyssalSolidCaveFormationFeature::new));

    public static void registerType() {
        Registry.register(
                BuiltInRegistries.FEATURE_TYPE,
                Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "solid_cave_formation"),
                CODEC
        );
    }

    @Override
    public MapCodec<? extends Feature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        if (minLength > maxLength || minRadius > maxRadius) {
            return false;
        }

        Direction supportFace = hangsFromCeiling ? Direction.DOWN : Direction.UP;
        BlockPos support = hangsFromCeiling ? origin.above() : origin.below();
        if (!isAir(level.getBlockState(origin))
                || !level.getBlockState(support).isFaceSturdy(level, support, supportFace)) {
            return false;
        }

        int mainLength = randomBetween(random, minLength, maxLength);
        double mainRadius = randomBetween(random, minRadius, maxRadius);
        boolean placed = growMassiveTooth(level, origin, mainLength, mainRadius, random);

        // Satellite teeth overlap the main body and one another, making a single broad rock mass
        // instead of a row of disconnected, needle-thin columns.
        int placedSatellites = 0;
        int attempts = Math.max(4, satelliteCount * 5);
        for (int attempt = 0; attempt < attempts && placedSatellites < satelliteCount; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int distance = 2 + random.nextInt(Math.max(1, mainRadius > 3.5 ? 4 : 3));
            int dx = (int) Math.round(Math.cos(angle) * distance);
            int dz = (int) Math.round(Math.sin(angle) * distance);
            if (dx == 0 && dz == 0) {
                continue;
            }

            BlockPos satelliteAnchor = findNearbyAnchor(level, origin, dx, dz, hangsFromCeiling);
            if (satelliteAnchor == null) {
                continue;
            }

            int satelliteLength = Math.max(6, mainLength - 2 - random.nextInt(4));
            double satelliteRadius = Math.max(1.35, mainRadius * (0.52 + random.nextDouble() * 0.16));
            if (growMassiveTooth(level, satelliteAnchor, satelliteLength, satelliteRadius, random)) {
                placed = true;
                placedSatellites++;
            }
        }

        return placed;
    }

    private static int randomBetween(RandomSource random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private BlockPos findNearbyAnchor(WorldGenLevel level, BlockPos origin, int dx, int dz, boolean hanging) {
        Direction face = hanging ? Direction.DOWN : Direction.UP;
        int[] verticalOffsets = {0, -1, 1, -2, 2};

        for (int dy : verticalOffsets) {
            BlockPos airPos = origin.offset(dx, dy, dz);
            BlockPos supportPos = hanging ? airPos.above() : airPos.below();
            if (isAir(level.getBlockState(airPos))
                    && level.getBlockState(supportPos).isFaceSturdy(level, supportPos, face)) {
                return airPos;
            }
        }
        return null;
    }

    private boolean growMassiveTooth(WorldGenLevel level, BlockPos anchor, int length,
                                     double baseRadius, RandomSource random) {
        int verticalDirection = hangsFromCeiling ? -1 : 1;
        double radiusX = baseRadius * (0.92 + random.nextDouble() * 0.16);
        double radiusZ = baseRadius * (0.92 + random.nextDouble() * 0.16);
        int centerOffsetX = 0;
        int centerOffsetZ = 0;
        boolean placedAny = false;

        BlockState pillar = AbyssContent.ABYSSAL_PILLAR.defaultBlockState();
        for (int layer = 0; layer < length; layer++) {
            int y = anchor.getY() + verticalDirection * layer;
            if (y < level.getMinY() || y >= level.getMaxY()) {
                break;
            }

            // Keep the body broad for most of its length, then taper only slightly at the end.
            // This produces blunt, chunky teeth rather than comically long pointed needles.
            double progress = length <= 1 ? 1.0 : layer / (double) (length - 1);
            double taper = 0.88 - 0.34 * progress;
            double irregularity = (random.nextDouble() - 0.5) * 0.42;
            double layerRadiusX = Math.max(1.15, radiusX * taper + irregularity);
            double layerRadiusZ = Math.max(1.15, radiusZ * taper - irregularity * 0.6);

            if (layer > 1 && layer % 3 == 0 && random.nextFloat() < 0.42F) {
                centerOffsetX += random.nextInt(3) - 1;
                centerOffsetZ += random.nextInt(3) - 1;
            }

            BlockPos center = new BlockPos(anchor.getX() + centerOffsetX, y, anchor.getZ() + centerOffsetZ);
            if (!isAir(level.getBlockState(center))) {
                break;
            }

            int boundX = (int) Math.ceil(layerRadiusX);
            int boundZ = (int) Math.ceil(layerRadiusZ);
            for (int dx = -boundX; dx <= boundX; dx++) {
                for (int dz = -boundZ; dz <= boundZ; dz++) {
                    double ellipse = (dx * dx) / (layerRadiusX * layerRadiusX)
                            + (dz * dz) / (layerRadiusZ * layerRadiusZ);
                    double edgeNoise = (random.nextDouble() - 0.5) * 0.22;
                    if (ellipse > 1.0 + edgeNoise) {
                        continue;
                    }

                    BlockPos target = center.offset(dx, 0, dz);
                    if (target.getY() >= level.getMinY() && target.getY() < level.getMaxY()
                            && isAir(level.getBlockState(target))) {
                        level.setBlock(target, pillar, 2);
                        placedAny = true;
                    }
                }
            }
        }
        return placedAny;
    }

    private static boolean isAir(BlockState state) {
        return state.isAir();
    }
}
