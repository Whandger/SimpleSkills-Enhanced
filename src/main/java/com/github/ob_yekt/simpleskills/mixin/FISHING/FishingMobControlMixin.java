package com.github.ob_yekt.simpleskills.mixin.FISHING;

import com.github.ob_yekt.simpleskills.fishing.FishingManager;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pauses the vanilla AI (goals, navigation, travel) of a fish while a fishing session drives it.
 *
 * <p>Unlike {@code setNoAi(true)} this is not saved in the entity NBT nor synced to clients, so a
 * crash or a server stop in the middle of a fight can never leave a permanently frozen fish: the
 * moment the session ends, the fish simply behaves normally again.</p>
 */
@Mixin(Mob.class)
public abstract class FishingMobControlMixin {

    @Inject(method = "isEffectiveAi", at = @At("HEAD"), cancellable = true)
    private void simpleskills$pauseAiDuringFishing(CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;
        if (!self.level().isClientSide() && FishingManager.isControlled(self)) {
            cir.setReturnValue(false);
        }
    }
}
