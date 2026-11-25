package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Monumental ruined Gothic Cathedral-Castle in The Abyss.
 *
 * <p>Spans 169 x 169 blocks ({@code -HALF..+HALF} on X and Z) and rises over 100 blocks from the
 * subterranean dungeons up to the Sovereign Citadel Needle Spire, featuring:
 * <ul>
 *   <li>Ruined outer curtain walls with jagged breaches, buttresses, and fallen rubble</li>
 *   <li>Eight perimeter/gatehouse towers of varied heights with steep gothic spires</li>
 *   <li>Monumental arched approach viaduct and gigantic pointed-arch cathedral entrance</li>
 *   <li>Elevated arched sky-bridges spanning between towers and upper citadel galleries</li>
 *   <li>Double-height vaulted Cathedral Nave with flying buttresses and imperial staircase</li>
 *   <li>Two-story Abyssal Library with bookshelves, mezzanine balcony, and enchanting rotunda</li>
 *   <li>Sovereign Throne Room with vaulted ribs, stained-glass rose window, and gold/obsidian dais</li>
 *   <li>Subterranean Dungeons with iron-bar prison cells, ossuary crypt, and armory vault</li>
 * </ul>
 */
public class AbyssalCastlePiece extends StructurePiece {

    /** Loot table used by every chest in the castle (data/simpleskills/loot_table/chests/abyssal_castle.json). */
    public static final ResourceKey<LootTable> LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "chests/abyssal_castle"));

    public static final StructurePieceType TYPE = (context, tag) -> new AbyssalCastlePiece(tag);

    /** Half the footprint: the castle spans -HALF..+HALF on X and Z (169 x 169 blocks). */
    public static final int HALF = 84;
    public static final int MAX_HEIGHT = 104;
    public static final int BELOW_FLOOR_DEPTH = 24;

    private static final int CURTAIN_X = 73;
    private static final int CURTAIN_Z_NORTH = -75;
    private static final int CURTAIN_Z_SOUTH = 53;
    private static final int CURTAIN_H = 22;

    /**
     * Tower definitions: {centerX, centerZ, halfRadius, wallTopY, spirePeakY, ruinedFlag (1 = partially collapsed)}.
     */
    private static final int[][] TOWERS = {
            {-66,  46, 7, 36, 52, 1}, // SW Ruined Bastion Tower
            { 66,  46, 7, 48, 70, 0}, // SE Tall Foreground Cathedral Spire
            {-66, -12, 6, 42, 60, 0}, // West Library Watchtower
            { 66,  -8, 6, 33, 45, 1}, // East Ruined Belfry Tower
            {-64, -66, 8, 54, 78, 0}, // NW High Citadel Tower
            { 64, -66, 7, 50, 72, 0}, // NE Abyssal Observatory Spire
            {-11,  50, 4, 34, 48, 0}, // West Gatehouse Spire
            { 11,  50, 4, 37, 52, 0}  // East Gatehouse Spire (asymmetric height)
    };

    /**
     * Loot chests placed on solid floors across towers, library, throne room, spire, and dungeons:
     * {dx, dy, dz, facingOrdinal (0=NORTH, 1=SOUTH, 2=WEST, 3=EAST)}.
     */
    private static final int[][] CHEST_OFFSETS = {
            // Towers
            {-66, 23,  46, 0},
            { 66, 23,  46, 0},
            {-66, 23, -12, 3},
            { 66, 23,  -8, 2},
            {-64, 39, -66, 1},
            { 64, 39, -66, 1},
            {-11, 22,  50, 1},
            { 11, 22,  50, 1},
            // Library (lower & mezzanine)
            {-49, 15, -18, 3},
            {-49, 23,  18, 3},
            // Throne Room (flanking the Sovereign Dais)
            { -5, 18, -40, 1},
            {  5, 18, -40, 1},
            // Upper Keep & Summit Spire
            {  0, 44, -35, 1},
            {  0, 69, -29, 1},
            // East Ruined Chapel
            { 45, 15, -16, 2},
            // Subterranean Dungeons (Ossuary, Armory, Deep Cellar)
            {-29, -14, -22, 1},
            { 29, -14, -22, 1},
            {  0, -14,  -3, 1}
    };

    // Cached BlockStates for zero allocation during postProcess
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState DEEPSLATE_BRICKS = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState CRACKED_DEEPSLATE_BRICKS = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState DEEPSLATE_TILES = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState CRACKED_DEEPSLATE_TILES = Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState POLISHED_DEEPSLATE = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    private static final BlockState CHISELED_DEEPSLATE = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
    private static final BlockState COBBLED_DEEPSLATE = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
    private static final BlockState DEEPSLATE_BRICK_WALL = Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState();
    private static final BlockState DEEPSLATE_BRICK_SLAB_BOTTOM = Blocks.DEEPSLATE_BRICK_SLAB.defaultBlockState()
            .setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
    private static final BlockState POLISHED_DEEPSLATE_SLAB_BOTTOM = Blocks.POLISHED_DEEPSLATE_SLAB.defaultBlockState()
            .setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);

    private static final BlockState POLISHED_BLACKSTONE_BRICKS = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    private static final BlockState CHISELED_BLACKSTONE = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
    private static final BlockState GILDED_BLACKSTONE = Blocks.GILDED_BLACKSTONE.defaultBlockState();
    private static final BlockState OBSIDIAN = Blocks.OBSIDIAN.defaultBlockState();
    private static final BlockState CRYING_OBSIDIAN = Blocks.CRYING_OBSIDIAN.defaultBlockState();
    private static final BlockState GOLD_BLOCK = Blocks.GOLD_BLOCK.defaultBlockState();
    private static final BlockState AMETHYST_BLOCK = Blocks.AMETHYST_BLOCK.defaultBlockState();
    private static final BlockState BUDDING_AMETHYST = Blocks.BUDDING_AMETHYST.defaultBlockState();

    private static final BlockState STAIRS_NORTH = Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
            .setValue(BlockStateProperties.HALF, Half.BOTTOM);
    private static final BlockState STAIRS_SOUTH = Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
            .setValue(BlockStateProperties.HALF, Half.BOTTOM);
    private static final BlockState THRONE_SEAT = Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
            .setValue(BlockStateProperties.HALF, Half.BOTTOM);

    private static final BlockState IRON_BARS = Blocks.IRON_BARS.defaultBlockState();
    private static final BlockState SOUL_LANTERN_HANGING = Blocks.SOUL_LANTERN.defaultBlockState()
            .setValue(BlockStateProperties.HANGING, true);
    private static final BlockState SOUL_LANTERN_STANDING = Blocks.SOUL_LANTERN.defaultBlockState()
            .setValue(BlockStateProperties.HANGING, false);
    private static final BlockState SOUL_FIRE = Blocks.SOUL_FIRE.defaultBlockState();
    private static final BlockState SOUL_SOIL = Blocks.SOUL_SOIL.defaultBlockState();

    private static final BlockState PURPLE_GLASS = Blocks.STAINED_GLASS_PANE.pick(DyeColor.PURPLE).defaultBlockState();
    private static final BlockState MAGENTA_GLASS = Blocks.STAINED_GLASS_PANE.pick(DyeColor.MAGENTA).defaultBlockState();
    private static final BlockState BLACK_GLASS = Blocks.STAINED_GLASS_PANE.pick(DyeColor.BLACK).defaultBlockState();
    private static final BlockState TINTED_GLASS = Blocks.TINTED_GLASS.defaultBlockState();

    private static final BlockState BOOKSHELF = Blocks.BOOKSHELF.defaultBlockState();
    private static final BlockState LECTERN = Blocks.LECTERN.defaultBlockState();
    private static final BlockState ENCHANTING_TABLE = Blocks.ENCHANTING_TABLE.defaultBlockState();
    private static final BlockState PURPLE_CARPET = Blocks.CARPET.pick(DyeColor.PURPLE).defaultBlockState();
    private static final BlockState BLACK_CARPET = Blocks.CARPET.pick(DyeColor.BLACK).defaultBlockState();
    private static final BlockState PURPLE_WOOL = Blocks.WOOL.pick(DyeColor.PURPLE).defaultBlockState();
    private static final BlockState RED_WOOL = Blocks.WOOL.pick(DyeColor.RED).defaultBlockState();

    private static final BlockState COBWEB = Blocks.COBWEB.defaultBlockState();
    private static final BlockState SKELETON_SKULL = Blocks.SKELETON_SKULL.defaultBlockState();
    private static final BlockState WITHER_SKULL = Blocks.WITHER_SKELETON_SKULL.defaultBlockState();
    private static final BlockState BARREL = Blocks.BARREL.defaultBlockState();
    private static final BlockState ANVIL = Blocks.ANVIL.defaultBlockState();
    private static final BlockState GRINDSTONE = Blocks.GRINDSTONE.defaultBlockState();
    private static final BlockState CAULDRON = Blocks.CAULDRON.defaultBlockState();

    private final BlockPos center;
    private final long seed;

    public AbyssalCastlePiece(BlockPos center, long seed) {
        super(TYPE, 0, boxFor(center));
        this.center = center;
        this.seed = seed;
    }

    public AbyssalCastlePiece(CompoundTag tag) {
        this(new BlockPos(tag.getIntOr("cx", 0), tag.getIntOr("cy", 0), tag.getIntOr("cz", 0)),
                tag.getLongOr("seed", 0L));
    }

    /** Bounding box of the whole monumental castle, including dungeons, foundations, and the high spire. */
    public static BoundingBox boxFor(BlockPos c) {
        return new BoundingBox(c.getX() - HALF, c.getY() - BELOW_FLOOR_DEPTH, c.getZ() - HALF,
                c.getX() + HALF, c.getY() + MAX_HEIGHT, c.getZ() + HALF);
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
                            RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos pivot) {
        BoundingBox box = getBoundingBox();
        int x0 = Math.max(chunkBB.minX(), box.minX());
        int x1 = Math.min(chunkBB.maxX(), box.maxX());
        int z0 = Math.max(chunkBB.minZ(), box.minZ());
        int z1 = Math.min(chunkBB.maxZ(), box.maxZ());
        int yMin = Math.max(Math.max(chunkBB.minY(), box.minY()), level.getMinY() + 1);
        int yMax = Math.min(chunkBB.maxY(), box.maxY());
        if (x0 > x1 || z0 > z1 || yMin > yMax) {
            return;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int dx = x - center.getX();
                int dz = z - center.getZ();

                // 1. Above ground floor (dy = 0 .. MAX_HEIGHT - 1)
                for (int dy = 0; dy < MAX_HEIGHT; dy++) {
                    int y = center.getY() + dy;
                    if (y < yMin || y > yMax) {
                        continue;
                    }
                    BlockState state = layoutAbove(dx, dy, dz);
                    if (state != null) {
                        cursor.set(x, y, z);
                        level.setBlock(cursor, state, 2);
                    }
                }

                // 2. Main ground slab & subterranean dungeons (dy = -1 .. -17)
                for (int dy = -1; dy >= -17; dy--) {
                    int y = center.getY() + dy;
                    if (y < yMin || y > yMax) {
                        continue;
                    }
                    BlockState state = layoutBelow(dx, dy, dz);
                    if (state != null) {
                        cursor.set(x, y, z);
                        level.setBlock(cursor, state, 2);
                    }
                }

                // 3. Solid foundation beneath dungeons/courtyard down to natural cavern rock
                int depth = 0;
                for (int dy = -18; dy >= -BELOW_FLOOR_DEPTH && depth < 12; dy--) {
                    int y = center.getY() + dy;
                    if (y < yMin || y > yMax) {
                        continue;
                    }
                    if (!isInsideCastlePlateau(dx, dz) && !isUnderViaductPier(dx, dz)) {
                        break;
                    }
                    cursor.set(x, y, z);
                    if (!isOpen(level.getBlockState(cursor))) {
                        break;
                    }
                    level.setBlock(cursor, AbyssContent.ABYSSAL_STONE.defaultBlockState(), 2);
                    depth++;
                }
            }
        }

        placeChests(level, chunkBB, box);
    }

    private void placeChests(WorldGenLevel level, BoundingBox chunkBB, BoundingBox box) {
        for (int[] offset : CHEST_OFFSETS) {
            int x = center.getX() + offset[0];
            int y = center.getY() + offset[1];
            int z = center.getZ() + offset[2];
            if (!chunkBB.isInside(x, y, z) || !box.isInside(x, y, z)) {
                continue;
            }
            Direction facing = switch (offset[3]) {
                case 1 -> Direction.SOUTH;
                case 2 -> Direction.WEST;
                case 3 -> Direction.EAST;
                default -> Direction.NORTH;
            };
            BlockState chest = Blocks.CHEST.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            BlockPos pos = new BlockPos(x, y, z);
            level.setBlock(pos, chest, 2);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof RandomizableContainerBlockEntity container) {
                container.setLootTable(LOOT_TABLE, seed ^ (offset[0] * 31L + offset[2] * 17L + offset[1] * 13L));
            }
        }
    }

    // =========================================================================
    // ABOVE-GROUND MONUMENTAL LAYOUT (dy >= 0)
    // =========================================================================

    private BlockState layoutAbove(int dx, int dy, int dz) {
        int ax = Math.abs(dx);

        // 1. South Monumental Approach Viaduct Bridge & Stairs (dz = 55..84)
        if (dz >= 55 && dz <= 84 && ax <= 8) {
            return approachViaductAt(dx, dy, dz);
        }

        // 2. Gigantic Cathedral Gatehouse & Portal (dx = -14..14, dz = 44..55)
        if (ax <= 14 && dz >= 44 && dz <= 55) {
            return gatehouseAt(dx, dy, dz);
        }

        // 3. Eight Perimeter & Gatehouse Towers of Varied Heights
        for (int i = 0; i < TOWERS.length; i++) {
            int[] t = TOWERS[i];
            int tdx = dx - t[0];
            int tdz = dz - t[1];
            int r = t[2];
            if (Math.abs(tdx) <= r + 1 && Math.abs(tdz) <= r + 1) {
                BlockState towerBlock = towerAt(i, tdx, dy, tdz, r, t[3], t[4], t[5] == 1);
                if (towerBlock != null) {
                    return towerBlock;
                }
            }
        }

        // 4. Elevated Suspended Arched Sky-Bridges
        BlockState skyBridge = skyBridgesAt(dx, dy, dz);
        if (skyBridge != null) {
            return skyBridge;
        }

        // 5. Central Sovereign Citadel Keep & Towering Needle Spire (dx = -22..22, dz = -46..-4)
        if (ax <= 22 && dz >= -46 && dz <= -4) {
            return throneAndKeepAt(dx, dy, dz);
        }

        // 6. Grand Cathedral Nave Core (dx = -20..20, dz = -4..44)
        if (ax <= 20 && dz >= -4 && dz <= 44) {
            return cathedralNaveAt(dx, dy, dz);
        }

        // 7. West Wing: Grand Two-Story Abyssal Library (dx = -53..-20, dz = -24..22)
        if (dx >= -53 && dx <= -20 && dz >= -24 && dz <= 22) {
            return libraryWingAt(dx, dy, dz);
        }

        // 8. East Wing: Ruined Gothic Chapter Hall & Chapel (dx = 20..50, dz = -20..18)
        if (dx >= 20 && dx <= 50 && dz >= -20 && dz <= 18) {
            return eastChapelWingAt(dx, dy, dz);
        }

        // 9. Exterior Flying Buttresses bracing the South Cathedral Nave (ax = 21..27, dz = 23..42)
        if (ax >= 21 && ax <= 27 && dz >= 23 && dz <= 42) {
            if (dz % 8 == 0) {
                if (ax >= 26 && dy <= 24) {
                    return wallStone(dx, dy, dz);
                }
                int archTop = 27 - (ax - 20);
                if (dy >= archTop - 2 && dy <= archTop) {
                    return DEEPSLATE_BRICKS;
                }
            }
        }

        // 10. Ruined Outer Curtain Walls (muralhas destruídas)
        if (isInsideCastlePlateau(dx, dz)) {
            BlockState curtain = ruinedCurtainWallAt(dx, dy, dz);
            if (curtain != null) {
                return curtain;
            }
        }

        // 11. Courtyard: shattered colonnade pillars, rubble mounds & fallen masonry
        if (isInsideCastlePlateau(dx, dz)) {
            return courtyardAt(dx, dy, dz);
        }

        // Outside plateau: clear air inside castle envelope so exterior silhouette stays dramatic
        return dy < 40 ? AIR : null;
    }

    // -------------------------------------------------------------------------
    // 1. Monumental Approach Viaduct Bridge (dz = 55..84)
    // -------------------------------------------------------------------------
    private BlockState approachViaductAt(int dx, int dy, int dz) {
        int ax = Math.abs(dx);
        int deckY = dz >= 64 ? (84 - dz) / 2 : 10;

        if (ax == 7) {
            if (dy == deckY + 1) {
                return (dz & 1) == 0 ? DEEPSLATE_BRICK_WALL : POLISHED_DEEPSLATE_SLAB_BOTTOM;
            }
            if (dy == deckY + 2 && dz % 6 == 0) {
                return SOUL_LANTERN_STANDING;
            }
            if (dy > deckY + 1) {
                return AIR;
            }
        }
        if (ax == 8) {
            if (dz % 8 == 0 && dy <= deckY + 1) {
                return wallStone(dx, dy, dz);
            }
            return AIR;
        }

        if (ax <= 6) {
            if (dy > deckY) {
                return AIR;
            }
            if (dy == deckY) {
                if (dz >= 64 && (dz & 1) == 1 && ax <= 5) {
                    return STAIRS_NORTH;
                }
                if (ax == 0) {
                    return GILDED_BLACKSTONE;
                }
                return ax == 6 ? POLISHED_BLACKSTONE_BRICKS : DEEPSLATE_TILES;
            }
            boolean pier = (dz >= 55 && dz <= 57) || (dz >= 66 && dz <= 68) || (dz >= 76 && dz <= 78) || dz >= 83;
            if (!pier && ax <= 5) {
                int spanCenter = dz < 66 ? 61 : (dz < 76 ? 72 : 80);
                if (isInsideGothicArch(dz - spanCenter, dy, 3, 0, Math.max(2, deckY - 5), Math.max(4, deckY - 2))) {
                    return AIR;
                }
            }
            return wallStone(dx, dy, dz);
        }
        return AIR;
    }

    // -------------------------------------------------------------------------
    // 2. Gigantic Cathedral Entrance & Gatehouse (dx = -14..14, dz = 44..55)
    // -------------------------------------------------------------------------
    private BlockState gatehouseAt(int dx, int dy, int dz) {
        int ax = Math.abs(dx);

        if (ax >= 8) {
            return null; // Delegated to Twin Gatehouse Towers (#6 and #7)
        }

        int floorLevel = 10;
        boolean portalArchWall = (dz >= 52 && dz <= 54) || (dz >= 45 && dz <= 46);
        if (portalArchWall && dy <= 42) {
            // Gigantic pointed gothic entrance arch (11 blocks wide, 20 blocks tall)
            if (isInsideGothicArch(dx, dy, 5, floorLevel + 1, 19, 29)) {
                if (dz == 53 && dy >= 24 && (ax & 1) == 0) {
                    return IRON_BARS;
                }
                return AIR;
            }
            // Recessed decorative archivolts framing the gigantic entrance
            if (dz == 54 && isInsideGothicArch(dx, dy, 6, floorLevel + 1, 20, 31)) {
                return CHISELED_BLACKSTONE;
            }
            // Upper portal rose/lancet window (dy = 33..40)
            if (dz == 53 && isInsideGothicArch(dx, dy, 3, 33, 36, 40)) {
                return stainedGlass(dx, dy, dz);
            }
            if (dz == 53 && dy == 32 && ax <= 3) {
                return DEEPSLATE_BRICK_SLAB_BOTTOM;
            }
            return wallStone(dx, dy, dz);
        }

        if (ax == 7 && dy <= 42) {
            if (dy == 15 && dz == 49) {
                return SOUL_LANTERN_HANGING;
            }
            return wallStone(dx, dy, dz);
        }

        // Steep pointed gable roof crowning the monumental entrance (dy = 42..50)
        if (dy >= 42 && dy <= 50) {
            int roofHalf = 8 - (dy - 42);
            if (ax == roofHalf || ax == roofHalf - 1) {
                return DEEPSLATE_TILES;
            }
            if (ax < roofHalf - 1 && dz >= 46 && dz <= 53) {
                return dy == 42 ? DEEPSLATE_BRICKS : AIR;
            }
            return AIR;
        }

        if (dy < floorLevel) {
            return wallStone(dx, dy, dz);
        }
        if (dy == floorLevel) {
            return ax == 0 ? GILDED_BLACKSTONE : DEEPSLATE_TILES;
        }
        if (dy == 30) {
            return DEEPSLATE_BRICKS;
        }
        return AIR;
    }

    // -------------------------------------------------------------------------
    // 3. Gothic Towers with Varied Heights & Needle Spires
    // -------------------------------------------------------------------------
    private BlockState towerAt(int idx, int tdx, int dy, int tdz, int r, int wallTop, int spirePeak, boolean ruined) {
        int adx = Math.abs(tdx);
        int adz = Math.abs(tdz);
        int cheb = Math.max(adx, adz);
        int manh = adx + adz;

        int octLimit = 2 * r - 2;
        boolean insideOct = cheb <= r && manh <= octLimit;
        boolean onWall = insideOct && (cheb == r || manh == octLimit);

        boolean buttress = (adx == r + 1 && adz <= 1) || (adz == r + 1 && adx <= 1);
        if (buttress && dy <= wallTop - 4) {
            if (ruined && rnd(tdx, dy, tdz, 10 + idx) < crumble(dy, wallTop)) {
                return AIR;
            }
            return wallStone(tdx, dy, tdz);
        }

        // Steep pointed needle spire above wallTop
        if (dy >= wallTop) {
            if (dy > spirePeak) {
                return AIR;
            }
            if (ruined && dy > wallTop + (spirePeak - wallTop) / 3) {
                if (rnd(tdx, dy, tdz, 20 + idx) < 0.35F + 0.55F * ((float) (dy - wallTop) / (spirePeak - wallTop))) {
                    return AIR;
                }
            }
            if (dy == spirePeak && tdx == 0 && tdz == 0) {
                return OBSIDIAN;
            }
            float progress = (float) (dy - wallTop) / Math.max(1, spirePeak - wallTop);
            int currR = Math.max(0, Math.round(r * (1.0F - progress)));
            int currOct = Math.max(0, 2 * currR - 1);
            boolean inSpire = cheb <= currR && manh <= currOct;
            boolean spireShell = inSpire && (cheb == currR || manh >= currOct - 1);
            if (spireShell) {
                return ((dy + adx + adz) & 1) == 0 ? DEEPSLATE_TILES : POLISHED_BLACKSTONE_BRICKS;
            }
            if (dy <= wallTop + 6 && adx == r - 1 && adz == r - 1) {
                return dy == wallTop + 6 ? DEEPSLATE_BRICK_WALL : POLISHED_BLACKSTONE_BRICKS;
            }
            return AIR;
        }

        if (!insideOct) {
            return null;
        }

        if (ruined && dy > wallTop - 10 && rnd(tdx, dy, tdz, 30 + idx) < crumble(dy, wallTop)) {
            return AIR;
        }

        if (onWall) {
            if (dy == wallTop - 1 && ((tdx + tdz) & 1) == 1) {
                return AIR;
            }
            if (dy >= 0 && dy <= 5 && (adx <= 1 || adz <= 1)) {
                return AIR;
            }
            if ((dy >= 22 && dy <= 26 && (adx <= 1 || adz <= 1))
                    || (dy >= 38 && dy <= 42 && (adx <= 1 || adz <= 1))) {
                return AIR;
            }
            if (((dy >= 10 && dy <= 16) || (dy >= 28 && dy <= 34)) && (tdx == 0 || tdz == 0)) {
                return stainedGlass(tdx, dy, tdz);
            }
            return wallStone(tdx, dy, tdz);
        }

        if (dy > 0 && dy % 11 == 0) {
            int angleStep = (dy / 11) & 3;
            boolean stairHole = switch (angleStep) {
                case 0 -> tdx >= 1 && tdx <= 2 && tdz >= -1 && tdz <= 1;
                case 1 -> tdz >= 1 && tdz <= 2 && tdx >= -1 && tdx <= 1;
                case 2 -> tdx <= -1 && tdx >= -2 && tdz >= -1 && tdz <= 1;
                default -> tdz <= -1 && tdz >= -2 && tdx >= -1 && tdx <= 1;
            };
            if (!stairHole) {
                return floorStone(tdx, dy, tdz);
            }
        }

        if (tdx == 0 && tdz == 0) {
            return AbyssContent.ABYSSAL_PILLAR.defaultBlockState();
        }
        int spiralDir = Math.floorMod(dy, 8);
        int sx = spiralDir == 0 || spiralDir == 1 || spiralDir == 7 ? 1 : (spiralDir >= 3 && spiralDir <= 5 ? -1 : 0);
        int sz = spiralDir >= 1 && spiralDir <= 3 ? 1 : (spiralDir >= 5 && spiralDir <= 7 ? -1 : 0);
        if (tdx == sx && tdz == sz && (sx != 0 || sz != 0)) {
            return POLISHED_DEEPSLATE_SLAB_BOTTOM;
        }

        return AIR;
    }

    // -------------------------------------------------------------------------
    // 4. Elevated Suspended Arched Sky-Bridges (Pontes Suspensas)
    // -------------------------------------------------------------------------
    private BlockState skyBridgesAt(int dx, int dy, int dz) {
        // Bridge A: West Library Mezzanine (dx=-53) to West Watchtower (dx=-60), dz = -14..-10, deck at dy=21
        if (dx >= -60 && dx <= -53 && dz >= -14 && dz <= -10) {
            return archBridgeSegment(dx, dy, dz, -56, 3, 21);
        }
        // Bridge B: East Chapel Wing (dx=50) to East Belfry Tower (dx=60), dz = -10..-6, deck at dy=21
        if (dx >= 50 && dx <= 60 && dz >= -10 && dz <= -6) {
            return archBridgeSegment(dx, dy, dz, 55, 4, 21);
        }
        // Bridge C: North High Citadel Viaduct spanning NW Tower (-56) to NE Tower (57), dz = -68..-64, deck at dy=37
        if (dx >= -56 && dx <= 57 && dz >= -68 && dz <= -64) {
            int spanCenter = Math.floorDiv(dx + 7, 14) * 14;
            return archBridgeSegment(dx, dy, dz, spanCenter, 5, 37);
        }
        return null;
    }

    private BlockState archBridgeSegment(int dx, int dy, int dz, int spanCenter, int archHalfW, int deckY) {
        int edgeDist;
        if (dz >= -14 && dz <= -10) {
            edgeDist = Math.abs(dz + 12);
        } else if (dz >= -10 && dz <= -6) {
            edgeDist = Math.abs(dz + 8);
        } else {
            edgeDist = Math.abs(dz + 66);
        }

        if (dy == deckY) {
            return edgeDist == 2 ? POLISHED_BLACKSTONE_BRICKS : DEEPSLATE_TILES;
        }
        if (dy == deckY + 1 && edgeDist == 2) {
            return ((dx + dz) & 1) == 0 ? DEEPSLATE_BRICK_WALL : DEEPSLATE_BRICK_SLAB_BOTTOM;
        }
        if (dy == deckY + 2 && edgeDist == 2 && Math.floorMod(dx, 6) == 0) {
            return SOUL_LANTERN_STANDING;
        }
        if (dy >= deckY - 6 && dy < deckY) {
            if (isInsideGothicArch(dx - spanCenter, dy, archHalfW, deckY - 6, deckY - 4, deckY - 2)) {
                return AIR;
            }
            return wallStone(dx, dy, dz);
        }
        return dy > deckY ? AIR : null;
    }

    // -------------------------------------------------------------------------
    // 5. Sovereign Throne Room & High Citadel Spire (dx = -22..22, dz = -46..-4)
    // -------------------------------------------------------------------------
    private BlockState throneAndKeepAt(int dx, int dy, int dz) {
        int ax = Math.abs(dx);

        // --- HIGH CITADEL KEEP & NEEDLE SPIRE (dy = 43 .. 103) ---
        if (dy >= 43) {
            if (dy >= 68) {
                int sdz = dz + 29;
                int asz = Math.abs(sdz);
                if (ax > 11 || asz > 11) {
                    return AIR;
                }
                if (dy == 103 && dx == 0 && sdz == 0) {
                    return OBSIDIAN;
                }
                // Open arched belfry shrine at dy = 68..76
                if (dy <= 76 && ax <= 8 && asz <= 8) {
                    if (ax >= 6 && asz >= 6) {
                        return AbyssContent.ABYSSAL_PILLAR.defaultBlockState();
                    }
                    if (dy == 76 && (ax == 8 || asz == 8)) {
                        return DEEPSLATE_BRICKS;
                    }
                    if (dy == 75 && dx == 0 && sdz == 0) {
                        return SOUL_LANTERN_HANGING;
                    }
                    return AIR;
                }
                // Steep needle spire pyramid from dy = 77 to dy = 102
                float t = (float) (dy - 77) / 26.0F;
                int halfR = Math.max(0, Math.round(9.0F * (1.0F - t)));
                if (ax <= halfR && asz <= halfR && (ax + asz) <= 2 * halfR - 1) {
                    if (ax == halfR || asz == halfR || (ax + asz) >= 2 * halfR - 2) {
                        return ((dy + ax + asz) & 1) == 0 ? DEEPSLATE_TILES : POLISHED_BLACKSTONE_BRICKS;
                    }
                }
                return AIR;
            }

            // Upper Keep Sanctum (dy = 43 .. 67, ax <= 15, dz = -43..-15)
            int kdz = dz + 29;
            int akz = Math.abs(kdz);
            // Exterior flying buttresses on the Upper Keep
            if (ax >= 16 && ax <= 20 && akz <= 14 && (kdz == -10 || kdz == 0 || kdz == 10)) {
                int buttressTop = 62 - (ax - 15) * 2;
                if (ax == 20 && dy <= 54) {
                    return wallStone(dx, dy, dz);
                }
                if (dy >= buttressTop - 3 && dy <= buttressTop) {
                    return DEEPSLATE_BRICKS;
                }
                return AIR;
            }
            if (ax > 15 || akz > 14) {
                return AIR;
            }
            if (ax == 15 || akz == 14) {
                if (dy >= 48 && dy <= 59 && (kdz == -5 || kdz == 5 || dx == -6 || dx == 6)) {
                    return stainedGlass(dx, dy, dz);
                }
                return wallStone(dx, dy, dz);
            }
            if (dy == 67) {
                return DEEPSLATE_TILES;
            }
            BlockState keepStair = keepSwitchbackStair(dx, dy, dz);
            if (keepStair != null) {
                return keepStair;
            }
            if (dy == 43 && ax <= 2 && akz <= 2) {
                return (ax == 0 && akz == 0) ? BUDDING_AMETHYST : GILDED_BLACKSTONE;
            }
            if (dy == 66 && ax == 0 && akz == 0) {
                return SOUL_LANTERN_HANGING;
            }
            return AIR;
        }

        // Keep switchback stairwell shaft passes through corner (dx = -17..-13, dz = -20..-11)
        if (dx >= -18 && dx <= -12 && dz >= -21 && dz <= -10 && dy >= 14) {
            if (dx == -18 || dx == -12 || dz == -21 || dz == -10) {
                if (dz == -10 && dx >= -16 && dx <= -14 && dy >= 14 && dy <= 18) {
                    return AIR;
                }
                return wallStone(dx, dy, dz);
            }
            BlockState stair = keepSwitchbackStair(dx, dy, dz);
            return stair != null ? stair : AIR;
        }

        // --- SOVEREIGN THRONE ROOM (dy = 13 .. 42, ax <= 22, dz = -46..-4) ---
        if (dy >= 13) {
            if (dy == 13) {
                return floorStone(dx, dy, dz);
            }
            if (dy == 42) {
                return DEEPSLATE_TILES;
            }

            if (ax == 22 || dz == -46 || dz == -4) {
                // Grand South Entry Arch from Imperial Staircase Landing (dz == -4, dx = -4..4, dy = 14..25)
                if (dz == -4 && isInsideGothicArch(dx, dy, 4, 14, 21, 25)) {
                    return AIR;
                }
                // West & East Gallery Doorways at dz = -12..-8, dy = 14..19
                if (ax == 22 && dz >= -12 && dz <= -8 && dy >= 14 && dy <= 19) {
                    return AIR;
                }
                // Giant Abyssal Rose Window behind the Sovereign Throne (dz == -46)
                if (dz == -46 && isInsideGothicArch(dx, dy, 5, 24, 31, 38)) {
                    if (ax == 0 || dy == 31) {
                        return DEEPSLATE_BRICK_WALL;
                    }
                    return stainedGlass(dx, dy, dz);
                }
                // Tall Lancet Stained-Glass Windows along West & East walls
                if (ax == 22 && (dz == -18 || dz == -26 || dz == -34 || dz == -40)) {
                    if (dy >= 18 && dy <= 34) {
                        return stainedGlass(dx, dy, dz);
                    }
                }
                return wallStone(dx, dy, dz);
            }

            // Colonnade of Abyssal Pillars & Overhead Pointed Gothic Ceiling Ribs (ax == 11)
            if (ax == 11 && (dz == -10 || dz == -18 || dz == -26 || dz == -34 || dz == -42)) {
                if (dy <= 29) {
                    return AbyssContent.ABYSSAL_PILLAR.defaultBlockState();
                }
            }
            if ((dz == -10 || dz == -18 || dz == -26 || dz == -34 || dz == -42) && dy >= 28 && dy <= 41) {
                if (!isInsideGothicArch(dx, dy, 11, 14, 28, 41)) {
                    return DEEPSLATE_BRICKS;
                }
            }

            // Soul-Fire Braziers along the royal aisle (ax == 8, dz = -14, -22, -30)
            if (ax == 8 && (dz == -14 || dz == -22 || dz == -30)) {
                if (dy == 14) return CHISELED_BLACKSTONE;
                if (dy == 15) return SOUL_SOIL;
                if (dy == 16) return SOUL_FIRE;
            }

            // Royal Crimson & Purple Carpet Runner (ax <= 3, dz = -35..-5)
            if (dy == 14 && ax <= 3 && dz >= -35 && dz <= -5) {
                if (ax == 3) return GILDED_BLACKSTONE;
                return ax <= 1 ? RED_WOOL : PURPLE_WOOL;
            }

            // --- THE SOVEREIGN DAIS & THRONE (ax <= 7, dz = -44..-36) ---
            if (ax <= 7 && dz >= -44 && dz <= -36) {
                int daisTop = dz >= -38 ? 14 + (-36 - dz) : 17;
                if (dy <= daisTop) {
                    if (ax == 7 || dz == -36) {
                        return GOLD_BLOCK;
                    }
                    return (ax + dz) % 2 == 0 ? OBSIDIAN : POLISHED_BLACKSTONE_BRICKS;
                }
                if (dz == -41 && dx == 0 && dy == 18) {
                    return THRONE_SEAT;
                }
                if (dz == -41 && ax == 1 && dy == 18) {
                    return GOLD_BLOCK;
                }
                if (dz == -42 && ax <= 2 && dy >= 18 && dy <= 23) {
                    if (dx == 0 && dy >= 19 && dy <= 21) {
                        return AMETHYST_BLOCK;
                    }
                    if (dy == 23 && ax == 1) {
                        return AIR;
                    }
                    return ax == 2 ? GOLD_BLOCK : CRYING_OBSIDIAN;
                }
                if (dz == -42 && ax == 5 && dy >= 18 && dy <= 20) {
                    return dy == 20 ? BUDDING_AMETHYST : AMETHYST_BLOCK;
                }
            }

            return AIR;
        }

        // --- LOWER ROYAL RELIC CRYPT UNDER THRONE ROOM (dy = 0 .. 12, ax <= 20, dz = -44..-5) ---
        if (ax > 20 || dz < -44) {
            return AIR;
        }
        if (ax == 20 || dz == -44) {
            if (ax == 20 && Math.abs(dz + 24) <= 1 && dy <= 4) {
                return AIR;
            }
            return wallStone(dx, dy, dz);
        }
        if (ax == 8 && (dz == -14 || dz == -24 || dz == -34)) {
            return AbyssContent.ABYSSAL_PILLAR.defaultBlockState();
        }
        if (dy == 0 && ax <= 2 && dz >= -38 && dz <= -8) {
            return BLACK_CARPET;
        }
        return AIR;
    }

    private BlockState keepSwitchbackStair(int dx, int dy, int dz) {
        int step = Math.floorMod(dy - 14, 16);
        if (step < 8) {
            int targetZ = -12 - step;
            if ((dx == -16 || dx == -15) && dz == targetZ) {
                return STAIRS_NORTH;
            }
            if ((dx == -16 || dx == -15) && dz == -20 && step == 7) {
                return DEEPSLATE_TILES;
            }
        } else {
            int s = step - 8;
            int targetZ = -19 + s;
            if ((dx == -14 || dx == -13) && dz == targetZ) {
                return STAIRS_SOUTH;
            }
            if ((dx == -14 || dx == -13) && dz == -11 && s == 7) {
                return DEEPSLATE_TILES;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // 6. Grand Cathedral Nave & Imperial Staircase (dx = -20..20, dz = -4..44)
    // -------------------------------------------------------------------------
    private BlockState cathedralNaveAt(int dx, int dy, int dz) {
        int ax = Math.abs(dx);

        // Upper Triforium & Twin Cathedral Nave Spires (dy >= 29)
        if (dy >= 29) {
            if (dz >= 33 && dz <= 43 && ax >= 9 && ax <= 19) {
                int sdx = Math.abs(ax - 14);
                int sdz = Math.abs(dz - 38);
                int peak = dx < 0 ? 82 : 86;
                if (dy <= 54) {
                    if (sdx == 5 || sdz == 5) {
                        if (dy >= 34 && dy <= 46 && (sdx == 0 || sdz == 0)) {
                            return stainedGlass(dx, dy, dz);
                        }
                        return wallStone(dx, dy, dz);
                    }
                    return AIR;
                }
                if (dy <= peak) {
                    float t = (float) (dy - 55) / (peak - 55);
                    int hr = Math.max(0, Math.round(5.0F * (1.0F - t)));
                    if (sdx <= hr && sdz <= hr && (sdx == hr || sdz == hr)) {
                        return DEEPSLATE_TILES;
                    }
                }
                return AIR;
            }

            if (dy <= 45) {
                int roofHalf = 20 - (dy - 29);
                if (ax == roofHalf || ax == roofHalf - 1) {
                    return DEEPSLATE_TILES;
                }
                if (ax < roofHalf - 1 && (dz == -4 || dz == 44)) {
                    return wallStone(dx, dy, dz);
                }
            }
            return AIR;
        }

        if (dy == 28) {
            return DEEPSLATE_TILES;
        }

        // Nave Outer Walls (ax == 20 or dz == 44 or dz == -4)
        if (ax == 20 || dz == 44 || dz == -4) {
            if (dz == 44 && isInsideGothicArch(dx, dy, 5, 1, 19, 29)) {
                return AIR;
            }
            if (dz == -4 && dy >= 13) {
                if (dy == 13) return floorStone(dx, dy, dz);
                if (isInsideGothicArch(dx, dy, 4, 14, 21, 25)) {
                    return AIR;
                }
                return wallStone(dx, dy, dz);
            }
            if (ax == 20 && dz >= 19 && dz <= 22 && dy <= 5) {
                return AIR;
            }
            // West & East Upper Gallery Doors into Library & East Chapel (ax == 20, dz = -2..2, dy = 14..19)
            if (ax == 20 && dz >= -2 && dz <= 2 && dy >= 14 && dy <= 19) {
                return AIR;
            }
            if (ax == 20 && (dz == 4 || dz == 12 || dz == 28 || dz == 36) && dy >= 5 && dy <= 21) {
                return stainedGlass(dx, dy, dz);
            }
            return wallStone(dx, dy, dz);
        }

        // 1. Gatehouse-to-Nave Ceremonial Descent Staircase (ax <= 5, dz = 34..43)
        if (ax <= 5 && dz >= 34 && dz <= 43) {
            int stepY = dz - 34;
            if (dy < stepY) {
                return wallStone(dx, dy, dz);
            }
            if (dy == stepY) {
                return STAIRS_SOUTH;
            }
            return AIR;
        }
        if (ax == 6 && dz >= 34 && dz <= 43) {
            int stepY = dz - 34;
            if (dy <= stepY) return wallStone(dx, dy, dz);
            if (dy == stepY + 1) return DEEPSLATE_BRICK_WALL;
        }

        // 2. Grand Imperial Staircase ascending to Throne Room & Library Landing (ax <= 5, dz = -3..9)
        if (ax <= 5 && dz >= -3 && dz <= 9) {
            int stepY = 9 - dz;
            if (dy <= stepY) {
                return wallStone(dx, dy, dz);
            }
            if (dy == stepY + 1) {
                return STAIRS_NORTH;
            }
            return AIR;
        }
        if (ax == 6 && dz >= -3 && dz <= 9) {
            int stepY = 9 - dz;
            if (dy <= stepY + 1) return wallStone(dx, dy, dz);
            if (dy == stepY + 2) return DEEPSLATE_BRICK_WALL;
        }
        // Upper Mezzanine Cross-Gallery at North end of Nave (dz = -3..-1, ax >= 7, dy = 13)
        if (dz >= -3 && dz <= -1 && ax >= 7 && dy == 13) {
            return floorStone(dx, dy, dz);
        }
        if (dz == 0 && ax >= 7 && dy == 14) {
            return DEEPSLATE_BRICK_WALL;
        }

        // 3. Enclosed Dungeon Descent Stairwell on East Side of Nave (dx = 13..18, dz = 11..28)
        if (dx >= 13 && dx <= 18 && dz >= 11 && dz <= 28) {
            if (dx == 13 || dx == 18 || dz == 28) {
                if (dy <= 6) return wallStone(dx, dy, dz);
            } else if (dz == 11) {
                if (dy >= 4 && dy <= 6) return wallStone(dx, dy, dz);
            } else {
                if (dy <= 5) return AIR;
            }
        }

        // Towering Nave Colonnade Pillars (ax == 9, dz = 0, 8, 16, 24, 32, 40)
        if (ax == 9 && dz >= 0 && dz <= 40 && dz % 8 == 0) {
            return AbyssContent.ABYSSAL_PILLAR.defaultBlockState();
        }
        if (dz >= 0 && dz <= 40 && dz % 8 == 0 && dy >= 17 && dy <= 27) {
            if (!isInsideGothicArch(dx, dy, 9, 0, 17, 27)) {
                return DEEPSLATE_BRICKS;
            }
        }

        if (ax == 7 && (dz == 12 || dz == 20 || dz == 28)) {
            if (dy == 0) return CHISELED_BLACKSTONE;
            if (dy == 1) return SOUL_SOIL;
            if (dy == 2) return SOUL_FIRE;
        }

        if (dy == 0 && ax <= 2 && dz >= 10 && dz <= 33) {
            return ax == 2 ? BLACK_CARPET : PURPLE_CARPET;
        }

        return AIR;
    }

    // -------------------------------------------------------------------------
    // 7. West Wing: Grand Two-Story Abyssal Library (dx = -53..-20, dz = -24..22)
    // -------------------------------------------------------------------------
    private BlockState libraryWingAt(int dx, int dy, int dz) {
        int az = Math.abs(dz);
        boolean westWall = dx == -53;
        boolean eastWall = dx == -20 || (dx == -22 && dz < -4);

        // Ground Cloister Colonnade below Library (dy = 0 .. 12)
        if (dy <= 12) {
            if (dx < -52 || az > 22) {
                return AIR;
            }
            if (westWall || dx == -20 || az == 22) {
                if (westWall && dy <= 8 && (Math.abs(dz) <= 2 || Math.abs(dz - 10) <= 2 || Math.abs(dz + 10) <= 2)) {
                    return AIR;
                }
                return wallStone(dx, dy, dz);
            }
            if ((dx == -32 || dx == -42) && (dz == -12 || dz == 0 || dz == 12)) {
                return AbyssContent.ABYSSAL_PILLAR.defaultBlockState();
            }
            return AIR;
        }

        if (dy > 35 || dx < -53 || az > 22) {
            return AIR;
        }
        if (dy == 13) {
            return floorStone(dx, dy, dz);
        }
        if (dy == 31) {
            return DEEPSLATE_TILES;
        }
        if (dy >= 32) {
            int inset = (dy - 31) * 3;
            if (dx >= -53 + inset && dx <= -20 - inset && az <= 22 - inset) {
                return DEEPSLATE_TILES;
            }
            return AIR;
        }

        // Outer Library Walls
        if (westWall || eastWall || az == 22) {
            // East gallery doorway from Grand Staircase Landing (dx == -20, dz = -2..2, dy = 14..19)
            if (dx == -20 && az <= 2 && dy >= 14 && dy <= 19) {
                return AIR;
            }
            // Throne Room West doorway (dx == -22, dz = -12..-8, dy = 14..19)
            if (dx == -22 && dz >= -12 && dz <= -8 && dy >= 14 && dy <= 19) {
                return AIR;
            }
            // West sky-bridge doorway from Mezzanine (dx == -53, dz = -13..-11, dy = 22..26)
            if (westWall && dz >= -13 && dz <= -11 && dy >= 22 && dy <= 26) {
                return AIR;
            }
            // Tall Library Stained-Glass Lancet Windows on West wall
            if (westWall && (dz == -16 || dz == -4 || dz == 6 || dz == 16) && dy >= 16 && dy <= 28) {
                return stainedGlass(dx, dy, dz);
            }
            return wallStone(dx, dy, dz);
        }

        // Mezzanine Staircase ascending along West interior (dx = -48..-45, dz = 10..17, dy = 14..21)
        if (dx >= -48 && dx <= -45 && dz >= 10 && dz <= 17) {
            int stepY = 14 + (dz - 10);
            if (dy < stepY) {
                return wallStone(dx, dy, dz);
            }
            if (dy == stepY) {
                return STAIRS_SOUTH;
            }
            return AIR;
        }

        // Mezzanine Balcony Floor at dy = 21
        boolean inMezzanineZone = dx <= -47 || dx >= -27 || az >= 17;
        if (dy == 21 && inMezzanineZone) {
            if (!(dx >= -48 && dx <= -45 && dz >= 16 && dz <= 18)) {
                return POLISHED_DEEPSLATE;
            }
        }
        if (dy == 22 && ((dx == -47 && az <= 17) || (dx == -27 && az <= 17) || (az == 17 && dx >= -47 && dx <= -27))) {
            if (!(dx >= -48 && dx <= -45 && dz == 17)) {
                return DEEPSLATE_BRICK_WALL;
            }
        }

        // Towering Bookshelf Alcoves along North, South, and East interior walls
        if ((az == 21 || ((dx == -21 || (dx == -23 && dz < -4)) && az >= 4)) && ((dy >= 14 && dy <= 19) || (dy >= 22 && dy <= 28))) {
            if (dx % 4 == 0 || dz % 4 == 0) {
                return AbyssContent.ABYSSAL_PILLAR.defaultBlockState();
            }
            return BOOKSHELF;
        }

        // Free-standing Double-Sided Bookshelf Stacks on Lower Floor (dy = 14..18)
        if (dy >= 14 && dy <= 18 && (dx == -31 || dx == -43) && ((dz >= -15 && dz <= -7) || (dz >= 7 && dz <= 15))) {
            return (dz == -15 || dz == -7 || dz == 7 || dz == 15)
                    ? AbyssContent.ABYSSAL_PILLAR.defaultBlockState()
                    : BOOKSHELF;
        }

        // Central Enchanting & Scholar Rotunda at (-37, 14, 0)
        if (dx == -37 && dz == 0 && dy == 14) {
            return ENCHANTING_TABLE;
        }
        if (dy == 14 && ((dx == -34 && dz == 0) || (dx == -40 && dz == 0) || (dx == -37 && dz == -3) || (dx == -37 && dz == 3))) {
            return LECTERN;
        }
        if (dy == 14 && Math.abs(dx + 37) <= 4 && az <= 4 && !(dx == -37 && dz == 0)) {
            return PURPLE_CARPET;
        }
        if (dy == 30 && dx == -37 && dz == 0) {
            return SOUL_LANTERN_HANGING;
        }

        return AIR;
    }

    // -------------------------------------------------------------------------
    // 8. East Wing: Ruined Gothic Chapter Hall & Chapel (dx = 20..50, dz = -20..18)
    // -------------------------------------------------------------------------
    private BlockState eastChapelWingAt(int dx, int dy, int dz) {
        boolean westWall = dx == 20 || (dx == 22 && dz < -4);

        if (dy <= 12) {
            if (westWall || dx == 50 || dz == -20 || dz == 18) {
                if (dx == 50 && dy <= 7 && Math.abs(dz) <= 10 && dz % 6 == 0) {
                    return AIR;
                }
                return wallStone(dx, dy, dz);
            }
            return AIR;
        }

        if (dy > 32) {
            return AIR;
        }
        if (dy == 13) {
            return floorStone(dx, dy, dz);
        }
        if (dy >= 29) {
            if (rnd(dx, dy, dz, 41) < 0.42F) {
                return AIR;
            }
            int inset = (dy - 29) * 2;
            if (dx >= 20 + inset && dx <= 50 - inset && dz >= -20 + inset && dz <= 18 - inset) {
                return CRACKED_DEEPSLATE_TILES;
            }
            return AIR;
        }

        if (westWall || dx == 50 || dz == -20 || dz == 18) {
            if (dx == 20 && Math.abs(dz) <= 2 && dy >= 14 && dy <= 19) {
                return AIR;
            }
            if (dx == 22 && dz >= -12 && dz <= -8 && dy >= 14 && dy <= 19) {
                return AIR;
            }
            if (dx == 50 && dz >= -9 && dz <= -7 && dy >= 22 && dy <= 26) {
                return AIR;
            }
            if (dx == 50 && (dz == -14 || dz == -2 || dz == 10) && dy >= 16 && dy <= 25) {
                return rnd(dx, dy, dz, 42) < 0.35F ? AIR : stainedGlass(dx, dy, dz);
            }
            if (dy > 23 && rnd(dx, dy, dz, 43) < 0.25F) {
                return AIR;
            }
            return wallStone(dx, dy, dz);
        }

        if (dx == 44 && dz == -14 && dy == 14) {
            return CHISELED_BLACKSTONE;
        }
        if (dx == 44 && dz == -14 && dy == 15) {
            return SOUL_LANTERN_STANDING;
        }
        return AIR;
    }

    // -------------------------------------------------------------------------
    // 9. Ruined Outer Curtain Walls (Muralhas Destruídas)
    // -------------------------------------------------------------------------
    private BlockState ruinedCurtainWallAt(int dx, int dy, int dz) {
        int ax = Math.abs(dx);
        boolean onWestEastWall = (ax >= CURTAIN_X - 1 && ax <= CURTAIN_X + 1) && dz >= CURTAIN_Z_NORTH && dz <= CURTAIN_Z_SOUTH;
        boolean onNorthWall = (dz >= CURTAIN_Z_NORTH - 1 && dz <= CURTAIN_Z_NORTH + 1) && ax <= CURTAIN_X + 1;
        boolean onSouthWall = (dz >= CURTAIN_Z_SOUTH - 1 && dz <= CURTAIN_Z_SOUTH + 1) && ax <= CURTAIN_X + 1 && ax > 14;

        boolean buttress = ((ax == CURTAIN_X + 2 && dz % 12 == 0 && dz >= CURTAIN_Z_NORTH && dz <= CURTAIN_Z_SOUTH)
                || (dz == CURTAIN_Z_NORTH - 2 && dx % 12 == 0 && ax <= CURTAIN_X));

        if (!onWestEastWall && !onNorthWall && !onSouthWall && !buttress) {
            return null;
        }

        int localH = CURTAIN_H;
        if (dx < -60) {
            int dist = Math.abs(dz + 34);
            if (dist < 11) {
                localH = 3 + dist + (int) (rnd(dx, 0, dz, 51) * 3);
            }
        }
        if (dx > 60) {
            int dist = Math.abs(dz - 22);
            if (dist < 12) {
                localH = 2 + dist + (int) (rnd(dx, 0, dz, 52) * 3);
            }
        }
        if (dz < -65) {
            int d1 = Math.abs(dx + 28);
            int d2 = Math.abs(dx - 32);
            int d = Math.min(d1, d2);
            if (d < 10) {
                localH = 4 + d + (int) (rnd(dx, 0, dz, 53) * 3);
            }
        }

        if (dy >= localH) {
            return AIR;
        }
        if (localH == CURTAIN_H && dy == CURTAIN_H - 1 && ((dx + dz) & 1) == 1) {
            return AIR;
        }
        if (dy >= localH - 3 && rnd(dx, dy, dz, 54) < 0.32F) {
            return AIR;
        }
        return wallStone(dx, dy, dz);
    }

    // -------------------------------------------------------------------------
    // 10. Courtyard Colonnade, Rubble Piles & Fallen Masonry
    // -------------------------------------------------------------------------
    private BlockState courtyardAt(int dx, int dy, int dz) {
        int ax = Math.abs(dx);

        if ((ax == 58 || dz == -58 || dz == 46) && (dx % 10 == 0 || dz % 10 == 0)
                && ax <= 64 && dz >= -62 && dz <= 46) {
            int pHeight = 5 + (int) (rnd(dx, 0, dz, 61) * 14);
            if (dy < pHeight) {
                return AbyssContent.ABYSSAL_PILLAR.defaultBlockState();
            }
        }

        int breachDistWest = Math.hypot(dx + 70, dz + 34) < 10 ? (int) Math.hypot(dx + 70, dz + 34) : 99;
        int breachDistEast = Math.hypot(dx - 70, dz - 22) < 11 ? (int) Math.hypot(dx - 70, dz - 22) : 99;
        int breachDistNorth = Math.hypot(dx + 28, dz + 72) < 9 ? (int) Math.hypot(dx + 28, dz + 72) : 99;
        int minBreach = Math.min(breachDistWest, Math.min(breachDistEast, breachDistNorth));
        if (minBreach < 10) {
            int rubbleH = Math.max(0, (10 - minBreach) / 3 + (int) (rnd(dx, 0, dz, 62) * 2) - 1);
            if (dy < rubbleH) {
                float r = rnd(dx, dy, dz, 63);
                if (r < 0.35F) return COBBLED_DEEPSLATE;
                if (r < 0.70F) return AbyssContent.CRACKED_ABYSSAL_STONE.defaultBlockState();
                return DEEPSLATE_BRICK_SLAB_BOTTOM;
            }
        }

        if (dy == 0 && rnd(dx, 0, dz, 64) < 0.035F) {
            return rnd(dx, 1, dz, 65) < 0.5F
                    ? AbyssContent.CRACKED_ABYSSAL_STONE.defaultBlockState()
                    : DEEPSLATE_BRICK_SLAB_BOTTOM;
        }

        return AIR;
    }

    // =========================================================================
    // SUBTERRANEAN DUNGEONS & FLOOR SLAB (dy = -1 .. -17)
    // =========================================================================

    private BlockState layoutBelow(int dx, int dy, int dz) {
        if (!isInsideCastlePlateau(dx, dz)) {
            return null;
        }

        if (dx >= 14 && dx <= 17 && dz >= 12 && dz <= 27) {
            int stairY = 12 - dz;
            if (dy > stairY) {
                return AIR;
            }
            if (dy == stairY && dy <= -1) {
                return STAIRS_NORTH;
            }
            return wallStone(dx, dy, dz);
        }

        if (dy == -1) {
            return floorStone(dx, dy, dz);
        }

        if (Math.abs(dx) <= 36 && dz >= -28 && dz <= 26 && dy >= -16 && dy <= -7) {
            return dungeonAt(dx, dy, dz);
        }

        return null;
    }

    private BlockState dungeonAt(int dx, int dy, int dz) {
        int ax = Math.abs(dx);

        if (ax >= 35 || dz <= -27 || dz >= 25 || dy == -16 || dy == -7) {
            if (dy == -15 && ax < 35 && dz > -27 && dz < 25) {
                return floorStone(dx, dy, dz);
            }
            return wallStone(dx, dy, dz);
        }

        if (dy == -15) {
            return floorStone(dx, dy, dz);
        }
        if (dy == -8) {
            return DEEPSLATE_BRICKS;
        }

        if (dx >= 14 && dx <= 17 && dz == 24) {
            return dy <= -10 ? AIR : wallStone(dx, dy, dz);
        }

        // --- DUNGEON ZONE 1: CENTRAL GALLERY & PRISON CELL BLOCKS (dz = -6..23) ---
        if (dz >= -6 && dz <= 23) {
            if (ax <= 5) {
                if (ax == 5 && dz % 6 == 0) {
                    return AbyssContent.ABYSSAL_PILLAR.defaultBlockState();
                }
                if (dy == -9 && dx == 0 && dz % 8 == 0) {
                    return SOUL_LANTERN_HANGING;
                }
                if (dy == -9 && rnd(dx, dy, dz, 71) < 0.08F) {
                    return COBWEB;
                }
                return AIR;
            }

            if (ax == 6) {
                int cellLocal = Math.floorMod(dz + 6, 6);
                if (cellLocal == 0 || dy >= -10) {
                    return wallStone(dx, dy, dz);
                }
                if (cellLocal == 3 && dy <= -12) {
                    return AIR;
                }
                return IRON_BARS;
            }
            if (Math.floorMod(dz + 6, 6) == 0) {
                return wallStone(dx, dy, dz);
            }
            if (dy == -14) {
                if (ax == 32 && Math.floorMod(dz + 6, 6) == 2) {
                    return SKELETON_SKULL;
                }
                if (ax == 32 && Math.floorMod(dz + 6, 6) == 4 && rnd(dx, dy, dz, 72) < 0.5F) {
                    return BARREL;
                }
            }
            if (dy == -9 && rnd(dx, dy, dz, 73) < 0.12F) {
                return COBWEB;
            }
            return AIR;
        }

        if (dz == -7) {
            if (((dx >= -24 && dx <= -20) || (dx >= 20 && dx <= 24)) && dy <= -10) {
                return AIR;
            }
            return wallStone(dx, dy, dz);
        }

        if (ax <= 12) {
            return wallStone(dx, dy, dz);
        }

        // --- DUNGEON ZONE 2: WEST OSSUARY CRYPT (dx = -34..-13, dz = -26..-8) ---
        if (dx < 0) {
            if ((dx == -32 || dx == -15) && (dz == -12 || dz == -18) && dy == -14) {
                return POLISHED_DEEPSLATE_SLAB_BOTTOM;
            }
            if ((dx == -32 || dx == -15) && (dz == -11 || dz == -17) && dy == -14) {
                return WITHER_SKULL;
            }
            if (dy == -9 && dx == -22 && dz == -16) {
                return SOUL_LANTERN_HANGING;
            }
            return AIR;
        }

        // --- DUNGEON ZONE 3: EAST ABYSSAL ARMORY & TORTURE VAULT (dx = 13..34, dz = -26..-8) ---
        if (dy == -14) {
            if (dx == 16 && dz == -12) return ANVIL;
            if (dx == 16 && dz == -16) return GRINDSTONE;
            if (dx == 16 && dz == -20) return CAULDRON;
            if (dx == 22 && dz == -16) return CHISELED_BLACKSTONE;
            if ((dx == 15 || dx == 32) && (dz == -10 || dz == -24)) return GILDED_BLACKSTONE;
        }
        if (dy == -13 && dx == 22 && dz == -16) return SOUL_SOIL;
        if (dy == -12 && dx == 22 && dz == -16) return SOUL_FIRE;
        if (dy == -9 && dx == 22 && dz == -12) return SOUL_LANTERN_HANGING;
        return AIR;
    }

    // =========================================================================
    // GEOMETRIC & MATERIAL HELPERS
    // =========================================================================

    private static boolean isInsideCastlePlateau(int dx, int dz) {
        return Math.abs(dx) <= CURTAIN_X + 2 && dz >= CURTAIN_Z_NORTH - 2 && dz <= CURTAIN_Z_SOUTH + 2;
    }

    private static boolean isUnderViaductPier(int dx, int dz) {
        return Math.abs(dx) <= 7 && dz >= 54 && dz <= 84;
    }

    private static boolean isInsideGothicArch(int u, int y, int halfW, int yBase, int ySpring, int yApex) {
        if (y < yBase || y > yApex) {
            return false;
        }
        int au = Math.abs(u);
        if (y <= ySpring) {
            return au <= halfW;
        }
        if (yApex <= ySpring) {
            return false;
        }
        float t = (float) (y - ySpring) / (float) (yApex - ySpring);
        int allowed = Math.round(halfW * (float) Math.pow(1.0 - t, 0.72));
        return au <= allowed;
    }

    private BlockState wallStone(int dx, int dy, int dz) {
        float roll = rnd(dx, dy, dz, 7);
        if (roll < 0.04F) {
            return AbyssContent.AMETHYST_CORRUPTED_STONE.defaultBlockState();
        }
        if (roll < 0.22F) {
            return AbyssContent.CRACKED_ABYSSAL_STONE.defaultBlockState();
        }
        if (roll < 0.44F) {
            return DEEPSLATE_BRICKS;
        }
        if (roll < 0.54F) {
            return CRACKED_DEEPSLATE_BRICKS;
        }
        if (roll < 0.60F) {
            return POLISHED_BLACKSTONE_BRICKS;
        }
        return AbyssContent.ABYSSAL_STONE.defaultBlockState();
    }

    private BlockState floorStone(int dx, int dy, int dz) {
        float roll = rnd(dx, dy, dz, 8);
        if (roll < 0.16F) {
            return AbyssContent.CRACKED_ABYSSAL_STONE.defaultBlockState();
        }
        if (roll < 0.42F) {
            return DEEPSLATE_TILES;
        }
        if (roll < 0.50F) {
            return CRACKED_DEEPSLATE_TILES;
        }
        return AbyssContent.ABYSSAL_STONE.defaultBlockState();
    }

    private BlockState stainedGlass(int dx, int dy, int dz) {
        float roll = rnd(dx, dy, dz, 9);
        if (roll < 0.40F) {
            return PURPLE_GLASS;
        }
        if (roll < 0.65F) {
            return MAGENTA_GLASS;
        }
        if (roll < 0.85F) {
            return TINTED_GLASS;
        }
        return BLACK_GLASS;
    }

    private static float crumble(int dy, int height) {
        float p = Math.max(0.0F, Math.min(1.0F, (float) dy / Math.max(1, height)));
        return 0.06F + 0.52F * p * p;
    }

    private float rnd(int dx, int dy, int dz, int salt) {
        long h = seed + salt * 0x632BE59BD9B4E019L;
        h ^= dx * 0x9E3779B97F4A7C15L;
        h ^= dy * 0xC2B2AE3D27D4EB4FL;
        h ^= dz * 0x165667B19E3779F9L;
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return (h >>> 40) / (float) (1 << 24);
    }

    private static boolean isOpen(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty();
    }
}
