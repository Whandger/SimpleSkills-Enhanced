package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.managers.DatabaseManager;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Detecta a primeira entrada no abismo (Y entre -256 e -80, o mesmo recorte do AbyssWorldgen).
 * Na primeira vez, grava a marca no banco e avisa o cliente, que toca a trilha. As entradas
 * seguintes ficam a cargo do cliente (AbyssThemeMusic), que toca de vez em quando.
 */
public final class AbyssEntryTracker {
    /** Intervalo entre verificações (20 ticks = 1 s): barato o bastante para não pesar no servidor. */
    private static final int CHECK_EVERY_TICKS = 20;

    /** Jogadores já conferidos nesta sessão de servidor, para não consultar o banco a cada verificação. */
    private static final Set<UUID> CHECKED = new HashSet<>();
    private static int tickCounter = 0;

    private AbyssEntryTracker() {}

    public static void registerPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(AbyssEntryPayload.TYPE, AbyssEntryPayload.CODEC);
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(AbyssEntryTracker::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CHECKED.clear());
    }

    private static void tick(MinecraftServer server) {
        if (++tickCounter % CHECK_EVERY_TICKS != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            if (CHECKED.contains(id)) continue;
            if (!AbyssWorldgen.isAbyssY(player.blockPosition().getY())) continue;
            CHECKED.add(id);

            String key = id.toString();
            DatabaseManager db = DatabaseManager.getInstance();
            if (db.hasEnteredAbyss(key)) continue;

            db.markAbyssEntered(key);
            if (ServerPlayNetworking.canSend(player, AbyssEntryPayload.TYPE)) {
                ServerPlayNetworking.send(player, new AbyssEntryPayload(true));
            }
        }
    }
}
