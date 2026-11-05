
package com.github.ob_yekt.simpleskills.managers;

import com.github.ob_yekt.simpleskills.Defense;
import com.github.ob_yekt.simpleskills.GatheringSpeed;
import com.github.ob_yekt.simpleskills.Ranged;
import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.Slaying;
import com.github.ob_yekt.simpleskills.managers.DatabaseManager.SkillData;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Map;

/**
 * Manages player attribute modifications based on skills and Ironman mode.
 */
public class AttributeManager {

    private static final Identifier AGILITY_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("simpleskills", "agility_bonus");

    private static final Identifier IRONMAN_HEALTH_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("simpleskills", "ironman_health_reduction");

    private static final Identifier DEFENSE_KNOCKBACK_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("simpleskills", "defense_knockback_resistance");

    private static final int MAX_SKILL_LEVEL = 99;

    private AttributeManager() {
    }

    public static void registerPlayerEvents() {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (newPlayer == null) {
                Simpleskills.LOGGER.warn("Null newPlayer in respawn event.");
                return;
            }

            refreshAllAttributes(newPlayer);

            Simpleskills.LOGGER.debug(
                    "Reapplied attributes for player {} on respawn",
                    newPlayer.getName().getString()
            );
        });
    }

    public static void refreshAllAttributes(ServerPlayer player) {
        if (player == null) {
            Simpleskills.LOGGER.warn("Null player in refreshAllAttributes.");
            return;
        }

        String playerUuid = player.getStringUUID();

        clearSkillAttributes(player);
        clearIronmanAttributes(player);
        refreshSkillAttributes(player);

        if (DatabaseManager.getInstance().isPlayerInIronmanMode(playerUuid)) {
            applyIronmanAttributes(player);
        }

        Simpleskills.LOGGER.debug(
                "Refreshed all attributes for player: {}",
                player.getName().getString()
        );
    }

    public static void clearSkillAttributes(ServerPlayer player) {
        if (player == null) {
            return;
        }

        AttributeInstance moveSpeed =
                player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (moveSpeed != null) {
            moveSpeed.removeModifier(AGILITY_MODIFIER_ID);
        }

        AttributeInstance knockbackResistance =
                player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);

        if (knockbackResistance != null) {
            knockbackResistance.removeModifier(DEFENSE_KNOCKBACK_MODIFIER_ID);
        }
    }

    public static void applyIronmanAttributes(ServerPlayer player) {
        if (player == null) {
            return;
        }

        AttributeInstance healthAttribute =
                player.getAttribute(Attributes.MAX_HEALTH);

        if (healthAttribute == null) {
            return;
        }

        healthAttribute.removeModifier(IRONMAN_HEALTH_MODIFIER_ID);

        double healthReduction =
                ConfigManager.getFeatureConfig().get("ironman_health_reduction") != null
                        ? ConfigManager.getFeatureConfig()
                                .get("ironman_health_reduction")
                                .getAsDouble()
                        : -10.0;

        healthAttribute.addPermanentModifier(
                new AttributeModifier(
                        IRONMAN_HEALTH_MODIFIER_ID,
                        healthReduction,
                        AttributeModifier.Operation.ADD_VALUE
                )
        );
    }

    public static void clearIronmanAttributes(ServerPlayer player) {
        if (player == null) {
            return;
        }

        AttributeInstance healthAttribute =
                player.getAttribute(Attributes.MAX_HEALTH);

        if (healthAttribute != null) {
            healthAttribute.removeModifier(IRONMAN_HEALTH_MODIFIER_ID);
        }
    }

    public static void refreshSkillAttributes(ServerPlayer player) {
        if (player == null) {
            return;
        }

        String playerUuid = player.getStringUUID();

        Map<String, SkillData> skills =
                DatabaseManager.getInstance().getAllSkills(playerUuid);

        for (Skills skill : Skills.values()) {
            updatePlayerAttributes(
                    player,
                    skill,
                    skills.getOrDefault(
                            skill.getId(),
                            new SkillData(0, 1)
                    )
            );
        }
    }

    public static void updatePlayerAttributes(
            ServerPlayer player,
            Skills skill,
            SkillData skillData
    ) {
        if (player == null || skill == null || skillData == null) {
            return;
        }

        int skillLevel = Math.max(
                0,
                Math.min(skillData.level(), MAX_SKILL_LEVEL)
        );

        switch (skill) {

            case AGILITY -> {
                AttributeInstance moveSpeed =
                        player.getAttribute(Attributes.MOVEMENT_SPEED);

                if (moveSpeed == null) {
                    return;
                }

                moveSpeed.removeModifier(AGILITY_MODIFIER_ID);

                if (skillLevel > 0) {
                    double bonusSpeed = skillLevel * 0.003;

                    moveSpeed.addPermanentModifier(
                            new AttributeModifier(
                                    AGILITY_MODIFIER_ID,
                                    bonusSpeed,
                                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                            )
                    );
                }
            }

            case MINING, WOODCUTTING, EXCAVATING -> {
                // Update Gathering Speed cache.
                GatheringSpeed.setServerLevel(
                        player,
                        skill,
                        skillLevel
                );
            }

            case SLAYING -> {
                // Update Slaying cache for sword combat bonuses.
                Slaying.setServerLevel(
                        player.getUUID(),
                        skillLevel
                );
            }

            case RANGED -> {
                // Update Ranged cache for projectile bonuses.
                Ranged.setServerLevel(
                        player.getUUID(),
                        skillLevel
                );
            }

            case DEFENSE -> {
                // Update Defense cache for passive bonuses.
                Defense.setServerLevel(
                        player.getUUID(),
                        skillLevel
                );

                AttributeInstance knockbackResistance =
                        player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);

                if (knockbackResistance == null) {
                    return;
                }

                knockbackResistance.removeModifier(
                        DEFENSE_KNOCKBACK_MODIFIER_ID
                );

                double bonus = Defense.getKnockbackResistance(
                        player.getUUID()
                );

                if (bonus > 0.0) {
                    knockbackResistance.addPermanentModifier(
                            new AttributeModifier(
                                    DEFENSE_KNOCKBACK_MODIFIER_ID,
                                    bonus,
                                    AttributeModifier.Operation.ADD_VALUE
                            )
                    );
                }
            }

            case FARMING, FISHING, SMITHING,
                 ALCHEMY, COOKING, CRAFTING,
                 ENCHANTING, PRAYER -> {
                // No direct attribute modifiers for these skills.
            }
        }
    }
}
