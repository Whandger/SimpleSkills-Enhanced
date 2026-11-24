package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Tenacity shortens harmful status effects while the player's crafted armor is equipped. */
@Mixin(LivingEntity.class)
public abstract class CraftingTenacityMixin {
    @ModifyVariable(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private MobEffectInstance simpleskills$reduceHarmfulEffectDuration(MobEffectInstance effect) {
        if (!((Object) this instanceof Player player)) return effect;
        return CraftingEquipmentManager.applyTenacityToEffect(player, effect);
    }
}
