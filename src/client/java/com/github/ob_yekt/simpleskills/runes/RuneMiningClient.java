package com.github.ob_yekt.simpleskills.runes;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Client-side bridge from vanilla's real destroyProgress to the server-owned block state. */
public final class RuneMiningClient {
    private static BlockPos lastPos;
    private static int lastCharge = -1;
    private static long lastSentTick = Long.MIN_VALUE;

    private RuneMiningClient() {}

    public static void sync(BlockPos pos, float progress) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || pos == null) { clear(); return; }
        BlockState state = minecraft.level.getBlockState(pos);
        if (!(state.getBlock() instanceof RuneOreBlock)) { clear(); return; }

        BlockPos immutable = pos.immutable();
        int charge = Math.max(1, Math.min(9, (int) Math.ceil(Math.max(0.0F, Math.min(1.0F, progress)) * 9.0F)));
        long now = minecraft.level.getGameTime();
        boolean changedTarget = lastPos == null || !lastPos.equals(immutable);
        if (changedTarget) {
            sendClear();
            lastPos = immutable;
            lastCharge = -1;
        }
        if (charge != lastCharge || now - lastSentTick >= 4L) {
            ClientPlayNetworking.send(new RuneMiningPayload(immutable.getX(), immutable.getY(), immutable.getZ(), charge));
            lastCharge = charge;
            lastSentTick = now;
        }
    }

    public static void clear() {
        sendClear();
        lastPos = null;
        lastCharge = -1;
        lastSentTick = Long.MIN_VALUE;
    }

    private static void sendClear() {
        if (lastPos != null && Minecraft.getInstance().getConnection() != null) {
            ClientPlayNetworking.send(new RuneMiningPayload(lastPos.getX(), lastPos.getY(), lastPos.getZ(), 0));
        }
    }
}
