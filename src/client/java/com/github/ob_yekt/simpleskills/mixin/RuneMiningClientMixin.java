package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.runes.RuneMiningClient;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class RuneMiningClientMixin {
    @Shadow private BlockPos destroyBlockPos;
    @Shadow private float destroyProgress;
    @Shadow private boolean isDestroying;

    @Inject(method = "startDestroyBlock", at = @At("TAIL"))
    private void simpleskills$startRuneMining(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && this.isDestroying) RuneMiningClient.sync(pos, 0.01F);
        else RuneMiningClient.clear();
    }

    @Inject(method = "continueDestroyBlock", at = @At("TAIL"))
    private void simpleskills$updateRuneMining(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (this.isDestroying && this.destroyBlockPos != null) {
            RuneMiningClient.sync(this.destroyBlockPos, this.destroyProgress);
        } else {
            RuneMiningClient.clear();
        }
    }

    @Inject(method = "stopDestroyBlock", at = @At("TAIL"))
    private void simpleskills$stopRuneMining(CallbackInfo ci) {
        RuneMiningClient.clear();
    }
}
