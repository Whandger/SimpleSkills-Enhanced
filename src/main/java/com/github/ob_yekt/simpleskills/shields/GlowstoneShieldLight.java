package com.github.ob_yekt.simpleskills.shields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Portable light while a glowstone shield is held in either hand.
 *
 * Uses a single temporary light block per player at the eye position (or adjacent air cell):
 * - Glowstone Shield: light level 13
 * - Diamond Glowstone Shield: light level 14
 * - Netherite Glowstone Shield: light level 15
 */
public final class GlowstoneShieldLight {
    private static final int GLOWSTONE_LIGHT_LEVEL = 13;
    private static final int DIAMOND_LIGHT_LEVEL = 14;
    private static final int NETHERITE_LIGHT_LEVEL = 15;

    private static final class Source {
        final ServerLevel level;
        final BlockPos cell;
        final BlockState original;
        final int lightLevel;

        Source(ServerLevel level, BlockPos cell, BlockState original, int lightLevel) {
            this.level = level;
            this.cell = cell;
            this.original = original;
            this.lightLevel = lightLevel;
        }
    }

    private static final Map<UUID, Source> SOURCES = new HashMap<>();

    private GlowstoneShieldLight() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(GlowstoneShieldLight::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (player != null) release(player.getUUID());
        });
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            ItemStack heldShield = heldGlowstoneShield(player);
            if (heldShield.isEmpty() || !(player.level() instanceof ServerLevel level)) {
                release(playerId);
                continue;
            }

            int lightLevel = getLightLevel(heldShield);
            Source current = SOURCES.get(playerId);
            BlockPos ownedCell = (current != null && current.level == level) ? current.cell : null;

            BlockPos eyeCell = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
            BlockPos target = findUsableCell(level, eyeCell, ownedCell);
            if (target == null) {
                release(playerId);
                continue;
            }

            if (current != null && current.level == level && current.cell.equals(target) && current.lightLevel == lightLevel) {
                BlockState state = level.getBlockState(target);
                if (state.is(Blocks.LIGHT) && state.getValue(LightBlock.LEVEL) == lightLevel) {
                    continue;
                }
            }

            release(playerId);
            if (!level.isLoaded(target) || !level.getBlockState(target).isAir()) {
                continue;
            }

            BlockState original = level.getBlockState(target);
            BlockState lightState = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lightLevel);
            if (level.setBlock(target, lightState, Block.UPDATE_CLIENTS)) {
                SOURCES.put(playerId, new Source(level, target, original, lightLevel));
            }
        }
    }

    private static ItemStack heldGlowstoneShield(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(GlowstoneShields.GLOWSTONE_SHIELDS)) return mainHand;
        ItemStack offHand = player.getOffhandItem();
        return offHand.is(GlowstoneShields.GLOWSTONE_SHIELDS) ? offHand : ItemStack.EMPTY;
    }

    private static int getLightLevel(ItemStack stack) {
        if (stack.is(GlowstoneShields.NETHERITE_GLOWSTONE_SHIELD)) return NETHERITE_LIGHT_LEVEL;
        if (stack.is(GlowstoneShields.DIAMOND_GLOWSTONE_SHIELD)) return DIAMOND_LIGHT_LEVEL;
        return GLOWSTONE_LIGHT_LEVEL;
    }

    private static BlockPos findUsableCell(ServerLevel level, BlockPos position, BlockPos ownedCell) {
        if (canUse(level, position, ownedCell)) return position;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = position.relative(direction);
            if (canUse(level, neighbor, ownedCell)) return neighbor;
        }
        return null;
    }

    private static boolean canUse(ServerLevel level, BlockPos position, BlockPos ownedCell) {
        if (!level.isLoaded(position)) return false;
        return position.equals(ownedCell) || level.getBlockState(position).isAir();
    }

    private static void release(UUID playerId) {
        Source source = SOURCES.remove(playerId);
        if (source == null || !source.level.isLoaded(source.cell)) return;
        BlockState current = source.level.getBlockState(source.cell);
        if (current.is(Blocks.LIGHT) && current.getValue(LightBlock.LEVEL) == source.lightLevel) {
            source.level.setBlock(source.cell, source.original, Block.UPDATE_CLIENTS);
        }
    }

    /** Restores all temporary light sources when the server stops. */
    public static void clearAll() {
        for (UUID playerId : new ArrayList<>(SOURCES.keySet())) release(playerId);
    }
}
