
package com.github.ob_yekt.simpleskills.mixin;

import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FireworkRocketEntity.class)
public interface FireworkRocketAccessor {

    @Accessor("life")
    int simpleskills$getLife();

    @Accessor("life")
    void simpleskills$setLife(int life);

    @Accessor("lifetime")
    int simpleskills$getLifetime();

    @Accessor("lifetime")
    void simpleskills$setLifetime(int lifetime);
}
