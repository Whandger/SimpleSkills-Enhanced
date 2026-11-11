package com.github.ob_yekt.simpleskills.mixin.DEFENSE;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShulkerBullet.class)
public interface ShulkerBulletTargetAccessor {
    @Accessor("finalTarget")
    void simpleskills$setFinalTarget(@Nullable EntityReference<Entity> target);
}
