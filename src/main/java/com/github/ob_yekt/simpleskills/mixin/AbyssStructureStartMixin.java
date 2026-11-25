package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.abyss.AbyssalCastleStructure;
import com.github.ob_yekt.simpleskills.abyss.AbyssWorldgen;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Final safety net for structure-based generation. Any non-Abyss structure whose complete
 * bounding box intersects the Abyss Y band is skipped in the Overworld. This also covers
 * structures added by other mods, without changing their generation outside the Abyss.
 */
@Mixin(StructureStart.class)
public abstract class AbyssStructureStartMixin {

    @Shadow public abstract Structure getStructure();
    @Shadow public abstract BoundingBox getBoundingBox();

    @Inject(method = "placeInChunk", at = @At("HEAD"), cancellable = true)
    private void simpleskills$blockNonAbyssStructuresInsideAbyss(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox chunkBounds,
            ChunkPos chunkPos,
            CallbackInfo ci
    ) {
        if (!AbyssWorldgen.isOverworld(level)) {
            return;
        }

        // The castle is the one intentional structure allowed to occupy this Y range.
        if (this.getStructure().type() == AbyssalCastleStructure.TYPE) {
            return;
        }

        BoundingBox structureBounds = this.getBoundingBox();
        if (AbyssWorldgen.intersectsAbyss(structureBounds.minY(), structureBounds.maxY())) {
            ci.cancel();
        }
    }
}
