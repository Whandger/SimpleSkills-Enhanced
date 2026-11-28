package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.abyss.AbyssWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Disables block-entity mob spawners located inside The Abyss. */
@Mixin(BaseSpawner.class)
public abstract class AbyssBaseSpawnerMixin {

    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private void simpleskills$disableSpawnerInAbyss(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        if (Level.OVERWORLD.equals(level.dimension()) && AbyssWorldgen.isAbyssY(pos.getY())) {
            ci.cancel();
        }
    }
}
