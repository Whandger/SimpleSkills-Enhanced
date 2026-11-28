package com.github.ob_yekt.simpleskills.mixin;

import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents the Overworld's global deep-lava layer and lava aquifers from
 * placing lava inside The Abyss. The Y check keeps all blocks above the
 * Abyssal layer untouched.
 */
@Mixin(Aquifer.NoiseBasedAquifer.class)
public abstract class AbyssAquiferMixin {

    @Inject(method = "computeSubstance", at = @At("RETURN"), cancellable = true)
    private void simpleskills$removeAbyssalLava(
            int blockX,
            int blockY,
            int blockZ,
            double density,
            CallbackInfoReturnable<BlockState> cir
    ) {
        if (blockY < -256 || blockY > -80) {
            return;
        }

        BlockState generatedState = cir.getReturnValue();
        if (generatedState != null && generatedState.is(Blocks.LAVA)) {
            cir.setReturnValue(Blocks.AIR.defaultBlockState());
        }
    }
}
