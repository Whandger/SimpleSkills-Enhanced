package com.github.ob_yekt.simpleskills.crafting;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Prevents durable crafting outputs from being consumed when the player's 36-slot
 * main inventory cannot receive the newly generated equipment.
 */
public final class CraftingInventoryGuard {
    private static final int MAIN_INVENTORY_SLOTS = 36;

    private CraftingInventoryGuard() {}

    public static boolean canTakeResult(Player player, ItemStack result) {
        if (player == null || result == null || result.isEmpty()
                || result.get(DataComponents.MAX_DAMAGE) == null) {
            return true;
        }

        for (int slot = 0; slot < MAIN_INVENTORY_SLOTS; slot++) {
            if (player.getInventory().getItem(slot).isEmpty()) return true;
        }
        return false;
    }

    public static void notifyInventoryFull(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("message.simpleskills.crafting.inventory_full"), true);
        }
    }
}
