package com.github.ob_yekt.simpleskills;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side Defense skill cache and passive bonus calculations.
 */
public final class Defense {

    private Defense() {}

    public static final float DAMAGE_REDUCTION_PER_LEVEL = 0.003f;
    public static final float ARMOR_PROTECTION_PER_LEVEL = 0.01f;
    public static final double KNOCKBACK_RESISTANCE_PER_LEVEL = 0.005;

    private static final int MAX_LEVEL = 99;

    private static final Map<UUID, Integer> SERVER_LEVELS =
            new ConcurrentHashMap<>();

    public static void setServerLevel(UUID playerUuid, int level) {
        if (playerUuid == null) {
            return;
        }

        SERVER_LEVELS.put(
                playerUuid,
                Math.max(0, Math.min(level, MAX_LEVEL))
        );
    }

    public static int getSkillLevelFromCache(UUID playerUuid) {
        if (playerUuid == null) {
            return 0;
        }

        return SERVER_LEVELS.getOrDefault(playerUuid, 0);
    }

    public static float getDamageReduction(UUID playerUuid) {
        return getSkillLevelFromCache(playerUuid)
                * DAMAGE_REDUCTION_PER_LEVEL;
    }

    public static float getArmorProtectionChance(UUID playerUuid) {
        return getSkillLevelFromCache(playerUuid)
                * ARMOR_PROTECTION_PER_LEVEL;
    }

    public static double getKnockbackResistance(UUID playerUuid) {
        return getSkillLevelFromCache(playerUuid)
                * KNOCKBACK_RESISTANCE_PER_LEVEL;
    }

    public static boolean shouldPreventArmorDurabilityLoss(
            UUID playerUuid,
            net.minecraft.util.RandomSource random
    ) {
        float chance = getArmorProtectionChance(playerUuid);

        return chance > 0.0f
                && random.nextFloat() < chance;
    }

    public static void clearServerLevels(UUID playerUuid) {
        if (playerUuid != null) {
            SERVER_LEVELS.remove(playerUuid);
        }
    }
}
