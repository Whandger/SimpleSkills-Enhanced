package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Generates a colossal, open geode-crown of angled hexagonal crimson crystal prisms
 * enclosing a spacious boss arena with a grand arched entrance and branching floor corruption.
 */
public record CrimsonCrystalHeartFeature() implements Feature {
    public static final MapCodec<CrimsonCrystalHeartFeature> CODEC =
            MapCodec.unit(new CrimsonCrystalHeartFeature());

    private static final double ARENA_FLOOR_RADIUS = 10.5;
    private static final int MONOLITH_MAX_HEIGHT = 56;
    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    public static void registerType() {
        Registry.register(BuiltInRegistries.FEATURE_TYPE,
                Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "crimson_crystal_heart"), CODEC);
    }

    @Override
    public MapCodec<? extends Feature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        if (!AbyssWorldgen.isOverworld(level) || !AbyssWorldgen.isAbyssY(origin.getY())) return false;

        AbyssWorldgen.CrimsonCrystalRegion region = AbyssWorldgen.crimsonRegionAt(
                origin.getX(), origin.getZ(), level.getSeed());
        if (region == null) return false;

        int sourceChunkX = origin.getX() >> 4;
        int sourceChunkZ = origin.getZ() >> 4;
        if (sourceChunkX != Math.floorDiv(region.centerX(), 16)
                || sourceChunkZ != Math.floorDiv(region.centerZ(), 16)) {
            return false;
        }

        GroundAnchor ground = findGroundAnchor(level, region.centerX(), region.centerZ());
        if (ground == null) return false;

        BlockPos base = new BlockPos(ground.x, ground.y, ground.z);
        int availableHeight = AbyssWorldgen.MAX_Y - base.getY() - 4;
        int monolithHeight = Math.min(MONOLITH_MAX_HEIGHT, availableHeight);
        if (monolithHeight < 30) return false;

        CrimsonCrystalGeometry.SafeBounds bounds = CrimsonCrystalGeometry.SafeBounds.fromOrigin(origin);
        Direction entranceDir = chooseBestEntrance(level, base, bounds);
        Direction rearDir = entranceDir.getOpposite();

        // Prevent duplicate generation if another feature pass touches the region.
        BlockPos marker = base.relative(rearDir, 12).above(14);
        if (bounds.contains(marker.getX(), marker.getZ())
                && CrimsonCrystalGeometry.isCrystalBlock(level.getBlockState(marker))) {
            return false;
        }

        // 1. Corrupt the ground bed beneath the entire formation so roots blend into the cavern.
        buildCorruptedGroundBed(level, base, bounds, random);

        // 2. Monumental rear throne monolith + twin flanking guardian prisms.
        buildThroneMonolithGroup(level, base, bounds, rearDir, monolithHeight, random);

        // 3. Open crown of 14 outward-tilted hexagonal crystal blades forming the arena perimeter.
        buildCrownRingPrisms(level, base, bounds, entranceDir, monolithHeight, random);

        // 4. Outer low-angle buttress shards bursting from the corrupted stone collar.
        buildOuterButtressShards(level, base, bounds, entranceDir, random);

        // 5. Carve the spacious boss combat chamber & monumental arched entrance.
        carveBossArenaAndEntrance(level, base, bounds, entranceDir);

        // 6. Pave a smooth, trip-free boss arena floor with subtle crimson corruption inlays.
        buildArenaFloor(level, base, bounds, entranceDir, random);

        // 7. Spread organic branching corruption veins outward from the outer crystal ring.
        CrimsonCorruptionVeins.generate(level, bounds, base, random, 14, 30, 11);
        return true;
    }

    private static GroundAnchor findGroundAnchor(WorldGenLevel level, int centerX, int centerZ) {
        GroundAnchor best = null;
        double bestScore = Double.MAX_VALUE;
        int highestUsableFloor = AbyssWorldgen.MAX_Y - 34;

        for (int y = highestUsableFloor; y >= AbyssWorldgen.MIN_Y + 2; y--) {
            BlockPos floor = new BlockPos(centerX, y, centerZ);
            if (!CrimsonCrystalGeometry.isNaturalRock(level.getBlockState(floor))) continue;
            if (!level.getBlockState(floor.above()).isAir()
                    || !level.getBlockState(floor.above(2)).isAir()
                    || !level.getBlockState(floor.above(3)).isAir()) {
                continue;
            }

            int clearHeight = 0;
            for (int up = 1; up <= 48; up++) {
                if (!level.getBlockState(floor.above(up)).isAir()) break;
                clearHeight++;
            }
            if (clearHeight < 18) continue;

            double score = Math.abs(y - (-218)) * 0.12 - Math.min(clearHeight, 42) * 0.10;
            if (score < bestScore) {
                bestScore = score;
                best = new GroundAnchor(centerX, y, centerZ);
            }
        }
        return best;
    }

    /**
     * Picks the entrance direction that faces open cavern space while keeping maximum
     * chunk-write clearance on the opposite (rear throne monolith) side.
     */
    private static Direction chooseBestEntrance(WorldGenLevel level, BlockPos base,
                                                CrimsonCrystalGeometry.SafeBounds bounds) {
        Direction bestDir = Direction.SOUTH;
        double bestScore = -1.0E9;

        for (Direction dir : HORIZONTAL) {
            Direction across = dir.getClockWise();
            int openAir = 0;
            for (int dist = 13; dist <= 18; dist++) {
                BlockPos sample = base.relative(dir, dist).above(7);
                if (!bounds.contains(sample.getX(), sample.getZ())) break;
                for (int side = -4; side <= 4; side++) {
                    for (int dy = -4; dy <= 5; dy++) {
                        BlockPos check = sample.relative(across, side).above(dy);
                        if (bounds.contains(check.getX(), check.getZ())
                                && level.getBlockState(check).isAir()) {
                            openAir++;
                        }
                    }
                }
            }

            int rearClearance = bounds.clearance(base.getX(), base.getZ(), dir.getOpposite());
            double score = openAir + rearClearance * 3.5;
            if (score > bestScore) {
                bestScore = score;
                bestDir = dir;
            }
        }
        return bestDir;
    }

    private static void buildCorruptedGroundBed(WorldGenLevel level, BlockPos base,
                                                CrimsonCrystalGeometry.SafeBounds bounds,
                                                RandomSource random) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -18; dx <= 18; dx++) {
            for (int dz = -18; dz <= 18; dz++) {
                int wx = base.getX() + dx;
                int wz = base.getZ() + dz;
                if (!bounds.contains(wx, wz)) continue;

                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist < 9.5 || dist > 17.5) continue;
                double noise = Math.sin(dx * 0.45 + dz * 0.31) * 1.4 + (random.nextDouble() - 0.5) * 1.8;
                if (dist + noise > 16.8) continue;

                for (int dy = -2; dy <= 1; dy++) {
                    int wy = base.getY() + dy;
                    if (!AbyssWorldgen.isAbyssY(wy)) continue;
                    pos.set(wx, wy, wz);
                    BlockState existing = level.getBlockState(pos);
                    if (!CrimsonCrystalGeometry.isNaturalRock(existing)) continue;

                    BlockState corruption = (dist + noise < 14.5)
                            ? AbyssContent.HEAVY_CRIMSON_CORRUPTED_STONE.defaultBlockState()
                            : AbyssContent.CRIMSON_CORRUPTED_STONE.defaultBlockState();
                    level.setBlock(pos, corruption, 2);
                }
            }
        }
    }

    private static void buildThroneMonolithGroup(WorldGenLevel level, BlockPos base,
                                                 CrimsonCrystalGeometry.SafeBounds bounds,
                                                 Direction rearDir, int monolithHeight,
                                                 RandomSource random) {
        double rearAngle = Math.atan2(rearDir.getStepZ(), rearDir.getStepX());
        int rearClearance = bounds.clearance(base.getX(), base.getZ(), rearDir);
        double ringDist = Math.min(13.0, Math.max(11.0, rearClearance - 8.0));

        // Central colossal throne prism leaning slightly outward so it towers cleanly over the arena.
        double tilt = 0.16;
        double vertical = Math.sqrt(1.0 - tilt * tilt);
        CrimsonCrystalGeometry.placeHexPrism(
                level, bounds,
                base.getX() + 0.5 + Math.cos(rearAngle) * ringDist,
                base.getY() - 1.5,
                base.getZ() + 0.5 + Math.sin(rearAngle) * ringDist,
                Math.cos(rearAngle) * tilt,
                vertical,
                Math.sin(rearAngle) * tilt,
                monolithHeight,
                4.5,
                0.72,
                random.nextDouble() * Math.PI,
                4,
                0.045F,
                random
        );

        // Twin flanking guardian prisms beside the monolith.
        for (int side : new int[]{-1, 1}) {
            double guardAngle = rearAngle + side * 0.42;
            double guardTilt = 0.30 + random.nextDouble() * 0.05;
            double guardVert = Math.sqrt(1.0 - guardTilt * guardTilt);
            double guardLen = monolithHeight * (0.64 + random.nextDouble() * 0.08);
            CrimsonCrystalGeometry.placeHexPrism(
                    level, bounds,
                    base.getX() + 0.5 + Math.cos(guardAngle) * (ringDist - 0.4),
                    base.getY() - 1.5,
                    base.getZ() + 0.5 + Math.sin(guardAngle) * (ringDist - 0.4),
                    Math.cos(guardAngle) * guardTilt,
                    guardVert,
                    Math.sin(guardAngle) * guardTilt,
                    guardLen,
                    3.2,
                    0.70,
                    random.nextDouble() * Math.PI,
                    3,
                    0.05F,
                    random
            );
        }
    }

    private static void buildCrownRingPrisms(WorldGenLevel level, BlockPos base,
                                             CrimsonCrystalGeometry.SafeBounds bounds,
                                             Direction entranceDir, int monolithHeight,
                                             RandomSource random) {
        double entranceAngle = Math.atan2(entranceDir.getStepZ(), entranceDir.getStepX());
        double rearAngle = entranceAngle + Math.PI;
        int ringCount = 14;

        for (int i = 0; i < ringCount; i++) {
            double angle = (Math.PI * 2.0 * i) / ringCount + 0.14;

            // Leave the wide entrance passage open at the front and avoid overlapping the rear monolith.
            double distToEntrance = angularDistance(angle, entranceAngle);
            double distToRear = angularDistance(angle, rearAngle);
            if (distToEntrance < 0.46 || distToRear < 0.52) continue;

            boolean isGatekeeper = distToEntrance < 0.74;
            double maxSafeRing = maxRadialClearance(base, bounds, angle) - 5.5;
            double ringRadius = Mth.clamp(isGatekeeper ? 12.2 : 11.8 + random.nextDouble() * 1.1, 10.6, Math.max(10.8, maxSafeRing));

            double tilt = isGatekeeper
                    ? 0.42 + random.nextDouble() * 0.06
                    : 0.32 + random.nextDouble() * 0.20;
            double vertical = Math.sqrt(1.0 - tilt * tilt);

            double baseLen = isGatekeeper
                    ? Math.min(monolithHeight * 0.56, 27.0 + random.nextDouble() * 5.0)
                    : Math.min(monolithHeight * 0.50, 19.0 + random.nextDouble() * 10.0);
            double prismRadius = isGatekeeper
                    ? 2.8 + random.nextDouble() * 0.45
                    : 2.2 + random.nextDouble() * 0.75;

            CrimsonCrystalGeometry.placeHexPrism(
                    level, bounds,
                    base.getX() + 0.5 + Math.cos(angle) * ringRadius,
                    base.getY() - 1.5,
                    base.getZ() + 0.5 + Math.sin(angle) * ringRadius,
                    Math.cos(angle) * tilt,
                    vertical,
                    Math.sin(angle) * tilt,
                    baseLen,
                    prismRadius,
                    0.70,
                    random.nextDouble() * Math.PI,
                    3,
                    0.055F,
                    random
            );
        }
    }

    private static void buildOuterButtressShards(WorldGenLevel level, BlockPos base,
                                                 CrimsonCrystalGeometry.SafeBounds bounds,
                                                 Direction entranceDir, RandomSource random) {
        double entranceAngle = Math.atan2(entranceDir.getStepZ(), entranceDir.getStepX());
        int shardCount = 16;

        for (int i = 0; i < shardCount; i++) {
            double angle = (Math.PI * 2.0 * i) / shardCount + (random.nextDouble() - 0.5) * 0.22;
            if (angularDistance(angle, entranceAngle) < 0.52) continue;

            double maxSafeRing = maxRadialClearance(base, bounds, angle) - 5.0;
            double startDist = Mth.clamp(13.2 + random.nextDouble() * 1.2, 11.8, Math.max(12.0, maxSafeRing));
            double tilt = 0.60 + random.nextDouble() * 0.18;
            double vertical = Math.sqrt(1.0 - tilt * tilt);

            CrimsonCrystalGeometry.placeHexPrism(
                    level, bounds,
                    base.getX() + 0.5 + Math.cos(angle) * startDist,
                    base.getY() - 1.0,
                    base.getZ() + 0.5 + Math.sin(angle) * startDist,
                    Math.cos(angle) * tilt,
                    vertical,
                    Math.sin(angle) * tilt,
                    8.5 + random.nextDouble() * 6.5,
                    1.25 + random.nextDouble() * 0.65,
                    0.66,
                    random.nextDouble() * Math.PI,
                    2,
                    0.06F,
                    random
            );
        }
    }

    /**
     * Carves an open circular arena with outward-flaring geode walls and a wide arched doorway
     * so the boss has plenty of unobstructed movement and jump headroom.
     */
    private static void carveBossArenaAndEntrance(WorldGenLevel level, BlockPos base,
                                                  CrimsonCrystalGeometry.SafeBounds bounds,
                                                  Direction entranceDir) {
        Direction across = entranceDir.getClockWise();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // Circular geode chamber widening upward along the outward-tilted crystal walls.
        for (int dy = 1; dy <= 24; dy++) {
            int y = base.getY() + dy;
            if (!AbyssWorldgen.isAbyssY(y)) break;
            double clearRadius = 9.6 + dy * 0.24;
            int bound = Mth.ceil(clearRadius);

            for (int dx = -bound; dx <= bound; dx++) {
                for (int dz = -bound; dz <= bound; dz++) {
                    if (dx * dx + dz * dz > clearRadius * clearRadius) continue;
                    int wx = base.getX() + dx;
                    int wz = base.getZ() + dz;
                    if (!bounds.contains(wx, wz)) continue;
                    pos.set(wx, y, wz);
                    carveIfAllowed(level, pos);
                }
            }
        }

        // Grand arched entrance corridor through the front crystal gap.
        for (int forwardDist = 4; forwardDist <= 20; forwardDist++) {
            for (int side = -5; side <= 5; side++) {
                int archHeight = 13 - (side * side) / 3;
                for (int dy = 1; dy <= archHeight; dy++) {
                    pos.set(base)
                            .move(entranceDir, forwardDist)
                            .move(across, side)
                            .move(Direction.UP, dy);
                    if (!bounds.contains(pos.getX(), pos.getZ()) || !AbyssWorldgen.isAbyssY(pos.getY())) continue;
                    carveIfAllowed(level, pos);
                }
            }
        }
    }

    private static void buildArenaFloor(WorldGenLevel level, BlockPos base,
                                        CrimsonCrystalGeometry.SafeBounds bounds,
                                        Direction entranceDir, RandomSource random) {
        Direction across = entranceDir.getClockWise();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int floorY = base.getY();
        int radius = Mth.ceil(ARENA_FLOOR_RADIUS);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > ARENA_FLOOR_RADIUS) continue;

                int wx = base.getX() + dx;
                int wz = base.getZ() + dz;
                if (!bounds.contains(wx, wz)) continue;

                pos.set(wx, floorY, wz);
                BlockState existing = level.getBlockState(pos);
                if (!CrimsonCrystalGeometry.isReplaceableByCrystal(existing)) continue;

                BlockState floorState;
                if (dist > 8.6) {
                    floorState = random.nextFloat() < 0.45F
                            ? AbyssContent.HEAVY_CRIMSON_CORRUPTED_STONE.defaultBlockState()
                            : AbyssContent.ABYSSAL_TILES.defaultBlockState();
                } else if (Math.abs(dist - 5.2) < 0.55 && random.nextFloat() < 0.65F) {
                    floorState = AbyssContent.CRIMSON_CORRUPTED_STONE.defaultBlockState();
                } else {
                    floorState = random.nextFloat() < 0.25F
                            ? AbyssContent.ABYSSAL_TILES.defaultBlockState()
                            : AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState();
                }
                level.setBlock(pos, floorState, 2);
            }
        }

        // Smooth entrance threshold leading into the arena.
        for (int forwardDist = 8; forwardDist <= 16; forwardDist++) {
            for (int side = -4; side <= 4; side++) {
                pos.set(base).move(entranceDir, forwardDist).move(across, side);
                if (!bounds.contains(pos.getX(), pos.getZ())) continue;
                BlockState existing = level.getBlockState(pos);
                if (CrimsonCrystalGeometry.isNaturalRock(existing) || CrimsonCrystalGeometry.isCrystalBlock(existing)) {
                    BlockState pathState = Math.abs(side) >= 3
                            ? AbyssContent.HEAVY_CRIMSON_CORRUPTED_STONE.defaultBlockState()
                            : AbyssContent.ABYSSAL_TILES.defaultBlockState();
                    level.setBlock(pos, pathState, 2);
                }
            }
        }
    }

    private static void carveIfAllowed(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (CrimsonCrystalGeometry.isReplaceableByCrystal(state)) {
            level.setBlock(pos, Blocks.CAVE_AIR.defaultBlockState(), 2);
        }
    }

    private static double maxRadialClearance(BlockPos base, CrimsonCrystalGeometry.SafeBounds bounds, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double limitX = cos > 0.05 ? (bounds.maxX() - base.getX()) / cos
                : (cos < -0.05 ? (base.getX() - bounds.minX()) / -cos : 32.0);
        double limitZ = sin > 0.05 ? (bounds.maxZ() - base.getZ()) / sin
                : (sin < -0.05 ? (base.getZ() - bounds.minZ()) / -sin : 32.0);
        return Math.min(limitX, limitZ);
    }

    private static double angularDistance(double a, double b) {
        double diff = Math.abs(a - b) % (Math.PI * 2.0);
        return Math.min(diff, Math.PI * 2.0 - diff);
    }

    private record GroundAnchor(int x, int y, int z) {}
}

