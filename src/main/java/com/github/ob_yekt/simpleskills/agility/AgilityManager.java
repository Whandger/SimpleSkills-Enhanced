package com.github.ob_yekt.simpleskills.agility;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.managers.ConfigManager;
import com.github.ob_yekt.simpleskills.managers.XPManager;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative action abilities unlocked through Agility. */
public final class AgilityManager {
    private static final Map<UUID, Long> DASH_READY_AT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> DASH_IFRAMES_UNTIL = new ConcurrentHashMap<>();
    private static final Set<UUID> DOUBLE_JUMP_USED = ConcurrentHashMap.newKeySet();

    private AgilityManager() {}

    public static void registerPayloads() {
        PayloadTypeRegistry.serverboundPlay().register(AgilityActionPayload.TYPE, AgilityActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AgilityLevelPayload.TYPE, AgilityLevelPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AgilityDashCooldownPayload.TYPE, AgilityDashCooldownPayload.CODEC);
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(AgilityActionPayload.TYPE, (payload, context) ->
                context.server().execute(() -> handleAction(context.player(), payload.action())));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.onGround()) DOUBLE_JUMP_USED.remove(player.getUUID());
            }
            DASH_IFRAMES_UNTIL.entrySet().removeIf(entry -> {
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                return player == null || entry.getValue() <= player.level().getGameTime();
            });
            DASH_READY_AT.entrySet().removeIf(entry -> server.getPlayerList().getPlayer(entry.getKey()) == null);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.player.getUUID();
            DASH_READY_AT.remove(id);
            DASH_IFRAMES_UNTIL.remove(id);
            DOUBLE_JUMP_USED.remove(id);
        });
    }

    public static void syncLevel(ServerPlayer player, int level) {
        if (player == null || player.connection == null || !ServerPlayNetworking.canSend(player, AgilityLevelPayload.TYPE)) return;
        ServerPlayNetworking.send(player, new AgilityLevelPayload(
                Math.max(0, Math.min(99, level)),
                intValue("dash_unlocked_at", 15),
                intValue("double_jump_unlocked_at", 10)));
    }

    public static void notifyUnlocks(ServerPlayer player, int oldLevel, int newLevel) {
        notifyUnlock(player, oldLevel, newLevel, "double_jump_unlocked_at", 10,
                "message.simpleskills.agility.double_jump_unlocked");
        notifyUnlock(player, oldLevel, newLevel, "dash_unlocked_at", 15, "message.simpleskills.agility.dash_unlocked");
    }

    private static void notifyUnlock(ServerPlayer player, int oldLevel, int newLevel, String configKey,
            int fallback, String messageKey, Object... args) {
        int required = intValue(configKey, fallback);
        if (oldLevel < required && newLevel >= required) {
            Object[] messageArgs = new Object[args.length + 1];
            messageArgs[0] = required;
            System.arraycopy(args, 0, messageArgs, 1, args.length);
            player.sendSystemMessage(Component.translatable(messageKey, messageArgs));
        }
    }

    private static void handleAction(ServerPlayer player, int action) {
        if (player == null || !player.isAlive() || player.isSpectator()) return;
        switch (action) {
            case AgilityActionPayload.DASH -> dash(player);
            case AgilityActionPayload.DOUBLE_JUMP -> doubleJump(player);
            default -> { }
        }
    }

    private static void dash(ServerPlayer player) {
        int level = level(player);
        if (level < intValue("dash_unlocked_at", 15) || player.getFoodData().getFoodLevel() <= 0) return;
        long now = player.level().getGameTime();
        if (DASH_READY_AT.getOrDefault(player.getUUID(), 0L) > now) return;

        Vec3 direction = player.getLastClientMoveIntent();
        direction = new Vec3(direction.x, 0.0, direction.z);
        if (direction.lengthSqr() < 1.0E-4) {
            Vec3 look = player.getLookAngle();
            direction = new Vec3(look.x, 0.0, look.z);
        }
        if (direction.lengthSqr() < 1.0E-4) direction = new Vec3(0.0, 0.0, 1.0);
        direction = direction.normalize();

        double progress = Math.max(0.0, Math.min(1.0, (level - 15) / 84.0));
        double strength = 0.35 + 0.65 * progress;
        double impulse = doubleValue("dash_impulse", 1.2) * (1.0 + 99 * 0.005) * strength;
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x + direction.x * impulse,
                Math.max(velocity.y, doubleValue("dash_vertical_impulse", 0.35) * strength),
                velocity.z + direction.z * impulse);
        player.needsSync = true;
        player.syncVelocity = true;
        player.causeFoodExhaustion((float) doubleValue("dash_exhaustion", 1.0));

        int cooldown = Math.max(1, (int) Math.round(intValue("dash_cooldown_ticks_base", 100)
                - level * doubleValue("dash_cooldown_reduction_per_level", 0.5)));
        DASH_READY_AT.put(player.getUUID(), now + cooldown);
        if (ServerPlayNetworking.canSend(player, AgilityDashCooldownPayload.TYPE)) {
            ServerPlayNetworking.send(player, new AgilityDashCooldownPayload(cooldown));
        }
        int invulnerability = Math.max(0, Math.min(20, intValue("dash_iframe_ticks", 4)));
        if (invulnerability > 0) DASH_IFRAMES_UNTIL.put(player.getUUID(), now + invulnerability);

        ServerLevel world = (ServerLevel) player.level();
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.75F, 1.25F);
        Vec3 behind = player.position().subtract(direction.scale(0.35)).add(0.0, 0.15, 0.0);
        world.sendParticles(ParticleTypes.CLOUD, behind.x, behind.y, behind.z, 9, 0.18, 0.12, 0.18, 0.04);
        world.sendParticles(ParticleTypes.SWEEP_ATTACK, behind.x, behind.y, behind.z, 3, 0.2, 0.1, 0.2, 0.0);
    }

    private static void doubleJump(ServerPlayer player) {
        int agilityLevel = level(player);
        int unlockLevel = intValue("double_jump_unlocked_at", 10);
        if (agilityLevel < unlockLevel
                || player.onGround() || player.isFallFlying() || player.isPassenger()
                || player.getFoodData().getFoodLevel() <= 0
                || DOUBLE_JUMP_USED.contains(player.getUUID())) return;

        DOUBLE_JUMP_USED.add(player.getUUID());
        Vec3 velocity = player.getDeltaMovement();
        Vec3 direction = player.getLastClientMoveIntent();
        direction = new Vec3(direction.x, 0.0, direction.z);
        if (direction.lengthSqr() < 1.0E-4) {
            Vec3 look = player.getLookAngle();
            direction = new Vec3(look.x, 0.0, look.z);
        }
        if (direction.lengthSqr() < 1.0E-4) direction = new Vec3(0.0, 0.0, 1.0);
        direction = direction.normalize();

        double strength = Math.max(0.0, Math.min(1.0,
                (agilityLevel - unlockLevel) / (double) Math.max(1, 40 - unlockLevel)));
        double dashImpulse = doubleValue("dash_impulse", 1.2) * (1.0 + agilityLevel * 0.005);
        double forwardImpulse = dashImpulse / 3.0 * (0.5 + 0.5 * strength);
        double dashVertical = doubleValue("dash_vertical_impulse", 0.35);
        double targetVertical = 0.45 + 0.45 * strength;
        double vertical = Math.max(velocity.y,
                Math.max(doubleValue("double_jump_vertical_velocity", 0.9) * (0.5 + 0.5 * strength),
                        Math.max(dashVertical + 0.1, targetVertical)));
        player.setDeltaMovement(velocity.x + direction.x * forwardImpulse,
                vertical,
                velocity.z + direction.z * forwardImpulse);
        player.fallDistance = 0.0F;
        player.needsSync = true;
        player.syncVelocity = true;
        player.causeFoodExhaustion((float) doubleValue("double_jump_exhaustion", 1.0));

        ServerLevel world = (ServerLevel) player.level();
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.PLAYERS, 0.55F, 2.0F);
        world.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(),
                8, 0.25, 0.08, 0.25, 0.035);
    }

    public static boolean hasDashIFrames(ServerPlayer player) {
        return DASH_IFRAMES_UNTIL.getOrDefault(player.getUUID(), 0L) > player.level().getGameTime();
    }

    private static int level(ServerPlayer player) {
        return XPManager.getSkillLevel(player.getStringUUID(), Skills.AGILITY);
    }

    private static int intValue(String key, int fallback) {
        try {
            JsonObject config = ConfigManager.getAgilityConfig();
            return config.has(key) ? config.get(key).getAsInt() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static double doubleValue(String key, double fallback) {
        try {
            JsonObject config = ConfigManager.getAgilityConfig();
            return config.has(key) ? config.get(key).getAsDouble() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
