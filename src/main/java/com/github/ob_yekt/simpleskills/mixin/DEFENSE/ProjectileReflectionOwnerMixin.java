package com.github.ob_yekt.simpleskills.mixin.DEFENSE;

import com.github.ob_yekt.simpleskills.DefenseCombat;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Projectile.class)
public abstract class ProjectileReflectionOwnerMixin {
    @ModifyArg(
            method = "deflect(Lnet/minecraft/world/entity/projectile/ProjectileDeflection;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/EntityReference;ZLnet/minecraft/world/phys/Vec3;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;setOwner(Lnet/minecraft/world/entity/EntityReference;)V"),
            index = 0
    )
    private EntityReference<Entity> simpleskills$creditReflectedProjectileToDefender(
            EntityReference<Entity> previousOwner,
            @Local(argsOnly = true) ProjectileDeflection deflection,
            @Local(argsOnly = true) Entity deflectingEntity) {
        if (deflection == DefenseCombat.PROJECTILE_REFLECTION && deflectingEntity != null) {
            return EntityReference.of(deflectingEntity);
        }
        return previousOwner;
    }
}
