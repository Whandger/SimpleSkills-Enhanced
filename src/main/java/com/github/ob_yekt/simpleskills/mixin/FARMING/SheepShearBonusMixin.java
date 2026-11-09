package com.github.ob_yekt.simpleskills.mixin.FARMING;

import com.github.ob_yekt.simpleskills.farming.FarmingPerks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiConsumer;

/**
 * Animal Loot Bonus for sheep shearing. Uses the very same {@link FarmingPerks#rollAnimalLootBonus}
 * as killed animals (one shared percentage, no separate "Wool Bonus").
 *
 * The shearing player is only known in {@code mobInteract}, so it is remembered there and consumed by
 * {@code shear}. The extra wool is a copy of the wool the shear loot table actually produced, therefore
 * it always has the sheep's own colour. Shearing by dispensers or other non-player sources has no
 * player and keeps its vanilla result. The existing XP mixin (SheepShearingXPMixin) is untouched.
 */
@Mixin(Sheep.class)
public abstract class SheepShearBonusMixin {

    @Unique
    private ServerPlayer simpleskills$shearer;

    @Inject(method = "mobInteract", at = @At("HEAD"))
    private void simpleskills$rememberShearer(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        this.simpleskills$shearer = player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void simpleskills$forgetShearer(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        this.simpleskills$shearer = null;
    }

    @ModifyArg(
            method = "shear",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/sheep/Sheep;dropFromShearingLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/item/ItemInstance;Ljava/util/function/BiConsumer;)V"
            ),
            index = 3
    )
    private BiConsumer<ServerLevel, ItemStack> simpleskills$wrapWoolDrops(BiConsumer<ServerLevel, ItemStack> original) {
        ServerPlayer shearer = this.simpleskills$shearer;
        this.simpleskills$shearer = null; // one roll per shearing, never two

        if (shearer == null || !FarmingPerks.rollAnimalLootBonus(shearer)) {
            return original;
        }

        return (level, drop) -> {
            original.accept(level, drop);
            if (!drop.isEmpty()) {
                original.accept(level, drop.copy());
            }
        };
    }
}
