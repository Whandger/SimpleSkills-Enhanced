package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.GatheringSpeed;
import com.github.ob_yekt.simpleskills.utils.GatheringDurabilityContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Supplies profession protection context to the single hurtAndBreak durability roll. */
@Mixin(ItemStack.class)
public abstract class GatheringDurabilityMixin {

    @Inject(
            method = "mineBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD")
    )
    private void simpleskills$beginGatheringDurability(
            Level level,
            BlockState state,
            BlockPos pos,
            Player player,
            CallbackInfo ci
    ) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return;

        ItemStack stack = (ItemStack) (Object) this;
        double professionChance = GatheringSpeed.getDurabilityProtectionChance(player, state, stack);
        GatheringDurabilityContext.begin(stack, serverPlayer, professionChance);
    }

    @Inject(
            method = "mineBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("RETURN")
    )
    private void simpleskills$finishGatheringDurability(
            Level level,
            BlockState state,
            BlockPos pos,
            Player player,
            CallbackInfo ci
    ) {
        if (level.isClientSide() || player == null) return;
        GatheringDurabilityContext.end((ItemStack) (Object) this, player);
    }
}
