
package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.TreeFeller;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets TreeFeller process its visual falling blocks as drops when they land,
 * instead of allowing vanilla FallingBlockEntity to place them back in the world.
 */
@Mixin(FallingBlockEntity.class)
public abstract class TreeFellerFallingBlockMixin {

    @Inject(
            method = "tick()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;onGround()Z",
                    ordinal = 0,
                    shift = At.Shift.BEFORE
            ),
            cancellable = true
    )
    private void simpleskills$processTreeFellerLanding(CallbackInfo ci) {
        FallingBlockEntity entity = (FallingBlockEntity) (Object) this;

        if (TreeFeller.handleFallingBlockTick(entity)) {
            ci.cancel();
        }
    }
}
