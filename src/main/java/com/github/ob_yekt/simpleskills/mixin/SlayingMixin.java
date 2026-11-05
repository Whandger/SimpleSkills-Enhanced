
package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.Slaying;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(Player.class)
public abstract class SlayingMixin {

    @ModifyExpressionValue(
        method = "attack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;getAttackDamageBonus(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)F"
        )
    )
    private float simpleskills$applySlayingBonuses(
        float originalBonus,
        @Local(ordinal = 0) float baseDamage,
        @Local(ordinal = 0) ItemStack attackingItemStack,
        @Local(name = "attackStrengthScale") float attackStrengthScale
    ) {
        Player player = (Player) (Object) this;

        // Only charged sword attacks receive Slaying bonuses.
        if (!attackingItemStack.is(ItemTags.SWORDS)
                || attackStrengthScale <= 0.9F) {
            return originalBonus;
        }

        int level = Slaying.getSkillLevelFromCache(player.getUUID());

        if (level <= 0) {
            return originalBonus;
        }

        // Regular damage bonus.
        float multiplier = Slaying.getDamageMultiplier(player.getUUID());

        // One custom critical roll per attack.
        if (player.getRandom().nextFloat()
                < Slaying.getCriticalChance(player.getUUID())) {
            multiplier *= 1.5f;
        }

        // Apply bonuses to base damage and original weapon bonus.
        return originalBonus
            + (baseDamage + originalBonus) * (multiplier - 1.0f);
    }

    @ModifyExpressionValue(
        method = "attack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
        )
    )
    private boolean simpleskills$applyLifeSteal(
        boolean original,
        @Local(argsOnly = true) Entity target,
        @Local(name = "oldLivingEntityHealth") float oldHealth,
        @Local(name = "attackingItemStack") ItemStack attackingItemStack,
        @Local(name = "attackStrengthScale") float attackStrengthScale
    ) {
        Player player = (Player) (Object) this;

        if (!original || player.level().isClientSide()) {
            return original;
        }

        // Only charged sword attacks can activate Life Steal.
        if (!attackingItemStack.is(ItemTags.SWORDS)
                || attackStrengthScale <= 0.9F) {
            return original;
        }

        int level = Slaying.getSkillLevelFromCache(player.getUUID());

        if (level < 50 || !(target instanceof LivingEntity livingTarget)) {
            return original;
        }

        // Only count actual health damage, not absorption damage.
        if (livingTarget.getHealth() >= oldHealth) {
            return original;
        }

        // Life Steal I: 30%; Life Steal II: 60%.
        float chance = level >= 99 ? 0.60f : 0.30f;

        if (player.getRandom().nextFloat() < chance) {
            player.heal(1.0f);
        }

        return original;
    }
}
