package com.github.ob_yekt.simpleskills.mixin.CRAFTING;

import com.github.ob_yekt.simpleskills.shields.CraftingLevelLock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Impede a retirada, com clique normal, do escudo de glowstone enquanto o Crafting estiver abaixo do nível 15.
 * O shift-clique é bloqueado em CraftingScreenHandlerMixin e InventoryCraftingLockMixin.
 */
@Mixin(Slot.class)
public abstract class CraftingResultLockMixin {

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void simpleskills$lockCraftingResult(Player player, CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (!(self instanceof ResultSlot)) return;
        if (player instanceof ServerPlayer serverPlayer && CraftingLevelLock.isLocked(serverPlayer, self.getItem())) {
            CraftingLevelLock.deny(serverPlayer);
            cir.setReturnValue(false);
        }
    }
}
