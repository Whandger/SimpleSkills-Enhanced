
package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.Ranged;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.world.entity.LivingEntity.class)
public abstract class RangedDuplicateIFrameMixin {

    @ModifyExpressionValue(
            method = "hurtServer",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/LivingEntity;damageCooldownTime:I",
                    opcode = Opcodes.GETFIELD
            )
    )
    private int simpleskills$allowDuplicateArrowDamage(
            int originalCooldown,
            @Local(argsOnly = true) DamageSource source
    ) {
        if (source.getDirectEntity() instanceof AbstractArrow arrow
                && Ranged.isDuplicateProjectile(arrow)) {

            return 10;
        }

        return originalCooldown;
    }
}
