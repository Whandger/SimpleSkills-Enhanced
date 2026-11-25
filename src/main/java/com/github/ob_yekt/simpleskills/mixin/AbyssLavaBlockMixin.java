package com.github.ob_yekt.simpleskills.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps lava from flowing or being placed into the Abyss after chunk generation. */
@Mixin(LiquidBlock.class)
public abstract class AbyssLavaBlockMixin {

    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void simpleskills$removeLavaFromAbyss(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston,
            CallbackInfo ci
    ) {
        int y = pos.getY();
        if (y < -256 || y > -80 || !state.is(Blocks.LAVA)) {
            return;
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        ci.cancel();
    }
}
