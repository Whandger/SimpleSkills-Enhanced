package com.github.ob_yekt.simpleskills.abyss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared 3D hexagonal crystal prism rasterizer and chunk-safe bounds helper.
 * Produces faceted mineral blades with an authentic 6-sided shaft and sharp
 * pyramidal termination rather than rounded tubes or 1-block needles.
 */
final class CrimsonCrystalGeometry {
    private static final Direction[] ALL_DIRECTIONS = Direction.values();

    private CrimsonCrystalGeometry() {}

    record SafeBounds(int minX, int maxX, int minZ, int maxZ) {
        static SafeBounds fromOrigin(BlockPos origin) {
            int chunkX = origin.getX() >> 4;
            int chunkZ = origin.getZ() >> 4;
            return new SafeBounds(
                    (chunkX - 1) * 16 + 1,
                    (chunkX + 2) * 16 - 2,
                    (chunkZ - 1) * 16 + 1,
                    (chunkZ + 2) * 16 - 2
            );
        }

        boolean contains(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }

        int clearance(int x, int z, Direction dir) {
            return switch (dir) {
                case WEST -> x - minX;
                case EAST -> maxX - x;
                case NORTH -> z - minZ;
                case SOUTH -> maxZ - z;
                default -> 16;
            };
        }
    }

    /**
     * Places a tilted 3D hexagonal crystal prism along direction (dirX, dirY, dirZ).
     *
     * @param tipStartFraction Fraction of total length where the pyramidal point begins (e.g. 0.70)
     * @param collarDepth      Vertical thickness of the Heavy Crimson Corrupted Stone root collar (0 = none)
     * @param budChance        Probability per exposed shaft face block to sprout a crystal bud/cluster
     */
    static boolean placeHexPrism(
            WorldGenLevel level,
            SafeBounds bounds,
            double originX,
            double originY,
            double originZ,
            double dirX,
            double dirY,
            double dirZ,
            double length,
            double baseRadius,
            double tipStartFraction,
            double rollAngle,
            int collarDepth,
            float budChance,
            RandomSource random
    ) {
        double len = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (len < 1.0E-5 || length < 2.0 || baseRadius < 0.6) return false;

        double ux = dirX / len;
        double uy = dirY / len;
        double uz = dirZ / len;

        // Orthonormal basis (v, w) perpendicular to the prism growth axis u.
        double refX = Math.abs(uy) < 0.90 ? 0.0 : 1.0;
        double refY = Math.abs(uy) < 0.90 ? 1.0 : 0.0;
        double refZ = 0.0;

        double vx = uy * refZ - uz * refY;
        double vy = uz * refX - ux * refZ;
        double vz = ux * refY - uy * refX;
        double vLen = Math.sqrt(vx * vx + vy * vy + vz * vz);
        vx /= vLen;
        vy /= vLen;
        vz /= vLen;

        double wx = uy * vz - uz * vy;
        double wy = uz * vx - ux * vz;
        double wz = ux * vy - uy * vx;

        // Subtle asymmetry across the 3 pairs of opposite hexagonal faces.
        double[] cosA = new double[3];
        double[] sinA = new double[3];
        double[] faceScale = new double[3];
        for (int k = 0; k < 3; k++) {
            double theta = rollAngle + k * (Math.PI / 3.0);
            cosA[k] = Math.cos(theta);
            sinA[k] = Math.sin(theta);
            faceScale[k] = 0.88 + random.nextDouble() * 0.24;
        }

        double maxRadius = baseRadius * 1.25 + (collarDepth > 0 ? 1.8 : 0.5);
        double endX = originX + ux * length;
        double endY = originY + uy * length;
        double endZ = originZ + uz * length;

        int minX = Math.max(bounds.minX(), Mth.floor(Math.min(originX, endX) - maxRadius));
        int maxX = Math.min(bounds.maxX(), Mth.ceil(Math.max(originX, endX) + maxRadius));
        int minY = Math.max(AbyssWorldgen.MIN_Y + 1, Mth.floor(Math.min(originY, endY) - maxRadius));
        int maxY = Math.min(AbyssWorldgen.MAX_Y - 1, Mth.ceil(Math.max(originY, endY) + maxRadius));
        int minZ = Math.max(bounds.minZ(), Mth.floor(Math.min(originZ, endZ) - maxRadius));
        int maxZ = Math.min(bounds.maxZ(), Mth.ceil(Math.max(originZ, endZ) + maxRadius));

        long seed = level.getSeed();
        boolean placedAny = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            double dx = (x + 0.5) - originX;
            for (int y = minY; y <= maxY; y++) {
                double dy = (y + 0.5) - originY;
                for (int z = minZ; z <= maxZ; z++) {
                    double dz = (z + 0.5) - originZ;

                    double t = dx * ux + dy * uy + dz * uz;
                    if (t < -2.5 || t > length + 0.5) continue;

                    double progress = t / length;
                    double radius;
                    if (progress <= 0.0) {
                        radius = baseRadius * (1.0 + Math.min(0.28, Math.abs(progress) * 0.25));
                    } else if (progress < tipStartFraction) {
                        radius = baseRadius * (1.0 - 0.20 * (progress / tipStartFraction));
                    } else {
                        double tipProgress = (progress - tipStartFraction) / (1.0 - tipStartFraction);
                        radius = baseRadius * 0.80 * Math.max(0.0, 1.0 - tipProgress);
                    }

                    if (radius <= 0.08) continue;

                    double pv = dx * vx + dy * vy + dz * vz;
                    double pw = dx * wx + dy * wy + dz * wz;

                    boolean insideCrystal = true;
                    for (int k = 0; k < 3; k++) {
                        double dFace = Math.abs(pv * cosA[k] + pw * sinA[k]);
                        if (dFace > radius * faceScale[k]) {
                            insideCrystal = false;
                            break;
                        }
                    }

                    cursor.set(x, y, z);
                    if (!AbyssWorldgen.isCrimsonCrystalBiome(x, z, seed)) continue;
                    BlockState existing = level.getBlockState(cursor);

                    if (insideCrystal) {
                        if (isReplaceableByCrystal(existing)) {
                            level.setBlock(cursor, AbyssContent.RED_CRYSTAL_BLOCK.defaultBlockState(), 2);
                            placedAny = true;

                            if (budChance > 0.0F
                                    && progress > 0.14
                                    && progress < tipStartFraction + 0.08
                                    && random.nextFloat() < budChance) {
                                tryAttachExteriorBud(level, bounds, cursor, random);
                            }
                        }
                    } else if (collarDepth > 0 && progress >= -0.05 && progress <= 0.22
                            && Math.abs(y - originY) <= collarDepth) {
                        boolean insideCollar = true;
                        for (int k = 0; k < 3; k++) {
                            double dFace = Math.abs(pv * cosA[k] + pw * sinA[k]);
                            if (dFace > (radius + 1.45) * faceScale[k]) {
                                insideCollar = false;
                                break;
                            }
                        }
                        if (insideCollar && isNaturalRock(existing)) {
                            level.setBlock(cursor, AbyssContent.HEAVY_CRIMSON_CORRUPTED_STONE.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }

        // Rasterize a continuous central spine up to 96% of length so sharp crystal tips never detach.
        for (double t = 0.0; t <= length * 0.96; t += 0.45) {
            int sx = Mth.floor(originX + ux * t);
            int sy = Mth.floor(originY + uy * t);
            int sz = Mth.floor(originZ + uz * t);
            if (!bounds.contains(sx, sz) || !AbyssWorldgen.isAbyssY(sy)) continue;
            if (!AbyssWorldgen.isCrimsonCrystalBiome(sx, sz, seed)) continue;
            cursor.set(sx, sy, sz);
            if (isReplaceableByCrystal(level.getBlockState(cursor))) {
                level.setBlock(cursor, AbyssContent.RED_CRYSTAL_BLOCK.defaultBlockState(), 2);
                placedAny = true;
            }
        }

        return placedAny;
    }

    static void tryAttachExteriorBud(WorldGenLevel level, SafeBounds bounds, BlockPos support, RandomSource random) {
        BlockState supportState = level.getBlockState(support);
        if (!isCrystalBlock(supportState)) return;

        // A budding block is a central socket: every unoccupied, safe face receives a visible crystal.
        // Faces already touching any block are deliberately left bare.
        List<Direction> exposedFaces = new ArrayList<>(ALL_DIRECTIONS.length);
        for (Direction face : ALL_DIRECTIONS) {
            BlockPos target = support.relative(face);
            if (!bounds.contains(target.getX(), target.getZ())
                    || !AbyssWorldgen.isAbyssY(target.getY())
                    || !level.getBlockState(target).isAir()) {
                continue;
            }
            exposedFaces.add(face);
        }
        if (exposedFaces.isEmpty()) return;

        if (!supportState.is(AbyssContent.BUDDING_RED_CRYSTAL_BLOCK)) {
            level.setBlock(support, AbyssContent.BUDDING_RED_CRYSTAL_BLOCK.defaultBlockState(), 2);
        }

        for (Direction face : exposedFaces) {
            BlockPos target = support.relative(face);
            // Re-check because a prior placement or adjacent feature may have occupied this face.
            if (!level.getBlockState(target).isAir()) continue;

            float roll = random.nextFloat();
            BlockState crystal;
            if (roll < 0.18F) crystal = AbyssContent.RED_CRYSTAL_CLUSTER.defaultBlockState();
            else if (roll < 0.48F) crystal = AbyssContent.LARGE_RED_CRYSTAL_BUD.defaultBlockState();
            else if (roll < 0.78F) crystal = AbyssContent.MEDIUM_RED_CRYSTAL_BUD.defaultBlockState();
            else crystal = AbyssContent.SMALL_RED_CRYSTAL_BUD.defaultBlockState();

            level.setBlock(target, crystal.setValue(AmethystClusterBlock.FACING, face), 2);
        }
    }

    /**
     * Rechecks every budding block in a finished feature after carving is complete.
     * This repairs faces whose attached bud was removed when the arena or entrance was carved.
     */
    static void ensureBuddingFaces(WorldGenLevel level, SafeBounds bounds, BlockPos center,
                                   int horizontalRadius, int minY, int maxY, RandomSource random) {
        int minX = Math.max(bounds.minX(), center.getX() - horizontalRadius);
        int maxX = Math.min(bounds.maxX(), center.getX() + horizontalRadius);
        int minZ = Math.max(bounds.minZ(), center.getZ() - horizontalRadius);
        int maxZ = Math.min(bounds.maxZ(), center.getZ() + horizontalRadius);
        minY = Math.max(AbyssWorldgen.MIN_Y + 1, minY);
        maxY = Math.min(AbyssWorldgen.MAX_Y - 1, maxY);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    if (level.getBlockState(cursor).is(AbyssContent.BUDDING_RED_CRYSTAL_BLOCK)) {
                        tryAttachExteriorBud(level, bounds, cursor.immutable(), random);
                    }
                }
            }
        }
    }

    static boolean isCrystalBlock(BlockState state) {
        return state.is(AbyssContent.RED_CRYSTAL_BLOCK)
                || state.is(AbyssContent.BUDDING_RED_CRYSTAL_BLOCK);
    }

    static boolean isReplaceableByCrystal(BlockState state) {
        return state.isAir()
                || isCrystalBlock(state)
                || state.is(AbyssContent.RED_CRYSTAL_CLUSTER)
                || state.is(AbyssContent.LARGE_RED_CRYSTAL_BUD)
                || state.is(AbyssContent.MEDIUM_RED_CRYSTAL_BUD)
                || state.is(AbyssContent.SMALL_RED_CRYSTAL_BUD)
                || isNaturalRock(state);
    }

    static boolean isNaturalRock(BlockState state) {
        return state.is(AbyssContent.ABYSSAL_STONE)
                || state.is(AbyssContent.CRACKED_ABYSSAL_STONE)
                || state.is(AbyssContent.ABYSSAL_PILLAR)
                || state.is(AbyssContent.POLISHED_ABYSSAL_STONE)
                || state.is(AbyssContent.ABYSSAL_TILES)
                || state.is(AbyssContent.CRIMSON_CORRUPTED_STONE)
                || state.is(AbyssContent.HEAVY_CRIMSON_CORRUPTED_STONE)
                || state.is(AbyssContent.AMETHYST_CORRUPTED_STONE)
                || state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.COBBLED_DEEPSLATE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.BLACKSTONE);
    }
}

