package com.github.ob_yekt.simpleskills.mixin;

import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Widens the STRUCTURE_STARTS dependency in the 26.3 generation pyramid.
 *
 * ChunkPyramid builds its steps through builder callbacks/lambda methods, so modifying
 * constants in ChunkPyramid.<clinit> is not stable and fails on Minecraft 26.3.
 * Intercepting the actual Builder API is stable for this version and keeps the change
 * limited to the four steps that request structure starts at radius 8.
 */
@Mixin(ChunkStep.Builder.class)
public abstract class AbyssStructureReferenceChunkStepMixin {

    @Shadow @Final private ChunkStatus status;

    @ModifyVariable(
            method = "addRequirement",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private int simpleskills$extendStructureStartRadius(int originalRadius) {
        if (originalRadius != 8) {
            return originalRadius;
        }

        boolean structureDependentStep = this.status == ChunkStatus.STRUCTURE_REFERENCES
                || this.status == ChunkStatus.BIOMES
                || this.status == ChunkStatus.TERRAIN
                || this.status == ChunkStatus.FEATURES;

        return structureDependentStep
                ? MixinPlugin.getAbyssStructureReferenceRadius()
                : originalRadius;
    }
}
