package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.crafting.TranscendentAbilities;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the server-side impact effect for Transcendent crossbow bolts. */
@Mixin(Projectile.class)
public abstract class TranscendentProjectileMixin {
    @Inject(method = "onHit", at = @At("TAIL"))
    private void simpleskills$transcendentImpact(HitResult result, CallbackInfo ci) {
        TranscendentAbilities.onProjectileImpact((Projectile) (Object) this, result);
    }
}
