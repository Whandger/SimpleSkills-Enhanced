package com.github.ob_yekt.simpleskills.mixin.FARMING;

import com.github.ob_yekt.simpleskills.farming.AutoPlant;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Auto Plant hook. {@code Block.dropResources(state, level, pos, blockEntity, breaker, tool)} is the one
 * place vanilla spawns the drops of a block broken by an entity. When Auto Plant handles the harvest
 * it spawns the (seed-reduced) drops itself and cancels vanilla's call, so nothing is dropped twice.
 * In every other case ({@link AutoPlant#tryHarvestAndReplant} returns false) vanilla runs untouched.
 */
@Mixin(Block.class)
public abstract class AutoPlantMixin {

    @Inject(
            method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void simpleskills$autoPlant(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockEntity blockEntity,
            Entity breaker,
            ItemStack tool,
            CallbackInfo ci
    ) {
        if (AutoPlant.tryHarvestAndReplant(state, level, pos, blockEntity, breaker, tool)) {
            ci.cancel();
        }
    }
}
