package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.abyss.AbyssWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.SculkPatchFeature;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stops Deep Dark sculk patches from starting inside or spilling into The Abyss. */
@Mixin(SculkPatchFeature.class)
public abstract class AbyssSculkPatchMixin {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void simpleskills$stopSculkPatchesInAbyss(
            WorldGenLevel level,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BlockPos origin,
            CallbackInfoReturnable<Boolean> cir
    ) {
        // Sculk patches spread beyond their origin; the 16-block margin keeps them out of the boundary.
        if (AbyssWorldgen.isOverworld(level)
                && AbyssWorldgen.intersectsAbyss(origin.getY() - 16, origin.getY() + 16)) {
            cir.setReturnValue(false);
        }
    }
}
