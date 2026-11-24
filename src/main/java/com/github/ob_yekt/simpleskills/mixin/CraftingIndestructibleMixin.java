package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager;
import com.github.ob_yekt.simpleskills.utils.GatheringDurabilityContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies one Indestructible/profession durability roll to each durability-damage event. */
@Mixin(ItemStack.class)
public abstract class CraftingIndestructibleMixin {
    @Inject(
            method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void simpleskills$preventCraftedDurabilityLoss(int amount, LivingEntity entity,
                                                            EquipmentSlot slot, CallbackInfo ci) {
        if (amount <= 0 || !(entity instanceof ServerPlayer player)) return;

        ItemStack stack = (ItemStack) (Object) this;
        double professionChance = GatheringDurabilityContext.getProfessionChance(stack, player);
        double indestructibleChance = CraftingEquipmentManager.getIndestructibleChance(stack);
        // Add both sources once, retaining the existing 99% gathering-protection ceiling.
        double combinedChance = Math.min(0.99, professionChance + indestructibleChance);
        if (combinedChance > 0.0 && player.getRandom().nextDouble() < combinedChance) {
            ci.cancel();
        }
    }
}
