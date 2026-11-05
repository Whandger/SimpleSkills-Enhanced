
package com.github.ob_yekt.simpleskills;

import net.minecraft.world.entity.projectile.Projectile;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class Ranged {

    private Ranged() {}

    public static final float DAMAGE_PER_LEVEL = 0.005f;
    public static final float DUPLICATION_CHANCE_PER_LEVEL = 0.01f;

    private static final Map<UUID, Integer> SERVER_LEVELS =
            new ConcurrentHashMap<>();

    private static final Set<Projectile> DUPLICATE_PROJECTILES =
            Collections.synchronizedSet(
                    Collections.newSetFromMap(new WeakHashMap<>())
            );

    public static void setServerLevel(UUID playerUuid, int level) {
        if (playerUuid == null) {
            return;
        }

        int safeLevel = Math.max(0, Math.min(level, 99));
        SERVER_LEVELS.put(playerUuid, safeLevel);
    }

    public static int getSkillLevelFromCache(UUID playerUuid) {
        if (playerUuid == null) {
            return 0;
        }

        return SERVER_LEVELS.getOrDefault(playerUuid, 0);
    }

    public static float getDamageMultiplier(UUID playerUuid) {
        int level = getSkillLevelFromCache(playerUuid);
        return 1.0f + (level * DAMAGE_PER_LEVEL);
    }

    /**
     * Chance de tiro duplo:
     * Nível 1  = 2%
     * Nível 25 = 50%
     * Nível 50 = 100%
     */
    public static float getDoubleShotChance(UUID playerUuid) {
        int level = getSkillLevelFromCache(playerUuid);
        return Math.min(level / 50.0f, 1.0f);
    }

    /**
     * Chance adicional de tiro triplo:
     * Nível 50 = 0%
     * Nível 75 = aproximadamente 51%
     * Nível 99 = 100%
     */
    public static float getTripleShotChance(UUID playerUuid) {
        int level = getSkillLevelFromCache(playerUuid);

        if (level <= 50) {
            return 0.0f;
        }

        return Math.min((level - 50) / 49.0f, 1.0f);
    }

    /**
     * Mantido para compatibilidade com chamadas existentes.
     */
    public static float getDuplicationChance(UUID playerUuid) {
        return getDoubleShotChance(playerUuid);
    }

    public static void markDuplicateProjectile(Projectile projectile) {
        if (projectile != null) {
            DUPLICATE_PROJECTILES.add(projectile);
        }
    }

    public static boolean isDuplicateProjectile(Projectile projectile) {
        return projectile != null
                && DUPLICATE_PROJECTILES.contains(projectile);
    }

    public static void clearServerLevels(UUID playerUuid) {
        if (playerUuid != null) {
            SERVER_LEVELS.remove(playerUuid);
        }
    }
}
