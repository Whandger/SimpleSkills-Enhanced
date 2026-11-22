package com.github.ob_yekt.simpleskills.shields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * Minecraft's block-light scale ends at level 15. The base shield keeps the existing
 * level 14, the diamond shield uses level 15, and the netherite shield spreads level-15
 * light across the center plus available surrounding cells to illuminate a wider area.
 * Only air cells are used, and every temporary light block is restored afterwards.
 */
public final class GlowstoneShieldLight {
    private static final int GLOWSTONE_LIGHT_LEVEL = 14;
    private static final int DIAMOND_LIGHT_LEVEL = 15;
    private static final int NETHERITE_LIGHT_LEVEL = 15;

    private static final List<Direction> DIAMOND_SPREAD = List.of(Direction.EAST, Direction.WEST);
    private static final List<Direction> NETHERITE_SPREAD = List.of(
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN
    );

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

    private static final Map<UUID, List<Source>> SOURCES = new HashMap<>();

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
            List<Source> currentSources = SOURCES.getOrDefault(playerId, List.of());
            Set<BlockPos> ownedCells = new HashSet<>();
            for (Source source : currentSources) {
                if (source.level == level) ownedCells.add(source.cell);
            }

            BlockPos eyeCell = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
            BlockPos anchor = findUsableCell(level, eyeCell, ownedCells);
            if (anchor == null) {
                release(playerId);
                continue;
            }

            List<BlockPos> desiredCells = new ArrayList<>();
            desiredCells.add(anchor);
            for (Direction direction : spreadDirections(heldShield)) {
                BlockPos neighbor = anchor.relative(direction);
                if (!desiredCells.contains(neighbor) && canUse(level, neighbor, ownedCells)) {
                    desiredCells.add(neighbor);
                }
            }

            if (matchesCurrentSources(currentSources, desiredCells, level, lightLevel)) {
                continue;
            }

            release(playerId);
            List<Source> newSources = new ArrayList<>(desiredCells.size());
            for (BlockPos cell : desiredCells) {
                if (!level.isLoaded(cell) || !level.getBlockState(cell).isAir()) continue;
                BlockState original = level.getBlockState(cell);
                BlockState lightState = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lightLevel);
                if (level.setBlock(cell, lightState, Block.UPDATE_CLIENTS)) {
                    newSources.add(new Source(level, cell, original, lightLevel));
                }
            }
            if (!newSources.isEmpty()) SOURCES.put(playerId, newSources);
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

    private static List<Direction> spreadDirections(ItemStack stack) {
        if (stack.is(GlowstoneShields.NETHERITE_GLOWSTONE_SHIELD)) return NETHERITE_SPREAD;
        if (stack.is(GlowstoneShields.DIAMOND_GLOWSTONE_SHIELD)) return DIAMOND_SPREAD;
        return List.of();
    }

    private static boolean matchesCurrentSources(
            List<Source> current,
            List<BlockPos> desired,
            ServerLevel level,
            int lightLevel
    ) {
        if (current.size() != desired.size()) return false;
        for (int i = 0; i < desired.size(); i++) {
            Source source = current.get(i);
            if (source.level != level || !source.cell.equals(desired.get(i)) || source.lightLevel != lightLevel) {
                return false;
            }
            BlockState state = level.getBlockState(source.cell);
            if (!state.is(Blocks.LIGHT) || state.getValue(LightBlock.LEVEL) != lightLevel) return false;
        }
        return true;
    }

    private static BlockPos findUsableCell(ServerLevel level, BlockPos position, Set<BlockPos> ownedCells) {
        if (canUse(level, position, ownedCells)) return position;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = position.relative(direction);
            if (canUse(level, neighbor, ownedCells)) return neighbor;
        }
        return null;
    }

    private static boolean canUse(ServerLevel level, BlockPos position, Set<BlockPos> ownedCells) {
        if (!level.isLoaded(position)) return false;
        return ownedCells.contains(position) || level.getBlockState(position).isAir();
    }

    private static void release(UUID playerId) {
        List<Source> sources = SOURCES.remove(playerId);
        if (sources == null) return;
        for (Source source : sources) {
            if (!source.level.isLoaded(source.cell)) continue;
            BlockState current = source.level.getBlockState(source.cell);
            if (current.is(Blocks.LIGHT) && current.getValue(LightBlock.LEVEL) == source.lightLevel) {
                source.level.setBlock(source.cell, source.original, Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Restores all temporary light sources when the server stops. */
    public static void clearAll() {
        for (UUID playerId : new ArrayList<>(SOURCES.keySet())) release(playerId);
    }
}
