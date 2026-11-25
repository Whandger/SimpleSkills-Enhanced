package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Structure wrapper for {@link AbyssalCastlePiece}. Placed via
 * {@code data/simpleskills/worldgen/structure_set/abyssal_castle.json} inside The Abyss layer
 * ({@code Y = -256 .. -80}).
 */
public class AbyssalCastleStructure extends Structure {

    public static final MapCodec<AbyssalCastleStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(settingsCodec(instance))
                    .apply(instance, AbyssalCastleStructure::new));

    public static final StructureType<AbyssalCastleStructure> TYPE = () -> CODEC;

    /**
     * Ground courtyard Y bounds preserve the original lower castle placement
     * while keeping the 107-block Sovereign Spire inside The Abyss
     * ({@code Y = -256 .. -80}).
     */
    private static final int FLOOR_MIN_Y = -228;
    private static final int FLOOR_MAX_Y = -196;

    /** Minimum share of the cavern footprint that must be open so the monumental silhouette is visible. */
    private static final int OPEN_PERCENT = 36;

    public AbyssalCastleStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    /** Registers the structure type and its piece type. Must run before the registries freeze. */
    public static void register() {
        Identifier id = Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "abyssal_castle");
        Registry.register(BuiltInRegistries.STRUCTURE_TYPE, id, TYPE);
        Registry.register(BuiltInRegistries.STRUCTURE_PIECE, id, AbyssalCastlePiece.TYPE);
    }

    @Override
    public StructureType<?> type() {
        return TYPE;
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        RandomSource random = context.random();

        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();
        int y = FLOOR_MIN_Y + random.nextInt(FLOOR_MAX_Y - FLOOR_MIN_Y + 1);
        BlockPos center = new BlockPos(x, y, z);

        if (!hasOpenChamber(context, center)) {
            return Optional.empty();
        }

        AbyssalCastlePiece piece = new AbyssalCastlePiece(center, random.nextLong());
        Consumer<StructurePiecesBuilder> builder = pieces -> pieces.addPiece(piece);
        return Optional.of(new Structure.GenerationStub(center,
                Either.<Consumer<StructurePiecesBuilder>, StructurePiecesBuilder>left(builder)));
    }

    /**
     * Samples the base cavern terrain across the castle footprint at courtyard and mid-tower levels.
     */
    private static boolean hasOpenChamber(Structure.GenerationContext context, BlockPos center) {
        int samples = 0;
        int open = 0;
        int step = 21;
        for (int dx = -AbyssalCastlePiece.HALF; dx <= AbyssalCastlePiece.HALF; dx += step) {
            for (int dz = AbyssalCastlePiece.MIN_OFFSET_Z; dz <= AbyssalCastlePiece.MAX_OFFSET_Z; dz += step) {
                NoiseColumn column = context.chunkGenerator().getBaseColumn(
                        center.getX() + dx, center.getZ() + dz,
                        context.heightAccessor(), context.randomState());
                samples += 2;
                if (isOpen(column.getBlock(center.getY() + 2))) {
                    open++;
                }
                if (isOpen(column.getBlock(center.getY() + 18))) {
                    open++;
                }
            }
        }
        return open * 100 >= samples * OPEN_PERCENT;
    }

    private static boolean isOpen(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty();
    }
}
