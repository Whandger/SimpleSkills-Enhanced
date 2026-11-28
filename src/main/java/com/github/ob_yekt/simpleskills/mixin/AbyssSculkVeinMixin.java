package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.abyss.AbyssWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.MultifaceGrowthFeature;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents the vanilla sculk-vein feature from placing growth within the Abyss search radius. */
@Mixin(MultifaceGrowthFeature.class)
public abstract class AbyssSculkVeinMixin {
    @Shadow @Final private Block placeBlock;
    @Shadow @Final private int searchRange;

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void simpleskills$stopSculkVeinsInAbyss(
            WorldGenLevel level,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BlockPos origin,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (this.placeBlock == Blocks.SCULK_VEIN
                && AbyssWorldgen.isOverworld(level)
                && AbyssWorldgen.intersectsAbyss(origin.getY() - this.searchRange,
                        origin.getY() + this.searchRange)) {
            cir.setReturnValue(false);
        }
    }
}
