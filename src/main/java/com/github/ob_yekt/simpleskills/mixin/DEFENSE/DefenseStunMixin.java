package com.github.ob_yekt.simpleskills.mixin.DEFENSE;

import com.github.ob_yekt.simpleskills.DefenseCombat;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class DefenseStunMixin {
    @Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
    private void simpleskills$stopStunnedMobActions(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (DefenseCombat.isStunned(mob)) {
            mob.getNavigation().stop();
            mob.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            ci.cancel();
        }
    }

    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void simpleskills$stopStunnedMobAi(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (DefenseCombat.isStunned(mob)) {
            mob.getNavigation().stop();
            mob.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            ci.cancel();
        }
    }
}
