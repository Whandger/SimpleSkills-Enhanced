package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.GatheringSpeed;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gathering Durability: gives gathering tools a chance to avoid durability loss
 * when breaking blocks associated with their respective skills.
 *
 * Only affects block-breaking durability, preserving normal durability behavior
 * for combat and other item uses.
 */
@Mixin(ItemStack.class)
public abstract class GatheringDurabilityMixin {

    @Inject(
            method = "mineBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void simpleskills$reduceGatheringDurability(
            Level level,
            BlockState state,
            BlockPos pos,
            Player player,
            CallbackInfo ci
    ) {
        if (level.isClientSide() || player == null) {
            return;
        }

        ItemStack stack = (ItemStack) (Object) this;

        if (stack != player.getMainHandItem()) {
            return;
        }

        if (GatheringSpeed.shouldPreventDurabilityLoss(player, state, stack)) {
            ci.cancel();
        }
    }
}
