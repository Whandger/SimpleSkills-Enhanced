package com.github.ob_yekt.simpleskills.abyss;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

import java.util.ArrayList;
import java.util.List;

/**
 * A colossal Abyss masonry viaduct. Seed-stable erosion and pier-aware multi-bay
 * collapse patterns make individual bridges look like ancient ruined megastructures.
 */
public final class AbyssalBridgePiece extends StructurePiece {
    public static final int HALF_WIDTH = 10; // 21-block wide colossal viaduct
    private static final int WORLD_FLOOR = -255;
    private static final int MAX_PIER_DEPTH = 180;
    private static final int MAX_RUIN_REACH = 112;
    private static final int MAX_FALLEN_COLUMN_LENGTH = 84;
    // Maximum lateral travel caused by the diagonal fall plus the column's width.
    private static final int MAX_FALLEN_LATERAL_REACH = (int) Math.ceil(MAX_FALLEN_COLUMN_LENGTH * 0.37D) + 8;
    private static final int MAX_FALLEN_LATERAL_PROBE = MAX_FALLEN_LATERAL_REACH + 6;
    // Headroom above the deck that the piece carves itself. Matches the structure's clearance check.
    private static final int HEADROOM_HEIGHT = 10;

    public static final StructurePieceType TYPE = (context, tag) -> new AbyssalBridgePiece(tag);

    private final int startX;
    private final int startZ;
    private final int endX;
    private final int endZ;
    private final int baseY;
    private final boolean alongX;
    private final int archHeight;
    private final long seed;

    // This override is used only by the standalone preview app. World generation keeps AUTO.
    private String previewDamageMode = "AUTO";

    public AbyssalBridgePiece(int startX, int startZ, int endX, int endZ,
                              int baseY, boolean alongX, int archHeight, long seed) {
        super(TYPE, 0, createBoundingBox(startX, startZ, endX, endZ));
        this.startX = startX;
        this.startZ = startZ;
        this.endX = endX;
        this.endZ = endZ;
        this.baseY = baseY;
        this.alongX = alongX;
        this.archHeight = archHeight;
        this.seed = seed;
    }

    public AbyssalBridgePiece(CompoundTag tag) {
        this(tag.getIntOr("sx", 0), tag.getIntOr("sz", 0),
                tag.getIntOr("ex", 0), tag.getIntOr("ez", 0),
                tag.getIntOr("by", -160), tag.getBooleanOr("ax", true),
                tag.getIntOr("ah", 22), tag.getLongOr("seed", 0L));
    }

    /** Preview-only selector. Use AUTO in real world generation. */
    public void setPreviewDamageMode(String mode) {
        if (mode == null) {
            previewDamageMode = "AUTO";
            return;
        }
        String normalized = mode.trim().toUpperCase();
        previewDamageMode = switch (normalized) {
            case "INTACT", "WEATHERED", "WORN", "RUINED", "CENTER_COLLAPSE", "FALLEN_PIER", "TOTAL_RUIN" -> normalized;
            default -> "AUTO";
        };
    }

    public String getDamageDescription(int length) {
        int[] pierPositions = supportPositions(Math.max(1, length));
        return resolveDamageProfile(length, pierPositions).description;
    }

    private static BoundingBox createBoundingBox(int startX, int startZ, int endX, int endZ) {
        boolean alongX = startX != endX;
        int xReach = alongX ? Math.max(HALF_WIDTH + 6, MAX_FALLEN_LATERAL_REACH) : MAX_RUIN_REACH;
        int zReach = alongX ? MAX_RUIN_REACH : Math.max(HALF_WIDTH + 6, MAX_FALLEN_LATERAL_REACH);
        return new BoundingBox(
                Math.min(startX, endX) - xReach,
                WORLD_FLOOR,
                Math.min(startZ, endZ) - zReach,
                Math.max(startX, endX) + xReach,
                -8,
                Math.max(startZ, endZ) + zReach);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("sx", startX);
        tag.putInt("sz", startZ);
        tag.putInt("ex", endX);
        tag.putInt("ez", endZ);
        tag.putInt("by", baseY);
        tag.putBoolean("ax", alongX);
        tag.putInt("ah", archHeight);
        tag.putLong("seed", seed);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        int length = Math.abs(endX - startX) + Math.abs(endZ - startZ);
        if (length <= 0) return;

        int[] pierPositions = supportPositions(length);
        DamageProfile damage = resolveDamageProfile(length, pierPositions);

        for (int distance = 0; distance <= length; distance++) {
            double progress = distance / (double) length;
            int x = startX + (endX - startX) * distance / length;
            int z = startZ + (endZ - startZ) * distance / length;
            int deckY = getDeckY(progress);
            if (!isNearBox(box, x, z)) continue;

            buildDeckCrossSection(level, box, x, z, deckY, distance, length, pierPositions, damage);
            if (distance % 10 == 0 && !isInsideCollapseRibZone(distance, damage)) {
                buildUndersideRib(level, box, x, z, deckY, distance, damage);
            }
        }

        buildPiers(level, box, length, pierPositions, damage);
        if (damage.hasCollapse()) {
            for (CollapseZone zone : damage.collapseZones) {
                buildCollapsedSpanDebris(level, box, length, zone);
            }
        }
        buildEndAnchors(level, box, damage);
    }

    private void buildDeckCrossSection(WorldGenLevel level, BoundingBox box,
                                       int x, int z, int deckY, int distance, int length,
                                       int[] pierPositions, DamageProfile damage) {
        boolean[] hasParapetWall = new boolean[HALF_WIDTH * 2 + 1];
        int haunchDepth = computeHaunchDepth(distance, length, pierPositions);

        for (int offset = -HALF_WIDTH; offset <= HALF_WIDTH; offset++) {
            if (isCollapsedDeckBlock(distance, offset, damage)) continue;

            int px = x + (alongX ? 0 : offset);
            int pz = z + (alongX ? offset : 0);
            int absOffset = Math.abs(offset);
            int fractureProximity = distanceToCollapseEdge(distance, offset, damage);
            boolean nearFracture = fractureProximity >= 0 && fractureProximity <= 3;

            boolean missingTile = damage.wear >= 2
                    && !nearFracture
                    && damageRoll(distance, offset, 11) < damage.missingTileChance;
            boolean missingUpperFoundation = damage.wear >= 3
                    && !missingTile
                    && !nearFracture
                    && absOffset < HALF_WIDTH - 2
                    && damageRoll(distance, offset, 19) < damage.missingFoundationChance;

            // 1. Deep structural soffit & spandrel haunches (deckY - 3 and deeper near piers/cliffs).
            int bottomDrop = 3 + haunchDepth;
            for (int drop = 3; drop <= bottomDrop; drop++) {
                if (drop > 3 && absOffset == HALF_WIDTH && (distance % 2 == 1)) continue;
                if (damage.wear >= 3 && drop == bottomDrop && damageRoll(distance, offset + drop, 21) < 16) {
                    continue;
                }
                BlockState structural;
                if (drop == 3 && (absOffset == HALF_WIDTH || absOffset == HALF_WIDTH - 1)) {
                    structural = AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState();
                } else if (drop == 3 && (offset == 0 || absOffset == 5)) {
                    structural = AbyssContent.ABYSSAL_PILLAR.defaultBlockState();
                } else {
                    structural = AbyssContent.ABYSSAL_BRICKS.defaultBlockState();
                }
                place(level, box, px, deckY - drop, pz, structural);
            }

            // 2. Two upper sub-deck masonry courses (deckY - 2 and deckY - 1).
            place(level, box, px, deckY - 2, pz,
                    (absOffset == HALF_WIDTH)
                            ? AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState()
                            : AbyssContent.ABYSSAL_BRICKS.defaultBlockState());

            if (!missingUpperFoundation) {
                place(level, box, px, deckY - 1, pz, AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
            }

            if (missingTile) {
                place(level, box, px, deckY, pz, Blocks.AIR.defaultBlockState());
                continue;
            }

            // 3. Roadway surface (deckY): 21 blocks wide with outer cornices, side walkways, twin carriageways & royal spine.
            BlockState surface;
            if (absOffset >= HALF_WIDTH - 2 || absOffset == 7 || absOffset == 1) {
                surface = (nearFracture || (damage.wear >= 2 && damageRoll(distance, offset, 23) < 30))
                        ? AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState()
                        : AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState();
            } else if (nearFracture || isCracked(distance, offset, 7 + damage.wear * 13)) {
                surface = AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState();
            } else if (offset == 0) {
                if (distance % 32 == 16 && damage.wear < 3) {
                    surface = AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState();
                } else if (distance % 16 == 8 && damage.wear < 3) {
                    surface = AbyssContent.CHISELED_ABYSSAL_STONE.defaultBlockState();
                } else {
                    surface = AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState();
                }
            } else {
                surface = AbyssContent.ABYSSAL_TILES.defaultBlockState();
            }
            place(level, box, px, deckY, pz, surface);
            carveHeadroom(level, box, px, deckY, pz);

            // 4. Monumental parapets and bastion towers along the outer edges (±HALF_WIDTH).
            if (absOffset == HALF_WIDTH) {
                boolean strippedByCollapse = fractureProximity >= 0 && fractureProximity <= 2;
                boolean missingParapet = strippedByCollapse
                        || (damage.wear >= 1 && damageRoll(distance, offset, 31) < damage.missingParapetChance);
                if (!missingParapet) {
                    boolean isPylon = (distance % 10 == 0);
                    BlockState wall;
                    if (isPylon) {
                        wall = (nearFracture || (damage.wear >= 3 && damageRoll(distance, offset, 37) < 25))
                                ? AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState()
                                : AbyssContent.ABYSSAL_PILLAR.defaultBlockState();
                    } else {
                        wall = (nearFracture || (damage.wear >= 3 && damageRoll(distance, offset, 37) < 22))
                                ? AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState()
                                : AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState();
                    }
                    place(level, box, px, deckY + 1, pz, wall);
                    hasParapetWall[offset + HALF_WIDTH] = true;

                    if (!nearFracture
                            && isPylon
                            && damageRoll(distance, offset, 41) >= damage.missingParapetChance) {
                        place(level, box, px, deckY + 2, pz,
                                AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
                        if (distance % 30 == 0 && damage.wear < 3) {
                            place(level, box, px, deckY + 3, pz,
                                    AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
                            if (damage.wear < 2) {
                                place(level, box, px, deckY + 4, pz,
                                        AbyssContent.CHISELED_ABYSSAL_STONE.defaultBlockState());
                            }
                        }
                    }
                }
            }
        }

        // Place braziers on solid parapet bastions every 30 blocks (alternating sides).
        if (distance % 30 == 15) {
            int brazierSide = ((distance / 30) % 2 == 0) ? -HALF_WIDTH : HALF_WIDTH;
            int fractureProximity = distanceToCollapseEdge(distance, brazierSide, damage);
            boolean safeFromCollapse = fractureProximity < 0 || fractureProximity > 3;
            if (safeFromCollapse
                    && hasParapetWall[brazierSide + HALF_WIDTH]
                    && damageRoll(distance, brazierSide, 43) > damage.missingParapetChance + 12) {
                int brazierX = x + (alongX ? 0 : brazierSide);
                int brazierZ = z + (alongX ? brazierSide : 0);
                place(level, box, brazierX, deckY + 2, brazierZ,
                        AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
            }
        }
    }

    /** Extra masonry haunch depth near the cliff abutments and standing piers. */
    private int computeHaunchDepth(int distance, int length, int[] pierPositions) {
        int edgeDist = Math.min(distance, length - distance);
        int depth = 0;
        if (edgeDist <= 20) {
            depth = Math.max(depth, (21 - edgeDist) / 4); // up to +5 blocks near cliff walls
        }
        for (int pierDist : pierPositions) {
            int d = Math.abs(distance - pierDist);
            if (d <= 15) {
                depth = Math.max(depth, (16 - d) / 4); // up to +4 blocks flaring into pier capitals
            }
        }
        return depth;
    }

    /** Deep transverse arched ribs make the underside look like a monumental stone viaduct. */
    private void buildUndersideRib(WorldGenLevel level, BoundingBox box,
                                   int x, int z, int deckY, int distance, DamageProfile damage) {
        if (damage.wear >= 2 && damageRoll(distance, 0, 53) < 12 + damage.wear * 9) return;
        for (int offset = -HALF_WIDTH; offset <= HALF_WIDTH; offset++) {
            if (isCollapsedDeckBlock(distance, offset, damage)) continue;
            if (damage.wear >= 3 && damageRoll(distance, offset, 59) < 15) continue;
            int drop = 4 + (int) Math.round(5.0D * (1.0D - Math.abs(offset) / (double) HALF_WIDTH));
            int px = x + (alongX ? 0 : offset);
            int pz = z + (alongX ? offset : 0);
            BlockState rib = (Math.abs(offset) == HALF_WIDTH || offset == 0)
                    ? AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState()
                    : AbyssContent.ABYSSAL_BRICKS.defaultBlockState();
            place(level, box, px, deckY - drop, pz, rib);
        }

        // Five vertical spandrel webs tying the rib into the 4-block-thick deck.
        for (int side : new int[]{-HALF_WIDTH, -5, 0, 5, HALF_WIDTH}) {
            if (isCollapsedDeckBlock(distance, side, damage)) continue;
            int px = x + (alongX ? 0 : side);
            int pz = z + (alongX ? side : 0);
            int maxDrop = 3 + (int) Math.round(5.0D * (1.0D - Math.abs(side) / (double) HALF_WIDTH));
            if (damage.wear >= 3 && damageRoll(distance, side, 61) < 22) continue;
            for (int down = 4; down <= maxDrop; down++) {
                place(level, box, px, deckY - down, pz,
                        AbyssContent.ABYSSAL_PILLAR.defaultBlockState());
            }
        }
    }

    /**
     * Builds monumental 7x7 viaduct piers spaced across the chasm.
     * Piers inside a collapsed bay either crumble into a floor stump or topple sideways.
     */
    private void buildPiers(WorldGenLevel level, BoundingBox box, int length,
                            int[] positions, DamageProfile damage) {
        int fallenIndex = -1;
        if (damage.fallenPier && positions.length > 0) {
            fallenIndex = selectFallenPierIndex(positions, damage);
        }

        for (int index = 0; index < positions.length; index++) {
            int distance = positions[index];
            int x = startX + (endX - startX) * distance / length;
            int z = startZ + (endZ - startZ) * distance / length;
            int deckY = getDeckY(distance / (double) length);
            boolean insideCollapsedBay = isInsideCollapseRibZone(distance, damage);

            if (index == fallenIndex) {
                // No floor read at the pier centre: the centre can lie several chunks away, and the
                // worldgen log flagged unsafe reads there. Every floor read inside buildFallenPier is
                // guarded per column, so this call only touches blocks inside the current chunk.
                if (!fallenPierTouchesBox(box, x, z, deckY)) continue;
                buildFallenPier(level, box, x, z, deckY, index, !insideCollapsedBay);
                continue;
            }

            if (insideCollapsedBay) {
                // The pier failed along with the collapsed span above it: leave a shattered stump on the floor.
                if (standingPierTouchesBox(box, x, z, deckY)) {
                    buildCollapsedPierBase(level, box, x, z, deckY, index);
                }
                continue;
            }

            if (!standingPierTouchesBox(box, x, z, deckY)) continue;
            buildStandingPier(level, box, x, z, deckY, damage);
        }
    }

    /** Prefers toppling a pier inside or bordering a collapsed span so cause and effect align. */
    private int selectFallenPierIndex(int[] positions, DamageProfile damage) {
        if (damage.hasCollapse()) {
            List<Integer> failedCandidates = new ArrayList<>();
            List<Integer> borderCandidates = new ArrayList<>();
            for (int i = 0; i < positions.length; i++) {
                int dist = positions[i];
                if (isInsideCollapseRibZone(dist, damage)) {
                    failedCandidates.add(i);
                } else {
                    for (CollapseZone zone : damage.collapseZones) {
                        if (Math.abs(dist - zone.startDist) <= 14 || Math.abs(dist - zone.endDist) <= 14) {
                            borderCandidates.add(i);
                            break;
                        }
                    }
                }
            }
            if (!failedCandidates.isEmpty()) {
                return failedCandidates.get(Math.floorMod((int) mix(seed ^ 0x4F1BBCDCBFA54001L), failedCandidates.size()));
            }
            if (!borderCandidates.isEmpty()) {
                return borderCandidates.get(Math.floorMod((int) mix(seed ^ 0x4F1BBCDCBFA54001L), borderCandidates.size()));
            }
        }
        return Math.floorMod((int) mix(seed ^ 0x4F1BBCDCBFA54001L), positions.length);
    }

    /** Scales pier count with span length (~1 colossal pier every 110 blocks). */
    private int[] supportPositions(int length) {
        if (length < 140) {
            return new int[]{length / 2};
        }
        int intervals = Math.max(3, (int) Math.round(length / 110.0D));
        int count = intervals - 1;
        int[] positions = new int[count];
        for (int i = 0; i < count; i++) {
            positions[i] = (int) Math.round((length * (i + 1.0D)) / intervals);
        }
        return positions;
    }

    private void buildStandingPier(WorldGenLevel level, BoundingBox box, int x, int z,
                                   int deckY, DamageProfile damage) {
        int centerGround = findNaturalFloor(level, x, z, deckY);
        if (centerGround == Integer.MIN_VALUE || deckY - centerGround < 14) {
            return;
        }

        // 1. Broad 9x9 stepped masonry plinth seated flush on uneven cavern terrain.
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                int bx = x + dx;
                int bz = z + dz;
                int localGround = findNaturalFloor(level, bx, bz, deckY);
                if (localGround == Integer.MIN_VALUE || deckY - localGround < 14) continue;
                place(level, box, bx, localGround + 1, bz,
                        AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
                place(level, box, bx, localGround + 2, bz,
                        AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
                if (Math.abs(dx) <= 3 && Math.abs(dz) <= 3) {
                    place(level, box, bx, localGround + 3, bz,
                            AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState());
                    place(level, box, bx, localGround + 4, bz,
                            AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
                }
            }
        }

        // 2. Colossal 7x7 buttressed tower shaft (with trimmed corners -> 45-block cross-section).
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.abs(dx) == 3 && Math.abs(dz) == 3) continue;
                int bx = x + dx;
                int bz = z + dz;
                int localGround = findNaturalFloor(level, bx, bz, deckY);
                if (localGround == Integer.MIN_VALUE || deckY - localGround < 14) continue;

                boolean isInnerCore = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
                boolean isOuterButtress = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                for (int y = localGround + 5; y <= deckY - 8; y++) {
                    BlockState shaft;
                    if (damage.wear >= 2 && y % 13 == 0 && isOuterButtress) {
                        shaft = AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState();
                    } else if (isInnerCore) {
                        shaft = AbyssContent.ABYSSAL_BRICKS.defaultBlockState();
                    } else {
                        shaft = AbyssContent.ABYSSAL_PILLAR.defaultBlockState();
                    }
                    place(level, box, bx, y, bz, shaft);
                }
            }
        }

        // 3. Stepped corbeled capital supporting the 21-block-wide deck.
        fillSquare(level, box, x, deckY - 7, z, 3,
                AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
        fillSquare(level, box, x, deckY - 6, z, 4,
                AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState());
        fillSquare(level, box, x, deckY - 5, z, 4,
                AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
        fillSquare(level, box, x, deckY - 4, z, 4,
                AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState());
    }

    /** Shattered 9x9 pier stump left on the cavern floor when a pier collapses with the central span. */
    private void buildCollapsedPierBase(WorldGenLevel level, BoundingBox box,
                                        int x, int z, int deckY, int index) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                int bx = x + dx;
                int bz = z + dz;
                if (!box.isInside(bx, clampY(box, deckY), bz)) continue;
                int localFloor = findAbyssFloor(level, bx, bz, deckY);
                if (localFloor == Integer.MIN_VALUE) continue;
                int distSq = dx * dx + dz * dz;
                if (distSq > 18) continue;

                int roll = damageRoll(dx + 7, dz + 7, index + 33);
                if (distSq > 10 && roll < 38) continue;

                place(level, box, bx, localFloor + 1, bz,
                        (roll % 3 == 0)
                                ? AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState()
                                : AbyssContent.ABYSSAL_BRICKS.defaultBlockState());

                if (distSq <= 9) {
                    int stumpHeight = 2 + Math.floorMod(roll, 5);
                    for (int h = 2; h <= stumpHeight; h++) {
                        place(level, box, bx, localFloor + h, bz,
                                (h == stumpHeight)
                                        ? AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState()
                                        : AbyssContent.ABYSSAL_PILLAR.defaultBlockState());
                    }
                }
            }
        }
    }

    private boolean standingPierTouchesBox(BoundingBox box, int x, int z, int deckY) {
        int probeY = clampY(box, deckY);
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (box.isInside(x + dx, probeY, z + dz)) return true;
            }
        }
        return false;
    }

    private boolean fallenPierTouchesBox(BoundingBox box, int x, int z, int deckY) {
        int direction = (Math.floorMod((int) mix(seed ^ 0x71A7B31DL), 2) == 0) ? -1 : 1;
        int probeY = clampY(box, deckY);
        for (int along = 0; along <= MAX_FALLEN_COLUMN_LENGTH + 10; along++) {
            for (int across = -MAX_FALLEN_LATERAL_PROBE; across <= MAX_FALLEN_LATERAL_PROBE; across++) {
                int bx = x + (alongX ? across : direction * along);
                int bz = z + (alongX ? direction * along : across);
                if (box.isInside(bx, probeY, bz)) return true;
            }
        }
        return false;
    }

    /**
     * Builds a shattered 9x9 pier base and a fractured, diagonally-toppled colossal column
     * broken into grounded stone drums on the cavern floor.
     */
    private void buildFallenPier(WorldGenLevel level, BoundingBox box, int x, int z,
                                 int deckY, int index, boolean hasDeckAbove) {
        int direction = (Math.floorMod((int) mix(seed ^ 0x71A7B31DL), 2) == 0) ? -1 : 1;
        int driftSign = (Math.floorMod((int) mix(seed ^ 0x2E8A91C5D4B73019L), 2) == 0) ? -1 : 1;
        double driftSlope = driftSign * (0.22D + (Math.floorMod((int) mix(seed ^ 0x6D1F3A8B2C94E705L), 16) / 100.0D));

        // Length comes from the seed, not from the floor under the pier centre, so every chunk
        // that owns part of this column derives the same length without reading outside its range.
        int columnLength = MAX_FALLEN_COLUMN_LENGTH - Math.floorMod(damageRoll(index, 1, 113), 29);

        // 1. Upper snapped 7x7 stump attached to the bridge underside ONLY if the deck above still exists.
        if (hasDeckAbove) {
            for (int down = 4; down <= 10; down++) {
                int y = deckY - down;
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        if (Math.abs(dx) == 3 && Math.abs(dz) == 3) continue;
                        int snapRoll = damageRoll(dx + 4, dz + down, index + 13);
                        if (down >= 7 && (Math.abs(dx) == 3 || Math.abs(dz) == 3) && snapRoll < 65) continue;
                        if (down >= 9 && (Math.abs(dx) >= 2 || Math.abs(dz) >= 2) && snapRoll < 55) continue;
                        if (down == 10 && (Math.abs(dx) >= 1 || Math.abs(dz) >= 1 || snapRoll < 40)) continue;
                        place(level, box, x + dx, y, z + dz, down >= 8
                                ? AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState()
                                : AbyssContent.ABYSSAL_PILLAR.defaultBlockState());
                    }
                }
            }
        }

        // 2. Shattered 9x9 lower pedestal still rooted on the cavern floor where the tower snapped off.
        buildCollapsedPierBase(level, box, x, z, deckY, index);

        // 3. Toppled colossal shaft (7 blocks wide, 5 blocks tall) split into 4-5 broken drum segments.
        int startAlong = 6;
        int segmentCount = columnLength >= 48 ? 5 : 4;
        int usableLength = columnLength - startAlong;

        for (int seg = 0; seg < segmentCount; seg++) {
            int segStart = startAlong + (usableLength * seg) / segmentCount + (seg == 0 ? 0 : 1);
            int segEnd = startAlong + (usableLength * (seg + 1)) / segmentCount - (seg == segmentCount - 1 ? 0 : 1);
            if (segEnd <= segStart) continue;

            int lateralKick = (seg == 0) ? 0
                    : (Math.floorMod(damageRoll(seg, index, 41), 3) - 1) * (seg >= 2 ? 1 : 0);

            for (int along = segStart; along <= segEnd; along++) {
                int lateralCenter = (int) Math.round(along * driftSlope) + lateralKick;
                boolean isCapitalEnd = (seg == 0 && along <= segStart + 4);
                boolean isImpactEnd = (seg == segmentCount - 1 && along >= segEnd - 4);
                int halfWidth = isCapitalEnd ? 4 : 3;

                for (int across = -halfWidth; across <= halfWidth; across++) {
                    int lateralOffset = lateralCenter + across;
                    int bx = x + (alongX ? lateralOffset : direction * along);
                    int bz = z + (alongX ? direction * along : lateralOffset);
                    if (!box.isInside(bx, clampY(box, deckY), bz)) continue;

                    int localFloor = findAbyssFloor(level, bx, bz, deckY);
                    if (localFloor == Integer.MIN_VALUE) continue;
                    if (deckY - localFloor < 14) continue;

                    int maxH = 5;
                    if (Math.abs(across) == halfWidth) {
                        maxH = 3;
                    } else if (Math.abs(across) == halfWidth - 1) {
                        maxH = 4;
                    } else if (isImpactEnd) {
                        maxH = (Math.abs(across) <= 1) ? 3 : 1;
                    } else if (Math.abs(across) >= 1 && damageRoll(along, across, index + 53) < 26) {
                        maxH = 4;
                    }

                    for (int height = 1; height <= maxH; height++) {
                        BlockState state;
                        if (isCapitalEnd && height >= 3) {
                            state = AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState();
                        } else if (height >= 2 && height <= 4 && Math.abs(across) <= 1) {
                            state = AbyssContent.ABYSSAL_PILLAR.defaultBlockState();
                        } else if (height == maxH && damageRoll(along, across, index + 59) < 32) {
                            state = AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState();
                        } else {
                            state = AbyssContent.ABYSSAL_BRICKS.defaultBlockState();
                        }
                        place(level, box, bx, localFloor + height, bz, state);
                    }
                }

                // Scattered side chips along the fallen column so it sits bedded in rubble.
                for (int side : new int[]{-(halfWidth + 1), halfWidth + 1}) {
                    if (damageRoll(along, side, index + 63) < 55) continue;
                    int lateralOffset = lateralCenter + side;
                    int bx = x + (alongX ? lateralOffset : direction * along);
                    int bz = z + (alongX ? direction * along : lateralOffset);
                    if (!box.isInside(bx, clampY(box, deckY), bz)) continue;
                    int localFloor = findAbyssFloor(level, bx, bz, deckY);
                    if (localFloor == Integer.MIN_VALUE) continue;
                    place(level, box, bx, localFloor + 1, bz,
                            AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState());
                }
            }

            // Crushed stone joint between snapped segments.
            if (seg < segmentCount - 1) {
                int jointAlong = segEnd + 1;
                int lateralCenter = (int) Math.round(jointAlong * driftSlope) + lateralKick;
                for (int across = -4; across <= 4; across++) {
                    int bx = x + (alongX ? (lateralCenter + across) : direction * jointAlong);
                    int bz = z + (alongX ? direction * jointAlong : (lateralCenter + across));
                    if (!box.isInside(bx, clampY(box, deckY), bz)) continue;
                    int localFloor = findAbyssFloor(level, bx, bz, deckY);
                    if (localFloor == Integer.MIN_VALUE) continue;
                    int roll = damageRoll(jointAlong, across, index + 61);
                    if (roll < 24) continue;
                    place(level, box, bx, localFloor + 1, bz, roll % 2 == 0
                            ? AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState()
                            : AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
                }
            }
        }

        // 4. Grounded impact rubble apron around the shattered tip of the column.
        int tipLateral = (int) Math.round(columnLength * driftSlope);
        for (int along = columnLength - 6; along <= columnLength + 9; along++) {
            for (int across = -7; across <= 7; across++) {
                int lateralOffset = tipLateral + across;
                int px = x + (alongX ? lateralOffset : direction * along);
                int pz = z + (alongX ? direction * along : lateralOffset);
                if (!box.isInside(px, clampY(box, deckY), pz)) continue;
                int localFloor = findAbyssFloor(level, px, pz, deckY);
                if (localFloor == Integer.MIN_VALUE) continue;
                int roll = damageRoll(along, across, index + 67);
                if (roll < 24) continue;
                place(level, box, px, localFloor + 1, pz, roll % 2 == 0
                        ? AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState()
                        : AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
                if (roll > 70 && Math.abs(across) <= 4) {
                    place(level, box, px, localFloor + 2, pz,
                            AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
                }
            }
        }
    }

    /** Finds the nearest non-air, non-fluid floor under a proposed support. */
    private int findNaturalFloor(WorldGenLevel level, int x, int z, int deckY) {
        int lowest = Math.max(level.getMinY(), Math.max(WORLD_FLOOR, deckY - MAX_PIER_DEPTH));
        return scanFloor(level, x, z, deckY - 11, lowest);
    }

    /** Finds the actual cavern floor all the way to the bottom of the Abyss. */
    private int findAbyssFloor(WorldGenLevel level, int x, int z, int deckY) {
        int lowest = Math.max(level.getMinY(), WORLD_FLOOR);
        return scanFloor(level, x, z, deckY - 11, lowest);
    }

    private int scanFloor(WorldGenLevel level, int x, int z, int startY, int lowestY) {
        for (int y = startY; y >= lowestY; y--) {
            BlockState state = level.getBlockState(new BlockPos(x, y, z));
            if (!state.isAir()
                    && state.getFluidState().isEmpty()
                    && !isBridgeGeneratedBlock(state)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static boolean isBridgeGeneratedBlock(BlockState state) {
        return state.is(AbyssContent.ABYSSAL_BRICKS)
                || state.is(AbyssContent.POLISHED_ABYSSAL_STONE)
                || state.is(AbyssContent.CRACKED_ABYSSAL_TILES)
                || state.is(AbyssContent.CHISELED_ABYSSAL_STONE)
                || state.is(AbyssContent.ABYSSAL_TILES)
                || state.is(AbyssContent.ABYSSAL_BRICK_WALL)
                || state.is(AbyssContent.ABYSSAL_PILLAR)
                || state.is(AbyssContent.GILDED_ABYSSAL_BRICKS)
                || state.is(AbyssContent.ABYSSAL_BRAZIER);
    }

    /**
     * Rebuilds a collapsed bay on the cavern floor as fractured 21-block-wide roadway slabs,
     * toppled parapet sections, snapped ribs, and surrounding impact scatter.
     */
    private void buildCollapsedSpanDebris(WorldGenLevel level, BoundingBox box,
                                          int length, CollapseZone zone) {
        int center = zone.centerDist;
        int halfLength = zone.halfLength + 6;
        int scatterHalfWidth = HALF_WIDTH + 6;

        // Fast chunk-intersection cull so multi-hundred-block collapses remain instant to generate.
        int minDist = Math.max(0, center - halfLength);
        int maxDist = Math.min(length, center + halfLength);
        int x1 = startX + (endX - startX) * minDist / length;
        int z1 = startZ + (endZ - startZ) * minDist / length;
        int x2 = startX + (endX - startX) * maxDist / length;
        int z2 = startZ + (endZ - startZ) * maxDist / length;
        int minX = Math.min(x1, x2) - (alongX ? 2 : scatterHalfWidth);
        int maxX = Math.max(x1, x2) + (alongX ? 2 : scatterHalfWidth);
        int minZ = Math.min(z1, z2) - (alongX ? scatterHalfWidth : 2);
        int maxZ = Math.max(z1, z2) + (alongX ? scatterHalfWidth : 2);
        if (maxX < box.minX() || minX > box.maxX() || maxZ < box.minZ() || minZ > box.maxZ()) {
            return;
        }

        int x = startX + (endX - startX) * center / length;
        int z = startZ + (endZ - startZ) * center / length;
        int deckY = getDeckY(center / (double) length);

        for (int along = -halfLength; along <= halfLength; along++) {
            for (int across = -scatterHalfWidth; across <= scatterHalfWidth; across++) {
                int px = x + (alongX ? along : across);
                int pz = z + (alongX ? across : along);
                if (!box.isInside(px, clampY(box, deckY), pz)) continue;
                int localFloor = findAbyssFloor(level, px, pz, deckY);
                if (localFloor == Integer.MIN_VALUE) continue;

                int absAcross = Math.abs(across);
                if (absAcross > HALF_WIDTH) {
                    int scatterRoll = damageRoll(along + center, across, 109);
                    int distOut = absAcross - HALF_WIDTH;
                    if (scatterRoll < 48 + distOut * 9) continue;
                    place(level, box, px, localFloor + 1, pz,
                            (scatterRoll % 2 == 0)
                                    ? AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState()
                                    : AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
                    continue;
                }

                // Divide the fallen 21-block roadway into fractured 6-7 block slabs separated by jagged cracks.
                int slabId = Math.floorDiv(along + halfLength, 7);
                int slabShift = Math.floorMod(damageRoll(slabId, 0, 73), 3) - 1;
                boolean isCrackLine = Math.floorMod(along + halfLength + slabShift, 7) == 0;
                int roll = damageRoll(along + center, across, 79);

                if (isCrackLine && roll < 58) continue;
                int edgePenalty = absAcross == HALF_WIDTH ? 14 : 0;
                if (roll < 12 + edgePenalty) continue;

                // Layer 1 (localFloor + 1): crushed brick sub-deck of the fallen bridge.
                BlockState foundation = (roll % 3 == 0)
                        ? AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState()
                        : AbyssContent.ABYSSAL_BRICKS.defaultBlockState();
                place(level, box, px, localFloor + 1, pz, foundation);

                // Layer 2 (localFloor + 2): intact-looking broken roadway slabs (tiles, curbs, royal spine).
                int slabRoll = damageRoll(along + center, across, 97);
                boolean hasRoadwayDeck = !isCrackLine && slabRoll > 25 && Math.abs(along) < halfLength - 1;
                if (hasRoadwayDeck) {
                    BlockState surface;
                    if (absAcross >= HALF_WIDTH - 2 || absAcross == 7 || absAcross == 1) {
                        surface = (slabRoll % 3 == 0)
                                ? AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState()
                                : AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState();
                    } else if (across == 0) {
                        surface = (Math.abs(along) % 6 == 0)
                                ? AbyssContent.CHISELED_ABYSSAL_STONE.defaultBlockState()
                                : AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState();
                    } else {
                        surface = (slabRoll % 3 == 0)
                                ? AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState()
                                : AbyssContent.ABYSSAL_TILES.defaultBlockState();
                    }
                    place(level, box, px, localFloor + 2, pz, surface);

                    // Layer 3 (localFloor + 3): broken parapet wall chunks and masonry mounds on top of slabs.
                    if (absAcross == HALF_WIDTH && slabRoll > 48) {
                        place(level, box, px, localFloor + 3, pz,
                                AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState());
                    } else if (absAcross <= 6 && slabRoll > 85) {
                        place(level, box, px, localFloor + 3, pz,
                                AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
                    }
                }
            }
        }

        // Seven snapped underside rib beams lying across the 21-block-wide rubble field.
        for (int beam : new int[]{-9, -6, -3, 0, 3, 6, 9}) {
            for (int along = -halfLength + 1; along <= halfLength - 1; along++) {
                int px = x + (alongX ? along : beam);
                int pz = z + (alongX ? beam : along);
                if (!box.isInside(px, clampY(box, deckY), pz)) continue;
                int localFloor = findAbyssFloor(level, px, pz, deckY);
                if (localFloor == Integer.MIN_VALUE) continue;
                if (damageRoll(along + center, beam, 103) < 20) continue;
                place(level, box, px, localFloor + 1, pz,
                        beam == 0 ? AbyssContent.ABYSSAL_PILLAR.defaultBlockState()
                                : AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
            }
        }
    }

    /** Monumental cliffside bastions embedding the 21-block-wide bridge deep into both cavern walls. */
    private void buildEndAnchors(WorldGenLevel level, BoundingBox box, DamageProfile damage) {
        for (int endpoint = 0; endpoint < 2; endpoint++) {
            int x = endpoint == 0 ? startX : endX;
            int z = endpoint == 0 ? startZ : endZ;
            int deckY = baseY;
            int dir = (endpoint == 0) ? 1 : -1;

            // Deep 12-block abutment collar embedded into the rock wall, widening to 27 blocks across.
            for (int inward = 0; inward <= 12; inward++) {
                int px = x + (alongX ? dir * inward : 0);
                int pz = z + (alongX ? 0 : dir * inward);
                int wingHalfWidth = HALF_WIDTH + 3;
                for (int width = -wingHalfWidth; width <= wingHalfWidth; width++) {
                    int wx = px + (alongX ? 0 : width);
                    int wz = pz + (alongX ? width : 0);
                    for (int down = 1; down <= 6; down++) {
                        if (damage.wear >= 2 && down >= 5 && damageRoll(inward, width, 83 + endpoint) < 18) continue;
                        BlockState state = (down == 1 && Math.abs(width) > HALF_WIDTH)
                                ? AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState()
                                : AbyssContent.ABYSSAL_BRICKS.defaultBlockState();
                        place(level, box, wx, deckY - down, wz, state);
                    }
                }
            }

            // Twin towering cliff-threshold obelisks flanking the bridge entrance.
            for (int side : new int[]{-(HALF_WIDTH + 1), HALF_WIDTH + 1}) {
                int gx = x + (alongX ? 0 : side);
                int gz = z + (alongX ? side : 0);
                for (int h = 0; h <= 6; h++) {
                    if (damage.wear >= 3 && h >= 4 && damageRoll(endpoint, side + h, 87) < 35) break;
                    BlockState pylon = (h == 4)
                            ? AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState()
                            : (h >= 5 ? AbyssContent.CHISELED_ABYSSAL_STONE.defaultBlockState()
                            : AbyssContent.ABYSSAL_PILLAR.defaultBlockState());
                    place(level, box, gx, deckY + h, gz, pylon);
                }
            }
        }
    }

    private boolean isInsideCollapseRibZone(int distance, DamageProfile damage) {
        if (!damage.hasCollapse()) return false;
        for (CollapseZone zone : damage.collapseZones) {
            if (distance >= zone.startDist - 4 && distance <= zone.endDist + 4) {
                return true;
            }
        }
        return false;
    }

    private boolean isCollapsedDeckBlock(int distance, int offset, DamageProfile damage) {
        if (!damage.hasCollapse()) return false;
        for (CollapseZone zone : damage.collapseZones) {
            int start = zone.laneStart(offset, seed);
            int end = zone.laneEnd(offset, seed);
            if (distance >= start && distance <= end) {
                return true;
            }
        }
        return false;
    }

    private int distanceToCollapseEdge(int distance, int offset, DamageProfile damage) {
        if (!damage.hasCollapse()) return -1;
        int best = Integer.MAX_VALUE;
        for (CollapseZone zone : damage.collapseZones) {
            int start = zone.laneStart(offset, seed);
            int end = zone.laneEnd(offset, seed);
            if (distance >= start && distance <= end) return 0;
            int d = (distance < start) ? (start - distance) : (distance - end);
            if (d < best) best = d;
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    private static int clampY(BoundingBox box, int y) {
        return Math.max(box.minY(), Math.min(box.maxY(), y));
    }

    private void fillSquare(WorldGenLevel level, BoundingBox box, int centerX, int y, int centerZ,
                            int radius, BlockState state) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                place(level, box, centerX + dx, y, centerZ + dz, state);
            }
        }
    }

    /**
     * A cross-section reaches at most HALF_WIDTH sideways of its centre line, so sections far
     * outside this chunk can be skipped. Without this, every chunk walked the whole span.
     */
    private static boolean isNearBox(BoundingBox box, int x, int z) {
        int margin = HALF_WIDTH + 4;
        return x >= box.minX() - margin && x <= box.maxX() + margin
                && z >= box.minZ() - margin && z <= box.maxZ() + margin;
    }

    /**
     * Clears rock or formations above the deck so the structure's clearance check does not have
     * to reject a whole route for a few obstructions. The carve stays inside the bridge footprint.
     */
    private void carveHeadroom(WorldGenLevel level, BoundingBox box, int x, int deckY, int z) {
        for (int dy = 1; dy < HEADROOM_HEIGHT; dy++) {
            place(level, box, x, deckY + dy, z, Blocks.AIR.defaultBlockState());
        }
    }

    private void place(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        if (box.isInside(x, y, z)) {
            level.setBlock(new BlockPos(x, y, z), state, 2);
        }
    }

    private int getDeckY(double progress) {
        double arch = 4.0D * progress * (1.0D - progress);
        return baseY + (int) Math.round(archHeight * arch);
    }

    private boolean isCracked(int distance, int widthOffset, int chance) {
        return damageRoll(distance, widthOffset, 97) < chance;
    }

    /** Stable 0-99 hash for damage that is independent of chunk-generation order. */
    private int damageRoll(int distance, int widthOffset, int salt) {
        long value = seed
                ^ (distance * 0x9E3779B97F4A7C15L)
                ^ (widthOffset * 0xC2B2AE3D27D4EB4FL)
                ^ (salt * 0x165667B19E3779F9L);
        return (int) Math.floorMod(mix(value), 100L);
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return value;
    }

    private DamageProfile resolveDamageProfile(int length, int[] pierPositions) {
        String mode = previewDamageMode;
        if ("AUTO".equals(mode)) {
            int roll = (int) Math.floorMod(mix(seed ^ 0x52B37A9D4F1C6E01L), 100L);
            if (roll < 13) mode = "INTACT";
            else if (roll < 35) mode = "WEATHERED";
            else if (roll < 56) mode = "WORN";
            else if (roll < 70) mode = "RUINED";
            else if (roll < 82) mode = "FALLEN_PIER";
            else if (roll < 95) mode = "CENTER_COLLAPSE";
            else mode = "TOTAL_RUIN";
        }

        List<CollapseZone> zones = switch (mode) {
            case "CENTER_COLLAPSE" -> buildPierAwareCollapseZones(length, pierPositions, false);
            case "TOTAL_RUIN" -> buildPierAwareCollapseZones(length, pierPositions, true);
            default -> List.of();
        };

        return switch (mode) {
            case "INTACT" -> new DamageProfile(mode, "Quase intacta · desgaste mínimo",
                    0, false, 3, 0, 0, zones);
            case "WEATHERED" -> new DamageProfile(mode, "Desgastada · rachaduras e parapeitos falhos",
                    1, false, 8, 3, 4, zones);
            case "WORN" -> new DamageProfile(mode, "Muito antiga · pedras faltando e pilares gastos",
                    2, false, 17, 8, 10, zones);
            case "RUINED" -> new DamageProfile(mode, "Em ruínas · piso e sustentação danificados",
                    3, false, 25, 16, 19, zones);
            case "CENTER_COLLAPSE" -> new DamageProfile(mode,
                    "Colapso central · vãos entre pilares desabados e destroços no chão",
                    2, false, 15, 8, 11, zones);
            case "FALLEN_PIER" -> new DamageProfile(mode,
                    "Alicerce tombado · coluna colossal partida e caída em ângulo no chão do Abismo",
                    2, true, 17, 9, 10, zones);
            case "TOTAL_RUIN" -> new DamageProfile(mode,
                    "Ruína severa · múltiplos vãos centrais desabados e alicerce tombado no chão",
                    3, true, 26, 17, 20, zones);
            default -> new DamageProfile("INTACT", "Quase intacta · desgaste mínimo",
                    0, false, 3, 0, 0, List.of());
        };
    }

    /**
     * Builds pier-aware collapsed bays that scale with the bridge's total span:
     * - Short/medium bridges (1-3 piers): collapses the central bay(s), leaving 5-9 block
     *   cantilevered stumps projecting past the surviving flanking piers.
     * - Long/colossal bridges (4+ piers): collapses two asymmetric central spans separated by
     *   a surviving pier-supported viaduct island in the middle of the chasm.
     */
    private List<CollapseZone> buildPierAwareCollapseZones(int length, int[] piers, boolean severe) {
        List<CollapseZone> zones = new ArrayList<>();
        if (piers.length == 0) {
            int half = Math.max(18, length / 6);
            zones.add(new CollapseZone(length / 2 - half, length / 2 + half, 1));
            return zones;
        }

        // Cantilever overhang past surviving pier centers (5 to 9 blocks so pier capitals remain intact).
        int overhang = 6 + Math.floorMod((int) mix(seed ^ 0x3A71C9E2B4D05817L), 4);
        int n = piers.length;

        if (n == 1) {
            // Single central pier failed -> collapse spans ~38-50% of the central chasm.
            int half = Math.max(22, (int) Math.round(length * (severe ? 0.25D : 0.20D)));
            zones.add(new CollapseZone(piers[0] - half, piers[0] + half, 2));
        } else if (n == 2) {
            // Two flanking piers frame the central bay: deck collapses cleanly between their cantilevers.
            int start = piers[0] + overhang;
            int end = piers[1] - overhang;
            if (end - start < 16) {
                int half = Math.max(18, length / 6);
                zones.add(new CollapseZone(length / 2 - half, length / 2 + half, 3));
            } else {
                zones.add(new CollapseZone(start, end, 3));
            }
        } else if (n == 3) {
            // Three piers: central pier fails and both flanking bays collapse between outer piers 0 and 2.
            int start = piers[0] + overhang;
            int end = piers[2] - overhang;
            zones.add(new CollapseZone(start, end, 4));
        } else {
            // Colossal bridge (4+ piers): two asymmetric collapsed spans with a surviving
            // pier-supported viaduct island in the middle (matching monumental ruin silhouettes).
            int mid = n / 2;
            int islandLeftPier = Math.max(1, mid - 1);
            int islandRightPier = Math.min(n - 2, (n >= 6 && severe) ? mid + 1 : mid);

            // Left collapsed gap: 1 or 2 bays wide to the left of islandLeftPier.
            int leftSpanBays = (n >= 6 && (severe || Math.floorMod((int) mix(seed ^ 0x1D4E8B2C7A93F051L), 2) == 0)) ? 2 : 1;
            int leftAnchorIdx = Math.max(0, islandLeftPier - leftSpanBays);
            int leftStart = piers[leftAnchorIdx] + overhang;
            int leftEnd = piers[islandLeftPier] - overhang;
            if (leftEnd - leftStart >= 14) {
                zones.add(new CollapseZone(leftStart, leftEnd, 5));
            }

            // Right collapsed gap: 1 to 3 bays wide to the right of islandRightPier (wider chasm on long bridges).
            int rightSpanBays = (n >= 7 && severe) ? 3 : ((n >= 5) ? 2 : 1);
            int rightAnchorIdx = Math.min(n - 1, islandRightPier + rightSpanBays);
            int rightStart = piers[islandRightPier] + overhang;
            int rightEnd = piers[rightAnchorIdx] - overhang;
            if (rightEnd - rightStart >= 14) {
                zones.add(new CollapseZone(rightStart, rightEnd, 6));
            }
        }

        return zones;
    }

    private static final class CollapseZone {
        final int startDist;
        final int endDist;
        final int centerDist;
        final int halfLength;
        final int zoneSalt;

        CollapseZone(int startDist, int endDist, int zoneSalt) {
            this.startDist = Math.min(startDist, endDist);
            this.endDist = Math.max(startDist, endDist);
            this.centerDist = (this.startDist + this.endDist) / 2;
            this.halfLength = (this.endDist - this.startDist) / 2;
            this.zoneSalt = zoneSalt;
        }

        int laneStart(int offset, long seed) {
            // Center lanes fracture 1-3 blocks further back than outer stone stringers.
            int archPocket = Math.max(0, 3 - (Math.abs(offset) / 3));
            int jagged = Math.floorMod(laneHash(offset, -1, zoneSalt, seed), 5) - 2;
            return startDist - archPocket - jagged;
        }

        int laneEnd(int offset, long seed) {
            int archPocket = Math.max(0, 3 - (Math.abs(offset) / 3));
            int jagged = Math.floorMod(laneHash(offset, 1, zoneSalt, seed), 5) - 2;
            return endDist + archPocket + jagged;
        }

        private static long laneHash(int offset, int side, int zoneSalt, long seed) {
            long v = seed
                    ^ (offset * 0x9E3779B97F4A7C15L)
                    ^ (side * 0xC2B2AE3D27D4EB4FL)
                    ^ (zoneSalt * 0x165667B19E3779F9L);
            return mix(v);
        }
    }

    private static final class DamageProfile {
        final String mode;
        final String description;
        final int wear;
        final boolean fallenPier;
        final int missingTileChance;
        final int missingFoundationChance;
        final int missingParapetChance;
        final List<CollapseZone> collapseZones;

        DamageProfile(String mode, String description, int wear,
                      boolean fallenPier, int missingTileChance,
                      int missingFoundationChance, int missingParapetChance,
                      List<CollapseZone> collapseZones) {
            this.mode = mode;
            this.description = description;
            this.wear = wear;
            this.fallenPier = fallenPier;
            this.missingTileChance = missingTileChance;
            this.missingFoundationChance = missingFoundationChance;
            this.missingParapetChance = missingParapetChance;
            this.collapseZones = collapseZones;
        }

        boolean hasCollapse() {
            return !collapseZones.isEmpty();
        }
    }
}
