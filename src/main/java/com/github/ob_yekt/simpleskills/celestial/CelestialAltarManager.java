package com.github.ob_yekt.simpleskills.celestial;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Essence acquisition hooks for combat, Excavating and Fishing. */
public final class CelestialAltarManager {
    private static final long RECENT_PLAYER_DAMAGE_TICKS = 200L;
    private static final Map<UUID, RecentPlayerHit> RECENT_PLAYER_HITS = new ConcurrentHashMap<>();

    private record RecentPlayerHit(UUID playerId, long tick) {}

    private CelestialAltarManager() {}

    public static void registerPayloads() {
        PayloadTypeRegistry.serverboundPlay().register(CelestialAltarActionPayload.TYPE,
                CelestialAltarActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CelestialAltarVisualPayload.TYPE,
                CelestialAltarVisualPayload.CODEC);
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(CelestialAltarActionPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    if (player == null || payload == null || payload.targetId() == null
                            || payload.targetId().length() > 128) return;
                    if (player.containerMenu instanceof CelestialAltarMenu menu) {
                        menu.handleUiAction(player, payload.action(), payload.targetId(), payload.expectedRevision());
                    }
                }));
        ServerLivingEntityEvents.AFTER_DAMAGE.register(CelestialAltarManager::recordPlayerDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register(CelestialAltarManager::onLivingDeath);
        ServerTickEvents.END_SERVER_TICK.register(CelestialAltarManager::cleanupRecentHits);
    }

    private static void recordPlayerDamage(LivingEntity entity, DamageSource source,
                                           float originalAmount, float actualAmount, boolean blocked) {
        if (!(entity instanceof Enemy) || actualAmount <= 0.0F
                || !(entity.level() instanceof ServerLevel level)
                || !(source.getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        RECENT_PLAYER_HITS.put(entity.getUUID(), new RecentPlayerHit(attacker.getUUID(), level.getGameTime()));
    }

    private static void onLivingDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof Enemy) || !(entity.level() instanceof ServerLevel level)) return;

        RecentPlayerHit recent = RECENT_PLAYER_HITS.remove(entity.getUUID());
        UUID playerId = source.getEntity() instanceof ServerPlayer directKiller
                ? directKiller.getUUID()
                : recent != null && level.getGameTime() - recent.tick() <= RECENT_PLAYER_DAMAGE_TICKS
                    ? recent.playerId() : null;
        if (playerId == null) return;

        // Only hostile mobs with a direct or recent player damage contribution can roll the 0.1% drop.
        if (level.getRandom().nextFloat() < 0.001F) {
            ItemEntity essence = new ItemEntity(level, entity.getX(), entity.getY() + 0.25, entity.getZ(),
                    new ItemStack(CelestialAltarContent.TRANSCENDENCY_ESSENCE));
            essence.setDefaultPickUpDelay();
            level.addFreshEntity(essence);
        }
    }

    private static void cleanupRecentHits(MinecraftServer server) {
        for (Map.Entry<UUID, RecentPlayerHit> entry : RECENT_PLAYER_HITS.entrySet()) {
            boolean stale = server.overworld().getGameTime() - entry.getValue().tick() > RECENT_PLAYER_DAMAGE_TICKS;
            if (stale) RECENT_PLAYER_HITS.remove(entry.getKey(), entry.getValue());
        }
    }

    /** Excavating chance: 0.02% at level 1, scaling linearly to 0.20% at level 99. */
    public static void rollExcavatingEssence(ServerLevel level, BlockPos pos, int excavatingLevel) {
        double progress = (Math.max(1, Math.min(99, excavatingLevel)) - 1) / 98.0;
        double chance = 0.0002 + progress * 0.0018;
        if (level.getRandom().nextDouble() < chance) {
            ItemEntity essence = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5,
                    pos.getZ() + 0.5, new ItemStack(CelestialAltarContent.TRANSCENDENCY_ESSENCE));
            essence.setDefaultPickUpDelay();
            level.addFreshEntity(essence);
        }
    }

    /** Fishing chance: starts at 0.15% and scales gently to 0.20% at Fishing level 99. */
    public static void rollFishingEssence(ServerLevel level, int fishingLevel, java.util.List<ItemStack> bonusLoot) {
        if (bonusLoot == null) return;
        double progress = (Math.max(1, Math.min(99, fishingLevel)) - 1) / 98.0;
        double chance = 0.0015 + progress * 0.0005;
        if (level.getRandom().nextDouble() < chance) {
            bonusLoot.add(new ItemStack(CelestialAltarContent.TRANSCENDENCY_ESSENCE));
        }
    }
}
