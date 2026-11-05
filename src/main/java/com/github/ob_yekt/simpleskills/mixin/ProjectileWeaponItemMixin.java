
package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.Ranged;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

@Mixin(ProjectileWeaponItem.class)
public abstract class ProjectileWeaponItemMixin {

    private static final double DUPLICATE_OFFSET = 0.15;

    @Shadow
    protected abstract Projectile createProjectile(
            Level level,
            LivingEntity shooter,
            ItemStack heldItem,
            ItemStack projectile,
            boolean isCrit
    );

    @WrapOperation(
            method = "shoot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectile(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;)Lnet/minecraft/world/entity/projectile/Projectile;"
            )
    )
    private Projectile simpleskills$duplicateRangedProjectile(
            Projectile originalProjectile,
            ServerLevel level,
            ItemStack projectileStack,
            Consumer<Projectile> shootFunction,
            Operation<Projectile> original,
            @Local(argsOnly = true, ordinal = 0) LivingEntity shooter,
            @Local(argsOnly = true) ItemStack weapon,
            @Local(argsOnly = true) boolean isCrit
    ) {
        // Always spawn the original projectile normally first.
        Projectile spawnedProjectile = original.call(
                originalProjectile,
                level,
                projectileStack,
                shootFunction
        );

        // Only server players can receive the Ranged duplication perk.
        if (!(shooter instanceof ServerPlayer player)) {
            return spawnedProjectile;
        }

        Object weaponItem = this;

        boolean isBow = weaponItem instanceof BowItem;
        boolean isCrossbow = weaponItem instanceof CrossbowItem;

        if (!isBow && !isCrossbow) {
            return spawnedProjectile;
        }

        // Bows and crossbows can duplicate arrows.
        boolean isArrow = spawnedProjectile instanceof AbstractArrow;

        // Only crossbows can duplicate firework rockets.
        boolean isFirework = isCrossbow
                && spawnedProjectile instanceof FireworkRocketEntity;

        if (!isArrow && !isFirework) {
            return spawnedProjectile;
        }

        // Check the guaranteed double-shot progression.
        float doubleShotChance = Ranged.getDoubleShotChance(
                player.getUUID()
        );

        if (doubleShotChance <= 0.0F
                || level.getRandom().nextFloat() >= doubleShotChance) {
            return spawnedProjectile;
        }

        // First additional projectile: double shot.
        simpleskills$spawnDuplicate(
                spawnedProjectile,
                level,
                shooter,
                weapon,
                projectileStack,
                isCrit
        );

        // Check the additional triple-shot progression.
        float tripleShotChance = Ranged.getTripleShotChance(
                player.getUUID()
        );

        if (tripleShotChance > 0.0F
                && level.getRandom().nextFloat() < tripleShotChance) {

            // Second additional projectile: triple shot.
            simpleskills$spawnDuplicate(
                    spawnedProjectile,
                    level,
                    shooter,
                    weapon,
                    projectileStack,
                    isCrit
            );
        }

        return spawnedProjectile;
    }

    private void simpleskills$spawnDuplicate(
            Projectile originalProjectile,
            ServerLevel level,
            LivingEntity shooter,
            ItemStack weapon,
            ItemStack projectileStack,
            boolean isCrit
    ) {
        // Create the additional projectile using the original weapon implementation.
        Projectile duplicate = this.createProjectile(
                level,
                shooter,
                weapon,
                projectileStack,
                isCrit
        );

        Vec3 originalMovement = originalProjectile.getDeltaMovement();

        // Calculate a random position around the original trajectory.
        Vec3 randomOffset = simpleskills$getRandomOffset(
                originalMovement,
                level
        );

        duplicate.setPos(
                originalProjectile.getX() + randomOffset.x,
                originalProjectile.getY() + randomOffset.y,
                originalProjectile.getZ() + randomOffset.z
        );

        // Preserve the original trajectory and rotation.
        duplicate.setDeltaMovement(originalMovement);
        duplicate.setYRot(originalProjectile.getYRot());
        duplicate.setXRot(originalProjectile.getXRot());

        // Preserve the original firework's flight timing.
        if (originalProjectile instanceof FireworkRocketEntity originalRocket
                && duplicate instanceof FireworkRocketEntity duplicateRocket) {

            FireworkRocketAccessor originalAccessor =
                    (FireworkRocketAccessor) originalRocket;

            FireworkRocketAccessor duplicateAccessor =
                    (FireworkRocketAccessor) duplicateRocket;

            duplicateAccessor.simpleskills$setLife(
                    originalAccessor.simpleskills$getLife()
            );

            duplicateAccessor.simpleskills$setLifetime(
                    originalAccessor.simpleskills$getLifetime()
            );
        }

        // Prevent duplicate projectiles from granting additional Ranged XP.
        Ranged.markDuplicateProjectile(duplicate);

        // Spawn without consuming additional ammunition.
        Projectile.spawnProjectile(
                duplicate,
                level,
                projectileStack,
                ignored -> {
                }
        );
    }

    /**
     * Generates a random offset around the original projectile trajectory.
     *
     * The offset is perpendicular to the direction of travel,
     * allowing additional projectiles to appear above, below,
     * left, right, or diagonally around the original.
     */
    private Vec3 simpleskills$getRandomOffset(
            Vec3 originalMovement,
            ServerLevel level
    ) {
        Vec3 direction = originalMovement;

        if (direction.lengthSqr() < 0.0001) {
            direction = new Vec3(0.0, 0.0, 1.0);
        } else {
            direction = direction.normalize();
        }

        // Select a reference vector that is not parallel to the trajectory.
        Vec3 reference = Math.abs(direction.y) > 0.99
                ? new Vec3(1.0, 0.0, 0.0)
                : new Vec3(0.0, 1.0, 0.0);

        // Build two perpendicular vectors around the trajectory.
        Vec3 perpendicularA = direction.cross(reference).normalize();
        Vec3 perpendicularB = direction.cross(perpendicularA).normalize();

        // Random angle around the trajectory.
        double angle = level.getRandom().nextDouble() * Math.PI * 2.0;

        double offsetA = Math.cos(angle) * DUPLICATE_OFFSET;
        double offsetB = Math.sin(angle) * DUPLICATE_OFFSET;

        return perpendicularA.scale(offsetA)
                .add(perpendicularB.scale(offsetB));
    }
}
