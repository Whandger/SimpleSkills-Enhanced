package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Applies Tenacity to the Powder Snow FREEZE inside-block effect. */
@Mixin(Entity.class)
public abstract class CraftingFreezeTenacityMixin {
    @ModifyVariable(
            method = "setTicksFrozen(I)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private int simpleskills$reducePowderSnowFreezeBuildUp(int requestedTicks) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player) || !self.isInPowderSnow
                || requestedTicks <= self.getTicksFrozen()) {
            return requestedTicks;
        }
        return CraftingEquipmentManager.reduceFreezeMeterProgress(
                player, self.getTicksFrozen(), requestedTicks);
    }
}
