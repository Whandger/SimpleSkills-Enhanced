package com.github.ob_yekt.simpleskills.mixin.DEFENSE;

import com.github.ob_yekt.simpleskills.Defense;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import com.github.ob_yekt.simpleskills.DefenseCombat;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class ProjectileReflectionMixin {
    @Inject(method = "deflection", at = @At("HEAD"), cancellable = true)
    private void simpleskills$reflectEligibleProjectile(Projectile projectile,
            CallbackInfoReturnable<ProjectileDeflection> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player) || player.level().isClientSide()
                || Defense.getSkillLevelFromCache(player.getUUID()) < 30
                || !player.isBlocking() || !player.getUseItem().is(Items.SHIELD)
                || !simpleskills$isSupportedVanillaProjectile(projectile)) {
            return;
        }
        if (projectile instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) return;
        Vec3 towardProjectile = projectile.position().subtract(player.position()).multiply(1.0, 0.0, 1.0).normalize();
        Vec3 facing = player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
        if (towardProjectile.dot(facing) < 0.5) return;

        cir.setReturnValue(DefenseCombat.PROJECTILE_REFLECTION);
    }

    private static boolean simpleskills$isSupportedVanillaProjectile(Projectile projectile) {
        EntityType<?> type = projectile.getType();
        return type == EntityTypes.ARROW || type == EntityTypes.SPECTRAL_ARROW || type == EntityTypes.TRIDENT
                || type == EntityTypes.FIREBALL || type == EntityTypes.SMALL_FIREBALL
                || type == EntityTypes.WITHER_SKULL || type == EntityTypes.DRAGON_FIREBALL
                || type == EntityTypes.SHULKER_BULLET;
    }
}
