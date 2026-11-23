package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Aegis of Purity rejects every harmful potion effect while a Transcendent chestplate is worn. */
@Mixin(LivingEntity.class)
public abstract class AegisOfPurityMixin {
    @Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
    private void simpleskills$denyHarmfulEffect(MobEffectInstance effect,
                                                CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player
                && CraftingEquipmentManager.hasEquippedPerkInSlot(
                        player, EquipmentSlot.CHEST, "aegis_of_purity")
                && effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "forceAddEffect", at = @At("HEAD"), cancellable = true)
    private void simpleskills$denyForcedHarmfulEffect(MobEffectInstance effect,
                                                      net.minecraft.world.entity.Entity source,
                                                      org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if ((Object) this instanceof Player player
                && CraftingEquipmentManager.hasEquippedPerkInSlot(
                        player, EquipmentSlot.CHEST, "aegis_of_purity")
                && effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            ci.cancel();
        }
    }
}
