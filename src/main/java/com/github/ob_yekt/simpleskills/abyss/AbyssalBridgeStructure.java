package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.mixin.MixinPlugin;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Finds the shortest wall-to-wall crossing through an Abyss chamber and spans it with a
 * colossal viaduct.
 *
 * <p>The search is bounded by the structure-reference radius. A structure piece is only
 * placed in a chunk that holds a reference to the structure's start chunk, and that
 * reference only reaches {@code radius} chunks away. A bridge that extended further would
 * be cut off in mid-air, so every endpoint is searched within {@link #maxReach()}.
 */
public final class AbyssalBridgeStructure extends Structure {
    public static final MapCodec<AbyssalBridgeStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(settingsCodec(instance))
                    .apply(instance, AbyssalBridgeStructure::new));

    public static final StructureType<AbyssalBridgeStructure> TYPE = () -> CODEC;

    /** Run with -Dsimpleskills.abyssBridgeDebug=true to log every accepted and rejected bridge. */
    private static final boolean DEBUG = Boolean.getBoolean("simpleskills.abyssBridgeDebug");

    private static final int MIN_Y = -226;
    private static final int MAX_Y = -130;
    private static final int MIN_TOTAL_SPAN = 320;

    /** Keeps the far endpoint inside the last chunk the reference radius still reaches. */
    private static final int REFERENCE_MARGIN = 24;
    private static final int NO_WALL = Integer.MIN_VALUE;

    private static final int WALL_SCAN_STEP = 8;
    private static final int MIN_COARSE_WALL_SAMPLES = 3;
    private static final int MIN_WALL_FACE_SAMPLES = 5;
    private static final int MIN_WALL_DEPTH_SAMPLES = 5;
    private static final int MIN_WALL_VERTICAL_SAMPLES = 4;
    private static final int WALL_DEPTH = 8;

    private static final int CLEARANCE_HEIGHT = 10;
    private static final int CORRIDOR_SAMPLE_STEP = 4;
    /**
     * Share of corridor samples allowed to hit rock or formations. The piece carves its own
     * headroom, so a few obstructions do not force a longer route.
     */
    private static final int MAX_BLOCKED_CORRIDOR_PERCENT = 6;

    /**
     * Open air required between the deck and the floor beneath it. Piers only stand where the
     * floor is at least this far down, so a route closer than this would read as a bridge that
     * starts and ends on the ground.
     */
    private static final int MIN_FLOOR_CLEARANCE = 14;
    private static final int GROUND_SAMPLE_STEP = 8;
    private static final int MAX_GROUND_CONTACT_PERCENT = 10;

    private static final int[] LATERAL_OFFSETS = {-96, -48, 0, 48, 96};
    private static final int[] SEARCH_HEIGHTS = {-216, -194, -172, -150, -130};
    private static final int[] WIDTH_SAMPLES = {-10, -7, -3, 0, 3, 7, 10};
    private static final int[] COARSE_WIDTH_SAMPLES = {-10, -5, 0, 5, 10};
    private static final int[] CORRIDOR_WIDTH_SAMPLES = {-10, -8, -6, -4, -2, 0, 2, 4, 6, 8, 10};

    public AbyssalBridgeStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    public static void register() {
        Identifier id = Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "abyssal_bridge");
        Registry.register(BuiltInRegistries.STRUCTURE_TYPE, id, TYPE);
        Registry.register(BuiltInRegistries.STRUCTURE_PIECE, id, AbyssalBridgePiece.TYPE);
    }

    @Override
    public StructureType<?> type() {
        return TYPE;
    }

    /** Largest distance from the start chunk centre that the reference radius still serves. */
    static int maxReach() {
        return MixinPlugin.getAbyssStructureReferenceRadius() * 16 - REFERENCE_MARGIN;
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        int reach = maxReach();
        if (2 * reach < MIN_TOTAL_SPAN) {
            // With the vanilla radius (8) no bridge can fit inside the reference range.
            return Optional.empty();
        }

        ChunkPos chunkPos = context.chunkPos();
        int centerX = chunkPos.getMiddleBlockX();
        int centerZ = chunkPos.getMiddleBlockZ();
        long searchSeed = context.random().nextLong();
        int preferredY = (MIN_Y + MAX_Y) / 2;
        ColumnSampler sampler = new ColumnSampler(context);

        List<BridgeCandidate> candidates = new ArrayList<>();
        for (boolean alongX : new boolean[]{true, false}) {
            for (int lateralOffset : LATERAL_OFFSETS) {
                for (int y : SEARCH_HEIGHTS) {
                    int negativeReach = findWallReach(sampler, centerX, centerZ, alongX, -1, lateralOffset, y, reach);
                    if (negativeReach == NO_WALL) continue;

                    int positiveReach = findWallReach(sampler, centerX, centerZ, alongX, 1, lateralOffset, y, reach);
                    if (positiveReach == NO_WALL) continue;

                    int span = negativeReach + positiveReach;
                    if (span < MIN_TOTAL_SPAN) continue;

                    int startX = centerX + (alongX ? -negativeReach : lateralOffset);
                    int startZ = centerZ + (alongX ? lateralOffset : -negativeReach);
                    int endX = centerX + (alongX ? positiveReach : lateralOffset);
                    int endZ = centerZ + (alongX ? lateralOffset : positiveReach);

                    long pathKey = (((long) (alongX ? 1 : 2)) << 56)
                            ^ (((long) (lateralOffset + 128)) << 32)
                            ^ (y & 0xffffffffL);
                    int archHeight = Math.min(38, Math.max(16, span / 24))
                            + (int) Math.floorMod(mix(searchSeed ^ pathKey), 8L);

                    // Shorter wall-to-wall spans always rank first. The small penalties only break
                    // ties between equally short routes, favouring the chunk centre and preferred height.
                    long score = span * 10_000L
                            + Math.abs(lateralOffset) * 80L
                            + Math.abs(y - preferredY) * 15L
                            + Math.floorMod(mix(searchSeed ^ (pathKey * 31L)), 997L);

                    candidates.add(new BridgeCandidate(
                            startX, startZ, endX, endZ, y, alongX,
                            archHeight, span, lateralOffset, score));
                }
            }
        }

        if (candidates.isEmpty()) {
            debug("chunk %s: no wall pair within %d blocks", chunkPos, reach);
            return Optional.empty();
        }
        candidates.sort(Comparator.comparingLong(candidate -> candidate.score));

        int groundRejected = 0;
        int corridorRejected = 0;
        for (BridgeCandidate candidate : candidates) {
            GroundReport ground = inspectGroundClearance(sampler, candidate);
            if (!ground.passes()) {
                groundRejected++;
                continue;
            }

            CorridorReport report = inspectCorridor(sampler, candidate);
            if (!report.passes()) {
                corridorRejected++;
                continue;
            }

            debug("chunk %s: accepted span %d (%s, lateral %d, y %d), corridor %d/%d blocked, ground contact %d/%d, rejected so far: ground %d, corridor %d",
                    chunkPos, candidate.span, candidate.alongX ? "X" : "Z", candidate.lateralOffset,
                    candidate.baseY, report.blocked(), report.samples(),
                    ground.contact(), ground.samples(), groundRejected, corridorRejected);

            BlockPos structureOrigin = new BlockPos(centerX, candidate.baseY, centerZ);
            AbyssalBridgePiece piece = new AbyssalBridgePiece(
                    candidate.startX, candidate.startZ,
                    candidate.endX, candidate.endZ,
                    candidate.baseY, candidate.alongX,
                    candidate.archHeight,
                    mix(searchSeed ^ candidate.score));
            Consumer<StructurePiecesBuilder> builder = pieces -> pieces.addPiece(piece);
            return Optional.of(new Structure.GenerationStub(structureOrigin,
                    Either.<Consumer<StructurePiecesBuilder>, StructurePiecesBuilder>left(builder)));
        }

        debug("chunk %s: all %d candidates rejected (ground %d, corridor %d)",
                chunkPos, candidates.size(), groundRejected, corridorRejected);
        return Optional.empty();
    }

    /**
     * Returns the distance from the chunk centre to the first durable wall face in one
     * direction, or {@link #NO_WALL}. Coarse probes cheaply skip open air; every distance
     * inside a probed interval is then checked in order, so the nearest face always wins.
     */
    private static int findWallReach(ColumnSampler sampler,
                                     int centerX, int centerZ,
                                     boolean alongX, int direction,
                                     int lateralOffset, int y, int maxReach) {
        int previousProbe = 0;
        for (int probe = WALL_SCAN_STEP; probe <= maxReach; probe += WALL_SCAN_STEP) {
            if (coarseWallSamples(sampler, centerX, centerZ, alongX, direction, lateralOffset, probe, y)
                    >= MIN_COARSE_WALL_SAMPLES) {
                for (int distance = previousProbe + 1; distance <= probe; distance++) {
                    int x = centerX + (alongX ? direction * distance : lateralOffset);
                    int z = centerZ + (alongX ? lateralOffset : direction * distance);
                    if (isDurableWallFace(sampler, x, z, alongX, direction, y)) {
                        return distance;
                    }
                }
            }
            previousProbe = probe;
        }
        return NO_WALL;
    }

    private static int coarseWallSamples(ColumnSampler sampler,
                                         int centerX, int centerZ,
                                         boolean alongX, int direction,
                                         int lateralOffset, int distance, int y) {
        int x = centerX + (alongX ? direction * distance : lateralOffset);
        int z = centerZ + (alongX ? lateralOffset : direction * distance);
        int solid = 0;
        for (int widthOffset : COARSE_WIDTH_SAMPLES) {
            int sampleX = x + (alongX ? 0 : widthOffset);
            int sampleZ = z + (alongX ? widthOffset : 0);
            if (isSolid(sampler.column(sampleX, sampleZ).getBlock(y))) solid++;
        }
        return solid;
    }

    /**
     * A real anchor is a broad, vertically continuous rock face that is at least eight blocks
     * deep across most of the 21-block bridge width. Pillars and hanging formations fail the
     * depth test, so the search passes them and continues to the real chamber wall.
     */
    private static boolean isDurableWallFace(ColumnSampler sampler,
                                             int x, int z,
                                             boolean alongX, int direction, int y) {
        int face = 0;
        int depthFour = 0;
        int depthEight = 0;
        int vertical = 0;

        for (int widthOffset : WIDTH_SAMPLES) {
            int faceX = x + (alongX ? 0 : widthOffset);
            int faceZ = z + (alongX ? widthOffset : 0);
            NoiseColumn faceColumn = sampler.column(faceX, faceZ);

            boolean atFace = isSolid(faceColumn.getBlock(y));
            if (atFace) face++;
            // The face must rise at least six blocks above the deck. A floor ridge or ledge has
            // air above it, so it fails here even though it is solid at the deck level.
            if (atFace
                    && isSolid(faceColumn.getBlock(y - 2))
                    && isSolid(faceColumn.getBlock(y + 2))
                    && isSolid(faceColumn.getBlock(y + 6))) {
                vertical++;
            }

            int depthFourX = faceX + (alongX ? direction * 4 : 0);
            int depthFourZ = faceZ + (alongX ? 0 : direction * 4);
            if (isSolid(sampler.column(depthFourX, depthFourZ).getBlock(y))) depthFour++;

            int depthEightX = faceX + (alongX ? direction * WALL_DEPTH : 0);
            int depthEightZ = faceZ + (alongX ? 0 : direction * WALL_DEPTH);
            if (isSolid(sampler.column(depthEightX, depthEightZ).getBlock(y))) depthEight++;
        }

        return face >= MIN_WALL_FACE_SAMPLES
                && depthFour >= MIN_WALL_DEPTH_SAMPLES
                && depthEight >= MIN_WALL_DEPTH_SAMPLES - 1
                && vertical >= MIN_WALL_VERTICAL_SAMPLES;
    }

    private static boolean isSolid(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    /**
     * Samples the arched deck every few blocks across its full width and counts how much of
     * the clearance above it is blocked. The piece carves that clearance itself, so only
     * large obstructions reject a candidate.
     */
    private static CorridorReport inspectCorridor(ColumnSampler sampler, BridgeCandidate candidate) {
        int span = candidate.span;
        int samples = 0;
        int blocked = 0;
        for (int distance = 12; distance <= span - 12; distance += CORRIDOR_SAMPLE_STEP) {
            double progress = distance / (double) span;
            int deckY = deckY(candidate.baseY, candidate.archHeight, progress);
            int x = candidate.startX + (candidate.endX - candidate.startX) * distance / span;
            int z = candidate.startZ + (candidate.endZ - candidate.startZ) * distance / span;

            for (int widthOffset : CORRIDOR_WIDTH_SAMPLES) {
                int sampleX = x + (candidate.alongX ? 0 : widthOffset);
                int sampleZ = z + (candidate.alongX ? widthOffset : 0);
                samples++;
                if (!isClearColumn(sampler, sampleX, sampleZ, deckY)) blocked++;
            }
        }
        return new CorridorReport(samples, blocked);
    }

    /**
     * Counts how many samples along the deck have less than MIN_FLOOR_CLEARANCE of open air
     * below them. Routes that run along the cavern floor fail this check.
     */
    private static GroundReport inspectGroundClearance(ColumnSampler sampler, BridgeCandidate candidate) {
        int span = candidate.span;
        int samples = 0;
        int contact = 0;
        for (int distance = 12; distance <= span - 12; distance += GROUND_SAMPLE_STEP) {
            double progress = distance / (double) span;
            int deckY = deckY(candidate.baseY, candidate.archHeight, progress);
            int x = candidate.startX + (candidate.endX - candidate.startX) * distance / span;
            int z = candidate.startZ + (candidate.endZ - candidate.startZ) * distance / span;
            samples++;
            if (!hasOpenAirBelow(sampler, x, z, deckY)) contact++;
        }
        return new GroundReport(samples, contact);
    }

    private static boolean hasOpenAirBelow(ColumnSampler sampler, int x, int z, int deckY) {
        NoiseColumn column = sampler.column(x, z);
        for (int dy = 1; dy < MIN_FLOOR_CLEARANCE; dy++) {
            if (isSolid(column.getBlock(deckY - dy))) return false;
        }
        return true;
    }

    private static boolean isClearColumn(ColumnSampler sampler, int x, int z, int deckY) {
        NoiseColumn column = sampler.column(x, z);
        for (int dy = 0; dy < CLEARANCE_HEIGHT; dy++) {
            if (!column.getBlock(deckY + dy).isAir()) return false;
        }
        return true;
    }

    private static int deckY(int baseY, int archHeight, double progress) {
        double arch = 4.0D * progress * (1.0D - progress);
        return baseY + (int) Math.round(archHeight * arch);
    }

    private static void debug(String format, Object... args) {
        if (DEBUG) {
            Simpleskills.LOGGER.info("[AbyssBridge] " + String.format(format, args));
        }
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return value;
    }

    /**
     * Caches base columns for one search. Wall, depth and corridor probes overlap heavily,
     * so without the cache the same noise column would be recomputed many times.
     */
    private static final class ColumnSampler {
        private final Structure.GenerationContext context;
        private final Map<Long, NoiseColumn> cache = new HashMap<>();

        ColumnSampler(Structure.GenerationContext context) {
            this.context = context;
        }

        NoiseColumn column(int x, int z) {
            long key = ((long) x << 32) ^ (z & 0xffffffffL);
            NoiseColumn column = cache.get(key);
            if (column == null) {
                column = context.chunkGenerator().getBaseColumn(
                        x, z, context.heightAccessor(), context.randomState());
                cache.put(key, column);
            }
            return column;
        }
    }

    private record GroundReport(int samples, int contact) {
        boolean passes() {
            return samples > 0 && contact * 100 <= samples * MAX_GROUND_CONTACT_PERCENT;
        }
    }

    private record CorridorReport(int samples, int blocked) {
        boolean passes() {
            return samples > 0 && blocked * 100 <= samples * MAX_BLOCKED_CORRIDOR_PERCENT;
        }
    }

    private static final class BridgeCandidate {
        final int startX;
        final int startZ;
        final int endX;
        final int endZ;
        final int baseY;
        final boolean alongX;
        final int archHeight;
        final int span;
        final int lateralOffset;
        final long score;

        BridgeCandidate(int startX, int startZ, int endX, int endZ,
                        int baseY, boolean alongX, int archHeight,
                        int span, int lateralOffset, long score) {
            this.startX = startX;
            this.startZ = startZ;
            this.endX = endX;
            this.endZ = endZ;
            this.baseY = baseY;
            this.alongX = alongX;
            this.archHeight = archHeight;
            this.span = span;
            this.lateralOffset = lateralOffset;
            this.score = score;
        }
    }
}
