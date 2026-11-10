package com.github.ob_yekt.simpleskills.enchanting;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;

/**
 * Replaces the vanilla Enchanting Table menu with Rune Infusion.
 * The vanilla menu is never opened, so no XP / lapis is ever charged.
 */
public final class RuneInfusionTable {
    private RuneInfusionTable() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            if (!level.getBlockState(hit.getBlockPos()).is(Blocks.ENCHANTING_TABLE)) {
                return InteractionResult.PASS;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                RuneInfusionNetworking.openFromTable(serverPlayer);
            }
            return InteractionResult.SUCCESS;
        });
    }
}