package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives crafted equipment with Indestructible a chance to avoid ordinary durability damage. */
@Mixin(ItemStack.class)
public abstract class CraftingIndestructibleMixin {
    @Inject(
            method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void simpleskills$preventCraftedDurabilityLoss(int amount, LivingEntity entity,
                                                            EquipmentSlot slot, CallbackInfo ci) {
        if (amount > 0 && entity instanceof ServerPlayer player
                && CraftingEquipmentManager.shouldPreventDurabilityLoss((ItemStack) (Object) this, player)) {
            ci.cancel();
        }
    }
}
