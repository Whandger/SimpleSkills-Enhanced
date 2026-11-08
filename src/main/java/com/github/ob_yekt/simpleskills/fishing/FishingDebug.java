package com.github.ob_yekt.simpleskills.fishing;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player diagnostics of the fishing system ({@code /simpleskills fishing debug}).
 *
 * <p>When enabled, every decision of the session (spot validation, fish picked or rejected, approach
 * progress, bite, fight result) is printed to the player's chat and to the server log, which makes it
 * possible to see exactly why a fish does or does not come to the bait.</p>
 */
public final class FishingDebug {
    private static final Set<UUID> ENABLED = ConcurrentHashMap.newKeySet();

    private FishingDebug() {}

    /** Toggles the diagnostics of a player; returns the new state. */
    public static boolean toggle(ServerPlayer player) {
        if (ENABLED.remove(player.getUUID())) {
            return false;
        }
        ENABLED.add(player.getUUID());
        return true;
    }

    public static boolean isOn(ServerPlayer player) {
        return !ENABLED.isEmpty() && ENABLED.contains(player.getUUID());
    }

    /** Sends the message to the player (when debug is on) and always to the server log at INFO. */
    public static void info(ServerPlayer player, String message) {
        Simpleskills.LOGGER.info("[fishing] {}: {}", player.getName().getString(), message);
        if (isOn(player)) {
            player.sendSystemMessage(Component.literal("§8[fishing] §7" + message));
        }
    }

    /** Only when debug is on (high frequency messages). */
    public static void trace(ServerPlayer player, String message) {
        if (isOn(player)) {
            player.sendSystemMessage(Component.literal("§8[fishing] §7" + message));
        }
    }

    public static void clear() {
        ENABLED.clear();
    }
}
