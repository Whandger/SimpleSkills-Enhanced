package com.github.ob_yekt.simpleskills.runes;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative light/texture state while a player mines either Rune Ore. */
public final class RuneMiningManager {
    private static final long TIMEOUT_TICKS = 12L;
    private static final Map<UUID, ActiveMining> ACTIVE = new HashMap<>();

    private RuneMiningManager() {}

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(RuneMiningPayload.TYPE, RuneMiningPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RuneMiningPayload.TYPE, (payload, context) ->
                context.server().execute(() -> apply(context.player(), payload)));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<Map.Entry<UUID, ActiveMining>> iterator = ACTIVE.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, ActiveMining> entry = iterator.next();
                ActiveMining mining = entry.getValue();
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                long now = mining.level().getGameTime();
                if (player == null || now - mining.lastUpdate() > TIMEOUT_TICKS
                        || !isRuneOre(mining.level().getBlockState(mining.pos()))) {
                    reset(mining.level(), mining.pos());
                    iterator.remove();
                }
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ActiveMining mining = ACTIVE.remove(handler.player.getUUID());
            if (mining != null) reset(mining.level(), mining.pos());
        });

        // Purple dust burst after either Rune Ore is actually broken.
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerLevel level && state.getBlock() instanceof RuneOreBlock) {
                DustParticleOptions violet = new DustParticleOptions(0xA64DFF, 1.15F);
                level.sendParticles(violet, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        22, 0.32D, 0.32D, 0.32D, 0.035D);
            }
        });
    }

    private static void apply(ServerPlayer player, RuneMiningPayload payload) {
        if (player == null || payload == null || payload.charge() < 0 || payload.charge() > 9) return;
        BlockPos pos = new BlockPos(payload.x(), payload.y(), payload.z());
        ServerLevel level = (ServerLevel) player.level();
        if (!level.hasChunkAt(pos)
                || player.distanceToSqr(
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D
                ) > 64.0D) {
            return;
        }

        ActiveMining previous = ACTIVE.get(player.getUUID());
        if (previous != null && (!previous.level().equals(level) || !previous.pos().equals(pos))) {
            reset(previous.level(), previous.pos());
            ACTIVE.remove(player.getUUID());
        }

        BlockState state = level.getBlockState(pos);
        if (!isRuneOre(state)) {
            ActiveMining old = ACTIVE.remove(player.getUUID());
            if (old != null) reset(old.level(), old.pos());
            return;
        }

        if (payload.charge() == 0) {
            reset(level, pos);
            ACTIVE.remove(player.getUUID());
            return;
        }

        int charge = Math.max(1, Math.min(9, payload.charge()));
        BlockState charged = state.setValue(RuneOreBlock.CHARGE, charge);
        if (charged != state) level.setBlock(pos, charged, 3);
        ACTIVE.put(player.getUUID(), new ActiveMining(level, pos.immutable(), level.getGameTime()));
    }

    private static boolean isRuneOre(BlockState state) {
        return state.getBlock() instanceof RuneOreBlock;
    }

    private static void reset(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return;
        BlockState state = level.getBlockState(pos);
        if (isRuneOre(state) && state.getValue(RuneOreBlock.CHARGE) != 0) {
            level.setBlock(pos, state.setValue(RuneOreBlock.CHARGE, 0), 3);
        }
    }

    private record ActiveMining(ServerLevel level, BlockPos pos, long lastUpdate) {}
}
