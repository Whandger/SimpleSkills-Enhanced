package com.github.ob_yekt.simpleskills.abyss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Generates organic, tapering, branching crimson-corruption veins creeping across the
 * cavern floor from crystal formations.
 *
 * Heavy corruption (level II) forms thick cores near the crystal roots and occasionally
 * sprouts small crystal buds; normal corruption (level I) forms tapering borders and
 * branching tendrils extending outward.
 */
public final class CrimsonCorruptionVeins {
    private CrimsonCorruptionVeins() {}

    /** Backward-compatible overload for callers without explicit chunk bounds. */
    public static void generate(WorldGenLevel level, BlockPos crystalRoot,
                                RandomSource random, int maximumRadius, int mainVeins) {
        if (crystalRoot == null) return;
        CrimsonCrystalGeometry.SafeBounds bounds = CrimsonCrystalGeometry.SafeBounds.fromOrigin(crystalRoot);
        generate(level, bounds, crystalRoot, random, 2, maximumRadius, mainVeins);
    }

    static void generate(WorldGenLevel level,
                         CrimsonCrystalGeometry.SafeBounds bounds,
                         BlockPos crystalRoot,
                         RandomSource random,
                         int innerStartRadius,
                         int maximumRadius,
                         int mainVeins) {
        if (level == null || crystalRoot == null || random == null
                || !AbyssWorldgen.isOverworld(level)
                || !AbyssWorldgen.isAbyssY(crystalRoot.getY())
                || !AbyssWorldgen.isCrimsonCrystalBiome(
                        crystalRoot.getX(), crystalRoot.getZ(), level.getSeed())) {
            return;
        }

        int count = Mth.clamp(mainVeins, 1, 14);
        double rotation = random.nextDouble() * Math.PI * 2.0;
        double waveSeed = random.nextDouble() * Math.PI * 2.0;

        // Compact heavy corruption halo hugging the crystal base before the veins branch out.
        int haloOuter = innerStartRadius + 2;
        BlockPos.MutableBlockPos haloPos = new BlockPos.MutableBlockPos();
        for (int dx = -haloOuter; dx <= haloOuter; dx++) {
            for (int dz = -haloOuter; dz <= haloOuter; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist < Math.max(0.0, innerStartRadius - 1.5) || dist > haloOuter) continue;
                if (random.nextFloat() > 0.68F) continue;

                int wx = crystalRoot.getX() + dx;
                int wz = crystalRoot.getZ() + dz;
                if (!bounds.contains(wx, wz)) continue;
                if (!AbyssWorldgen.isCrimsonCrystalBiome(wx, wz, level.getSeed())) continue;

                int groundY = findGroundY(level, wx, wz, crystalRoot.getY(), 5);
                if (groundY == Integer.MIN_VALUE) continue;

                haloPos.set(wx, groundY, wz);
                BlockState state = dist <= innerStartRadius + 0.8
                        ? AbyssContent.HEAVY_CRIMSON_CORRUPTED_STONE.defaultBlockState()
                        : AbyssContent.CRIMSON_CORRUPTED_STONE.defaultBlockState();
                level.setBlock(haloPos, state, 2);
            }
        }

        for (int vein = 0; vein < count; vein++) {
            double baseAngle = rotation + (Math.PI * 2.0 * vein / count)
                    + (random.nextDouble() - 0.5) * 0.42;
            double startDist = Math.max(1.0, innerStartRadius - 0.5 + random.nextDouble() * 1.4);
            double startX = crystalRoot.getX() + 0.5 + Math.cos(baseAngle) * startDist;
            double startZ = crystalRoot.getZ() + 0.5 + Math.sin(baseAngle) * startDist;

            int availableSteps = Math.max(6, maximumRadius - innerStartRadius);
            int length = Mth.nextInt(random, (int) (availableSteps * 0.65), availableSteps);
            double rootWidth = innerStartRadius >= 8 ? 2.6 : 1.9;

            growOrganicVein(
                    level, bounds, random,
                    crystalRoot,
                    startX, crystalRoot.getY(), startZ,
                    baseAngle, length, rootWidth,
                    2, waveSeed + vein * 1.85
            );
        }
    }

    private static void growOrganicVein(
            WorldGenLevel level,
            CrimsonCrystalGeometry.SafeBounds bounds,
            RandomSource random,
            BlockPos rootCenter,
            double x,
            int referenceY,
            double z,
            double angle,
            int length,
            double startWidth,
            int branchDepth,
            double waveSeed
    ) {
        BlockPos.MutableBlockPos floorPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos abovePos = new BlockPos.MutableBlockPos();
        long seed = level.getSeed();
        int currentY = referenceY;
        double currentAngle = angle;

        for (int step = 0; step < length; step++) {
            float progress = (float) step / Math.max(1, length);

            // Smooth continuous steering eliminates grid-aligned straight lines.
            double meander = Math.sin(step * 0.34 + waveSeed) * 0.15;
            double turbulence = (random.nextDouble() - 0.5) * 0.22;
            currentAngle += meander + turbulence;

            x += Math.cos(currentAngle) * 0.72;
            z += Math.sin(currentAngle) * 0.72;

            int bx = Mth.floor(x);
            int bz = Mth.floor(z);
            if (!bounds.contains(bx, bz)) break;
            if (!AbyssWorldgen.isCrimsonCrystalBiome(bx, bz, seed)) break;

            int floorY = findGroundY(level, bx, bz, currentY, 4);
            if (floorY == Integer.MIN_VALUE) break;
            currentY = floorY;

            // Taper smoothly from wide root to a thin 1-block tendril at the tip.
            double radius = Mth.lerp(progress, startWidth * 0.5, 0.32);
            int ceilR = Mth.ceil(radius);

            for (int dx = -ceilR; dx <= ceilR; dx++) {
                for (int dz = -ceilR; dz <= ceilR; dz++) {
                    double distSq = dx * dx + dz * dz;
                    if (distSq > radius * radius) continue;

                    // Faint organic breakup near the very tip of the tendril.
                    if (progress > 0.84f && random.nextFloat() < 0.28F) continue;

                    int px = bx + dx;
                    int pz = bz + dz;
                    if (!bounds.contains(px, pz)) continue;
                    if (!AbyssWorldgen.isCrimsonCrystalBiome(px, pz, seed)) continue;

                    int py = findGroundY(level, px, pz, currentY, 2);
                    if (py == Integer.MIN_VALUE) continue;

                    floorPos.set(px, py, pz);
                    BlockState existing = level.getBlockState(floorPos);
                    if (!isGroundForCorruption(existing)) continue;

                    double distFromCrystal = Math.sqrt(
                            Math.pow(px - rootCenter.getX(), 2.0)
                                    + Math.pow(pz - rootCenter.getZ(), 2.0));

                    // Level II core near the crystal and center of thick roots; Level I along outer edges & tips.
                    boolean isHeavyCore = (progress < 0.36f && distSq <= (radius * 0.65) * (radius * 0.65))
                            || (distFromCrystal <= 15.5 && progress < 0.25f);

                    BlockState corruption = isHeavyCore
                            ? AbyssContent.HEAVY_CRIMSON_CORRUPTED_STONE.defaultBlockState()
                            : AbyssContent.CRIMSON_CORRUPTED_STONE.defaultBlockState();
                    level.setBlock(floorPos, corruption, 2);

                    // Occasional tiny crystal buds sprouting out of heavy corrupted roots.
                    if (isHeavyCore && distSq <= 0.6 && random.nextFloat() < 0.055F) {
                        abovePos.set(px, py + 1, pz);
                        if (AbyssWorldgen.isAbyssY(abovePos.getY()) && level.getBlockState(abovePos).isAir()) {
                            BlockState bud = random.nextFloat() < 0.30F
                                    ? AbyssContent.MEDIUM_RED_CRYSTAL_BUD.defaultBlockState()
                                    : AbyssContent.SMALL_RED_CRYSTAL_BUD.defaultBlockState();
                            level.setBlock(abovePos, bud.setValue(AmethystClusterBlock.FACING, Direction.UP), 2);
                        }
                    }
                }
            }

            // Fork into smaller secondary and tertiary veins.
            if (branchDepth > 0 && step >= 4 && step <= length - 4 && random.nextFloat() < 0.16F) {
                double side = random.nextBoolean() ? 1.0 : -1.0;
                double branchAngle = currentAngle + side * (0.52 + random.nextDouble() * 0.45);
                int branchLength = Math.max(4, (int) ((length - step) * (0.45 + random.nextDouble() * 0.25)));
                double branchWidth = Math.max(0.95, startWidth * (1.0 - progress) * 0.72);

                growOrganicVein(
                        level, bounds, random,
                        rootCenter,
                        x, currentY, z,
                        branchAngle, branchLength, branchWidth,
                        branchDepth - 1, waveSeed + 2.7
                );
            }
        }
    }

    private static int findGroundY(WorldGenLevel level, int x, int z, int aroundY, int verticalRange) {
        BlockPos.MutableBlockPos floor = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy <= verticalRange; dy++) {
            int[] offsets = dy == 0 ? new int[]{0} : new int[]{dy, -dy};
            for (int offset : offsets) {
                int y = aroundY + offset;
                if (!AbyssWorldgen.isAbyssY(y) || !AbyssWorldgen.isAbyssY(y + 1)) continue;
                floor.set(x, y, z);
                above.set(x, y + 1, z);
                BlockState aboveState = level.getBlockState(above);
                if (isGroundForCorruption(level.getBlockState(floor))
                        && (aboveState.isAir() || aboveState.canBeReplaced())) {
                    return y;
                }
            }
        }
        return Integer.MIN_VALUE;
    }

    private static boolean isGroundForCorruption(BlockState state) {
        return state.is(AbyssContent.ABYSSAL_STONE)
                || state.is(AbyssContent.CRACKED_ABYSSAL_STONE)
                || state.is(AbyssContent.ABYSSAL_PILLAR)
                || state.is(AbyssContent.POLISHED_ABYSSAL_STONE)
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

