
package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.Ranged;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class RangedProjectileDamageMixin {

    @ModifyVariable(
            method = "hurtServer",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float simpleskills$applyRangedDamageBonus(
            float damage,
            @Local(argsOnly = true) DamageSource source
    ) {
        // Ignore invalid damage values.
        if (damage <= 0.0F) {
            return damage;
        }

        // Only eligible arrow-type projectiles receive this bonus.
        if (!(source.getDirectEntity() instanceof AbstractArrow projectile)) {
            return damage;
        }

        // The projectile must belong to a player.
        if (!(projectile.getOwner() instanceof ServerPlayer player)) {
            return damage;
        }

        // Apply the Ranged multiplier from the server-side skill cache.
        float multiplier = Ranged.getDamageMultiplier(player.getUUID());

        return damage * multiplier;
    }
}
