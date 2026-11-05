package com.github.ob_yekt.simpleskills.mixin.DEFENSE;

import com.github.ob_yekt.simpleskills.Defense;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Applies the passive Defense armor durability protection.
 *
 * Each Defense level grants a 1% chance to prevent armor durability loss.
 * At level 99, the maximum protection chance is 99%.
 *
 * The chance is rolled independently for each eligible armor durability
 * loss event. Non-player entities and non-armor equipment are unaffected.
 */
@Mixin(LivingEntity.class)
public abstract class DefenseArmorDurabilityMixin {

    @WrapOperation(
            method = "doHurtEquipment",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"
            )
    )
    private void simpleskills$protectArmorDurability(
            ItemStack stack,
            int amount,
            LivingEntity entity,
            EquipmentSlot slot,
            Operation<Void> original
    ) {
        if (entity instanceof ServerPlayer player
                && slot.isArmor()
                && Defense.shouldPreventArmorDurabilityLoss(
                        player.getUUID(),
                        player.getRandom()
                )) {
            return;
        }

        original.call(stack, amount, entity, slot);
    }
}
