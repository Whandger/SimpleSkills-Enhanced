package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.abyss.AbyssWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SculkSpreader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops catalyst/vein spread cursors when they reach the Abyss, including spread crossing its border. */
@Mixin(SculkSpreader.ChargeCursor.class)
public abstract class AbyssSculkSpreadCursorMixin {
    @Shadow private BlockPos pos;
    @Shadow private int charge;

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void simpleskills$stopSculkSpreadInAbyss(
            LevelAccessor level,
            BlockPos originPos,
            RandomSource random,
            SculkSpreader spreader,
            boolean spreadVeins,
            CallbackInfo ci
    ) {
        if (AbyssWorldgen.isOverworld(level)
                && AbyssWorldgen.intersectsAbyss(this.pos.getY() - 1, this.pos.getY() + 1)) {
            this.charge = 0;
            ci.cancel();
        }
    }
}
