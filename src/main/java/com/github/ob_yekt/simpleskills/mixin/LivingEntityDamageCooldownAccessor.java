package com.github.ob_yekt.simpleskills.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.world.entity.LivingEntity;

/** Accesses the vanilla damage cooldown for a separate, server-authoritative affix hit. */
@Mixin(LivingEntity.class)
public interface LivingEntityDamageCooldownAccessor {
    @Accessor("damageCooldownTime")
    int simpleskills$getDamageCooldownTime();

    @Accessor("damageCooldownTime")
    void simpleskills$setDamageCooldownTime(int ticks);
}
