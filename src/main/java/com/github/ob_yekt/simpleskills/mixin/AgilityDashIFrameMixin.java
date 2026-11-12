package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.agility.AgilityManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class AgilityDashIFrameMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void simpleskills$cancelDamageDuringDash(ServerLevel level, DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        if (AgilityManager.hasDashIFrames((ServerPlayer) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
