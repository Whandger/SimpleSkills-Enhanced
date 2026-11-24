package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Phantom Stride raises step height and removes Soul Sand's movement multiplier. */
@Mixin(LivingEntity.class)
public abstract class PhantomStrideMixin {
    @ModifyReturnValue(method = "maxUpStep", at = @At("RETURN"))
    private float simpleskills$stepUpOneBlock(float original) {
        if ((Object) this instanceof Player player
                && CraftingEquipmentManager.hasEquippedPerkInSlot(
                        player, EquipmentSlot.LEGS, "phantom_stride")) {
            return Math.max(original, 1.0F);
        }
        return original;
    }

    @ModifyReturnValue(method = "getBlockSpeedFactor", at = @At("RETURN"))
    private float simpleskills$ignoreSoulSandSlowdown(float original) {
        if (!((Object) this instanceof Player player)
                || !CraftingEquipmentManager.hasEquippedPerkInSlot(
                        player, EquipmentSlot.LEGS, "phantom_stride")) return original;
        BlockPos below = BlockPos.containing(player.getX(), player.getBoundingBox().minY - 0.2D, player.getZ());
        return player.level().getBlockState(below).is(Blocks.SOUL_SAND) ? 1.0F : original;
    }
}
