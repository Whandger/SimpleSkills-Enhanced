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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Generates multi-prism hexagonal red-crystal clusters attached to cavern floors,
 * ceilings, or side walls, surrounded by localized crimson stone corruption.
 */
public record CrimsonCrystalSpireFeature(
        String mode,
        int minLength,
        int maxLength,
        int minRadius,
        int maxRadius,
        int clusterCount
) implements Feature {
    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    public static final MapCodec<CrimsonCrystalSpireFeature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("mode").forGetter(CrimsonCrystalSpireFeature::mode),
            Codec.intRange(8, 30).fieldOf("min_length").forGetter(CrimsonCrystalSpireFeature::minLength),
            Codec.intRange(8, 30).fieldOf("max_length").forGetter(CrimsonCrystalSpireFeature::maxLength),
            Codec.intRange(1, 5).fieldOf("min_radius").forGetter(CrimsonCrystalSpireFeature::minRadius),
            Codec.intRange(1, 5).fieldOf("max_radius").forGetter(CrimsonCrystalSpireFeature::maxRadius),
            Codec.intRange(0, 10).fieldOf("cluster_count").forGetter(CrimsonCrystalSpireFeature::clusterCount)
    ).apply(instance, CrimsonCrystalSpireFeature::new));

    public static void registerType() {
        Registry.register(BuiltInRegistries.FEATURE_TYPE,
                Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "crimson_crystal_spire"), CODEC);
    }

    @Override
    public MapCodec<? extends Feature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        if (minLength > maxLength || minRadius > maxRadius
                || !AbyssWorldgen.isOverworld(level)
                || !AbyssWorldgen.isAbyssY(origin.getY())
                || !AbyssWorldgen.isCrimsonCrystalBiome(origin.getX(), origin.getZ(), level.getSeed())) {
            return false;
        }

        CrimsonCrystalGeometry.SafeBounds bounds = CrimsonCrystalGeometry.SafeBounds.fromOrigin(origin);

        return switch (mode) {
            case "floor" -> {
                BlockPos anchor = findVerticalAnchor(level, origin, false);
                boolean grown = anchor != null && growVerticalCluster(level, bounds, anchor, Direction.UP, random);
                if (grown && anchor != null && random.nextFloat() < 0.82F) {
                    int baseR = between(random, minRadius, maxRadius);
                    CrimsonCorruptionVeins.generate(level, bounds, anchor.below(), random, Math.max(2, baseR), 18, 4);
                }
                yield grown;
            }
            case "ceiling" -> {
                BlockPos anchor = findVerticalAnchor(level, origin, true);
                yield anchor != null && growVerticalCluster(level, bounds, anchor, Direction.DOWN, random);
            }
            case "wall" -> {
                WallAnchor anchor = findWallAnchor(level, origin, random);
                yield anchor != null && growWallCluster(level, bounds, anchor, random);
            }
            default -> false;
        };
    }

    private BlockPos findVerticalAnchor(WorldGenLevel level, BlockPos origin, boolean ceiling) {
        for (int distance = 0; distance <= 26; distance++) {
            int[] offsets = distance == 0 ? new int[]{0} : new int[]{distance, -distance};
            for (int offset : offsets) {
                BlockPos candidate = origin.offset(0, offset, 0);
                BlockPos support = ceiling ? candidate.above() : candidate.below();
                Direction supportFace = ceiling ? Direction.DOWN : Direction.UP;
                if (candidate.getY() > AbyssWorldgen.MIN_Y && candidate.getY() < AbyssWorldgen.MAX_Y
                        && level.getBlockState(candidate).isAir()
                        && CrimsonCrystalGeometry.isNaturalRock(level.getBlockState(support))
                        && level.getBlockState(support).isFaceSturdy(level, support, supportFace)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private WallAnchor findWallAnchor(WorldGenLevel level, BlockPos origin, RandomSource random) {
        int start = random.nextInt(HORIZONTAL.length);
        for (int directionIndex = 0; directionIndex < HORIZONTAL.length; directionIndex++) {
            Direction direction = HORIZONTAL[(start + directionIndex) % HORIZONTAL.length];
            for (int distance = 0; distance <= 12; distance++) {
                int[] verticalOffsets = distance == 0 ? new int[]{0} : new int[]{0, 2, -2, 4, -4};
                for (int dy : verticalOffsets) {
                    BlockPos candidate = origin.relative(direction, distance).above(dy);
                    BlockPos support = candidate.relative(direction.getOpposite());
                    BlockState supportState = level.getBlockState(support);
                    if (!AbyssWorldgen.isAbyssY(candidate.getY()) || !level.getBlockState(candidate).isAir()
                            || !CrimsonCrystalGeometry.isNaturalRock(supportState)
                            || !supportState.isFaceSturdy(level, support, direction)) {
                        continue;
                    }
                    boolean open = true;
                    for (int step = 1; step <= 4; step++) {
                        if (!level.getBlockState(candidate.relative(direction, step)).isAir()) {
                            open = false;
                            break;
                        }
                    }
                    if (open) return new WallAnchor(candidate, direction);
                }
            }
        }
        return null;
    }

    private boolean growVerticalCluster(WorldGenLevel level, CrimsonCrystalGeometry.SafeBounds bounds,
                                        BlockPos anchor, Direction verticalDir, RandomSource random) {
        int primaryLength = between(random, minLength, maxLength);
        double primaryRadius = between(random, minRadius, maxRadius) * (0.95 + random.nextDouble() * 0.25);
        double verticalSign = verticalDir == Direction.UP ? 1.0 : -1.0;
        float budChance = Mth.clamp(0.03F + clusterCount * 0.008F, 0.03F, 0.10F);

        // Primary inclined hexagonal blade.
        double mainAzimuth = random.nextDouble() * Math.PI * 2.0;
        double mainTilt = 0.14 + random.nextDouble() * 0.24; // ~8° to ~22° tilt
        double mainVert = Math.sqrt(1.0 - mainTilt * mainTilt) * verticalSign;

        boolean placed = CrimsonCrystalGeometry.placeHexPrism(
                level, bounds,
                anchor.getX() + 0.5,
                anchor.getY() + 0.5 - verticalSign * 1.2,
                anchor.getZ() + 0.5,
                Math.cos(mainAzimuth) * mainTilt,
                mainVert,
                Math.sin(mainAzimuth) * mainTilt,
                primaryLength,
                primaryRadius,
                0.70,
                random.nextDouble() * Math.PI,
                2,
                budChance,
                random
        );

        // 2 to 4 companion crystal shards bursting outward from the same mineral root.
        int companions = 2 + random.nextInt(3);
        for (int i = 0; i < companions; i++) {
            double azimuth = mainAzimuth + Math.PI * 0.55 + (Math.PI * 1.45 * i / companions)
                    + (random.nextDouble() - 0.5) * 0.35;
            double tilt = 0.38 + random.nextDouble() * 0.34; // ~22° to ~45° outward splay
            double vert = Math.sqrt(1.0 - tilt * tilt) * verticalSign;
            double subLength = primaryLength * (0.42 + random.nextDouble() * 0.28);
            double subRadius = Math.max(0.95, primaryRadius * (0.50 + random.nextDouble() * 0.22));
            double offsetDist = Math.min(1.6, primaryRadius * 0.45);

            placed |= CrimsonCrystalGeometry.placeHexPrism(
                    level, bounds,
                    anchor.getX() + 0.5 + Math.cos(azimuth) * offsetDist,
                    anchor.getY() + 0.5 - verticalSign * 0.8,
                    anchor.getZ() + 0.5 + Math.sin(azimuth) * offsetDist,
                    Math.cos(azimuth) * tilt,
                    vert,
                    Math.sin(azimuth) * tilt,
                    subLength,
                    subRadius,
                    0.66,
                    random.nextDouble() * Math.PI,
                    2,
                    budChance,
                    random
            );
        }

        return placed;
    }

    private boolean growWallCluster(WorldGenLevel level, CrimsonCrystalGeometry.SafeBounds bounds,
                                    WallAnchor anchor, RandomSource random) {
        int primaryLength = Math.min(18, between(random, minLength, maxLength));
        double primaryRadius = between(random, minRadius, maxRadius) * (0.90 + random.nextDouble() * 0.20);
        float budChance = Mth.clamp(0.035F + clusterCount * 0.008F, 0.035F, 0.10F);

        Direction outDir = anchor.direction;
        Direction acrossDir = outDir.getClockWise();

        // Main wall prism angles outward and slightly upward (12°–26°) for natural tension.
        double pitch = 0.22 + random.nextDouble() * 0.24;
        double yaw = (random.nextDouble() - 0.5) * 0.30;
        double dirX = outDir.getStepX() + acrossDir.getStepX() * yaw;
        double dirY = pitch;
        double dirZ = outDir.getStepZ() + acrossDir.getStepZ() * yaw;

        boolean placed = CrimsonCrystalGeometry.placeHexPrism(
                level, bounds,
                anchor.pos.getX() + 0.5 - outDir.getStepX() * 1.2,
                anchor.pos.getY() + 0.5,
                anchor.pos.getZ() + 0.5 - outDir.getStepZ() * 1.2,
                dirX, dirY, dirZ,
                primaryLength,
                primaryRadius,
                0.68,
                random.nextDouble() * Math.PI,
                2,
                budChance,
                random
        );

        // 1 to 2 fanned companion shards from the wall socket.
        int companions = 1 + random.nextInt(2);
        for (int i = 0; i < companions; i++) {
            double sideSign = (i == 0 ? 1.0 : -1.0) * (random.nextBoolean() ? 1.0 : -1.0);
            double subYaw = yaw + sideSign * (0.35 + random.nextDouble() * 0.25);
            double subPitch = pitch + (random.nextDouble() - 0.35) * 0.35;
            placed |= CrimsonCrystalGeometry.placeHexPrism(
                    level, bounds,
                    anchor.pos.getX() + 0.5 - outDir.getStepX() * 0.8 + acrossDir.getStepX() * sideSign * 0.7,
                    anchor.pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 0.8,
                    anchor.pos.getZ() + 0.5 - outDir.getStepZ() * 0.8 + acrossDir.getStepZ() * sideSign * 0.7,
                    outDir.getStepX() + acrossDir.getStepX() * subYaw,
                    subPitch,
                    outDir.getStepZ() + acrossDir.getStepZ() * subYaw,
                    primaryLength * (0.48 + random.nextDouble() * 0.24),
                    Math.max(0.9, primaryRadius * 0.58),
                    0.65,
                    random.nextDouble() * Math.PI,
                    1,
                    budChance,
                    random
            );
        }

        return placed;
    }

    private static int between(RandomSource random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private record WallAnchor(BlockPos pos, Direction direction) {}
}

