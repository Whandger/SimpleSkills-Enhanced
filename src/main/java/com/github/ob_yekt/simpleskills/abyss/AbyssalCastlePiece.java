package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Monumental Gothic Abyssal Castle anchored onto a craggy subterranean mountain massif.
 * - Broad mountain massif beneath nearly the full castle footprint, tapering toward the Abyss floor
 * - Anchored deep into solid Abyss bedrock/stone (BELOW_FLOOR_DEPTH = 72, solidDepth >= 4)
 * - Stepped Gothic Spires up to MAX_HEIGHT = 108
 * - Double flying buttresses, sloped tile roofs, South Viaduct (dz = 55..96) with 4 gothic arches
 * - 34 validated chests across 3 themed loot tables
 */
public class AbyssalCastlePiece extends StructurePiece {

    public static final int BELOW_FLOOR_DEPTH = 64;
    public static final int MAX_HEIGHT = 108;

    // Resolve the vanilla chain by registry ID for Minecraft 26.3 mappings.
    private static final BlockState CHAIN = BuiltInRegistries.BLOCK
            .getValue(Identifier.fromNamespaceAndPath("minecraft", "chain")).defaultBlockState();

    private static final ResourceKey<LootTable> LOOT_CASTLE = ResourceKey.create(
            Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "chests/abyssal_castle"));
    private static final ResourceKey<LootTable> LOOT_LIBRARY = ResourceKey.create(
            Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "chests/abyssal_castle_library"));
    private static final ResourceKey<LootTable> LOOT_SANCTUM = ResourceKey.create(
            Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "chests/abyssal_castle_sanctum"));

    public static final StructurePieceType TYPE = (context, tag) -> new AbyssalCastlePiece(tag);

    /** Half-width of the castle's X footprint; its Z footprint is offset to make room for the south viaduct. */
    public static final int HALF = 88;
    public static final int MIN_OFFSET_Z = -72;
    public static final int MAX_OFFSET_Z = 104;

    private final BlockPos center;
    private final long seed;

    public AbyssalCastlePiece(BlockPos center, long seed) {
        super(TYPE, 0, createBoundingBox(center));
        this.center = center;
        this.seed = seed;
    }

    public AbyssalCastlePiece(CompoundTag tag) {
        this(new BlockPos(tag.getIntOr("cx", 0), tag.getIntOr("cy", 0), tag.getIntOr("cz", 0)),
                tag.getLongOr("seed", 0L));
    }

    private static BoundingBox createBoundingBox(BlockPos c) {
        return new BoundingBox(
                c.getX() - HALF, Math.max(-254, c.getY() - BELOW_FLOOR_DEPTH), c.getZ() + MIN_OFFSET_Z,
                c.getX() + HALF, c.getY() + MAX_HEIGHT, c.getZ() + MAX_OFFSET_Z
        );
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("cx", center.getX());
        tag.putInt("cy", center.getY());
        tag.putInt("cz", center.getZ());
        tag.putLong("seed", seed);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        // 1. Craggy Mountain Massif Foundation anchored to solid floor (solidDepth >= 4)
        buildCraggyMountainFoundation(level, random, box);

        // 2. Main Citadel Walls, Courtyard, Keep, Nave, Library & Crypts
        buildCitadelCore(level, random, box);

        // 3. 8 Perimeter & Gatehouse Towers (including 2 partially collapsed towers & spiral stairs)
        buildPerimeterTowers(level, random, box);

        // 4. Twin Nave Towers (dx = +-14, dz = 38, heights 84 and 88) & Double Flying Buttresses
        buildTwinNaveTowersAndButtresses(level, random, box);

        // 5. Sovereign Central Spire (dy = 43..107)
        buildSovereignCentralSpire(level, random, box);

        // 6. Monumental South Viaduct & Suspended Bridge (dz = 55..96) with 4 Gothic Arches
        buildSouthViaduct(level, random, box);

        // 7. Place & Validate All 34 Chests across 3 Loot Tables
        placeAllCastleChests(level, random, box);
    }

    private void buildCraggyMountainFoundation(WorldGenLevel level, RandomSource random, BoundingBox box) {
        int yMinLimit = Math.max(level.getMinY() + 2, -254);

        // Process only columns inside the current chunk. Terrain reads must never
        // cross chunk boundaries while a structure piece is being generated.
        int minDx = Math.max(-HALF, box.minX() - center.getX());
        int maxDx = Math.min(HALF, box.maxX() - center.getX());
        int minDz = Math.max(MIN_OFFSET_Z, box.minZ() - center.getZ());
        int maxDz = Math.min(MAX_OFFSET_Z, box.maxZ() - center.getZ());

        // The upper shoulder follows the actual castle footprint, with only a small
        // overhang. The massif then widens downward to meet the surrounding cave floor.
        // Its center is slightly south of the citadel to account for the southern wing.
        final double foundationCenterZ = 10.0;
        final double foundationRadiusX = 45.0;
        final double foundationRadiusZ = 50.0;

        for (int dx = minDx; dx <= maxDx; dx++) {
            for (int dz = minDz; dz <= maxDz; dz++) {
                int wx = center.getX() + dx;
                int wz = center.getZ() + dz;
                double relativeDz = dz - foundationCenterZ;
                double normDist = Math.hypot(dx / foundationRadiusX, relativeDz / foundationRadiusZ);
                double ridge = ridgeNoise(dx, (int) relativeDz);

                int solidDepth = 0;
                for (int d = 0; d <= BELOW_FLOOR_DEPTH; d++) {
                    int wy = center.getY() - d;
                    if (wy < yMinLimit) {
                        break;
                    }

                    double depthProgress = d / (double) BELOW_FLOOR_DEPTH;
                    // Start just beyond the castle footprint and flare outward as the
                    // foundation descends. Ridge variation keeps the silhouette uneven.
                    double maxRadiusFactor = 1.08 + (depthProgress * 0.85) + ((ridge - 0.5) * 0.10);
                    if (normDist > maxRadiusFactor) {
                        continue;
                    }

                    BlockPos pos = new BlockPos(wx, wy, wz);
                    BlockState existing = level.getBlockState(pos);
                    if (d > 4 && existing.isSolidRender() && !existing.is(Blocks.WATER) && !existing.is(Blocks.LAVA)) {
                        solidDepth++;
                        if (solidDepth >= 4) {
                            break;
                        }
                    } else {
                        solidDepth = 0;
                    }

                    if (d == 0 && normDist <= 1.02) {
                        // Keep the castle's original floor height; the overhanging rim
                        // is rocky rather than creating another broad, flat platform.
                        setSafe(level, box, pos, pickFloorBlock(random, dx, dz));
                    } else {
                        boolean isEdge = normDist > (maxRadiusFactor - 0.13);
                        setSafe(level, box, pos, pickMountainBlock(random, dx, wy, dz, isEdge));
                    }
                }
            }
        }
    }

    private void buildCitadelCore(WorldGenLevel level, RandomSource random, BoundingBox box) {
        // Outer Curtain Walls (dx = -34..34, dz = -40..54, height 18)
        for (int dy = 1; dy <= 18; dy++) {
            for (int dx = -34; dx <= 34; dx++) {
                for (int dz = -40; dz <= 54; dz++) {
                    boolean isWall = (Math.abs(dx) == 34 || dz == -40 || dz == 54);
                    if (!isWall) continue;

                    // South Gatehouse arch opening
                    if (dz == 54 && isInsideGothicArch(dx, dy, 0, 5, 11)) {
                        continue;
                    }

                    BlockPos pos = center.offset(dx, dy, dz);
                    if (dy == 18 && (dx + dz) % 2 == 0) {
                        setSafe(level, box, pos, AbyssContent.ABYSSAL_BRICK_SLAB.defaultBlockState());
                    } else if (dy == 17) {
                        setSafe(level, box, pos, AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState());
                    } else {
                        setSafe(level, box, pos, pickWallMasonry(random, dx, dy, dz));
                    }
                }
            }
        }

        // Grand Nave & Throne Hall (dx = -16..16, dz = -24..36, walls up to dy = 29, sloped tile roof dy = 29..45)
        for (int dy = 1; dy <= 29; dy++) {
            for (int dx = -16; dx <= 16; dx++) {
                for (int dz = -24; dz <= 36; dz++) {
                    boolean isSideWall = Math.abs(dx) == 16;
                    boolean isEndWall = (dz == -24 || dz == 36);
                    if (!isSideWall && !isEndWall) {
                        // Interior floor levels
                        if (dy == 12 && Math.abs(dx) >= 10) {
                            setSafe(level, box, center.offset(dx, dy, dz), AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState());
                        }
                        continue;
                    }

                    // South Nave portal arch
                    if (dz == 36 && isInsideGothicArch(dx, dy, 0, 4, 10)) {
                        continue;
                    }

                    // Stained-glass rose windows at South Portal (dz = 36, dy = 18) and behind Throne (dz = -24, dy = 18)
                    if ((dz == 36 || dz == -24) && Math.hypot(dx, dy - 18) <= 4.2) {
                        BlockState glass = ((Math.abs(dx) + dy) % 2 == 0)
                                ? Blocks.STAINED_GLASS.pick(DyeColor.PURPLE).defaultBlockState()
                                : Blocks.STAINED_GLASS.pick(DyeColor.MAGENTA).defaultBlockState();
                        setSafe(level, box, center.offset(dx, dy, dz), glass);
                        // Backlight rose window with abyssal_brazier
                        int backZ = dz == 36 ? 35 : -23;
                        if (dx == 0 && dy == 18) {
                            setSafe(level, box, center.offset(0, 18, backZ), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
                        }
                        continue;
                    }

                    // Side lancet windows every 6 blocks
                    if (isSideWall && Math.abs(dz % 6) <= 1 && dy >= 6 && dy <= 15) {
                        if (Math.abs(dz % 6) == 0) {
                            setSafe(level, box, center.offset(dx, dy, dz), AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState());
                        } else if (dy == 10) {
                            setSafe(level, box, center.offset(dx, dy, dz), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
                        }
                        continue;
                    }

                    setSafe(level, box, center.offset(dx, dy, dz), pickWallMasonry(random, dx, dy, dz));
                }
            }
        }

        // Sloped Gothic Tile Roof over the Nave (dy = 29..45)
        for (int step = 0; step <= 16; step++) {
            int dy = 29 + step;
            int halfW = 17 - step;
            for (int dz = -25; dz <= 37; dz++) {
                if (halfW > 0) {
                    setSafe(level, box, center.offset(-halfW, dy, dz),
                            AbyssContent.ABYSSAL_TILE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
                    setSafe(level, box, center.offset(halfW, dy, dz),
                            AbyssContent.ABYSSAL_TILE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
                    if (halfW > 1) {
                        setSafe(level, box, center.offset(-halfW + 1, dy, dz), AbyssContent.ABYSSAL_TILES.defaultBlockState());
                        setSafe(level, box, center.offset(halfW - 1, dy, dz), AbyssContent.ABYSSAL_TILES.defaultBlockState());
                    }
                } else {
                    setSafe(level, box, center.offset(0, dy, dz), AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
                }
            }
        }

        // Sovereign Throne Dais (dz = -20..-14)
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -20; dz <= -14; dz++) {
                setSafe(level, box, center.offset(dx, 1, dz), AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState());
                if (Math.abs(dx) <= 2 && dz <= -16) {
                    setSafe(level, box, center.offset(dx, 2, dz), AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
                }
            }
        }
        setSafe(level, box, center.offset(0, 3, -19), AbyssContent.CHISELED_ABYSSAL_STONE.defaultBlockState());
        setSafe(level, box, center.offset(-3, 2, -16), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
        setSafe(level, box, center.offset(3, 2, -16), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());

        // West 2-Story Library & East Ruined Scriptorium Wings
        buildSideWings(level, random, box);
    }

    private void buildSideWings(WorldGenLevel level, RandomSource random, BoundingBox box) {
        // West Library Wing (dx = -30..-17, dz = -14..22, roof dy = 32..38)
        for (int dy = 1; dy <= 32; dy++) {
            for (int dx = -30; dx <= -17; dx++) {
                for (int dz = -14; dz <= 22; dz++) {
                    boolean wall = (dx == -30 || dz == -14 || dz == 22);
                    if (wall) {
                        setSafe(level, box, center.offset(dx, dy, dz), pickWallMasonry(random, dx, dy, dz));
                    } else if (dy == 16) {
                        setSafe(level, box, center.offset(dx, dy, dz), AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState());
                    } else if ((dy == 2 || dy == 17) && (dx == -28 || dx == -24) && dz % 3 != 0) {
                        setSafe(level, box, center.offset(dx, dy, dz), Blocks.BOOKSHELF.defaultBlockState());
                    }
                }
            }
        }
        for (int step = 0; step <= 6; step++) {
            int dy = 32 + step;
            for (int dz = -14; dz <= 22; dz++) {
                setSafe(level, box, center.offset(-30 + step, dy, dz),
                        AbyssContent.ABYSSAL_TILE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
                setSafe(level, box, center.offset(-17 - step, dy, dz),
                        AbyssContent.ABYSSAL_TILE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
            }
        }

        // East Wing (partially ruined scriptorium, dx = 17..30, dz = -14..22)
        for (int dy = 1; dy <= 24; dy++) {
            for (int dx = 17; dx <= 30; dx++) {
                for (int dz = -14; dz <= 22; dz++) {
                    boolean wall = (dx == 30 || dz == -14 || dz == 22);
                    if (!wall) continue;
                    // Ruined jagged top above dy = 15
                    if (dy > 15 && ((dx * 7 + dz * 13) % 5 < (dy - 15))) {
                        continue;
                    }
                    BlockState state = (dy > 14 && random.nextFloat() < 0.45f)
                            ? AbyssContent.CRACKED_ABYSSAL_BRICKS.defaultBlockState()
                            : pickWallMasonry(random, dx, dy, dz);
                    setSafe(level, box, center.offset(dx, dy, dz), state);
                }
            }
        }
    }

    private void buildPerimeterTowers(WorldGenLevel level, RandomSource random, BoundingBox box) {
        int[][] towers = {
                {-34, -40, 46, 0}, // NW Corner
                { 34, -40, 44, 0}, // NE Corner
                {-34,  54, 42, 0}, // SW Corner
                { 34,  54, 34, 1}, // SE Corner (partially collapsed)
                {-10,  54, 48, 0}, // South Gatehouse West
                { 10,  54, 48, 0}, // South Gatehouse East
                {-34,   8, 38, 0}, // West Mid Bastion
                { 34,   8, 28, 1}  // East Mid Bastion (partially collapsed)
        };

        for (int[] t : towers) {
            buildOctagonalTower(level, random, box, t[0], t[1], 5, t[2], t[3] == 1);
        }
    }

    private void buildOctagonalTower(WorldGenLevel level, RandomSource random, BoundingBox box,
                                     int cx, int cz, int radius, int height, boolean ruined) {
        for (int dy = 1; dy <= height; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int distSq = dx * dx + dz * dz;
                    if (distSq > radius * radius + 2) continue;

                    if (ruined && dy > height - 8 && ((dx * 11 + dz * 17 + dy) % 7 < (dy - (height - 8)))) {
                        continue;
                    }

                    boolean outerRing = distSq >= (radius - 1) * (radius - 1);
                    BlockPos pos = center.offset(cx + dx, dy, cz + dz);

                    if (outerRing) {
                        if (!ruined && dy == height) {
                            if ((dx + dz) % 2 == 0) {
                                setSafe(level, box, pos, AbyssContent.ABYSSAL_BRICK_SLAB.defaultBlockState());
                            }
                        } else if (!ruined && dy == height - 1) {
                            setSafe(level, box, pos, AbyssContent.CHISELED_ABYSSAL_STONE.defaultBlockState());
                        } else {
                            setSafe(level, box, pos, pickWallMasonry(random, cx + dx, dy, cz + dz));
                        }
                    } else if (dy % 10 == 0 && !ruined) {
                        setSafe(level, box, pos, AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState());
                    }
                }
            }
            // Central spiral staircase spine
            if (!ruined || dy < height - 6) {
                setSafe(level, box, center.offset(cx, dy, cz), AbyssContent.ABYSSAL_PILLAR.defaultBlockState());
                double angle = (dy * Math.PI) / 4.0;
                int sx = cx + (int) Math.round(Math.cos(angle) * 2.0);
                int sz = cz + (int) Math.round(Math.sin(angle) * 2.0);
                setSafe(level, box, center.offset(sx, dy, sz), AbyssContent.ABYSSAL_BRICK_SLAB.defaultBlockState());
            }
        }
    }

    private void buildTwinNaveTowersAndButtresses(WorldGenLevel level, RandomSource random, BoundingBox box) {
        // Twin Nave Towers at dx = +-14, dz = 38 with asymmetric heights 84 and 88
        buildOctagonalTower(level, random, box, -14, 38, 5, 84, false);
        buildOctagonalTower(level, random, box,  14, 38, 5, 88, false);

        // Lancet windows + internal Abyssal Braziers in Twin Towers
        for (int tx : new int[]{-14, 14}) {
            for (int wy : new int[]{54, 68}) {
                for (int h = 0; h < 6; h++) {
                    setSafe(level, box, center.offset(tx, wy + h, 43), AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState());
                }
                setSafe(level, box, center.offset(tx, wy + 2, 41), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
            }
        }

        // Double Flying Buttresses along the Nave (dz = -16, -4, 8, 20)
        for (int bz : new int[]{-16, -4, 8, 20}) {
            for (int side : new int[]{-1, 1}) {
                // Outer pier at dx = side * 24
                for (int dy = 1; dy <= 64; dy++) {
                    setSafe(level, box, center.offset(side * 24, dy, bz), AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState());
                    setSafe(level, box, center.offset(side * 25, dy, bz), AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
                }
                // Pinnacle on outer pier
                for (int py = 65; py <= 69; py++) {
                    setSafe(level, box, center.offset(side * 24, py, bz), AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState());
                }
                // Lower & Upper flying arch spans
                for (int tierBase : new int[]{16, 44}) {
                    for (int step = 0; step <= 7; step++) {
                        int ax = side * (23 - step);
                        int ay = tierBase + step;
                        setSafe(level, box, center.offset(ax, ay, bz), AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
                        setSafe(level, box, center.offset(ax, ay + 1, bz),
                                AbyssContent.ABYSSAL_BRICK_STAIRS.defaultBlockState()
                                        .setValue(BlockStateProperties.HORIZONTAL_FACING, side < 0 ? Direction.EAST : Direction.WEST));
                    }
                }
            }
        }
    }

    private void buildSovereignCentralSpire(WorldGenLevel level, RandomSource random, BoundingBox box) {
        // Sovereign Central Spire (dy = 43..107) centered at (0, 6)
        int cz = 6;
        // Octagonal Sanctum & Open Belfry (dy = 43..78)
        for (int dy = 43; dy <= 78; dy++) {
            for (int dx = -7; dx <= 7; dx++) {
                for (int dz = -7; dz <= 7; dz++) {
                    int d2 = dx * dx + dz * dz;
                    if (d2 > 52) continue;
                    BlockPos pos = center.offset(dx, dy, cz + dz);

                    if (dy == 43 || dy == 62) {
                        setSafe(level, box, pos, AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
                    } else if (dy >= 63 && dy <= 75) {
                        // Open belfry with 4 abyssal_pillar columns & hanging brazier lantern
                        if (Math.abs(dx) == 4 && Math.abs(dz) == 4) {
                            setSafe(level, box, pos, AbyssContent.ABYSSAL_PILLAR.defaultBlockState());
                        } else if (dx == 0 && dz == 0 && dy >= 69) {
                            setSafe(level, box, pos, dy == 69
                                    ? AbyssContent.ABYSSAL_BRAZIER.defaultBlockState()
                                    : CHAIN);
                        }
                    } else if (d2 >= 36) {
                        setSafe(level, box, pos, pickWallMasonry(random, dx, dy, cz + dz));
                    }
                }
            }
        }

        // Corner pinnacles at dy = 76..84
        for (int px : new int[]{-5, 5}) {
            for (int pz : new int[]{-5, 5}) {
                for (int py = 76; py <= 84; py++) {
                    setSafe(level, box, center.offset(px, py, cz + pz), AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState());
                }
            }
        }

        // Faceted Conical Tile Roof & Gilded/Obsidian Finial (dy = 79..107)
        for (int step = 0; step <= 20; step++) {
            int dy = 79 + step;
            double r = Math.max(0.0, 6.5 - (step * 0.32));
            int rInt = (int) Math.ceil(r);
            for (int dx = -rInt; dx <= rInt; dx++) {
                for (int dz = -rInt; dz <= rInt; dz++) {
                    if (dx * dx + dz * dz <= r * r) {
                        setSafe(level, box, center.offset(dx, dy, cz + dz), AbyssContent.ABYSSAL_TILES.defaultBlockState());
                    }
                }
            }
        }
        for (int dy = 100; dy <= 107; dy++) {
            setSafe(level, box, center.offset(0, dy, cz),
                    dy <= 103 ? AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState());
        }
    }

    private void buildSouthViaduct(WorldGenLevel level, RandomSource random, BoundingBox box) {
        // Monumental South Viaduct (dz = 55..96, dx = -6..6) with 4 Gothic Arch Spans & Piers to Cave Floor
        int yMinLimit = Math.max(level.getMinY() + 2, -254);

        for (int dz = 55; dz <= 96; dz++) {
            boolean isPierZ = (dz == 56 || dz == 66 || dz == 76 || dz == 86 || dz == 95);
            for (int dx = -6; dx <= 6; dx++) {
                // Deck at dy = 0..1
                BlockState deckBlock = Math.abs(dx) <= 1
                        ? AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState()
                        : AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState();
                setSafe(level, box, center.offset(dx, 0, dz), deckBlock);

                // Parapets on edges (dx = +-6) & Brazier Beacons every 6 blocks
                if (Math.abs(dx) == 6) {
                    if (dz % 6 == 0) {
                        setSafe(level, box, center.offset(dx, 1, dz), AbyssContent.CHISELED_ABYSSAL_STONE.defaultBlockState());
                        setSafe(level, box, center.offset(dx, 2, dz), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
                    } else {
                        BlockState parapet = (dz % 2 == 0)
                                ? AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState()
                                : AbyssContent.ABYSSAL_BRICK_SLAB.defaultBlockState();
                        setSafe(level, box, center.offset(dx, 1, dz), parapet);
                    }
                }

                // Extend piers down to mountain flank & cave floor
                if (isPierZ && Math.abs(dx) >= 3) {
                    int solidDepth = 0;
                    for (int d = 1; d <= BELOW_FLOOR_DEPTH; d++) {
                        int wy = center.getY() - d;
                        if (wy < yMinLimit) break;
                        BlockPos pPos = new BlockPos(center.getX() + dx, wy, center.getZ() + dz);
                        // Structure pieces are processed one chunk at a time. Never inspect
                        // terrain outside this chunk's X/Z bounds during world generation.
                        if (!box.isInside(pPos.getX(), center.getY(), pPos.getZ())) {
                            continue;
                        }
                        BlockState existing = level.getBlockState(pPos);
                        if (existing.isSolidRender() && !existing.is(Blocks.WATER) && !existing.is(Blocks.LAVA)) {
                            solidDepth++;
                            if (solidDepth >= 4) break;
                        } else {
                            solidDepth = 0;
                        }
                        setSafe(level, box, pPos, pickWallMasonry(random, dx, -d, dz));
                    }
                } else if (!isPierZ) {
                    // Gothic arch ribs underneath the bridge deck (dy = -1..-5)
                    int archCenterZ = ((dz - 56) / 10) * 10 + 61;
                    int relZ = dz - archCenterZ;
                    for (int d = 1; d <= 5; d++) {
                        if (!isInsideGothicArch(relZ, 6 - d, 0, 4, 5)) {
                            setSafe(level, box, center.offset(dx, -d, dz), AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    private void placeAllCastleChests(WorldGenLevel level, RandomSource random, BoundingBox box) {
        List<ChestSpec> specs = new ArrayList<>();

        // 18 Chests: simpleskills:chests/abyssal_castle (Towers, Guardrooms, Courtyard, Nave, Dungeons)
        int[][] castleCoords = {
                {-34, 1, -40}, {-34, 21, -40}, {34, 1, -40}, {34, 21, -40},
                {-34, 1, 54},  {-34, 21, 54},  {34, 1, 54},  {-10, 1, 54},
                {10, 1, 54},   {-34, 1, 8},    {-34, 21, 8}, {34, 1, 8},
                {-12, 1, 28},  {12, 1, 28},    {-12, 13, 12},{12, 13, 12},
                {-20, -6, -10},{20, -6, 18}
        };
        for (int[] c : castleCoords) {
            specs.add(new ChestSpec(c[0], c[1], c[2], LOOT_CASTLE));
        }

        // 7 Chests: simpleskills:chests/abyssal_castle_library (2-Story Library, Enchanting Rotunda, East Scriptorium, Astral Observatory)
        int[][] libraryCoords = {
                {-26, 1, -8}, {-22, 1, 14}, {-26, 17, -4}, {-22, 17, 16},
                {-14, 41, 38}, {24, 1, 4},  {14, 61, 38}
        };
        for (int[] c : libraryCoords) {
            specs.add(new ChestSpec(c[0], c[1], c[2], LOOT_LIBRARY));
        }

        // 9 Chests: simpleskills:chests/abyssal_castle_sanctum (Throne Room, Relic Crypt, High Tower Treasury, Sovereign Spire Sanctum, Secret Forge Vault, Catacomb)
        int[][] sanctumCoords = {
                {-3, 2, -19}, {3, 2, -19}, {0, -8, -18}, {-14, 71, 38},
                {14, 71, 38}, {-2, 44, 6}, {2, 44, 6},   {0, 63, 6},
                {0, -12, 24}
        };
        for (int[] c : sanctumCoords) {
            specs.add(new ChestSpec(c[0], c[1], c[2], LOOT_SANCTUM));
        }

        for (ChestSpec spec : specs) {
            BlockPos pos = center.offset(spec.dx, spec.dy, spec.dz);
            if (!box.isInside(pos)) continue;

            // Ensure solid floor beneath chest & clear air at chest position
            BlockPos below = pos.below();
            if (!level.getBlockState(below).isSolidRender()) {
                level.setBlock(below, AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState(), 2);
            }
            level.setBlock(pos, Blocks.CHEST.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), 2);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RandomizableContainerBlockEntity container) {
                long chestSeed = seed ^ (spec.dx * 31L + spec.dz * 17L + spec.dy * 13L);
                container.setLootTable(spec.lootTable, chestSeed);
            }
        }
    }

    private boolean isInsideGothicArch(int x, int y, int cx, int halfWidth, int height) {
        int relX = Math.abs(x - cx);
        if (relX > halfWidth || y < 1 || y > height) return false;
        double shoulderY = height * 0.55;
        if (y <= shoulderY) return relX < halfWidth;
        double t = (y - shoulderY) / (height - shoulderY);
        double maxAllowedX = halfWidth * (1.0 - t * t);
        return relX <= maxAllowedX;
    }

    private double ridgeNoise(int x, int z) {
        double n1 = Math.sin(x * 0.09 + z * 0.07) * Math.cos(z * 0.08 - x * 0.05);
        double n2 = Math.sin(x * 0.21 - z * 0.19) * 0.5;
        return Math.abs(n1 + n2);
    }

    private BlockState pickMountainBlock(RandomSource random, int dx, int wy, int dz, boolean isEdge) {
        // Coordinate-based hashing keeps the material pattern stable across chunk boundaries.
        // The edge flag uses a separate salt so mountain rims get a distinct natural pattern.
        long hash = seed
                ^ ((long) dx * 341873128712L)
                ^ ((long) wy * 132897987541L)
                ^ ((long) dz * 42317861L)
                ^ (isEdge ? 0x6A09E667F3BCC909L : 0x3C6EF372FE94F82AL);
        hash = (hash ^ (hash >>> 30)) * 0xBF58476D1CE4E5B9L;
        hash = (hash ^ (hash >>> 27)) * 0x94D049BB133111EBL;
        hash ^= hash >>> 31;
        int roll = (int) Math.floorMod(hash, 100L);

        // Exact mountain material distribution: 62% stone, 18% cracked, 10% frosted,
        // 6% pillar ribs and 4% amethyst-corrupted stone.
        if (roll < 62) {
            return AbyssContent.ABYSSAL_STONE.defaultBlockState();
        }
        if (roll < 80) {
            return AbyssContent.CRACKED_ABYSSAL_STONE.defaultBlockState();
        }
        if (roll < 90) {
            return AbyssContent.FROSTED_ABYSSAL_STONE.defaultBlockState();
        }
        if (roll < 96) {
            return AbyssContent.ABYSSAL_PILLAR.defaultBlockState();
        }
        return AbyssContent.AMETHYST_CORRUPTED_STONE.defaultBlockState();
    }

    private BlockState pickWallMasonry(RandomSource random, int dx, int dy, int dz) {
        if (dy % 8 == 0) {
            return AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState();
        }
        float r = random.nextFloat();
        if (r < 0.14f) {
            return AbyssContent.CRACKED_ABYSSAL_BRICKS.defaultBlockState();
        }
        if (r < 0.20f) {
            return AbyssContent.CHISELED_ABYSSAL_STONE.defaultBlockState();
        }
        return AbyssContent.ABYSSAL_BRICKS.defaultBlockState();
    }

    private BlockState pickFloorBlock(RandomSource random, int dx, int dz) {
        if ((dx == 0 || dz == 0) && Math.abs(dx) <= 14 && Math.abs(dz) <= 36) {
            return AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState();
        }
        return random.nextFloat() < 0.25f
                ? AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState()
                : AbyssContent.ABYSSAL_TILES.defaultBlockState();
    }

    private void setSafe(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        if (box.isInside(pos) && pos.getY() > level.getMinY() + 1) {
            level.setBlock(pos, state, 2);
        }
    }

    private record ChestSpec(int dx, int dy, int dz, ResourceKey<LootTable> lootTable) {}
}
