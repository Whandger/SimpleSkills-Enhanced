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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Monumental Gothic Abyssal Castle with multi-level subterranean dungeons & sewers,
 * soaring needle-spired towers, a cathedral-scale Royal Throne Hall, and 11 interconnected
 * interior rooms across two stories.
 *
 * Silhouette: flying arches run from the curtain walls over the wings to the nave, curtain walls
 * carry outward buttresses and merlons, towers step in under a projecting cornice and rise into
 * slender needle spires, and the wing eaves are corniced. Most openings are dark glass with about
 * 40% glowing warm from within.
 *
 * Ruin features, chosen per seed: some perimeter towers lean or topple onto the courtyard ground,
 * curtain walls can suffer collapsed breaches with fallen rubble fans, and roofs can collapse along
 * a stretch with a jagged cut line, scattering fallen roof tiles below and bringing down the central
 * spire if the nave roof beneath it gives way.
 */
public class AbyssalCastlePiece extends StructurePiece {

    public static final int BELOW_FLOOR_DEPTH = 64;
    public static final int MAX_HEIGHT = 122;

    public static final int HALF = 90;
    public static final int MIN_OFFSET_Z = -74;
    public static final int MAX_OFFSET_Z = 104;

    private static final double FOUNDATION_RADIUS_X = 68.0;
    private static final double FOUNDATION_RADIUS_Z = 68.0;
    private static final int PLATFORM_MIN_X = -42;
    private static final int PLATFORM_MAX_X = 42;
    private static final int PLATFORM_MIN_Z = -48;
    private static final int PLATFORM_MAX_Z = 60;

    /** Chance (0-99) that a perimeter tower leans. */
    public static final int LEAN_CHANCE = 35;
    /** Chance (0-99) that a perimeter tower has collapsed onto the ground. */
    public static final int TOWER_FALL_CHANCE = 35;
    /** Chance (0-99) that a curtain wall stretch has a collapsed breach. */
    public static final int WALL_COLLAPSE_CHANCE = 45;
    /** Chance (0-99) that a roof has a collapsed stretch. */
    public static final int COLLAPSE_CHANCE = 50;
    /** Percent of roof cells missing at random, for a broken look. */
    public static final int ROOF_HOLE_PERCENT = 6;

    private static final BlockState CHAIN = resolveChainState();

    private static final ResourceKey<LootTable> LOOT_CASTLE = ResourceKey.create(
            Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "chests/abyssal_castle"));
    private static final ResourceKey<LootTable> LOOT_LIBRARY = ResourceKey.create(
            Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "chests/abyssal_castle_library"));
    private static final ResourceKey<LootTable> LOOT_SANCTUM = ResourceKey.create(
            Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "chests/abyssal_castle_sanctum"));

    public static final StructurePieceType TYPE = (context, tag) -> new AbyssalCastlePiece(tag);

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

    private static BlockState resolveChainState() {
        Block chain = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("minecraft", "chain"));
        if (chain == Blocks.AIR) {
            chain = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("minecraft", "iron_chain"));
        }
        return (chain != null && chain != Blocks.AIR ? chain : Blocks.IRON_BARS).defaultBlockState();
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
        // 1. Craggy Mountain Massif Foundation anchored to solid Abyss floor
        buildCraggyMountainFoundation(level, box);

        // 2. Subterranean Level -2: Abyssal Sewers with water channels, iron grates & smuggler stash
        buildSubterraneanSewers(level, box);

        // 3. Subterranean Level -1: Dungeons, 8 Iron-Barred Prison Cells, Warden Post & Relic Crypt
        buildSubterraneanDungeonsAndCrypt(level, box);

        // 4. Outer Curtain Walls, Crenellations, Outward Buttresses, South Gatehouse & Courtyard
        buildCurtainWallsAndCourtyard(level, box);
        buildCurtainButtresses(level, box);

        // 5. Grand Royal Throne Hall & Cathedral Nave (with colonnade, chandeliers, stained glass & mezzanine)
        buildRoyalThroneHall(level, box);

        // 6. Two-Story West & East Wings (11 furnished, interconnected smaller rooms + internal stairs)
        buildSideWingsAndRooms(level, box);
        buildWingCornices(level, box);

        // 7. Towers with setbacks, Gothic needle spires, Fallen Towers, Flying Arches & Sovereign Spire
        buildAllTowersAndSpires(level, box);

        // 8. Monumental South Viaduct with 4 Gothic Arches
        buildSouthViaduct(level, box);

        // 9. Place & Seed All 44 Chests across 3 Loot Tables
        placeAllCastleChests(level, box);
    }

    // =========================================================================
    // 1. CRAGGY MOUNTAIN FOUNDATION
    // =========================================================================

    private void buildCraggyMountainFoundation(WorldGenLevel level, BoundingBox box) {
        int yMinLimit = Math.max(level.getMinY() + 2, -254);

        // Keep every terrain read inside the current structure chunk bounds.
        int minDx = Math.max(-HALF, box.minX() - center.getX());
        int maxDx = Math.min(HALF, box.maxX() - center.getX());
        int minDz = Math.max(MIN_OFFSET_Z, box.minZ() - center.getZ());
        int maxDz = Math.min(MAX_OFFSET_Z, box.maxZ() - center.getZ());

        for (int dx = minDx; dx <= maxDx; dx++) {
            for (int dz = minDz; dz <= maxDz; dz++) {
                int wx = center.getX() + dx;
                int wz = center.getZ() + dz;
                double relativeDz = dz - 8.0;
                double normDist = Math.hypot(dx / FOUNDATION_RADIUS_X, relativeDz / FOUNDATION_RADIUS_Z);
                int surfaceDrop = getFoundationSurfaceDrop(dx, dz);

                int solidDepth = 0;
                for (int d = surfaceDrop; d <= BELOW_FLOOR_DEPTH; d++) {
                    int wy = center.getY() - d;
                    if (wy < yMinLimit) break;

                    double depthBelowSurface = d - surfaceDrop;
                    double depthProgress = depthBelowSurface / (double) Math.max(1, BELOW_FLOOR_DEPTH - surfaceDrop);
                    double maxRadiusFactor = foundationRadiusFactor(dx, (int) relativeDz, depthProgress);
                    if (normDist > maxRadiusFactor) continue;

                    BlockPos pos = new BlockPos(wx, wy, wz);
                    BlockState existing = level.getBlockState(pos);

                    // Keep enough solid rock around the underground rooms before joining natural terrain.
                    if (depthBelowSurface > 23 && existing.isSolidRender()
                            && !existing.is(Blocks.WATER) && !existing.is(Blocks.LAVA)) {
                        solidDepth++;
                        if (solidDepth >= 4) break;
                    } else {
                        solidDepth = 0;
                    }

                    if (d == 0 && isCastlePlatform(dx, dz)) {
                        setSafe(level, box, pos, pickFloorBlock(dx, dz));
                    } else {
                        boolean isEdge = d == surfaceDrop || normDist > (maxRadiusFactor - 0.15);
                        setSafe(level, box, pos, pickMountainBlock(dx, wy, dz, isEdge));
                    }
                }
            }
        }
    }

    // =========================================================================
    // 2. SUBTERRANEAN LEVEL -2: ABYSSAL SEWERS (dy = -21 .. -13)
    // =========================================================================

    private void buildSubterraneanSewers(WorldGenLevel level, BoundingBox box) {
        // Main North-South Collector Tunnel (dx = -5..5, dz = -32..56, dy = -21..-13)
        carveSewerTunnelZ(level, box, 0, -32, 56);

        // Three East-West Cross Canals (dz = -16, 8, 32; dx = -30..30)
        for (int canalZ : new int[]{-16, 8, 32}) {
            carveSewerTunnelX(level, box, canalZ, -30, 30);
        }

        // Iron-Bar Sluice Gates & Portcullises along Main Sewer Tunnel
        for (int gateZ : new int[]{-24, -4, 20, 44}) {
            buildSewerGrateZ(level, box, 0, gateZ, true);
        }
        // Heavy South Outflow Grates overlooking the Abyss under the Viaduct (dz = 56)
        buildSewerGrateZ(level, box, 0, 56, false);

        // Iron-Bar Grates on East-West Canals
        for (int canalZ : new int[]{-16, 8, 32}) {
            buildSewerGrateX(level, box, -16, canalZ, true);
            buildSewerGrateX(level, box, 16, canalZ, true);
            buildSewerGrateX(level, box, -30, canalZ, false);
            buildSewerGrateX(level, box, 30, canalZ, false);
        }

        // Secret Smuggler's Cistern Vault at NW corner (dx = -28..-20, dz = -30..-20, dy = -20..-14)
        fillVolume(level, box, -28, -20, -30, -20, -14, -20, Blocks.AIR.defaultBlockState());
        for (int x = -28; x <= -20; x++) {
            for (int z = -30; z <= -20; z++) {
                setSafe(level, box, center.offset(x, -21, z), AbyssContent.CRACKED_ABYSSAL_BRICKS.defaultBlockState());
                setSafe(level, box, center.offset(x, -13, z), AbyssContent.CRACKED_ABYSSAL_BRICKS.defaultBlockState());
            }
        }
        // Broken iron-bar access between West Cross Canal (dz = -16) and Smuggler Vault (dz = -20)
        for (int x = -26; x <= -22; x++) {
            for (int z = -19; z <= -18; z++) {
                for (int y = -20; y <= -16; y++) {
                    setSafe(level, box, center.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
            for (int y = -20; y <= -16; y++) {
                // Leave a 2-block corroded gap at x = -24, y = -20..-19 so players can squeeze through
                if (x == -24 && y <= -19) continue;
                setSafe(level, box, center.offset(x, y, -20), ironBars(false, false, true, true));
            }
        }
        setSafe(level, box, center.offset(-27, -20, -29), Blocks.BARREL.defaultBlockState());
        setSafe(level, box, center.offset(-26, -20, -29), Blocks.BARREL.defaultBlockState());
        setSafe(level, box, center.offset(-21, -20, -29), Blocks.COBWEB.defaultBlockState());
        setSafe(level, box, center.offset(-24, -14, -25), hangingLantern(true));

        // Service Stairwell climbing from Sewers (dy = -20) to Dungeons (dy = -10) at dx = -28..-25, dz = 35..46
        fillVolume(level, box, -28, -20, 35, -25, -5, 46, Blocks.AIR.defaultBlockState());
        for (int step = 0; step <= 10; step++) {
            int sy = -20 + step;
            int sz = 36 + step;
            for (int sx = -28; sx <= -25; sx++) {
                setSafe(level, box, center.offset(sx, sy, sz), stairs(Direction.SOUTH, Half.BOTTOM));
                for (int fillY = -21; fillY < sy; fillY++) {
                    setSafe(level, box, center.offset(sx, fillY, sz), AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
                }
            }
        }
        setSafe(level, box, center.offset(-27, -14, 37), hangingLantern(true));
        setSafe(level, box, center.offset(-27, -5, 44), hangingLantern(true));
    }

    private void carveSewerTunnelZ(WorldGenLevel level, BoundingBox box, int cx, int zMin, int zMax) {
        for (int z = zMin; z <= zMax; z++) {
            for (int dx = -5; dx <= 5; dx++) {
                int x = cx + dx;
                int absDx = Math.abs(dx);
                int ceilingY = absDx <= 2 ? -13 : (absDx <= 4 ? -14 : -15);

                // Floor & central water channel
                if (absDx <= 2) {
                    setSafe(level, box, center.offset(x, -21, z), AbyssContent.CRACKED_ABYSSAL_BRICKS.defaultBlockState());
                    setSafe(level, box, center.offset(x, -20, z), Blocks.WATER.defaultBlockState());
                } else {
                    BlockState walk = coordMod(x, -20, z, 4) == 0
                            ? AbyssContent.CRACKED_ABYSSAL_BRICKS.defaultBlockState()
                            : AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState();
                    setSafe(level, box, center.offset(x, -20, z), walk);
                }

                // Air cavity
                for (int y = -19; y <= ceilingY; y++) {
                    setSafe(level, box, center.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }

                // Vaulted brick ceiling
                setSafe(level, box, center.offset(x, ceilingY + 1, z), AbyssContent.ABYSSAL_BRICKS.defaultBlockState());

                // Side masonry walls
                if (absDx == 5) {
                    for (int y = -20; y <= ceilingY + 1; y++) {
                        BlockState wall = (z % 6 == 0)
                                ? AbyssContent.ABYSSAL_PILLAR.defaultBlockState()
                                : pickWallMasonry(x, y, z);
                        setSafe(level, box, center.offset(x, y, z), wall);
                    }
                    if (z % 12 == 0) {
                        setSafe(level, box, center.offset(dx < 0 ? x + 1 : x - 1, -16, z), hangingLantern(true));
                    }
                }
            }
        }
    }

    private void carveSewerTunnelX(WorldGenLevel level, BoundingBox box, int cz, int xMin, int xMax) {
        for (int x = xMin; x <= xMax; x++) {
            // Avoid overwriting the central water channel of the N-S collector
            boolean inMainChannel = Math.abs(x) <= 2;
            for (int dz = -4; dz <= 4; dz++) {
                int z = cz + dz;
                int absDz = Math.abs(dz);
                int ceilingY = absDz <= 1 ? -14 : (absDz <= 3 ? -15 : -16);

                if (!inMainChannel) {
                    if (absDz <= 1) {
                        setSafe(level, box, center.offset(x, -21, z), AbyssContent.CRACKED_ABYSSAL_BRICKS.defaultBlockState());
                        setSafe(level, box, center.offset(x, -20, z), Blocks.WATER.defaultBlockState());
                    } else {
                        setSafe(level, box, center.offset(x, -20, z), AbyssContent.CRACKED_ABYSSAL_BRICKS.defaultBlockState());
                    }
                }

                for (int y = -19; y <= ceilingY; y++) {
                    setSafe(level, box, center.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
                setSafe(level, box, center.offset(x, ceilingY + 1, z), AbyssContent.ABYSSAL_BRICKS.defaultBlockState());

                if (absDz == 4 && Math.abs(x) > 5) {
                    for (int y = -20; y <= ceilingY + 1; y++) {
                        setSafe(level, box, center.offset(x, y, z), pickWallMasonry(x, y, z));
                    }
                }
            }
        }
    }

    private void buildSewerGrateZ(WorldGenLevel level, BoundingBox box, int cx, int z, boolean walkGap) {
        for (int dx = -4; dx <= 4; dx++) {
            int x = cx + dx;
            for (int y = -19; y <= -14; y++) {
                // Keep 2-block-wide open walkway on West side if navigable
                if (walkGap && (dx == -4 || dx == -3) && y <= -17) continue;
                setSafe(level, box, center.offset(x, y, z), ironBars(false, false, true, true));
            }
        }
    }

    private void buildSewerGrateX(WorldGenLevel level, BoundingBox box, int x, int cz, boolean walkGap) {
        for (int dz = -3; dz <= 3; dz++) {
            int z = cz + dz;
            for (int y = -19; y <= -15; y++) {
                if (walkGap && (dz == -3 || dz == -2) && y <= -17) continue;
                setSafe(level, box, center.offset(x, y, z), ironBars(true, true, false, false));
            }
        }
    }

    // =========================================================================
    // 3. SUBTERRANEAN LEVEL -1: DUNGEONS, CELLS & ROYAL CRYPT (dy = -11 .. -2)
    // =========================================================================

    private void buildSubterraneanDungeonsAndCrypt(WorldGenLevel level, BoundingBox box) {
        // Main Dungeon Gallery (dx = -6..6, dz = -18..48, dy = -10..-3)
        carveChamber(level, box, -6, -10, -18, 6, -3, 48,
                AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState(),
                AbyssContent.ABYSSAL_BRICKS.defaultBlockState());

        // Vaulted Dungeon Pillars & Hanging Braziers
        for (int z = -14; z <= 44; z += 6) {
            for (int y = -10; y <= -3; y++) {
                setSafe(level, box, center.offset(-6, y, z), AbyssContent.ABYSSAL_PILLAR.defaultBlockState());
                setSafe(level, box, center.offset(6, y, z), AbyssContent.ABYSSAL_PILLAR.defaultBlockState());
            }
            if (z % 12 == 0) {
                setSafe(level, box, center.offset(0, -3, z), CHAIN);
                setSafe(level, box, center.offset(0, -4, z), hangingLantern(true));
            }
        }

        // 8 Prison Cells with Iron Bars (4 West: dx = -16..-7, 4 East: dx = 7..16)
        int[] cellStartsZ = {-14, -6, 2, 10};
        for (int cIdx = 0; cIdx < cellStartsZ.length; cIdx++) {
            int z0 = cellStartsZ[cIdx];
            int z1 = z0 + 6;
            buildPrisonCell(level, box, -16, -7, z0, z1, true, cIdx);
            buildPrisonCell(level, box, 7, 16, z0, z1, false, cIdx + 4);
        }

        // Warden's Guardroom (West: dx = -28..-17, dz = 18..36, dy = -10..-3)
        carveChamber(level, box, -28, -10, 18, -17, -3, 36,
                AbyssContent.ABYSSAL_TILES.defaultBlockState(),
                AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
        // Doorway from Main Dungeon Gallery (dx = -6) through to Warden's Room & Sewer Stairwell
        carveChamber(level, box, -24, -10, 24, -6, -6, 28,
                AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState(),
                AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
        // Connect Warden's Room to Sewer Stair landing at z = 44..46
        carveChamber(level, box, -28, -10, 36, -24, -5, 46,
                AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState(),
                AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
        // Warden's furniture
        setSafe(level, box, center.offset(-26, -10, 20), Blocks.ANVIL.defaultBlockState());
        setSafe(level, box, center.offset(-25, -10, 20), Blocks.GRINDSTONE.defaultBlockState());
        setSafe(level, box, center.offset(-26, -10, 32), Blocks.BARREL.defaultBlockState());
        setSafe(level, box, center.offset(-25, -10, 32), Blocks.BARREL.defaultBlockState());
        setSafe(level, box, center.offset(-22, -10, 20), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
        setSafe(level, box, center.offset(-22, -4, 27), hangingLantern(true));

        // Torture & Secret Alchemy Forge Chamber (East: dx = 17..28, dz = 18..36, dy = -10..-3)
        carveChamber(level, box, 17, -10, 18, 28, -3, 36,
                AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState(),
                AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
        carveChamber(level, box, 6, -10, 24, 17, -6, 28,
                AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState(),
                AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
        // Iron-bar interrogation cage inside East dungeon room
        for (int x = 22; x <= 26; x++) {
            for (int y = -10; y <= -4; y++) {
                if (x == 24 && y <= -8) continue;
                setSafe(level, box, center.offset(x, y, 22), ironBars(false, false, true, true));
            }
        }
        setSafe(level, box, center.offset(24, -4, 20), CHAIN);
        setSafe(level, box, center.offset(24, -5, 20), CHAIN);
        setSafe(level, box, center.offset(19, -10, 33), Blocks.BLAST_FURNACE.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        setSafe(level, box, center.offset(21, -10, 33), Blocks.CAULDRON.defaultBlockState());
        setSafe(level, box, center.offset(26, -10, 33), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());

        // Royal Relic Crypt at North End (dx = -11..11, dz = -34..-19, dy = -10..-2)
        carveChamber(level, box, -11, -10, -34, 11, -2, -19,
                AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState(),
                AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
        // Iron-barred Crypt Gatehouse at dz = -18
        for (int x = -5; x <= 5; x++) {
            for (int y = -10; y <= -4; y++) {
                if (Math.abs(x) <= 1 && y <= -7) continue;
                setSafe(level, box, center.offset(x, y, -18), ironBars(false, false, true, true));
            }
        }
        // Royal Sarcophagi & Amethyst Pillars inside Crypt
        for (int sx : new int[]{-6, 6}) {
            for (int sz = -30; sz <= -23; sz++) {
                setSafe(level, box, center.offset(sx, -10, sz), AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
                setSafe(level, box, center.offset(sx, -9, sz), AbyssContent.ABYSSAL_TILE_SLAB.defaultBlockState());
            }
            setSafe(level, box, center.offset(sx, -10, -21), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
        }
        setSafe(level, box, center.offset(-9, -10, -32), AbyssContent.AMETHYST_CORRUPTED_STONE.defaultBlockState());
        setSafe(level, box, center.offset(9, -10, -32), AbyssContent.AMETHYST_CORRUPTED_STONE.defaultBlockState());

        // Grand Ascending Stairwell from Dungeon (dy = -10) up to SE Courtyard (dy = 1) at dx = 18..22, dz = 37..48
        carveChamber(level, box, 6, -10, 42, 22, -6, 48,
                AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState(),
                AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
        fillVolume(level, box, 18, -10, 36, 22, 5, 48, Blocks.AIR.defaultBlockState());
        for (int step = 0; step <= 10; step++) {
            int sy = -10 + step;
            int sz = 46 - step;
            for (int sx = 18; sx <= 22; sx++) {
                setSafe(level, box, center.offset(sx, sy, sz), stairs(Direction.NORTH, Half.BOTTOM));
                for (int fillY = -11; fillY < sy; fillY++) {
                    setSafe(level, box, center.offset(sx, fillY, sz), AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
                }
            }
        }
        // Surface stairwell parapet around (dx = 17..23, dz = 35..48)
        for (int z = 35; z <= 48; z++) {
            setSafe(level, box, center.offset(17, 1, z), AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState());
            setSafe(level, box, center.offset(23, 1, z), AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState());
        }
        setSafe(level, box, center.offset(17, 1, 48), AbyssContent.CHISELED_ABYSSAL_STONE.defaultBlockState());
        setSafe(level, box, center.offset(17, 2, 48), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
        setSafe(level, box, center.offset(23, 1, 48), AbyssContent.CHISELED_ABYSSAL_STONE.defaultBlockState());
        setSafe(level, box, center.offset(23, 2, 48), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
    }

    private void buildPrisonCell(WorldGenLevel level, BoundingBox box, int x0, int x1, int z0, int z1,
                                 boolean westSide, int cellIndex) {
        carveChamber(level, box, x0, -10, z0, x1, -4, z1,
                AbyssContent.CRACKED_ABYSSAL_STONE.defaultBlockState(),
                AbyssContent.ABYSSAL_BRICKS.defaultBlockState());

        int barX = westSide ? x1 : x0;
        int doorZ = (z0 + z1) / 2;
        for (int z = z0 + 1; z <= z1 - 1; z++) {
            for (int y = -10; y <= -4; y++) {
                if (z == doorZ && y <= -8) continue; // Open doorway into cell
                if (y == -10) {
                    setSafe(level, box, center.offset(barX, y, z), AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState());
                } else {
                    setSafe(level, box, center.offset(barX, y, z), ironBars(true, true, false, false));
                }
            }
        }

        // Cell interior details: hanging chains, cobwebs, prisoner bones
        int innerX = westSide ? x0 + 2 : x1 - 2;
        setSafe(level, box, center.offset(innerX, -4, z0 + 2), CHAIN);
        setSafe(level, box, center.offset(innerX, -5, z0 + 2), CHAIN);
        if (cellIndex % 2 == 0) {
            setSafe(level, box, center.offset(westSide ? x0 + 1 : x1 - 1, -10, z1 - 1), Blocks.BONE_BLOCK.defaultBlockState());
            setSafe(level, box, center.offset(westSide ? x0 + 2 : x1 - 2, -10, z1 - 1), Blocks.SKELETON_SKULL.defaultBlockState());
        } else {
            setSafe(level, box, center.offset(westSide ? x0 + 1 : x1 - 1, -4, z1 - 1), Blocks.COBWEB.defaultBlockState());
        }
    }

    // =========================================================================
    // 4. OUTER CURTAIN WALLS, GATEHOUSE & COURTYARD (dy = 1 .. 19)
    // =========================================================================

    private void buildCurtainWallsAndCourtyard(WorldGenLevel level, BoundingBox box) {
        // Outer Curtain Walls (dx = -36..36, dz = -42..54, height 18)
        for (int dy = 1; dy <= 18; dy++) {
            for (int dx = -36; dx <= 36; dx++) {
                for (int dz = -42; dz <= 54; dz++) {
                    boolean isOuter = (Math.abs(dx) == 36 || dz == -42 || dz == 54);
                    boolean isWalkway = (dy == 16) && (Math.abs(dx) >= 34 || dz <= -40 || dz >= 52);
                    boolean isInnerParapet = (dy == 17) && (Math.abs(dx) == 34 || dz == -40 || dz == 52);

                    int wallTopY = getCurtainWallTopY(dx, dz);
                    if (dy > wallTopY) continue;

                    if (isWalkway) {
                        setSafe(level, box, center.offset(dx, dy, dz), AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState());
                        continue;
                    }
                    if (isInnerParapet) {
                        setSafe(level, box, center.offset(dx, dy, dz), AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState());
                        continue;
                    }
                    if (!isOuter) continue;

                    // South Gatehouse Gothic Arch Opening (dz == 54)
                    if (dz == 54 && isInsideGothicArch(dx, dy, 0, 5, 13)) {
                        // Raised iron portcullis teeth at the top of the gatehouse arch
                        if (dy >= 10 && Math.abs(dx) <= 3) {
                            setSafe(level, box, center.offset(dx, dy, dz), ironBars(false, false, true, true));
                        }
                        continue;
                    }

                    // Ruined breaches in the mid curtain wall (~4% of cells, dy = 10..15)
                    if (dy >= 10 && dy <= 15 && coordMod(dx, dy, dz, 100) < 4) continue;

                    BlockPos pos = center.offset(dx, dy, dz);
                    if (dy == 17) {
                        // Stone course under the crenellations
                        setSafe(level, box, pos, AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState());
                    } else {
                        setSafe(level, box, pos, pickWallMasonry(dx, dy, dz));
                    }
                }
            }
        }

        // Crenellations: alternating merlons on top of the outer curtain (dy = 19)
        for (int dx = -36; dx <= 36; dx++) {
            for (int dz = -42; dz <= 54; dz++) {
                boolean isOuter = (Math.abs(dx) == 36 || dz == -42 || dz == 54);
                if (isOuter && (dx + dz) % 2 != 0 && getCurtainWallTopY(dx, dz) >= 18) {
                    setSafe(level, box, center.offset(dx, 19, dz), AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState());
                }
            }
        }

        // Fallen rubble piles on the ground beside any collapsed curtain wall breaches
        buildFallenWallRubble(level, box);

        // Courtyard Braziers flanking the South Approach (dz = 40..51)
        for (int z : new int[]{42, 48}) {
            for (int side : new int[]{-6, 6}) {
                setSafe(level, box, center.offset(side, 1, z), AbyssContent.CHISELED_ABYSSAL_STONE.defaultBlockState());
                setSafe(level, box, center.offset(side, 2, z), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
            }
        }
    }

    /**
     * Outward buttresses on the curtain walls, every 12 blocks or so. Each one projects two blocks
     * out, tapers to one block near the top, and ends in a slab cap. Skipped where the massif does not
     * reach or where the curtain wall has collapsed.
     */
    private void buildCurtainButtresses(WorldGenLevel level, BoundingBox box) {
        // West and east faces (outward = -x / +x), skipping the mid bastions around dz = 6
        for (int dz : new int[]{-30, -18, 18, 30, 42}) {
            buildButtress(level, box, -36, dz, -1, 0);
            buildButtress(level, box, 36, dz, 1, 0);
        }
        // North face (outward = -z)
        for (int dx : new int[]{-26, -16, -6, 6, 16, 26}) {
            buildButtress(level, box, dx, -42, 0, -1);
        }
        // South face (outward = +z), kept clear of the gate and the gatehouse towers
        for (int dx : new int[]{-26, -18, 18, 26}) {
            buildButtress(level, box, dx, 54, 0, 1);
        }
    }

    private void buildButtress(WorldGenLevel level, BoundingBox box, int wallX, int wallZ, int ox, int oz) {
        if (!hasFoundationAtSurface(wallX + ox, wallZ + oz)) return;

        int wallTopY = getCurtainWallTopY(wallX, wallZ);
        if (wallTopY < 7) return;
        int maxButtressY = Math.min(14, wallTopY - 2);

        for (int y = 1; y <= maxButtressY; y++) {
            int depth = y <= 9 ? 2 : 1;
            for (int w = -1; w <= 1; w++) {
                for (int d = 1; d <= depth; d++) {
                    int x = ox != 0 ? wallX + ox * d : wallX + w;
                    int z = oz != 0 ? wallZ + oz * d : wallZ + w;
                    setSafe(level, box, center.offset(x, y, z), pickWallMasonry(x, y, z));
                }
            }
        }
        for (int w = -1; w <= 1; w++) {
            int x = ox != 0 ? wallX + ox : wallX + w;
            int z = oz != 0 ? wallZ + oz : wallZ + w;
            setSafe(level, box, center.offset(x, maxButtressY + 1, z), AbyssContent.ABYSSAL_BRICK_SLAB.defaultBlockState());
        }
    }

    /**
     * Returns the maximum intact Y (1..18) for a curtain wall coordinate (dx, dz).
     * Five independent wall stretches (North, West, East, South-West, South-East) can
     * suffer a U-shaped jagged collapse breach per seed.
     */
    private int getCurtainWallTopY(int dx, int dz) {
        int topY = 18;
        // 0: North curtain wall (dz = -42, dx = -28..28)
        if (dz <= -40 && Math.abs(dx) <= 28) {
            topY = Math.min(topY, evaluateWallBreachTopY(0, dx, -20, 20));
        }
        // 1: West curtain wall (dx = -36, dz = -34..46, skipping mid bastion at dz = 6)
        if (dx <= -34 && dz >= -34 && dz <= 46 && Math.abs(dz - 6) > 7) {
            topY = Math.min(topY, evaluateWallBreachTopY(1, dz, -24, 36));
        }
        // 2: East curtain wall (dx = 36, dz = -34..46, skipping mid bastion at dz = 6)
        if (dx >= 34 && dz >= -34 && dz <= 46 && Math.abs(dz - 6) > 7) {
            topY = Math.min(topY, evaluateWallBreachTopY(2, dz, -24, 36));
        }
        // 3: South-West curtain wall (dz = 54, dx = -28..-17)
        if (dz >= 52 && dx >= -28 && dx <= -17) {
            topY = Math.min(topY, evaluateWallBreachTopY(3, dx, -23, -21));
        }
        // 4: South-East curtain wall (dz = 54, dx = 17..28)
        if (dz >= 52 && dx >= 17 && dx <= 28) {
            topY = Math.min(topY, evaluateWallBreachTopY(4, dx, 21, 23));
        }
        return topY;
    }

    private int evaluateWallBreachTopY(int wallId, int posAlong, int minCenter, int maxCenter) {
        if (coordMod(wallId, 21, 0, 100) >= WALL_COLLAPSE_CHANCE) return 18;
        int span = Math.max(1, maxCenter - minCenter + 1);
        int breachCenter = minCenter + coordMod(wallId, 22, 0, span);
        int halfWidth = (wallId >= 3 ? 4 : 5) + coordMod(wallId, 23, 0, wallId >= 3 ? 2 : 5);
        int dist = Math.abs(posAlong - breachCenter);
        if (dist > halfWidth) return 18;

        double t = dist / (double) halfWidth; // 0 at center of breach, 1 at edge
        int baseTop = (int) Math.round(1 + 14 * (t * t));
        int jag = coordMod(wallId, posAlong, 24, 4) - 1;
        return Math.max(1, Math.min(17, baseTop + jag));
    }

    /**
     * Scatters fallen wall masonry, slabs, stairs, and broken merlons on the courtyard ground
     * adjacent to every collapsed curtain wall breach.
     */
    private void buildFallenWallRubble(WorldGenLevel level, BoundingBox box) {
        // North wall rubble (falls inward +Z)
        scatterBreachRubbleAlongZ(level, box, 0, -41, 1, -28, 28);
        // West wall rubble (falls inward +X)
        scatterBreachRubbleAlongX(level, box, 1, -35, 1, -34, 46);
        // East wall rubble (falls inward -X)
        scatterBreachRubbleAlongX(level, box, 2, 35, -1, -34, 46);
        // South-West wall rubble (falls inward -Z)
        scatterBreachRubbleAlongZ(level, box, 3, 53, -1, -28, -17);
        // South-East wall rubble (falls inward -Z)
        scatterBreachRubbleAlongZ(level, box, 4, 53, -1, 17, 28);
    }

    private void scatterBreachRubbleAlongZ(WorldGenLevel level, BoundingBox box, int wallId,
                                           int startZ, int dirZ, int minX, int maxX) {
        for (int dx = minX; dx <= maxX; dx++) {
            int topY = getCurtainWallTopY(dx, startZ + (dirZ > 0 ? -1 : 1));
            if (topY >= 17) continue;
            int missingHeight = 18 - topY;
            int maxReach = 3 + (missingHeight / 2) + coordMod(dx, wallId, 25, 3);

            for (int step = 0; step < maxReach; step++) {
                int dz = startZ + dirZ * step;
                if (isInsideProtectedBuilding(dx, dz)) break;
                double falloff = 1.0 - (step / (double) maxReach);
                int pileH = (int) Math.round(falloff * (missingHeight / 4.5));
                if (pileH <= 0 && coordMod(dx, step, wallId, 100) < 45) {
                    pileH = 1;
                }
                for (int y = 1; y <= pileH; y++) {
                    setSafe(level, box, center.offset(dx, y, dz), pickRubbleBlock(dx, y, dz, y == pileH));
                }
            }
        }
    }

    private void scatterBreachRubbleAlongX(WorldGenLevel level, BoundingBox box, int wallId,
                                           int startX, int dirX, int minZ, int maxZ) {
        for (int dz = minZ; dz <= maxZ; dz++) {
            int topY = getCurtainWallTopY(startX + (dirX > 0 ? -1 : 1), dz);
            if (topY >= 17) continue;
            int missingHeight = 18 - topY;
            int maxReach = 3 + (missingHeight / 2) + coordMod(wallId, dz, 26, 3);

            for (int step = 0; step < maxReach; step++) {
                int dx = startX + dirX * step;
                if (isInsideProtectedBuilding(dx, dz)) break;
                double falloff = 1.0 - (step / (double) maxReach);
                int pileH = (int) Math.round(falloff * (missingHeight / 4.5));
                if (pileH <= 0 && coordMod(step, dz, wallId, 100) < 45) {
                    pileH = 1;
                }
                for (int y = 1; y <= pileH; y++) {
                    setSafe(level, box, center.offset(dx, y, dz), pickRubbleBlock(dx, y, dz, y == pileH));
                }
            }
        }
    }

    private BlockState pickRubbleBlock(int x, int y, int z, boolean topBlock) {
        int roll = coordMod(x, y, z, 100);
        if (topBlock) {
            if (roll < 35) return AbyssContent.ABYSSAL_BRICK_SLAB.defaultBlockState();
            if (roll < 55) {
                int directionIndex = coordMod(x, z, y, 4);
                Direction dir = directionIndex == 0 ? Direction.SOUTH
                        : directionIndex == 1 ? Direction.WEST
                        : directionIndex == 2 ? Direction.NORTH
                        : Direction.EAST;
                return stairs(dir, coordMod(x, y, z, 2) == 0 ? Half.BOTTOM : Half.TOP);
            }
            if (roll < 70) return AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState();
        }
        if (roll < 38) return AbyssContent.CRACKED_ABYSSAL_BRICKS.defaultBlockState();
        if (roll < 62) return AbyssContent.CRACKED_ABYSSAL_STONE.defaultBlockState();
        if (roll < 78) return AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState();
        return AbyssContent.ABYSSAL_BRICKS.defaultBlockState();
    }

    private BlockState pickRoofRubbleBlock(int x, int y, int z, boolean topBlock) {
        int roll = coordMod(x, y, z, 100);
        if (topBlock) {
            if (roll < 40) return AbyssContent.ABYSSAL_TILE_SLAB.defaultBlockState();
            if (roll < 68) {
                int directionIndex = coordMod(x, z, y, 4);
                Direction dir = directionIndex == 0 ? Direction.SOUTH
                        : directionIndex == 1 ? Direction.WEST
                        : directionIndex == 2 ? Direction.NORTH
                        : Direction.EAST;
                return stairs(dir, coordMod(x, y, z, 2) == 0 ? Half.BOTTOM : Half.TOP);
            }
        }
        if (roll < 42) return AbyssContent.ABYSSAL_TILES.defaultBlockState();
        if (roll < 66) return AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState();
        if (roll < 86) return AbyssContent.CRACKED_ABYSSAL_BRICKS.defaultBlockState();
        return AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState();
    }

    // =========================================================================
    // 5. GRAND ROYAL THRONE HALL & CATHEDRAL NAVE (dx = -16..16, dz = -26..36)
    // =========================================================================

    private void buildRoyalThroneHall(WorldGenLevel level, BoundingBox box) {
        // Outer Walls & Windows (dy = 1..30)
        for (int dy = 1; dy <= 30; dy++) {
            for (int dx = -16; dx <= 16; dx++) {
                for (int dz = -26; dz <= 36; dz++) {
                    boolean isSideWall = Math.abs(dx) == 16;
                    boolean isEndWall = (dz == -26 || dz == 36);

                    if (!isSideWall && !isEndWall) {
                        // Upper Mezzanine Galleries at dy = 14 along dx = +-11..15
                        if (dy == 14 && Math.abs(dx) >= 11 && dz >= -18 && dz <= 34) {
                            setSafe(level, box, center.offset(dx, dy, dz), AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState());
                            if (Math.abs(dx) == 11) {
                                setSafe(level, box, center.offset(dx, 15, dz), AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState());
                            }
                        }
                        continue;
                    }

                    // South Nave Portal Arch (dz == 36)
                    if (dz == 36 && isInsideGothicArch(dx, dy, 0, 5, 11)) {
                        continue;
                    }

                    // Doorways connecting Throne Hall to West & East Side Wings (Ground & Mezzanine)
                    if (isSideWall) {
                        boolean groundDoor = (dy >= 1 && dy <= 4) &&
                                ((dz >= -15 && dz <= -13) || (dz >= 5 && dz <= 7) || (dz >= 23 && dz <= 25));
                        boolean upperDoor = (dy >= 15 && dy <= 18) &&
                                ((dz >= -15 && dz <= -13) || (dz >= 5 && dz <= 7) || (dz >= 23 && dz <= 25));
                        if (groundDoor || upperDoor) continue;
                    }

                    // Rose windows at North (dz = -26) & South (dz = 36): dark tracery with a warm core
                    if ((dz == 36 || dz == -26) && Math.hypot(dx, dy - 19) <= 4.6) {
                        double dist = Math.hypot(dx, dy - 19);
                        BlockState glass;
                        if (dist <= 1.8) {
                            glass = Blocks.SHROOMLIGHT.defaultBlockState();
                        } else if ((Math.abs(dx) + dy) % 2 == 0) {
                            glass = Blocks.STAINED_GLASS.pick(DyeColor.GRAY).defaultBlockState();
                        } else {
                            glass = Blocks.STAINED_GLASS.pick(DyeColor.BLACK).defaultBlockState();
                        }
                        setSafe(level, box, center.offset(dx, dy, dz), glass);
                        int backZ = dz == 36 ? 35 : -25;
                        if (dx == 0 && dy == 19) {
                            setSafe(level, box, center.offset(0, 19, backZ), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
                        }
                        continue;
                    }

                    // Tall Gothic lancet windows along the side walls (dy = 6..12 and 19..25): dark glass, some lit
                    if (isSideWall && Math.abs(dz % 6) <= 1 && ((dy >= 6 && dy <= 12) || (dy >= 19 && dy <= 25))) {
                        if (Math.abs(dz % 6) == 0) {
                            setSafe(level, box, center.offset(dx, dy, dz), windowBlock(dx, dy <= 12 ? 0 : 1, dz));
                        } else {
                            setSafe(level, box, center.offset(dx, dy, dz), AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState());
                        }
                        continue;
                    }

                    setSafe(level, box, center.offset(dx, dy, dz), pickWallMasonry(dx, dy, dz));
                }
            }
        }

        // Sloped Gothic Tile Roof over the Nave (dy = 30..47), possibly collapsed
        for (int step = 0; step <= 17; step++) {
            int dy = 30 + step;
            int halfW = 17 - step;
            for (int dz = -27; dz <= 37; dz++) {
                if (halfW > 0) {
                    setRoof(level, box, -halfW, dy, dz, stairs(Direction.EAST, Half.BOTTOM), 8, 30);
                    setRoof(level, box, halfW, dy, dz, stairs(Direction.WEST, Half.BOTTOM), 8, 30);
                    if (halfW > 1) {
                        setRoof(level, box, -halfW + 1, dy, dz, AbyssContent.ABYSSAL_TILES.defaultBlockState(), 8, 30);
                        setRoof(level, box, halfW - 1, dy, dz, AbyssContent.ABYSSAL_TILES.defaultBlockState(), 8, 30);
                    }
                } else {
                    setRoof(level, box, 0, dy, dz, AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState(), 8, 30);
                }
            }
        }

        // Gothic Colonnade Pillars & Pointed Transverse Ceiling Ribs (dx = +-9)
        for (int pz : new int[]{-18, -10, -2, 6, 14, 22, 30}) {
            for (int side : new int[]{-1, 1}) {
                int px = side * 9;
                for (int dy = 1; dy <= 25; dy++) {
                    BlockState col = (dy == 1 || dy == 14 || dy == 25)
                            ? AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState()
                            : AbyssContent.ABYSSAL_PILLAR.defaultBlockState();
                    setSafe(level, box, center.offset(px, dy, pz), col);
                }
                // Brazier sconce mounted on each nave pillar
                setSafe(level, box, center.offset(px - side, 5, pz), AbyssContent.CHISELED_ABYSSAL_STONE.defaultBlockState());
                setSafe(level, box, center.offset(px - side, 6, pz), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
            }
            // Transverse pointed arch rib spanning across the nave ceiling (dy = 25..31)
            for (int rx = -9; rx <= 9; rx++) {
                int archY = 25 + (int) Math.round((9 - Math.abs(rx)) * 0.65);
                setSafe(level, box, center.offset(rx, archY, pz), AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
            }
        }

        // Hanging Royal Chandeliers at dz = -6, 10, 26 (or crashed on the floor if the roof above collapsed)
        for (int cz : new int[]{-6, 10, 26}) {
            boolean roofAboveGone = inCollapse(8, cz) || (isSovereignSpireCollapsed() && Math.abs(cz - 6) <= 8);
            if (!roofAboveGone) {
                for (int y = 21; y <= 30; y++) {
                    setSafe(level, box, center.offset(0, y, cz), CHAIN);
                }
                setSafe(level, box, center.offset(0, 20, cz), AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
                for (int arm : new int[]{-2, 2}) {
                    setSafe(level, box, center.offset(arm, 20, cz), AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
                    setSafe(level, box, center.offset(arm, 21, cz), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
                    setSafe(level, box, center.offset(0, 20, cz + arm), AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
                    setSafe(level, box, center.offset(0, 21, cz + arm), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
                }
            }
        }

        // Royal Carpet Aisle from Portal (dz = 34) to Throne Dais (dz = -13)
        for (int dz = -13; dz <= 34; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                setSafe(level, box, center.offset(dx, 0, dz), AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
                BlockState carpet = Math.abs(dx) <= 1
                        ? Blocks.CARPET.pick(DyeColor.RED).defaultBlockState()
                        : Blocks.CARPET.pick(DyeColor.PURPLE).defaultBlockState();
                setSafe(level, box, center.offset(dx, 1, dz), carpet);
            }
        }

        // Monumental Sovereign Throne & Canopy Baldachin (dz = -24 .. -14)
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -22; dz <= -14; dz++) {
                setSafe(level, box, center.offset(dx, 1, dz), AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState());
                if (Math.abs(dx) <= 4 && dz <= -16) {
                    setSafe(level, box, center.offset(dx, 2, dz), AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
                }
                if (Math.abs(dx) <= 3 && dz <= -18) {
                    setSafe(level, box, center.offset(dx, 3, dz), AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState());
                }
            }
        }
        // Throne Seat, Armrests, High Obsidian Back & Golden Canopy (dx = -2..2, dz = -22..-19)
        for (int dx = -2; dx <= 2; dx++) {
            setSafe(level, box, center.offset(dx, 4, -22), Blocks.OBSIDIAN.defaultBlockState());
            for (int y = 5; y <= 10; y++) {
                BlockState back = (dx == 0 && y >= 6 && y <= 8)
                        ? AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState()
                        : Blocks.OBSIDIAN.defaultBlockState();
                setSafe(level, box, center.offset(dx, y, -22), back);
            }
        }
        // Seat cushion & gilded armrests
        setSafe(level, box, center.offset(0, 4, -20), Blocks.WOOL.pick(DyeColor.RED).defaultBlockState());
        setSafe(level, box, center.offset(-1, 4, -20), AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
        setSafe(level, box, center.offset(1, 4, -20), AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
        setSafe(level, box, center.offset(-2, 4, -20), AbyssContent.CHISELED_ABYSSAL_STONE.defaultBlockState());
        setSafe(level, box, center.offset(2, 4, -20), AbyssContent.CHISELED_ABYSSAL_STONE.defaultBlockState());
        // Baldachin Canopy & Flanking Royal Braziers
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -22; dz <= -19; dz++) {
                setSafe(level, box, center.offset(dx, 11, dz), AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
            }
        }
        setSafe(level, box, center.offset(0, 12, -20), AbyssContent.CHISELED_ABYSSAL_STONE.defaultBlockState());
        for (int side : new int[]{-4, 4}) {
            setSafe(level, box, center.offset(side, 3, -17), AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
            setSafe(level, box, center.offset(side, 4, -17), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
        }

        // Secret Behind-Throne Treasury Passage (dz = -25..-23, dx = -5..5, accessible behind the throne)
        fillVolume(level, box, -5, 1, -25, 5, 4, -23, Blocks.AIR.defaultBlockState());
        setSafe(level, box, center.offset(0, 4, -24), hangingLantern(true));

        // Fallen roof tiles, stairs, slabs, and crashed chandeliers on the Throne Hall floor
        buildNaveCollapseRubble(level, box);
    }

    /**
     * Scatters mounds of fallen roof tiles, stairs, slabs, and cracked masonry across the Throne Hall
     * floor directly beneath any collapsed stretch of the Nave roof.
     */
    private void buildNaveCollapseRubble(WorldGenLevel level, BoundingBox box) {
        for (int z = -25; z <= 35; z++) {
            boolean collapsedHere = inCollapse(8, z) || (isSovereignSpireCollapsed() && z >= -2 && z <= 14);
            if (!collapsedHere) continue;

            for (int x = -15; x <= 15; x++) {
                // Keep the royal throne itself recognizable
                if (Math.abs(x) <= 3 && z >= -22 && z <= -18) continue;

                int density = coordMod(x, 8, z, 100);
                if (density >= 58) continue;

                int pileH = density < 12 ? 3 : (density < 30 ? 2 : 1);
                // Step up on top of the throne dais if rubble lands on dz = -22..-14
                int baseY = 1;
                if (Math.abs(x) <= 6 && z >= -22 && z <= -14) {
                    baseY = (Math.abs(x) <= 3 && z <= -18) ? 4 : (Math.abs(x) <= 4 && z <= -16 ? 3 : 2);
                }

                for (int h = 0; h < pileH; h++) {
                    int wy = baseY + h;
                    setSafe(level, box, center.offset(x, wy, z),
                            pickRoofRubbleBlock(x, wy, z, h == pileH - 1));
                }
            }
        }

        // Crashed chandelier remains on the floor for any chandelier whose roof above collapsed
        for (int cz : new int[]{-6, 10, 26}) {
            boolean roofAboveGone = inCollapse(8, cz) || (isSovereignSpireCollapsed() && Math.abs(cz - 6) <= 8);
            if (!roofAboveGone) continue;

            setSafe(level, box, center.offset(0, 1, cz), AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
            setSafe(level, box, center.offset(0, 2, cz), CHAIN);
            setSafe(level, box, center.offset(1, 1, cz), CHAIN);
            setSafe(level, box, center.offset(-2, 1, cz), AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
            setSafe(level, box, center.offset(2, 1, cz + 1), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
        }
    }

    // =========================================================================
    // 6. TWO-STORY SIDE WINGS & 11 INTERCONNECTED SMALLER ROOMS
    // =========================================================================

    private void buildSideWingsAndRooms(WorldGenLevel level, BoundingBox box) {
        // --- WEST WING (dx = -31..-17, dz = -24..32, 2 stories: dy = 1..13 and dy = 14..27) ---
        buildWingShell(level, box, -31, -17, -24, 32, 27, false, 11);

        // Interior partition walls in West Wing at dz = -4 and dz = 16 (both floors)
        buildPartitionWallX(level, box, -30, -17, -4, 1, 26, -24);
        buildPartitionWallX(level, box, -30, -17, 16, 1, 26, -24);

        // West Floor 1 Room A (dz = -23..-5): Grand Arcane Library
        for (int z = -22; z <= -6; z++) {
            if (z % 4 == 0) continue;
            for (int y = 1; y <= 5; y++) {
                setSafe(level, box, center.offset(-30, y, z), Blocks.BOOKSHELF.defaultBlockState());
                if (z != -14) {
                    setSafe(level, box, center.offset(-23, y, z), Blocks.BOOKSHELF.defaultBlockState());
                }
            }
        }
        setSafe(level, box, center.offset(-26, 1, -14), Blocks.LECTERN.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        setSafe(level, box, center.offset(-26, 13, -14), hangingLantern(true));

        // West Floor 1 Room B (dz = -3..15): Enchanting & Alchemy Rotunda
        setSafe(level, box, center.offset(-24, 1, 6), Blocks.ENCHANTING_TABLE.defaultBlockState());
        for (int z = 2; z <= 10; z += 2) {
            setSafe(level, box, center.offset(-28, 1, z), Blocks.BOOKSHELF.defaultBlockState());
            setSafe(level, box, center.offset(-28, 2, z), Blocks.BOOKSHELF.defaultBlockState());
        }
        setSafe(level, box, center.offset(-29, 1, -1), Blocks.BREWING_STAND.defaultBlockState());
        setSafe(level, box, center.offset(-28, 1, -1), Blocks.CAULDRON.defaultBlockState());
        setSafe(level, box, center.offset(-29, 1, 13), AbyssContent.AMETHYST_CORRUPTED_STONE.defaultBlockState());
        setSafe(level, box, center.offset(-29, 2, 13), Blocks.AMETHYST_CLUSTER.defaultBlockState());
        setSafe(level, box, center.offset(-24, 13, 6), hangingLantern(true));

        // West Floor 1 Room C (dz = 17..31): Scribe Study & Staircase to Floor 2
        for (int z = 18; z <= 22; z++) {
            setSafe(level, box, center.offset(-26, 1, z), Blocks.BOOKSHELF.defaultBlockState());
            setSafe(level, box, center.offset(-26, 2, z), Blocks.BOOKSHELF.defaultBlockState());
        }
        setSafe(level, box, center.offset(-20, 1, 20), Blocks.BARREL.defaultBlockState());
        setSafe(level, box, center.offset(-20, 1, 21), Blocks.BARREL.defaultBlockState());
        buildWingStaircase(level, box, -30, -28, 18, 30, 1, 14, Direction.SOUTH);

        // West Floor 2 Room A (dz = -23..-5, dy = 14..26): King's Royal Bedchamber
        // Royal canopy bed at dx = -29..-26, dz = -18..-13
        for (int bx = -29; bx <= -26; bx++) {
            for (int bz = -18; bz <= -13; bz++) {
                setSafe(level, box, center.offset(bx, 15, bz), AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
                setSafe(level, box, center.offset(bx, 16, bz), Blocks.WOOL.pick(DyeColor.RED).defaultBlockState());
            }
        }
        for (int px : new int[]{-29, -26}) {
            for (int pz : new int[]{-18, -13}) {
                for (int y = 17; y <= 20; y++) {
                    setSafe(level, box, center.offset(px, y, pz), AbyssContent.ABYSSAL_PILLAR.defaultBlockState());
                }
            }
        }
        for (int bx = -29; bx <= -26; bx++) {
            for (int bz = -18; bz <= -13; bz++) {
                setSafe(level, box, center.offset(bx, 21, bz), AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
            }
        }
        // Royal hearth fireplace on West wall (dz = -8)
        setSafe(level, box, center.offset(-30, 15, -8), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
        setSafe(level, box, center.offset(-24, 26, -14), hangingLantern(true));

        // West Floor 2 Room B (dz = -3..15, dy = 14..26): War Council Room
        for (int z = 1; z <= 11; z++) {
            setSafe(level, box, center.offset(-24, 15, z), Blocks.DARK_OAK_PLANKS.defaultBlockState());
            if (z % 2 == 1) {
                setSafe(level, box, center.offset(-26, 15, z), oakStairs(Direction.EAST));
                setSafe(level, box, center.offset(-22, 15, z), oakStairs(Direction.WEST));
            }
        }
        setSafe(level, box, center.offset(-24, 26, 6), hangingLantern(true));

        // West Floor 2 Room C (dz = 17..31, dy = 14..26): Astral Observatory
        setSafe(level, box, center.offset(-23, 15, 24), Blocks.CRYING_OBSIDIAN.defaultBlockState());
        setSafe(level, box, center.offset(-23, 16, 24), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
        setSafe(level, box, center.offset(-23, 26, 24), hangingLantern(true));

        // --- EAST WING (dx = 17..31, dz = -24..32, 2 stories: dy = 1..13 and dy = 14..25) ---
        buildWingShell(level, box, 17, 31, -24, 32, 26, true, 12);

        // Interior partition walls in East Wing at dz = -4 and dz = 17
        buildPartitionWallX(level, box, 17, 30, -4, 1, 25, 24);
        buildPartitionWallX(level, box, 17, 30, 17, 1, 25, 24);

        // East Floor 1 Room A (dz = -23..-5): Abyssal Forge & Armory
        setSafe(level, box, center.offset(29, 1, -18), Blocks.BLAST_FURNACE.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        setSafe(level, box, center.offset(29, 1, -16), Blocks.BLAST_FURNACE.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        setSafe(level, box, center.offset(29, 1, -14), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
        setSafe(level, box, center.offset(25, 1, -16), Blocks.ANVIL.defaultBlockState());
        setSafe(level, box, center.offset(25, 1, -14), Blocks.GRINDSTONE.defaultBlockState());
        // Iron-barred armory cage in corner (dx = 26..30, dz = -10..-6)
        for (int x = 26; x <= 30; x++) {
            for (int y = 1; y <= 4; y++) {
                if (x == 28 && y <= 2) continue;
                setSafe(level, box, center.offset(x, y, -10), ironBars(false, false, true, true));
            }
        }
        setSafe(level, box, center.offset(24, 13, -14), hangingLantern(true));

        // East Floor 1 Room B (dz = -3..16): Great Banquet Hall
        for (int z = 0; z <= 13; z++) {
            setSafe(level, box, center.offset(24, 1, z), Blocks.DARK_OAK_PLANKS.defaultBlockState());
            setSafe(level, box, center.offset(22, 1, z), oakStairs(Direction.EAST));
            setSafe(level, box, center.offset(26, 1, z), oakStairs(Direction.WEST));
        }
        setSafe(level, box, center.offset(30, 1, 6), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
        setSafe(level, box, center.offset(24, 13, 6), hangingLantern(true));

        // East Floor 1 Room C (dz = 18..31): Kitchen Pantry & Staircase to Floor 2
        setSafe(level, box, center.offset(20, 1, 20), Blocks.FURNACE.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        setSafe(level, box, center.offset(21, 1, 20), Blocks.CAULDRON.defaultBlockState());
        setSafe(level, box, center.offset(24, 1, 29), Blocks.BARREL.defaultBlockState());
        setSafe(level, box, center.offset(25, 1, 29), Blocks.BARREL.defaultBlockState());
        buildWingStaircase(level, box, 28, 30, 18, 30, 1, 14, Direction.SOUTH);

        // East Floor 2 Room A (dz = -23..-5, dy = 14..25): Royal Guard Barracks
        for (int bz : new int[]{-20, -14, -8}) {
            setSafe(level, box, center.offset(29, 15, bz), Blocks.DARK_OAK_PLANKS.defaultBlockState());
            setSafe(level, box, center.offset(28, 15, bz), Blocks.WOOL.pick(DyeColor.RED).defaultBlockState());
            setSafe(level, box, center.offset(29, 15, bz + 1), Blocks.DARK_OAK_PLANKS.defaultBlockState());
            setSafe(level, box, center.offset(28, 15, bz + 1), Blocks.WOOL.pick(DyeColor.RED).defaultBlockState());
        }
        setSafe(level, box, center.offset(24, 25, -14), hangingLantern(true));

        // East Floor 2 Room B (dz = -3..31, dy = 14..25): Gothic Dark Chapel
        setSafe(level, box, center.offset(24, 15, 28), Blocks.OBSIDIAN.defaultBlockState());
        setSafe(level, box, center.offset(23, 15, 28), AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
        setSafe(level, box, center.offset(25, 15, 28), AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
        setSafe(level, box, center.offset(24, 16, 28), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
        for (int pz : new int[]{4, 8, 12, 20}) {
            for (int px = 21; px <= 26; px++) {
                setSafe(level, box, center.offset(px, 15, pz), oakStairs(Direction.SOUTH));
            }
        }
        setSafe(level, box, center.offset(24, 25, 10), hangingLantern(true));

        // Fallen roof tiles & rubble on Floor 2 (and punching through to Floor 1) under collapsed wing roofs
        buildWingCollapseRubble(level, box, -30, -17, -23, 31, 11);
        buildWingCollapseRubble(level, box, 17, 30, -23, 31, 12);
    }

    /**
     * Scatters fallen roof tiles and masonry on Floor 2 (dy = 15..17) beneath collapsed stretches
     * of a side wing roof, occasionally punching through the Floor 2 slab (dy = 14) onto Floor 1.
     */
    private void buildWingCollapseRubble(WorldGenLevel level, BoundingBox box, int xMin, int xMax,
                                         int zMin, int zMax, int roofSalt) {
        for (int z = zMin; z <= zMax; z++) {
            if (!inCollapse(roofSalt, z)) continue;
            // Skip interior partition wall lines and stairwell steps
            if (z == -4 || z == 16 || z == 17) continue;

            for (int x = xMin; x <= xMax; x++) {
                // Keep wing staircases clear so players can still climb between floors
                if (z >= 18 && z <= 30 && (x <= -28 || x >= 28)) continue;

                int roll = coordMod(x, roofSalt, z, 100);
                if (roll >= 55) continue;

                // ~14% of collapsed roof hits punch a small hole through the Floor 2 slab onto Floor 1
                if (roll < 14 && Math.abs(x) >= 19 && Math.abs(x) <= 29) {
                    setSafe(level, box, center.offset(x, 14, z), Blocks.AIR.defaultBlockState());
                    setSafe(level, box, center.offset(x, 1, z), pickRoofRubbleBlock(x, 1, z, false));
                    if (roll < 6) {
                        setSafe(level, box, center.offset(x, 2, z), pickRoofRubbleBlock(x, 2, z, true));
                    }
                    continue;
                }

                int pileH = roll < 24 ? 2 : 1;
                for (int h = 0; h < pileH; h++) {
                    int wy = 15 + h;
                    setSafe(level, box, center.offset(x, wy, z),
                            pickRoofRubbleBlock(x, wy, z, h == pileH - 1));
                }
            }
        }
    }

    /**
     * Stone cornice slabs that project one block past each wing's outer wall, under the eaves,
     * so the roofline throws a shadow line onto the walls.
     */
    private void buildWingCornices(WorldGenLevel level, BoundingBox box) {
        for (int dz = -25; dz <= 33; dz++) {
            setSafe(level, box, center.offset(-32, 27, dz), AbyssContent.ABYSSAL_BRICK_SLAB.defaultBlockState());
            if (dz < 25) {
                setSafe(level, box, center.offset(32, 26, dz), AbyssContent.ABYSSAL_BRICK_SLAB.defaultBlockState());
            }
        }
    }

    private void buildWingShell(WorldGenLevel level, BoundingBox box, int xMin, int xMax, int zMin, int zMax,
                                int wallTopY, boolean weatheredEastEdge, int roofSalt) {
        for (int dy = 1; dy <= wallTopY; dy++) {
            for (int dx = xMin; dx <= xMax; dx++) {
                for (int dz = zMin; dz <= zMax; dz++) {
                    boolean isOuterX = (dx == xMin || dx == xMax);
                    boolean isOuterZ = (dz == zMin || dz == zMax);

                    // Atmospheric weathered ruin notch on East Wing upper SE corner
                    if (weatheredEastEdge && dy > 19 && dx >= 27 && dz >= 24
                            && ((dx * 7 + dz * 13) % 5 < (dy - 19))) {
                        continue;
                    }

                    if (isOuterX || isOuterZ) {
                        // Courtyard doors at North & South ends of each wing
                        if (isOuterZ && dy >= 1 && dy <= 4 && Math.abs(dx - (xMin + xMax) / 2) <= 1) {
                            continue;
                        }
                        // Outer lancet windows every 6 blocks: dark glass, some lit from within
                        if ((dx == -31 || dx == 31) && Math.abs(dz % 6) == 0
                                && ((dy >= 4 && dy <= 8) || (dy >= 17 && dy <= 21))) {
                            setSafe(level, box, center.offset(dx, dy, dz), windowBlock(dx, dy <= 8 ? 0 : 1, dz));
                            continue;
                        }
                        setSafe(level, box, center.offset(dx, dy, dz), pickWallMasonry(dx, dy, dz));
                    } else if (dy == 14) {
                        setSafe(level, box, center.offset(dx, dy, dz), AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState());
                    }
                }
            }
        }

        // Sloped Gothic Tile Roof over each Side Wing, possibly collapsed
        int width = xMax - xMin;
        int halfSpan = width / 2;
        for (int step = 0; step <= halfSpan; step++) {
            int dy = wallTopY + 1 + step;
            for (int dz = zMin; dz <= zMax; dz++) {
                if (weatheredEastEdge && dz >= 25 && step >= 3) continue;
                setRoof(level, box, xMin + step, dy, dz, stairs(Direction.EAST, Half.BOTTOM), roofSalt, wallTopY);
                setRoof(level, box, xMax - step, dy, dz, stairs(Direction.WEST, Half.BOTTOM), roofSalt, wallTopY);
            }
        }
    }

    private void buildPartitionWallX(WorldGenLevel level, BoundingBox box, int xMin, int xMax, int z,
                                     int yMin, int yMax, int doorX) {
        for (int x = xMin + 1; x <= xMax - 1; x++) {
            for (int y = yMin; y <= yMax; y++) {
                if (y == 14) continue; // Keep floor slab intact
                boolean isDoor = Math.abs(x - doorX) <= 1 && ((y >= 1 && y <= 4) || (y >= 15 && y <= 18));
                if (isDoor) continue;
                setSafe(level, box, center.offset(x, y, z), pickWallMasonry(x, y, z));
            }
        }
    }

    private void buildWingStaircase(WorldGenLevel level, BoundingBox box, int xMin, int xMax, int zStart, int zEnd,
                                    int yStart, int yEnd, Direction climbDir) {
        int steps = yEnd - yStart;
        for (int s = 0; s <= steps; s++) {
            int sy = yStart + s;
            int sz = zStart + s;
            if (sz > zEnd) break;
            for (int x = xMin; x <= xMax; x++) {
                // Carve stairwell opening through the dy = 14 floor slab
                for (int clearY = sy + 1; clearY <= yEnd + 4; clearY++) {
                    setSafe(level, box, center.offset(x, clearY, sz), Blocks.AIR.defaultBlockState());
                }
                setSafe(level, box, center.offset(x, sy, sz), stairs(climbDir, Half.BOTTOM));
            }
        }
    }

    // =========================================================================
    // 7. TOWERING GOTHIC NEEDLE SPIRES, FLYING ARCHES & SOVEREIGN SPIRE
    // =========================================================================

    private void buildAllTowersAndSpires(WorldGenLevel level, BoundingBox box) {
        // 8 Perimeter & Gatehouse Towers: {cx, cz, shaftHeight, spireHeight, ruined, canFall}
        int[][] towers = {
                {-36, -42, 44, 18, 0, 1}, // NW Corner
                { 36, -42, 42, 18, 0, 1}, // NE Corner
                {-36,  54, 40, 16, 0, 1}, // SW Corner
                { 36,  54, 32,  0, 1, 1}, // SE Corner (partially collapsed ruin)
                {-10,  54, 48, 20, 0, 0}, // South Gatehouse West
                { 10,  54, 48, 20, 0, 0}, // South Gatehouse East
                {-36,   6, 38, 16, 0, 1}, // West Mid Bastion
                { 36,   6, 28,  0, 1, 1}  // East Mid Bastion (partially collapsed ruin)
        };

        for (int[] t : towers) {
            buildGothicSpireTower(level, box, t[0], t[1], 5, t[2], t[3], t[4] == 1, true, t[5] == 1);
        }

        // Twin Cathedral Needle Towers at dx = +-14, dz = 38 (heights 98 and 106); they never lean or fall,
        // because their glowing windows are placed on fixed coordinates
        buildGothicSpireTower(level, box, -14, 38, 5, 72, 26, false, false, false);
        buildGothicSpireTower(level, box, 14, 38, 5, 78, 28, false, false, false);

        // Glowing lancet windows & interior beacons inside the Twin Nave Towers: always lit, the focal point
        for (int tx : new int[]{-14, 14}) {
            for (int wy : new int[]{46, 60}) {
                for (int h = 0; h < 6; h++) {
                    BlockState fill = (h == 0 || h == 5)
                            ? AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState()
                            : Blocks.SHROOMLIGHT.defaultBlockState();
                    setSafe(level, box, center.offset(tx, wy + h, 43), fill);
                }
                setSafe(level, box, center.offset(tx, wy + 2, 41), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
            }
        }

        // Flying arches: each runs from the curtain wall over the wing roofs up to the nave clerestory
        for (int bz : new int[]{-18, -6, 6, 18}) {
            for (int side : new int[]{-1, 1}) {
                buildFlyingArch(level, box, side, bz);
            }
        }

        // Sovereign Central Needle Spire (dy = 44..122) centered at (0, 6) - collapses if nave roof below falls
        buildSovereignCentralSpire(level, box);
    }

    /**
     * One flying buttress: a two-block-wide arch from the curtain top (x = 36, y = 18) rising over the
     * wing roof to the nave wall (x = 16, y = 30). Each column is joined to the previous one so the
     * arch stays continuous even where it is steep.
     */
    private void buildFlyingArch(WorldGenLevel level, BoundingBox box, int side, int bz) {
        int prevY = Integer.MIN_VALUE;
        for (int step = 0; step <= 20; step++) {
            double t = step / 20.0;
            int ax = side * (36 - step);
            int ay = (int) Math.round(18 + 12 * t + 24 * Math.sin(Math.PI * t));
            int from = prevY == Integer.MIN_VALUE ? ay : Math.min(prevY, ay);
            int to = prevY == Integer.MIN_VALUE ? ay : Math.max(prevY, ay);
            BlockState arch = (step == 10)
                    ? AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState()
                    : AbyssContent.ABYSSAL_BRICKS.defaultBlockState();
            for (int y = from - 1; y <= to; y++) {
                setSafe(level, box, center.offset(ax, y, bz), arch);
                setSafe(level, box, center.offset(ax, y, bz + 1), arch);
            }
            prevY = ay;
        }
    }

    /**
     * Tower with a setback: the lower shaft is full radius, a cornice ring of slabs projects one block
     * at the step, and the upper section is one block narrower. Sound towers taper into a needle spire.
     * Some perimeter towers lean sideways; others snap near the base and lie toppled across the ground.
     */
    private void buildGothicSpireTower(WorldGenLevel level, BoundingBox box,
                                       int cx, int cz, int radius, int shaftHeight, int spireHeight,
                                       boolean ruined, boolean canLean, boolean canFall) {
        // Check if this perimeter tower has toppled onto the courtyard ground
        if (canFall && isTowerFallen(cx, cz)) {
            buildFallenTower(level, box, cx, cz, radius, shaftHeight, spireHeight, ruined);
            return;
        }

        int setbackY = ruined ? Integer.MAX_VALUE : shaftHeight - 6;

        // Lean: chosen per tower from its position and the seed, 2..5 blocks at the top
        int leanX = 0, leanZ = 0;
        if (canLean && !ruined && coordMod(cx, 9, cz, 100) < LEAN_CHANCE) {
            int amt = 2 + coordMod(cx, 10, cz, 4);
            int dir = coordMod(cx, 11, cz, 4);
            leanX = dir == 0 ? amt : dir == 1 ? -amt : 0;
            leanZ = dir == 2 ? amt : dir == 3 ? -amt : 0;
        }

        for (int dy = 1; dy <= shaftHeight; dy++) {
            // Centre of this layer drifts linearly from the base up to the top
            int lx = cx + (int) Math.round(leanX * dy / (double) shaftHeight);
            int lz = cz + (int) Math.round(leanZ * dy / (double) shaftHeight);

            // Cornice: slabs one block outside the lower shaft, at the setback
            if (dy == setbackY) {
                for (int dx = -(radius + 1); dx <= radius + 1; dx++) {
                    for (int dz = -(radius + 1); dz <= radius + 1; dz++) {
                        int d2 = dx * dx + dz * dz;
                        if (d2 > radius * radius + 2 && d2 <= (radius + 1) * (radius + 1) + 2) {
                            setSafe(level, box, center.offset(lx + dx, dy, lz + dz),
                                    AbyssContent.ABYSSAL_BRICK_SLAB.defaultBlockState());
                        }
                    }
                }
            }

            int r = dy > setbackY ? radius - 1 : radius;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int distSq = dx * dx + dz * dz;
                    if (distSq > r * r + 2) continue;

                    if (ruined && dy > shaftHeight - 8 && ((dx * 11 + dz * 17 + dy) % 7 < (dy - (shaftHeight - 8)))) {
                        continue;
                    }

                    boolean outerRing = distSq >= (r - 1) * (r - 1);
                    BlockPos pos = center.offset(lx + dx, dy, lz + dz);

                    if (outerRing) {
                        boolean cardinal = (dx == 0 || dz == 0);
                        // Ground-level and battlement-level doorways into towers
                        boolean groundDoor = (dy >= 1 && dy <= 4) && cardinal;
                        boolean battlementDoor = !ruined && (dy >= 17 && dy <= 20) && cardinal;
                        if (groundDoor || battlementDoor) continue;

                        if (!ruined && dy == shaftHeight) {
                            setSafe(level, box, pos, AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
                        } else if (!ruined && dy == shaftHeight - 1) {
                            setSafe(level, box, pos, AbyssContent.CHISELED_ABYSSAL_STONE.defaultBlockState());
                        } else if (cardinal && dy >= 6 && dy < shaftHeight - 4 && (dy - 6) % 5 < 3) {
                            // Lancet windows on the four cardinal faces: dark glass, some lit from within
                            setSafe(level, box, pos, windowBlock(lx + dx, (dy - 6) / 5, lz + dz));
                        } else {
                            setSafe(level, box, pos, pickWallMasonry(lx + dx, dy, lz + dz));
                        }
                    } else if (dy % 14 == 0 && !ruined) {
                        // Intermediate tower room floors
                        setSafe(level, box, pos, AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState());
                    }
                }
            }

            // Central spiral staircase spine
            if (!ruined || dy < shaftHeight - 6) {
                setSafe(level, box, center.offset(lx, dy, lz), AbyssContent.ABYSSAL_PILLAR.defaultBlockState());
                double angle = (dy * Math.PI) / 4.0;
                int sx = lx + (int) Math.round(Math.cos(angle) * 2.0);
                int sz = lz + (int) Math.round(Math.sin(angle) * 2.0);
                setSafe(level, box, center.offset(sx, dy, sz), AbyssContent.ABYSSAL_BRICK_SLAB.defaultBlockState());
            }
        }

        if (ruined || spireHeight <= 0) return;

        // Crown: the centre of the top layer, which carries the lean
        int tx = cx + leanX;
        int tz = cz + leanZ;

        // 4 Corner Gothic Pinnacles around the crown of the tower (on the upper section's corners)
        int pOff = radius - 1;
        for (int px : new int[]{-pOff, pOff}) {
            for (int pz : new int[]{-pOff, pOff}) {
                for (int py = 1; py <= 6; py++) {
                    setSafe(level, box, center.offset(tx + px, shaftHeight + py, tz + pz),
                            py <= 4 ? AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState()
                                    : AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
                }
            }
        }

        // Taller, needle-like conical roof. Base sits on the upper section with a small eave;
        // the radius pinches faster toward the tip so the spire reads as a thin point.
        double baseR = radius - 0.5;
        int coneSteps = Math.max(10, spireHeight);
        for (int step = 0; step <= coneSteps; step++) {
            int dy = shaftHeight + 1 + step;
            double progress = step / (double) coneSteps;
            double coneR = Math.max(0.0, baseR * Math.pow(1.0 - progress, 1.5));
            int rInt = (int) Math.ceil(coneR);
            for (int dx = -rInt; dx <= rInt; dx++) {
                for (int dz = -rInt; dz <= rInt; dz++) {
                    if (dx * dx + dz * dz <= coneR * coneR + 0.25) {
                        BlockState roofBlock = (step % 5 == 0)
                                ? AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState()
                                : AbyssContent.ABYSSAL_TILES.defaultBlockState();
                        setSafe(level, box, center.offset(tx + dx, dy, tz + dz), roofBlock);
                    }
                }
            }
        }
        for (int f = 1; f <= 5; f++) {
            int dy = shaftHeight + 1 + coneSteps + f;
            setSafe(level, box, center.offset(tx, dy, tz),
                    f <= 2 ? AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState());
        }
    }

    /** Deterministically decides if an eligible perimeter tower has fallen over. */
    private boolean isTowerFallen(int cx, int cz) {
        return coordMod(cx, 30, cz, 100) < TOWER_FALL_CHANCE;
    }

    /**
     * Picks a normalized horizontal unit vector {ux, uz} along which a fallen tower lies,
     * preferring directions over the solid courtyard plateau so the fallen trunk rests on the ground.
     */
    private double[] getTowerFallVector(int cx, int cz) {
        double[][] candidates = {
                { 1.0,  0.0},
                {-1.0,  0.0},
                { 0.0,  1.0},
                { 0.0, -1.0},
                { 0.70710678,  0.70710678},
                {-0.70710678,  0.70710678},
                { 0.70710678, -0.70710678},
                {-0.70710678, -0.70710678}
        };
        List<double[]> valid = new ArrayList<>();
        for (double[] dir : candidates) {
            boolean ok = true;
            for (int dist : new int[]{12, 20, 28}) {
                int tx = cx + (int) Math.round(dir[0] * dist);
                int tz = cz + (int) Math.round(dir[1] * dist);
                if (!hasFoundationAtSurface(tx, tz) || isInsideProtectedBuilding(tx, tz)) {
                    ok = false;
                    break;
                }
            }
            if (ok) valid.add(dir);
        }
        if (valid.isEmpty()) {
            // Fallback pointing straight toward the castle center
            double vx = -cx;
            double vz = 6.0 - cz;
            double len = Math.max(1.0, Math.hypot(vx, vz));
            return new double[]{vx / len, vz / len};
        }
        return valid.get(coordMod(cx, 32, cz, valid.size()));
    }

    /**
     * Builds a jagged snapped stump at (cx, cz) plus the fallen horizontal tower cylinder,
     * cornice, spire, and crushed ground debris lying across the courtyard.
     */
    private void buildFallenTower(WorldGenLevel level, BoundingBox box,
                                  int cx, int cz, int radius, int shaftHeight, int spireHeight, boolean ruined) {
        double[] fallVec = getTowerFallVector(cx, cz);
        double ux = fallVec[0];
        double uz = fallVec[1];
        double px = -uz;
        double pz = ux;

        int stumpHeight = 6 + coordMod(cx, 31, cz, 5); // 6..10 blocks tall

        // 1. Standing Jagged Stump
        for (int dy = 1; dy <= stumpHeight + 3; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int distSq = dx * dx + dz * dz;
                    if (distSq > radius * radius + 2) continue;

                    // Slanted tear plane: higher on the side opposite the fall direction
                    double alongDir = dx * ux + dz * uz;
                    int localMaxY = stumpHeight
                            - (int) Math.round(alongDir * 0.45)
                            + (coordMod(cx + dx, 33, cz + dz, 3) - 1);
                    if (dy > localMaxY) continue;

                    boolean outerRing = distSq >= (radius - 1) * (radius - 1);
                    BlockPos pos = center.offset(cx + dx, dy, cz + dz);

                    if (outerRing) {
                        boolean cardinal = (dx == 0 || dz == 0);
                        if (dy >= 1 && dy <= 4 && cardinal) continue; // Keep ground doorway open
                        setSafe(level, box, pos, pickWallMasonry(cx + dx, dy, cz + dz));
                    } else if (dy == 1 && coordMod(cx + dx, 1, cz + dz, 100) < 35) {
                        // Fallen stone debris inside the open stump
                        setSafe(level, box, pos, pickRubbleBlock(cx + dx, 1, cz + dz, true));
                    }
                }
            }
            if (dy <= stumpHeight - 1) {
                setSafe(level, box, center.offset(cx, dy, cz), AbyssContent.ABYSSAL_PILLAR.defaultBlockState());
            }
        }

        // 2. Horizontal Fallen Tower Trunk & Spire embedded in the courtyard ground
        double startDist = radius + 2.0;
        double fallenShaftLen = Math.max(14.0, shaftHeight - stumpHeight);
        double fallenSpireLen = (ruined || spireHeight <= 0) ? 0.0 : spireHeight;
        double totalFallenLen = fallenShaftLen + fallenSpireLen;

        // Determine maximum travel distance before hitting a major building or cliff edge
        double maxReach = totalFallenLen;
        for (double s = 0.0; s <= totalFallenLen; s += 0.5) {
            int testX = cx + (int) Math.round(ux * (startDist + s));
            int testZ = cz + (int) Math.round(uz * (startDist + s));
            if (isInsideProtectedBuilding(testX, testZ) || !hasFoundationAtSurface(testX, testZ)) {
                maxReach = Math.max(6.0, s - 2.0);
                break;
            }
        }

        int reachRadius = (int) Math.ceil(startDist + maxReach + radius + 5);
        int minWx = cx - reachRadius;
        int maxWx = cx + reachRadius;
        int minWz = cz - reachRadius;
        int maxWz = cz + reachRadius;

        for (int wx = minWx; wx <= maxWx; wx++) {
            for (int wz = minWz; wz <= maxWz; wz++) {
                if (isInsideProtectedBuilding(wx, wz)) continue;

                double relX = wx - cx;
                double relZ = wz - cz;
                double along = relX * ux + relZ * uz;
                double perp = relX * px + relZ * pz;
                double s = along - startDist;

                if (s < -1.5 || s > maxReach + 4.0) continue;

                // Impact rubble heap if the tower crashed against a wing or cliff edge
                if (s > maxReach) {
                    if (Math.abs(perp) <= radius + 2.5) {
                        double crashFactor = 1.0 - ((s - maxReach) / 4.0);
                        int heapH = (int) Math.round(crashFactor * 4.0) + coordMod(wx, 34, wz, 2);
                        for (int y = 1; y <= heapH; y++) {
                            setSafe(level, box, center.offset(wx, y, wz), pickRubbleBlock(wx, y, wz, y == heapH));
                        }
                    }
                    continue;
                }

                // Compute horizontal cylinder radius and vertical center height (sunken into ground)
                double r;
                double cy;
                boolean isSpire = s > fallenShaftLen;
                double setbackS = fallenShaftLen - 6.0;

                if (!isSpire) {
                    r = (!ruined && s > setbackS) ? (radius - 1.0) : radius;
                    cy = 3.0; // Bottom arc embedded at dy = 0..1; upper crown rises to dy = 7..8
                } else {
                    double progress = (s - fallenShaftLen) / Math.max(10.0, fallenSpireLen);
                    r = Math.max(0.0, (radius - 0.5) * Math.pow(1.0 - progress, 1.5));
                    cy = 3.0 - (1.8 * progress); // Slopes down so needle tip rests on dy = 1
                }

                // Cornice slab ring around the setback step
                if (!ruined && Math.abs(s - setbackS) <= 0.75) {
                    for (int dy = 1; dy <= 9; dy++) {
                        double dY = dy - cy;
                        double radialDist = Math.hypot(perp, dY);
                        if (radialDist > radius - 0.2 && radialDist <= radius + 1.35) {
                            if (dY > 1.5 && coordMod(wx, dy, wz, 100) < 45) continue;
                            setSafe(level, box, center.offset(wx, dy, wz), AbyssContent.ABYSSAL_BRICK_SLAB.defaultBlockState());
                        }
                    }
                    continue;
                }

                int maxDy = Math.max(3, (int) Math.ceil(cy + r + 1.0));
                for (int dy = 1; dy <= maxDy; dy++) {
                    double dY = dy - cy;
                    double radialDist = Math.hypot(perp, dY);

                    // Jagged snapped fracture edge at the base of the fallen trunk
                    if (s < 2.2) {
                        double minBreakS = 0.4 + (dY * 0.22) + (coordMod(wx, dy, wz, 3) * 0.55);
                        if (s < minBreakS) continue;
                    }

                    if (radialDist <= r + 0.35) {
                        if (!isSpire) {
                            boolean outerShell = radialDist >= (r - 1.35);
                            boolean centralSpine = radialDist <= 1.15;

                            if (outerShell) {
                                // Shatter open ~42% of the upper arch so players can see & walk inside the fallen tower
                                if (dY > 1.2 && coordMod(wx, dy, wz, 100) < 42) {
                                    setSafe(level, box, center.offset(wx, dy, wz), Blocks.AIR.defaultBlockState());
                                    continue;
                                }
                                BlockState shellBlock;
                                if (!ruined && Math.abs(s - fallenShaftLen) <= 1.2) {
                                    shellBlock = AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState();
                                } else {
                                    shellBlock = pickWallMasonry(wx, dy, wz);
                                }
                                setSafe(level, box, center.offset(wx, dy, wz), shellBlock);
                            } else if (centralSpine && (!ruined || s < fallenShaftLen - 6.0)) {
                                setSafe(level, box, center.offset(wx, dy, wz), AbyssContent.ABYSSAL_PILLAR.defaultBlockState());
                            } else {
                                setSafe(level, box, center.offset(wx, dy, wz), Blocks.AIR.defaultBlockState());
                                if (dy == 1 && coordMod(wx, 35, wz, 100) < 30) {
                                    setSafe(level, box, center.offset(wx, 1, wz), pickRubbleBlock(wx, 1, wz, true));
                                }
                            }
                        } else {
                            // Fallen conical spire
                            double progress = (s - fallenShaftLen) / Math.max(10.0, fallenSpireLen);
                            if (progress > 0.65 && coordMod(wx, dy, wz, 100) < 45) continue;
                            BlockState spireBlock = ((int) Math.round(s) % 5 == 0)
                                    ? AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState()
                                    : AbyssContent.ABYSSAL_TILES.defaultBlockState();
                            setSafe(level, box, center.offset(wx, dy, wz), spireBlock);
                        }
                    } else if (dy <= 3 && radialDist <= r + 3.2 && s >= 0.0) {
                        // Ground impact debris skirt flanking both sides of the fallen cylinder
                        double edgeDist = radialDist - r;
                        int skirtH = edgeDist <= 1.3 ? 2 : 1;
                        if (dy <= skirtH && coordMod(wx, dy, wz, 100) < (edgeDist <= 1.3 ? 65 : 35)) {
                            setSafe(level, box, center.offset(wx, dy, wz), pickRubbleBlock(wx, dy, wz, dy == skirtH));
                        }
                    }
                }
            }
        }
    }

    /** Protects major interior wings, throne hall, stairwells, and fixed towers from fallen rubble clipping. */
    private boolean isInsideProtectedBuilding(int dx, int dz) {
        // Royal Throne Hall & Nave
        if (Math.abs(dx) <= 16 && dz >= -26 && dz <= 36) return true;
        // West & East Two-Story Wings
        if (dx >= -31 && dx <= -17 && dz >= -24 && dz <= 32) return true;
        if (dx >= 17 && dx <= 31 && dz >= -24 && dz <= 32) return true;
        // Dungeon Surface Stairwell Parapet
        if (dx >= 17 && dx <= 23 && dz >= 35 && dz <= 48) return true;
        // South Gatehouse Towers
        if (Math.hypot(dx + 10, dz - 54) <= 6.5 || Math.hypot(dx - 10, dz - 54) <= 6.5) return true;
        // Twin Cathedral Nave Towers
        if (Math.hypot(dx + 14, dz - 38) <= 6.5 || Math.hypot(dx - 14, dz - 38) <= 6.5) return true;
        return false;
    }

    /**
     * True when the Nave roof (salt 8) has collapsed beneath the Sovereign Central Spire (cz = 6, dz = -1..13),
     * meaning the central spire cannot remain standing in mid-air and has crashed into the Throne Hall.
     */
    private boolean isSovereignSpireCollapsed() {
        for (int z = -1; z <= 13; z++) {
            if (inCollapse(8, z)) return true;
        }
        return false;
    }

    private void buildSovereignCentralSpire(WorldGenLevel level, BoundingBox box) {
        if (isSovereignSpireCollapsed()) {
            buildCollapsedSovereignSpireRubble(level, box);
            return;
        }

        int cz = 6;
        // Octagonal Sanctum & High Belfry (dy = 44..82)
        for (int dy = 44; dy <= 82; dy++) {
            for (int dx = -7; dx <= 7; dx++) {
                for (int dz = -7; dz <= 7; dz++) {
                    int d2 = dx * dx + dz * dz;
                    if (d2 > 52) continue;
                    BlockPos pos = center.offset(dx, dy, cz + dz);

                    if (dy == 44 || dy == 64) {
                        setSafe(level, box, pos, AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
                    } else if (dy >= 65 && dy <= 79) {
                        // Open Gothic Belfry with 8 slender columns & hanging brazier lantern
                        if ((Math.abs(dx) == 5 && Math.abs(dz) == 3) || (Math.abs(dx) == 3 && Math.abs(dz) == 5)) {
                            setSafe(level, box, pos, AbyssContent.ABYSSAL_PILLAR.defaultBlockState());
                        } else if (dx == 0 && dz == 0 && dy >= 71) {
                            setSafe(level, box, pos, dy == 71
                                    ? AbyssContent.ABYSSAL_BRAZIER.defaultBlockState()
                                    : CHAIN);
                        }
                    } else if (d2 >= 36) {
                        boolean cardinal = (dx == 0 || dz == 0);
                        if (cardinal && dy >= 46 && dy <= 62 && (dy - 46) % 6 < 3) {
                            // Sanctum lancets: dark glass, some lit from within
                            setSafe(level, box, pos, windowBlock(dx, (dy - 46) / 6, dz));
                        } else {
                            setSafe(level, box, pos, pickWallMasonry(dx, dy, cz + dz));
                        }
                    }
                }
            }
        }

        // 4 Flying Corner Pinnacles (dy = 80..93)
        for (int px : new int[]{-5, 5}) {
            for (int pz : new int[]{-5, 5}) {
                for (int py = 80; py <= 93; py++) {
                    setSafe(level, box, center.offset(px, py, cz + pz),
                            py <= 89 ? AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState()
                                     : AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
                }
            }
        }

        // Soaring Needle-Sharp Conical Roof (dy = 83..112) + 10-Block Finial (dy = 113..122)
        for (int step = 0; step <= 29; step++) {
            int dy = 83 + step;
            double r = Math.max(0.0, 6.8 * Math.pow(1.0 - (step / 29.0), 1.4));
            int rInt = (int) Math.ceil(r);
            for (int dx = -rInt; dx <= rInt; dx++) {
                for (int dz = -rInt; dz <= rInt; dz++) {
                    if (dx * dx + dz * dz <= r * r + 0.25) {
                        BlockState tile = (step % 6 == 0)
                                ? AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState()
                                : AbyssContent.ABYSSAL_TILES.defaultBlockState();
                        setSafe(level, box, center.offset(dx, dy, cz + dz), tile);
                    }
                }
            }
        }
        for (int dy = 113; dy <= 122; dy++) {
            setSafe(level, box, center.offset(0, dy, cz),
                    dy <= 116 ? AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState());
        }
    }

    /**
     * Builds the massive wreckage mound of the Sovereign Central Spire inside the Royal Throne Hall
     * directly below the collapsed nave roof, including shattered sanctum rings, fallen belfry pillars,
     * and the toppled needle spire with its obsidian finial.
     */
    private void buildCollapsedSovereignSpireRubble(WorldGenLevel level, BoundingBox box) {
        int cz = 6;

        // 1. Crashed Octagonal Sanctum & Belfry Mound (dx = -8..8, dz = -2..14, dy = 1..6)
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = cz - 8; dz <= cz + 8; dz++) {
                double dist = Math.hypot(dx, dz - cz);
                if (dist > 8.2) continue;

                double norm = dist / 8.2;
                int jag = coordMod(dx, 40, dz, 3) - 1;
                int moundH = Math.max(1, (int) Math.round((1.0 - norm * norm) * 5.5) + jag);

                for (int y = 1; y <= moundH; y++) {
                    int roll = coordMod(dx, y + 40, dz, 100);
                    BlockState state;
                    if (y == moundH && roll < 30) {
                        state = AbyssContent.ABYSSAL_TILE_SLAB.defaultBlockState();
                    } else if (roll < 20) {
                        state = AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState();
                    } else if (roll < 34) {
                        state = AbyssContent.ABYSSAL_PILLAR.defaultBlockState();
                    } else if (roll < 62) {
                        state = AbyssContent.CRACKED_ABYSSAL_BRICKS.defaultBlockState();
                    } else if (roll < 82) {
                        state = AbyssContent.ABYSSAL_TILES.defaultBlockState();
                    } else {
                        state = AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState();
                    }
                    setSafe(level, box, center.offset(dx, y, dz), state);
                }

                // Scattered broken belfry lantern & chain remnants on top of the mound
                if (dx == 0 && dz == cz + 2) {
                    setSafe(level, box, center.offset(dx, moundH + 1, dz), AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
                } else if (Math.abs(dx) == 2 && dz == cz && coordMod(dx, 42, dz, 2) == 0) {
                    setSafe(level, box, center.offset(dx, moundH + 1, dz), CHAIN);
                }
            }
        }

        // 2. Toppled Conical Needle Spire & Obsidian Finial lying southward along the Throne Hall aisle (dz = 13..27)
        int lateralDrift = coordMod(6, 43, 0, 5) - 2; // -2..+2 blocks X offset
        for (int step = 0; step <= 14; step++) {
            int wz = 13 + step;
            double progress = step / 14.0;
            double tipX = lateralDrift * progress;
            double r = Math.max(0.5, 4.2 * Math.pow(1.0 - progress, 1.35));
            double cy = 1.5 - (0.7 * progress);

            int minX = (int) Math.floor(tipX - r - 1);
            int maxX = (int) Math.ceil(tipX + r + 1);
            int maxY = Math.max(2, (int) Math.ceil(cy + r));

            for (int wx = minX; wx <= maxX; wx++) {
                for (int dy = 1; dy <= maxY; dy++) {
                    double d = Math.hypot(wx - tipX, dy - cy);
                    if (d <= r + 0.25) {
                        if (dy > cy + 0.8 && coordMod(wx, dy, wz, 100) < 35) continue;
                        BlockState spireState;
                        if (step >= 11) {
                            spireState = Blocks.OBSIDIAN.defaultBlockState();
                        } else if (step % 4 == 0) {
                            spireState = AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState();
                        } else if (dy == maxY && coordMod(wx, 44, wz, 100) < 45) {
                            spireState = stairs(Direction.NORTH, Half.BOTTOM);
                        } else {
                            spireState = AbyssContent.ABYSSAL_TILES.defaultBlockState();
                        }
                        setSafe(level, box, center.offset(wx, dy, wz), spireState);
                    }
                }
            }
        }
    }



    // =========================================================================
    // 8. MONUMENTAL SOUTH VIADUCT (dz = 55..96)
    // =========================================================================

    private void buildSouthViaduct(WorldGenLevel level, BoundingBox box) {
        int yMinLimit = Math.max(level.getMinY() + 2, -254);

        for (int dz = 55; dz <= 96; dz++) {
            boolean isPierZ = (dz == 56 || dz == 66 || dz == 76 || dz == 86 || dz == 95);
            for (int dx = -6; dx <= 6; dx++) {
                BlockState deckBlock = Math.abs(dx) <= 1
                        ? AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState()
                        : AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState();
                setSafe(level, box, center.offset(dx, 0, dz), deckBlock);

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

                if (isPierZ && Math.abs(dx) >= 3) {
                    int solidDepth = 0;
                    for (int d = 1; d <= BELOW_FLOOR_DEPTH; d++) {
                        int wy = center.getY() - d;
                        if (wy < yMinLimit) break;
                        BlockPos pPos = new BlockPos(center.getX() + dx, wy, center.getZ() + dz);
                        if (!box.isInside(pPos.getX(), center.getY(), pPos.getZ())) continue;
                        BlockState existing = level.getBlockState(pPos);
                        if (existing.isSolidRender() && !existing.is(Blocks.WATER) && !existing.is(Blocks.LAVA)) {
                            solidDepth++;
                            if (solidDepth >= 4) break;
                        } else {
                            solidDepth = 0;
                        }
                        setSafe(level, box, pPos, pickWallMasonry(dx, -d, dz));
                    }
                } else if (!isPierZ) {
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

    // =========================================================================
    // 9. CHEST PLACEMENT & LOOT TABLES (44 VALIDATED CHESTS)
    // =========================================================================

    private void placeAllCastleChests(WorldGenLevel level, BoundingBox box) {
        List<ChestSpec> specs = new ArrayList<>();

        // 22 Chests: simpleskills:chests/abyssal_castle (Towers, Courtyard, Forge, Barracks, Dungeons & Sewers)
        int[][] castleCoords = {
                {-36, 1, -42}, {-36, 15, -42}, {36, 1, -42},  {36, 15, -42},
                {-36, 1, 54},  {-36, 15, 54},  {36, 1, 54},   {-10, 1, 54},
                {10, 1, 54},   {-36, 1, 6},    {-36, 15, 6},  {36, 1, 6},
                {29, 1, -8},   {29, 1, 29},    {22, 15, -20}, {22, 15, -8},
                {-14, -10, -12},{14, -10, -4}, {-14, -10, 12},{-26, -10, 34},
                {26, -10, 20}, {-22, -20, -28}
        };
        for (int[] c : castleCoords) {
            int cx = c[0];
            int cy = c[1];
            int cz = c[2];
            // If an upper-story perimeter tower chest belongs to a fallen tower, relocate it inside the fallen trunk
            if (cy == 15 && (Math.abs(cx) == 36) && isTowerFallen(cx, cz)) {
                double[] fallVec = getTowerFallVector(cx, cz);
                cx = cx + (int) Math.round(fallVec[0] * 14.0);
                cz = cz + (int) Math.round(fallVec[1] * 14.0);
                cy = 2;
            }
            specs.add(new ChestSpec(cx, cy, cz, LOOT_CASTLE));
        }

        // 10 Chests: simpleskills:chests/abyssal_castle_library (Library, Alchemy Rotunda, Scribe Study, War Council, Observatory, Twin Towers)
        int[][] libraryCoords = {
                {-29, 1, -20}, {-24, 1, -8},  {-29, 1, 10},  {-20, 1, 29},
                {-28, 15, 6},  {-28, 15, 28}, {-14, 29, 38}, {-14, 57, 38},
                {14, 29, 38},  {14, 57, 38}
        };
        for (int[] c : libraryCoords) {
            specs.add(new ChestSpec(c[0], c[1], c[2], LOOT_LIBRARY));
        }

        // 12 Chests: simpleskills:chests/abyssal_castle_sanctum (Throne Dais, Secret Behind-Throne Vault, Royal Bedroom, Chapel, Twin Belfries, Sovereign Spire, Relic Crypt)
        int[][] sanctumCoords = {
                {-4, 2, -19},  {4, 2, -19},   {-4, 1, -24},  {4, 1, -24},
                {-21, 15, -21},{21, 15, 29},  {-14, 71, 38}, {14, 71, 38},
                {-3, 45, 6},   {3, 45, 6},    {-8, -10, -31},{8, -10, -31}
        };
        for (int[] c : sanctumCoords) {
            int sx = c[0];
            int sy = c[1];
            int sz = c[2];
            // If the Sovereign Central Spire collapsed into the Throne Hall, relocate its 2 Sanctum chests onto the rubble below
            if (sy == 45 && sz == 6 && isSovereignSpireCollapsed()) {
                sx = sx < 0 ? -4 : 4;
                sz = sx < 0 ? 6 : 8;
                sy = 3;
            }
            specs.add(new ChestSpec(sx, sy, sz, LOOT_SANCTUM));
        }

        for (ChestSpec spec : specs) {
            BlockPos pos = center.offset(spec.dx, spec.dy, spec.dz);
            if (!box.isInside(pos)) continue;

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

    // =========================================================================
    // GEOMETRY & DETERMINISTIC BLOCK HELPERS
    // =========================================================================

    private void carveChamber(WorldGenLevel level, BoundingBox box, int x0, int y0, int z0, int x1, int y1, int z1,
                              BlockState floorState, BlockState ceilingState) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                setSafe(level, box, center.offset(x, y0 - 1, z), floorState);
                for (int y = y0; y <= y1; y++) {
                    setSafe(level, box, center.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
                setSafe(level, box, center.offset(x, y1 + 1, z), ceilingState);
            }
        }
    }

    private void fillVolume(WorldGenLevel level, BoundingBox box, int x0, int y0, int z0, int x1, int y1, int z1,
                            BlockState state) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    setSafe(level, box, center.offset(x, y, z), state);
                }
            }
        }
    }

    private static BlockState ironBars(boolean north, boolean south, boolean east, boolean west) {
        return Blocks.IRON_BARS.defaultBlockState()
                .setValue(BlockStateProperties.NORTH, north)
                .setValue(BlockStateProperties.SOUTH, south)
                .setValue(BlockStateProperties.EAST, east)
                .setValue(BlockStateProperties.WEST, west);
    }

    private static BlockState stairs(Direction facing, Half half) {
        return AbyssContent.ABYSSAL_TILE_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(BlockStateProperties.HALF, half);
    }

    private static BlockState oakStairs(Direction facing) {
        return Blocks.DARK_OAK_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(BlockStateProperties.HALF, Half.BOTTOM);
    }

    private static BlockState hangingLantern(boolean soul) {
        Block block = soul ? Blocks.SOUL_LANTERN : Blocks.LANTERN;
        return block.defaultBlockState().setValue(BlockStateProperties.HANGING, true);
    }

    /**
     * Fill for a gothic opening. Most are dark glass; about 40% glow warm amber from inside.
     * The choice is made per window (x, band, z), so one opening is either lit or dark as a whole.
     */
    private BlockState windowBlock(int x, int band, int z) {
        return coordMod(x, band, z, 100) < 40
                ? Blocks.SHROOMLIGHT.defaultBlockState()
                : Blocks.STAINED_GLASS.pick(DyeColor.BLACK).defaultBlockState();
    }

    /**
     * Roof cell that respects the collapse cut and the random holes. salt identifies which roof
     * this is, so the nave and each wing can collapse independently.
     */
    private void setRoof(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state,
                         int salt, int roofBaseY) {
        if (y >= roofCutY(salt, x, z, roofBaseY)) return;
        if (coordMod(x, y, z, 100) < ROOF_HOLE_PERCENT) return;
        setSafe(level, box, center.offset(x, y, z), state);
    }

    /** True when this roof has its collapsed stretch at this z (independent of y). */
    private boolean inCollapse(int salt, int z) {
        if (coordMod(salt, 7, 0, 100) >= COLLAPSE_CHANCE) return false;
        int zc = -10 + coordMod(salt, 8, 0, 40);
        int half = 5 + coordMod(salt, 9, 0, 6);
        return Math.abs(z - zc) <= half;
    }

    /**
     * Collapse: inside the collapsed stretch, everything above a jagged cut line is gone, which opens
     * the hall to the sky. Returns the first y that is missing, or MAX_VALUE if nothing is cut.
     */
    private int roofCutY(int salt, int x, int z, int roofBaseY) {
        if (salt == 8 && isSovereignSpireCollapsed() && Math.abs(x) <= 8 && z >= -2 && z <= 14) {
            return roofBaseY + 2 + coordMod(x, salt, z, 4);
        }
        if (!inCollapse(salt, z)) return Integer.MAX_VALUE;
        return roofBaseY + 2 + coordMod(x, salt, z, 6);
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

    /**
     * Flat structural plateau kept under the courtyard, curtain walls and tower footprints.
     * The surrounding mountain drops away irregularly, so this support area is not visible as a slab.
     */
    private boolean isCastlePlatform(int dx, int dz) {
        return dx >= PLATFORM_MIN_X && dx <= PLATFORM_MAX_X
                && dz >= PLATFORM_MIN_Z && dz <= PLATFORM_MAX_Z;
    }

    /**
     * Low-frequency terrain variation. Combining two scales avoids a perfect oval while keeping
     * the mountain continuous rather than turning it into random one-block spikes.
     */
    private double foundationDetailNoise(int x, int z) {
        // Seed offsets give each generated castle its own ridge layout while remaining chunk-stable.
        double seedX = (seed & 0xFFFFL) * 0.0007;
        double seedZ = ((seed >>> 16) & 0xFFFFL) * 0.0007;
        double broad = Math.sin(x * 0.045 + z * 0.035 + seedX)
                * Math.cos(z * 0.052 - x * 0.031 + seedZ);
        double medium = Math.sin(x * 0.13 - z * 0.11 + seedZ)
                * Math.cos(z * 0.12 + x * 0.08 + seedX);
        return broad * 0.65 + medium * 0.35;
    }

    /**
     * Number of blocks below the castle floor where the exposed mountain surface begins.
     * Distance from the castle platform creates the broad slope; deterministic noise breaks up
     * the edge into uneven ridges and shoulders.
     */
    private int getFoundationSurfaceDrop(int dx, int dz) {
        if (isCastlePlatform(dx, dz)) return 0;

        int outsideX = Math.max(0, Math.max(PLATFORM_MIN_X - dx, dx - PLATFORM_MAX_X));
        int outsideZ = Math.max(0, Math.max(PLATFORM_MIN_Z - dz, dz - PLATFORM_MAX_Z));
        double distance = Math.hypot(outsideX, outsideZ);
        double broadRidge = Math.sin(dx * 0.055 + dz * 0.041)
                * Math.cos(dz * 0.047 - dx * 0.033);
        double erosion = broadRidge * 1.9
                + foundationDetailNoise(dx, dz) * 1.0
                + (ridgeNoise(dx, dz - 8) - 0.5) * 1.0;
        return Math.max(0, Math.min(9, (int) Math.round(distance * 0.18 + erosion)));
    }

    /** Same irregular radial profile used by the foundation writer and support checks. */
    private double foundationRadiusFactor(int dx, int relativeDz, double depthProgress) {
        double ridge = ridgeNoise(dx, relativeDz);
        double detail = foundationDetailNoise(dx, relativeDz + 8);
        double factor = 1.10 + (depthProgress * 0.14) + ((ridge - 0.5) * 0.08) + (detail * 0.02);
        return Math.min(1.30, factor);
    }

    /**
     * Checks whether the mountain foundation reaches the top surface at this horizontal position.
     * This mirrors the same platform, surface drop and rough radial profile used during generation.
     */
    private boolean hasFoundationAtSurface(int dx, int dz) {
        if (isCastlePlatform(dx, dz)) return true;

        double relativeDz = dz - 8.0;
        double normDist = Math.hypot(dx / FOUNDATION_RADIUS_X, relativeDz / FOUNDATION_RADIUS_Z);
        double maxRadiusFactor = foundationRadiusFactor(dx, (int) relativeDz, 0.0);
        return normDist <= maxRadiusFactor;
    }

    private int coordMod(int x, int y, int z, int mod) {
        long hash = seed
                ^ ((long) x * 341873128712L)
                ^ ((long) y * 132897987541L)
                ^ ((long) z * 42317861L);
        hash = (hash ^ (hash >>> 30)) * 0xBF58476D1CE4E5B9L;
        hash = (hash ^ (hash >>> 27)) * 0x94D049BB133111EBL;
        hash ^= hash >>> 31;
        return (int) Math.floorMod(hash, (long) mod);
    }

    private BlockState pickMountainBlock(int dx, int wy, int dz, boolean isEdge) {
        long hash = seed
                ^ ((long) dx * 341873128712L)
                ^ ((long) wy * 132897987541L)
                ^ ((long) dz * 42317861L)
                ^ (isEdge ? 0x6A09E667F3BCC909L : 0x3C6EF372FE94F82AL);
        hash = (hash ^ (hash >>> 30)) * 0xBF58476D1CE4E5B9L;
        hash = (hash ^ (hash >>> 27)) * 0x94D049BB133111EBL;
        hash ^= hash >>> 31;
        int roll = (int) Math.floorMod(hash, 100L);

        // Custom Abyss palette only: rough edges use more cracked and corrupted variants.
        if (isEdge) {
            if (roll < 34) return AbyssContent.CRACKED_ABYSSAL_STONE.defaultBlockState();
            if (roll < 57) return AbyssContent.ABYSSAL_STONE.defaultBlockState();
            if (roll < 76) return AbyssContent.CRACKED_ABYSSAL_BRICKS.defaultBlockState();
            if (roll < 91) return AbyssContent.AMETHYST_CORRUPTED_STONE.defaultBlockState();
            return AbyssContent.ABYSSAL_BRICKS.defaultBlockState();
        }
        if (roll < 46) return AbyssContent.ABYSSAL_STONE.defaultBlockState();
        if (roll < 68) return AbyssContent.CRACKED_ABYSSAL_STONE.defaultBlockState();
        if (roll < 82) return AbyssContent.ABYSSAL_BRICKS.defaultBlockState();
        if (roll < 92) return AbyssContent.CRACKED_ABYSSAL_BRICKS.defaultBlockState();
        if (roll < 97) return AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState();
        return AbyssContent.AMETHYST_CORRUPTED_STONE.defaultBlockState();
    }

    private BlockState pickWallMasonry(int dx, int dy, int dz) {
        if (dy % 8 == 0) {
            return AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState();
        }
        int roll = coordMod(dx, dy, dz, 100);
        if (roll < 12) return AbyssContent.CRACKED_ABYSSAL_BRICKS.defaultBlockState();
        if (roll < 18) return AbyssContent.CHISELED_ABYSSAL_STONE.defaultBlockState();
        if (roll < 26) return AbyssContent.CRACKED_ABYSSAL_STONE.defaultBlockState();
        if (roll < 31) return AbyssContent.AMETHYST_CORRUPTED_STONE.defaultBlockState();
        return AbyssContent.ABYSSAL_BRICKS.defaultBlockState();
    }

    private BlockState pickFloorBlock(int dx, int dz) {
        if ((dx == 0 || dz == 0) && Math.abs(dx) <= 14 && Math.abs(dz) <= 36) {
            return AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState();
        }
        return coordMod(dx, 0, dz, 4) == 0
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
