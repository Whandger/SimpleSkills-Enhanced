package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.abyss.AbyssWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents sculk shriekers in/near The Abyss from summoning Wardens into the layer. */
@Mixin(SculkShriekerBlockEntity.class)
public abstract class AbyssWardenSummonMixin {

    @Inject(method = "trySummonWarden", at = @At("HEAD"), cancellable = true)
    private void simpleskills$preventAbyssWardenSummon(
            ServerLevel level,
            CallbackInfoReturnable<Boolean> cir
    ) {
        BlockPos shriekerPos = ((SculkShriekerBlockEntity) (Object) this).getBlockPos();
        if (Level.OVERWORLD.equals(level.dimension())
                && AbyssWorldgen.intersectsAbyss(shriekerPos.getY() - 16, shriekerPos.getY() + 16)) {
            cir.setReturnValue(false);
        }
    }
}
