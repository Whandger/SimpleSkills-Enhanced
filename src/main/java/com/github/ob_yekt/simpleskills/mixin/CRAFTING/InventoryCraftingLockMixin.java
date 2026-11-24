package com.github.ob_yekt.simpleskills.mixin.CRAFTING;

import com.github.ob_yekt.simpleskills.shields.CraftingLevelLock;
import com.github.ob_yekt.simpleskills.crafting.CraftingInventoryGuard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Crafting 2x2 do inventário: bloqueia o shift-clique do escudo de glowstone abaixo do nível 15.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryCraftingLockMixin {

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void simpleskills$blockLockedQuickMove(Player player, int slotIndex,
                                                   CallbackInfoReturnable<ItemStack> cir) {
        if (slotIndex != 0) return;
        InventoryMenu menu = (InventoryMenu) (Object) this;
        ItemStack result = menu.getSlot(0).getItem();
        if (!CraftingInventoryGuard.canTakeResult(player, result)) {
            CraftingInventoryGuard.notifyInventoryFull(player);
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        if (player instanceof ServerPlayer serverPlayer && CraftingLevelLock.isLocked(serverPlayer, result)) {
            CraftingLevelLock.deny(serverPlayer);
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
