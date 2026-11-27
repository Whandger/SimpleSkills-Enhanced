package com.github.ob_yekt.simpleskills.mixin.LORE;

import com.github.ob_yekt.simpleskills.managers.LoreManager;
import com.github.ob_yekt.simpleskills.abyss.AbyssalKatanaItems;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Prediction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public class PlayerInventoryMixin {
    @Inject(method = "setItem", at = @At("TAIL"))
    private void onSetStack(int slot, ItemStack stack, CallbackInfo ci) {
        LoreManager.applyLoreToStack(stack);
    }

    @Inject(method = "setChanged", at = @At("TAIL"))
    private void onMarkDirty(CallbackInfo ci) {
        Inventory inventory = (Inventory) (Object) this;
        LoreManager.applyLoreToInventory(inventory);
    }
    /**
     * A katana is always a two-handed weapon. Reject attempts to put any non-empty stack
     * into the offhand; on the server, move it to inventory or drop the exact remainder.
     */
    @Inject(method = "setItem", at = @At("HEAD"), cancellable = true)
    private void simpleskills$blockOffhandWhileKatanaEquipped(int slot, ItemStack stack, CallbackInfo ci) {
        if (slot != Inventory.SLOT_OFFHAND || stack == null || stack.isEmpty()) return;
        Inventory inventory = (Inventory) (Object) this;
        if (!AbyssalKatanaItems.isKatana(inventory.player.getMainHandItem().getItem())) return;
        ci.cancel();

        if (inventory.player.level().isClientSide() || !(inventory.player instanceof ServerPlayer serverPlayer)) return;
        ItemStack remaining = stack.copy();
        serverPlayer.getInventory().add(remaining);
        if (!remaining.isEmpty()) {
            ItemStack dropped = remaining.copy();
            serverPlayer.drop(dropped, false, Prediction.SERVER_ONLY);
            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.simpleskills.katana.offhand_dropped", dropped.getHoverName()), false);
        }
    }

}