package com.github.ob_yekt.simpleskills.mixin.DEFENSE;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractArrow.class)
public interface AbstractArrowDamageAccessor {
    @Accessor("baseDamage")
    double simpleskills$getBaseDamage();
}
