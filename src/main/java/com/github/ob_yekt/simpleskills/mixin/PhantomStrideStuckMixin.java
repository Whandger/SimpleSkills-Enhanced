package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents selected movement-stuck multipliers while Phantom Stride leggings are equipped. */
@Mixin(Entity.class)
public abstract class PhantomStrideStuckMixin {
    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void simpleskills$ignoreWebAndPowderSnow(BlockState state, Vec3 multiplier, CallbackInfo ci) {
        if ((Object) this instanceof Player player
                && CraftingEquipmentManager.hasEquippedPerkInSlot(
                        player, EquipmentSlot.LEGS, "phantom_stride")
                && (state.is(Blocks.COBWEB) || state.is(Blocks.POWDER_SNOW))) {
            ci.cancel();
        }
    }
}
