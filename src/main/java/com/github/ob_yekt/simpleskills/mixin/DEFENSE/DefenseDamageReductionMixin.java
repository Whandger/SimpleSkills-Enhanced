package com.github.ob_yekt.simpleskills.mixin.DEFENSE;

import com.github.ob_yekt.simpleskills.Defense;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Applies the passive Defense damage reduction to server-side players.
 *
 * Each Defense level grants 0.3% damage reduction.
 * At level 99, the maximum reduction is 29.7%.
 */
@Mixin(Player.class)
public abstract class DefenseDamageReductionMixin {

    @ModifyVariable(
            method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float simpleskills$applyDefenseDamageReduction(float amount) {
        if (amount <= 0.0F || !((Object) this instanceof ServerPlayer player)) {
            return amount;
        }

        float damageReduction = Defense.getDamageReduction(player.getUUID());

        if (damageReduction <= 0.0F) {
            return amount;
        }

        return amount * (1.0F - damageReduction);
    }
}
