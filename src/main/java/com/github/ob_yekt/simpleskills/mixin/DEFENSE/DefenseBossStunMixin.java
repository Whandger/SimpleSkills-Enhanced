package com.github.ob_yekt.simpleskills.mixin.DEFENSE;

import com.github.ob_yekt.simpleskills.DefenseCombat;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EnderDragon.class, WitherBoss.class})
public abstract class DefenseBossStunMixin {
    @Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
    private void simpleskills$haltBossWhileStunned(CallbackInfo ci) {
        net.minecraft.world.entity.Entity entity = (net.minecraft.world.entity.Entity) (Object) this;
        if (DefenseCombat.isStunned(entity)) {
            entity.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            ci.cancel();
        }
    }
}
