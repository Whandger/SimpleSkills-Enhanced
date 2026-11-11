package com.github.ob_yekt.simpleskills.enchanting;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Replaces the vanilla Enchanting Table menu with Rune Infusion.
 * The vanilla menu is never opened, so no XP / lapis is ever charged.
 *
 * The infusion can only be opened (and started) at an Enchanting Table: the server remembers the
 * last table each player opened and only accepts a start request while that table still exists and
 * the player is within reach of it.
 */
public final class RuneInfusionTable {
    private record Access(ResourceKey<Level> dimension, BlockPos pos) {}

    private static final Map<UUID, Access> OPENED = new ConcurrentHashMap<>();
    /** Same reach as a vanilla container (8 blocks). */
    private static final double MAX_DISTANCE_SQ = 8.0 * 8.0;

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
                OPENED.put(serverPlayer.getUUID(), new Access(level.dimension(), hit.getBlockPos().immutable()));
                RuneInfusionNetworking.openFromTable(serverPlayer);
            }
            return InteractionResult.SUCCESS;
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                OPENED.remove(handler.player.getUUID()));
    }

    /** True while the table this player last opened still exists and is within reach. */
    public static boolean isNearOpenedTable(ServerPlayer player) {
        if (player == null) return false;
        Access access = OPENED.get(player.getUUID());
        if (access == null) return false;
        if (!player.level().dimension().equals(access.dimension())) return false;
        if (!player.level().getBlockState(access.pos()).is(Blocks.ENCHANTING_TABLE)) return false;
        return player.distanceToSqr(access.pos().getX() + 0.5, access.pos().getY() + 0.5,
                access.pos().getZ() + 0.5) <= MAX_DISTANCE_SQ;
    }
}
