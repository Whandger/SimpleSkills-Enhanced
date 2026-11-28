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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates rare, leafless fossilized trees in The Abyss.
 *
 * Regular specimens have a twisted trunk, exposed roots and several broken limbs.
 * Ancient specimens are taller, broader at the base, fork into multiple leaders and
 * carry a much more elaborate crown. Every log is made from the stone-hard Abyssal timber.
 */
public record AbyssalDeadTreeFeature(int minHeight, int maxHeight, int branchChance) implements Feature {
    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    public static final MapCodec<AbyssalDeadTreeFeature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(4, 18).fieldOf("min_height").forGetter(AbyssalDeadTreeFeature::minHeight),
            Codec.intRange(4, 22).fieldOf("max_height").forGetter(AbyssalDeadTreeFeature::maxHeight),
            Codec.intRange(0, 100).fieldOf("branch_chance").forGetter(AbyssalDeadTreeFeature::branchChance)
    ).apply(instance, AbyssalDeadTreeFeature::new));

    public static void registerType() {
        Registry.register(BuiltInRegistries.FEATURE_TYPE,
                Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "abyssal_dead_tree"), CODEC);
    }

    @Override
    public MapCodec<? extends Feature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        if (minHeight > maxHeight || !AbyssWorldgen.isAbyssY(origin.getY())) return false;

        BlockPos ground = origin.below();
        BlockState support = level.getBlockState(ground);
        if (!support.isFaceSturdy(level, ground, Direction.UP) || !isNaturalAbyssStone(support)) {
            return false;
        }

        int height = minHeight + random.nextInt(maxHeight - minHeight + 1);
        boolean ancient = height >= 16;
        int crownHeadroom = ancient ? 12 : 10;
        if (origin.getY() + height + crownHeadroom >= AbyssWorldgen.MAX_Y) return false;

        // Side roots and limbs are allowed to stop against terrain, but every central trunk
        // segment must be clear before any block is written.
        Map<BlockPos, BlockState> plannedLogs = new LinkedHashMap<>();
        BlockState verticalLog = timber(Direction.Axis.Y);

        // The trunk bends slowly as it rises, creating a fossilized, wind-twisted silhouette.
        int[] trunkX = new int[height];
        int[] trunkZ = new int[height];
        int offsetX = 0;
        int offsetZ = 0;
        for (int y = 0; y < height; y++) {
            if (y >= 3 && y % 4 == 0 && random.nextInt(100) < (ancient ? 48 : 34)) {
                Direction bend = HORIZONTAL[random.nextInt(HORIZONTAL.length)];
                offsetX += bend.getStepX();
                offsetZ += bend.getStepZ();
                offsetX = Math.max(-2, Math.min(2, offsetX));
                offsetZ = Math.max(-2, Math.min(2, offsetZ));
            }
            trunkX[y] = offsetX;
            trunkZ[y] = offsetZ;
            BlockPos trunkPos = origin.offset(offsetX, y, offsetZ);
            if (!level.getBlockState(trunkPos).isAir()) return false;
            addLog(plannedLogs, level, trunkPos, verticalLog);

            // Low trunk flare: four connected ribs make the tree look rooted rather than like a pole.
            if (y <= (ancient ? 3 : 2)) {
                for (Direction direction : HORIZONTAL) {
                    BlockPos rib = trunkPos.relative(direction);
                    addLog(plannedLogs, level, rib, timber(direction.getAxis()));
                    if (ancient && y <= 1) {
                        BlockPos cornerRib = rib.relative(direction.getClockWise());
                        addLog(plannedLogs, level, cornerRib, timber(direction.getAxis()));
                    }
                }
            }
        }

        // Exposed, uneven roots spread over the stone floor. Ancient trees have much wider roots.
        buildRoots(plannedLogs, level, origin, ancient, random);

        // Large primary branches are guaranteed; branch_chance controls the smaller broken twigs.
        int primaryBranches = ancient ? 7 + random.nextInt(2) : 4 + random.nextInt(2);
        for (int branch = 0; branch < primaryBranches; branch++) {
            int usable = Math.max(2, height - 4);
            int branchY = Math.max(2, Math.min(height - 3,
                    2 + (int) Math.round((branch + 0.45) * usable / primaryBranches)));
            Direction direction = HORIZONTAL[(branch + random.nextInt(4)) % HORIZONTAL.length];
            int length = ancient ? 5 + random.nextInt(4) : 3 + random.nextInt(4);
            BlockPos branchBase = origin.offset(trunkX[branchY], branchY, trunkZ[branchY]);
            buildMainBranch(plannedLogs, level, branchBase, direction, length, ancient, random);
        }

        // Short snapped stubs and downward hooks break up the otherwise radial branch pattern.
        int brokenStubs = ancient ? 9 : 5;
        for (int i = 0; i < brokenStubs; i++) {
            if (random.nextInt(100) >= branchChance) continue;
            int y = 2 + random.nextInt(Math.max(1, height - 4));
            Direction direction = HORIZONTAL[random.nextInt(HORIZONTAL.length)];
            BlockPos start = origin.offset(trunkX[y], y, trunkZ[y]);
            int length = 1 + random.nextInt(3);
            boolean droop = random.nextInt(100) < 42;
            for (int step = 1; step <= length; step++) {
                BlockPos stub = start.relative(direction, step);
                if (droop && step > 1) stub = stub.below();
                addLog(plannedLogs, level, stub, timber(direction.getAxis()));
            }
        }

        // Broken crown: several narrow leaders split away from the old central trunk.
        buildBrokenCrown(plannedLogs, level, origin, trunkX, trunkZ, height, ancient, random);

        // Place only into air. Natural rock is never overwritten by roots, limbs or crown tips.
        int placed = 0;
        for (Map.Entry<BlockPos, BlockState> entry : plannedLogs.entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.getY() < AbyssWorldgen.MIN_Y || pos.getY() >= AbyssWorldgen.MAX_Y) continue;
            if (level.getBlockState(pos).isAir()) {
                level.setBlock(pos, entry.getValue(), 2);
                placed++;
            }
        }
        return placed >= height;
    }

    private static void buildRoots(Map<BlockPos, BlockState> planned, WorldGenLevel level,
                                   BlockPos origin, boolean ancient, RandomSource random) {
        int mainRootLength = ancient ? 4 + random.nextInt(4) : 2 + random.nextInt(3);
        for (Direction direction : HORIZONTAL) {
            int length = Math.max(2, mainRootLength + random.nextInt(3) - 1);
            BlockPos cursor = origin;
            for (int step = 1; step <= length; step++) {
                cursor = cursor.relative(direction);
                addLog(planned, level, cursor, timber(direction.getAxis()));
                if (step == 2 && ancient && random.nextBoolean()) {
                    addLog(planned, level, cursor.above(), timber(Direction.Axis.Y));
                }
            }

            // Each major root fractures into one short side root near its end.
            if (random.nextInt(100) < (ancient ? 88 : 58)) {
                Direction fork = random.nextBoolean() ? direction.getClockWise() : direction.getCounterClockWise();
                BlockPos forkStart = cursor;
                int forkLength = 1 + random.nextInt(3);
                for (int step = 1; step <= forkLength; step++) {
                    forkStart = forkStart.relative(fork);
                    addLog(planned, level, forkStart, timber(fork.getAxis()));
                }
            }
        }
    }

    private static void buildMainBranch(Map<BlockPos, BlockState> planned, WorldGenLevel level,
                                        BlockPos base, Direction direction, int length,
                                        boolean ancient, RandomSource random) {
        BlockPos cursor = base;
        for (int step = 1; step <= length; step++) {
            cursor = cursor.relative(direction);
            addLog(planned, level, cursor, timber(direction.getAxis()));

            // Limbs climb in crooked steps, then occasionally dip at their broken ends.
            if (step >= 2 && (step % 2 == 0 || (ancient && random.nextInt(3) == 0))) {
                cursor = cursor.above();
                addLog(planned, level, cursor, timber(Direction.Axis.Y));
            } else if (step == length && random.nextBoolean()) {
                cursor = cursor.below();
                addLog(planned, level, cursor, timber(Direction.Axis.Y));
            }
        }

        // A split tip gives the limb a recognizable dead-tree fork.
        Direction forkA = direction.getClockWise();
        Direction forkB = direction.getCounterClockWise();
        int forkLength = ancient ? 2 + random.nextInt(3) : 1 + random.nextInt(2);
        buildFork(planned, level, cursor, forkA, forkLength, random);
        if (ancient || random.nextBoolean()) {
            buildFork(planned, level, cursor, forkB, Math.max(1, forkLength - random.nextInt(2)), random);
        }
        if (random.nextInt(100) < (ancient ? 80 : 52)) {
            BlockPos snappedTip = cursor.above(1 + random.nextInt(3));
            addLog(planned, level, snappedTip, timber(Direction.Axis.Y));
        }
    }

    private static void buildFork(Map<BlockPos, BlockState> planned, WorldGenLevel level,
                                  BlockPos start, Direction direction, int length, RandomSource random) {
        BlockPos cursor = start;
        for (int step = 1; step <= length; step++) {
            cursor = cursor.relative(direction);
            addLog(planned, level, cursor, timber(direction.getAxis()));
            if (step == 1 || random.nextBoolean()) {
                cursor = cursor.above();
                addLog(planned, level, cursor, timber(Direction.Axis.Y));
            }
        }
    }

    private static void buildBrokenCrown(Map<BlockPos, BlockState> planned, WorldGenLevel level,
                                         BlockPos origin, int[] trunkX, int[] trunkZ, int height,
                                         boolean ancient, RandomSource random) {
        BlockPos crownBase = origin.offset(trunkX[height - 1], height - 1, trunkZ[height - 1]);
        int leaders = ancient ? 4 : 2;
        for (int i = 0; i < leaders; i++) {
            Direction direction = HORIZONTAL[(i * 2 + random.nextInt(4)) % HORIZONTAL.length];
            BlockPos cursor = crownBase;
            int reach = 1 + random.nextInt(ancient ? 4 : 3);
            for (int step = 1; step <= reach; step++) {
                cursor = cursor.relative(direction);
                addLog(planned, level, cursor, timber(direction.getAxis()));
                if (step % 2 == 0 || random.nextBoolean()) {
                    cursor = cursor.above();
                    addLog(planned, level, cursor, timber(Direction.Axis.Y));
                }
            }
            int spear = 2 + random.nextInt(ancient ? 5 : 3);
            for (int h = 1; h <= spear; h++) {
                BlockPos tip = cursor.above(h);
                addLog(planned, level, tip, timber(Direction.Axis.Y));
            }
        }

        // A few isolated splinters remain around the crown, like snapped limbs frozen in place.
        int splinters = ancient ? 5 : 2;
        for (int i = 0; i < splinters; i++) {
            int y = Math.max(2, height - 2 - random.nextInt(Math.min(5, height - 2)));
            Direction direction = HORIZONTAL[random.nextInt(HORIZONTAL.length)];
            BlockPos point = origin.offset(trunkX[y], y, trunkZ[y]).relative(direction, 1 + random.nextInt(2));
            addLog(planned, level, point, timber(direction.getAxis()));
            if (random.nextBoolean()) {
                addLog(planned, level, point.above(), timber(Direction.Axis.Y));
            }
        }
    }

    private static void addLog(Map<BlockPos, BlockState> planned, WorldGenLevel level,
                               BlockPos pos, BlockState state) {
        if (pos.getY() < AbyssWorldgen.MIN_Y || pos.getY() >= AbyssWorldgen.MAX_Y) return;
        if (planned.containsKey(pos) || level.getBlockState(pos).isAir()) {
            planned.putIfAbsent(pos.immutable(), state);
        }
    }

    private static BlockState timber(Direction.Axis axis) {
        return AbyssContent.ABYSSAL_CHARRED_TIMBER.defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, axis);
    }

    private static boolean isNaturalAbyssStone(BlockState state) {
        return state.is(AbyssContent.ABYSSAL_STONE)
                || state.is(AbyssContent.CRACKED_ABYSSAL_STONE)
                || (state.is(AbyssContent.CRIMSON_CORRUPTED_STONE) || state.is(AbyssContent.HEAVY_CRIMSON_CORRUPTED_STONE) || state.is(AbyssContent.AMETHYST_CORRUPTED_STONE))
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.COBBLED_DEEPSLATE)
                || state.is(Blocks.STONE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.BLACKSTONE);
    }
}
