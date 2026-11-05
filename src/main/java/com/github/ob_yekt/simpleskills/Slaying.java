
package com.github.ob_yekt.simpleskills;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slaying skill cache and combat bonus calculations.
 *
 * The actual skill level remains stored in the existing SimpleSkills database.
 * This class only keeps an in-memory copy to avoid database queries during combat.
 */
public final class Slaying {

    private Slaying() {}

    // Bonus per Slaying level
    public static final float DAMAGE_PER_LEVEL = 0.003f;
    public static final float CRITICAL_CHANCE_PER_LEVEL = 0.005f;

    // Player UUID -> Slaying level
    private static final Map<UUID, Integer> SERVER_LEVELS =
            new ConcurrentHashMap<>();

    /**
     * Updates the cached Slaying level.
     */
    public static void setServerLevel(UUID playerUuid, int level) {
        if (playerUuid == null) {
            return;
        }

        int safeLevel = Math.max(0, Math.min(level, 99));
        SERVER_LEVELS.put(playerUuid, safeLevel);
    }

    /**
     * Returns the cached Slaying level.
     */
    public static int getSkillLevelFromCache(UUID playerUuid) {
        if (playerUuid == null) {
            return 0;
        }

        return SERVER_LEVELS.getOrDefault(playerUuid, 0);
    }

    /**
     * Returns the sword damage multiplier.
     * Level 99 = 1.297 (+29.7% damage).
     */
    public static float getDamageMultiplier(UUID playerUuid) {
        int level = getSkillLevelFromCache(playerUuid);
        return 1.0f + (level * DAMAGE_PER_LEVEL);
    }

    /**
     * Returns the custom critical hit chance.
     * Level 99 = 0.495 (49.5% chance).
     */
    public static float getCriticalChance(UUID playerUuid) {
        int level = getSkillLevelFromCache(playerUuid);
        return Math.min(level * CRITICAL_CHANCE_PER_LEVEL, 0.495f);
    }

    /**
     * Removes the cached level when the player disconnects.
     */
    public static void clearServerLevels(UUID playerUuid) {
        if (playerUuid != null) {
            SERVER_LEVELS.remove(playerUuid);
        }
    }
}
