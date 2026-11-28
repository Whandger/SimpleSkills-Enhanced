package com.github.ob_yekt.simpleskills.mixin.LORE;

import com.github.ob_yekt.simpleskills.managers.LoreManager;
import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void materializeAbyssalLootRarity(Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemEntity itemEntity = (ItemEntity) (Object) this;
        ItemStack stack = itemEntity.getItem();
        if (CraftingEquipmentManager.hasPendingAbyssalLootRarity(stack)) {
            // Finalize the already-rolled Mythic/Transcendent tier before vanilla
            // transfers the stack to the player's inventory.
            CraftingEquipmentManager.applyAbyssalLootRarity(stack, serverPlayer);
            itemEntity.setItem(stack);
        }
    }

    @Inject(method = "playerTouch", at = @At("TAIL"))
    private void onPlayerCollision(Player player, CallbackInfo ci) {
        ItemEntity itemEntity = (ItemEntity) (Object) this;
        ItemStack stack = itemEntity.getItem();
        LoreManager.applyLoreToStack(stack);
    }
}