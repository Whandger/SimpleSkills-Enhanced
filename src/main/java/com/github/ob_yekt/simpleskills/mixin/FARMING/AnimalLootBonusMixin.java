package com.github.ob_yekt.simpleskills.mixin.FARMING;

import com.github.ob_yekt.simpleskills.farming.FarmingPerks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Animal Loot Bonus for killed animals.
 *
 * The main death loot table (vanilla's {@code dropFromLootTable(level, source, playerKilled)}) is left
 * completely alone: Looting, loot conditions and other mods keep working. We only (1) roll the shared
 * Animal Loot Bonus once per kill and (2) if it succeeds, wrap the consumer that receives the table's
 * results so each normal drop is delivered one extra time. Because extras are copies of drops the animal
 * really produced, nothing it would not drop can appear, and periodic drops (eggs) never pass through here.
 * Farming XP is not involved at all, so it cannot be duplicated.
 */
@Mixin(LivingEntity.class)
public abstract class AnimalLootBonusMixin {

    @Unique
    private boolean simpleskills$animalBonusPending;

    @Inject(
            method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V",
            at = @At("HEAD")
    )
    private void simpleskills$rollAnimalLootBonus(ServerLevel level, DamageSource source, boolean playerKilled, CallbackInfo ci) {
        this.simpleskills$animalBonusPending = false;

        LivingEntity self = (LivingEntity) (Object) this;
        // Farm animals only. Tamed pets (wolves, cats, parrots...) are not livestock.
        if (!playerKilled || !(self instanceof Animal) || self instanceof TamableAnimal) {
            return;
        }

        Player killer = self.getLastHurtByPlayer();
        if (killer instanceof ServerPlayer serverPlayer && FarmingPerks.rollAnimalLootBonus(serverPlayer)) {
            this.simpleskills$animalBonusPending = true;
        }
    }

    @Inject(
            method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V",
            at = @At("RETURN")
    )
    private void simpleskills$clearAnimalLootBonus(ServerLevel level, DamageSource source, boolean playerKilled, CallbackInfo ci) {
        this.simpleskills$animalBonusPending = false;
    }

    @ModifyVariable(
            method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;ZLnet/minecraft/resources/ResourceKey;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Consumer<ItemStack> simpleskills$wrapAnimalDrops(Consumer<ItemStack> original) {
        if (!this.simpleskills$animalBonusPending) {
            return original;
        }

        // Applied once per kill, never twice.
        this.simpleskills$animalBonusPending = false;

        return stack -> {
            original.accept(stack);
            if (!stack.isEmpty()) {
                original.accept(stack.copy());
            }
        };
    }
}
