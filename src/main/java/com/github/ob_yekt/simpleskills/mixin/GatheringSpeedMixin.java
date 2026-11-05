package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.GatheringSpeed;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gathering Speed: multiplies the final vanilla result of Player#getDestroySpeed once.
 * Runs on both the client (visual progress) and the server (effective progress); both read
 * the same levels (server cache / payload-synced client cache).
 */
@Mixin(Player.class)
public abstract class GatheringSpeedMixin {

    @Inject(method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F", at = @At("RETURN"), cancellable = true)
    private void simpleskills$applyGatheringSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        float multiplier = GatheringSpeed.getMultiplier((Player) (Object) this, state);
        if (multiplier != 1.0f) {
            cir.setReturnValue(cir.getReturnValueF() * multiplier);
        }
    }
}
