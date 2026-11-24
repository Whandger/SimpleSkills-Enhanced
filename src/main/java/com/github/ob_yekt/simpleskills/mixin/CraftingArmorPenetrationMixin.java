package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Reduces the target's physical armor before vanilla's armor mitigation formula is evaluated. */
@Mixin(LivingEntity.class)
public abstract class CraftingArmorPenetrationMixin {
    @ModifyArg(
            method = "getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F"),
            index = 3
    )
    private float simpleskills$applyArmorPenetration(float armor,
                                                     @Local(argsOnly = true) DamageSource source) {
        double penetration = 0.0;
        if (source.getEntity() instanceof Player attacker) {
            penetration = CraftingEquipmentManager.getArmorPenetration(attacker.getMainHandItem());
        }
        if (source.getDirectEntity() instanceof Projectile projectile) {
            penetration = Math.max(penetration,
                    CraftingEquipmentManager.getProjectileArmorPenetration(projectile));
        }
        return (float) (armor * (1.0 - Math.max(0.0, Math.min(0.60, penetration))));
    }
}
