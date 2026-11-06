
package com.github.ob_yekt.simpleskills.ui;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.GatheringSpeed;
import com.github.ob_yekt.simpleskills.Slaying;
import com.github.ob_yekt.simpleskills.Ranged;
import com.github.ob_yekt.simpleskills.VeinMining;
import com.github.ob_yekt.simpleskills.managers.ConfigManager;
import com.github.ob_yekt.simpleskills.managers.DatabaseManager;
import com.github.ob_yekt.simpleskills.managers.XPManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;

public class SkillTabMenu {

    private static final Map<UUID, Boolean> playerTabMenuVisibility = new HashMap<>();
    private static final NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.US);

    private static final int COLUMN_WIDTH_PIXELS = 240;
    private static final int COLUMN_GAP_SPACES = 3;

    // NOVA CONSTANTE: Controla o distanciamento da margem esquerda para centralizar o bloco
    private static final int LEFT_MARGIN_SPACES = 14; 

    private static final int XP_BAR_LENGTH = 22;

    private static final int SKILL_BLOCK_LINES = 7;
    private static final int MAX_BUFF_LINES = 4;

    public static void toggleTabMenuVisibility(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.literal("§6[simpleskills]§f This command can only be used by players."));
            return;
        }

        UUID playerUuid = player.getUUID();
        boolean isCurrentlyVisible = getVisibility(playerUuid);
        boolean newState = !isCurrentlyVisible;

        setVisibility(playerUuid, newState);

        if (newState) {
            updateTabMenu(player);
            player.sendSystemMessage(Component.literal("§6[simpleskills]§f Skill HUD enabled."), false);
        } else {
            sendEmptyTabList(player);
            player.sendSystemMessage(Component.literal("§6[simpleskills]§f Skill HUD disabled."), false);
        }
    }

    public static void updateTabMenu(ServerPlayer player) {
        UUID playerUuid = player.getUUID();

        if (!getVisibility(playerUuid)) {
            sendEmptyTabList(player);
            return;
        }

        DatabaseManager db = DatabaseManager.getInstance();
        String playerUuidStr = playerUuid.toString();

        if (db.getAllSkills(playerUuidStr).isEmpty()) {
            db.initializePlayer(playerUuidStr);
            Simpleskills.LOGGER.debug("Initialized skills for player {} due to tab menu update.", player.getName().getString());
        }

        try {
            Map<String, DatabaseManager.SkillData> skills = db.getAllSkills(playerUuidStr);
            int totalLevels = skills.values().stream().mapToInt(DatabaseManager.SkillData::level).sum();

            StringBuilder skillInfo = new StringBuilder();
            appendHeader(skillInfo);

            boolean isIronman = db.isPlayerInIronmanMode(playerUuidStr);
            int prestige = db.getPrestige(playerUuidStr);

            if (isIronman) {
                skillInfo.append("§cIronman Mode: §aENABLED\n");
            }
            if (prestige > 0) {
                skillInfo.append(String.format(Locale.US, "§6Prestige: §e★%d\n", prestige));
            }
            if (isIronman || prestige > 0) {
                skillInfo.append("\n");
            }

            List<List<String>> leftColumn = new ArrayList<>();
            List<List<String>> rightColumn = new ArrayList<>();
            int skillIndex = 0;

            for (Skills skillEnum : Skills.values()) {
                String skillName = skillEnum.getId();
                DatabaseManager.SkillData skill = skills.getOrDefault(skillName, new DatabaseManager.SkillData(0, 1));

                List<String> skillBlock = createSkillBlock(skillEnum, skill);

                if (skillIndex % 2 == 0) {
                    leftColumn.add(skillBlock);
                } else {
                    rightColumn.add(skillBlock);
                }
                skillIndex++;
            }

            appendTwoColumnLayout(skillInfo, leftColumn, rightColumn);

            skillInfo.append("\n")
                     .append("§8§m----------------------------------------------------------§r\n")
                     .append(String.format(Locale.US, "§b§lTOTAL LEVEL: §a%d§r\n", totalLevels))
                     .append("§6§l⚔ SIMPLE SKILLS ⚔§r");

            player.connection.send(new ClientboundTabListPacket(
                    Component.nullToEmpty(skillInfo.toString()),
                    Component.nullToEmpty("")
            ));

        } catch (Exception e) {
            Simpleskills.LOGGER.error("Failed to update skill menu for player {}: {}", player.getName().getString(), e.getMessage());
            player.sendSystemMessage(Component.literal("§6[simpleskills]§f Error: Failed to load skill data."), false);
        }
    }

    public static void clearPlayerVisibility(UUID playerUuid) {
        playerTabMenuVisibility.remove(playerUuid);
    }

    private static boolean getVisibility(UUID playerUuid) {
        return playerTabMenuVisibility.getOrDefault(
                playerUuid,
                DatabaseManager.getInstance().isTabMenuVisible(playerUuid.toString())
        );
    }

    private static void setVisibility(UUID playerUuid, boolean visible) {
        playerTabMenuVisibility.put(playerUuid, visible);
        DatabaseManager.getInstance().setTabMenuVisibility(playerUuid.toString(), visible);
    }

    private static void sendEmptyTabList(ServerPlayer player) {
        player.connection.send(new ClientboundTabListPacket(
                Component.nullToEmpty(""),
                Component.nullToEmpty("")
        ));
    }

    private static void appendHeader(StringBuilder skillInfo) {
        skillInfo.append("§6§m----------------------------------------------------------§r\n")
                 .append("§6§l⚔ SKILLS OVERVIEW ⚔§r\n")
                 .append("§7YOUR CHARACTER PROGRESSION§r\n")
                 .append("§6§m----------------------------------------------------------§r\n\n");
    }

    private static List<String> createSkillBlock(Skills skillEnum, DatabaseManager.SkillData skill) {
        List<String> lines = new ArrayList<>();
        boolean isMaxLevel = skill.level() >= XPManager.getMaxLevel();
        String levelColor = isMaxLevel ? "§f" : "§a";

        lines.add(String.format(Locale.US, "§6⭐ §e%s %sLv. %02d", skillEnum.getDisplayName(), levelColor, skill.level()));
        lines.add(createProgressBar(skill));
        lines.add(isMaxLevel ? "§fMAX LEVEL" : createXpLine(skill));

        List<String> buffLines = getSkillBuffLines(skillEnum, skill.level());
        for (int i = 0; i < MAX_BUFF_LINES; i++) {
            lines.add(i < buffLines.size() ? buffLines.get(i) : "");
        }

        while (lines.size() < SKILL_BLOCK_LINES) {
            lines.add("");
        }

        return lines;
    }

    private static void appendTwoColumnLayout(StringBuilder output, List<List<String>> leftColumn, List<List<String>> rightColumn) {
        int blockCount = Math.max(leftColumn.size(), rightColumn.size());

        for (int blockIndex = 0; blockIndex < blockCount; blockIndex++) {
            List<String> leftBlock = blockIndex < leftColumn.size() ? leftColumn.get(blockIndex) : createEmptySkillBlock();
            List<String> rightBlock = blockIndex < rightColumn.size() ? rightColumn.get(blockIndex) : createEmptySkillBlock();

            for (int lineIndex = 0; lineIndex < SKILL_BLOCK_LINES; lineIndex++) {
                String left = padToColumnWidth(leftBlock.get(lineIndex));
                String right = padToColumnWidth(rightBlock.get(lineIndex));

                // AQUI: Adicionado o "LEFT_MARGIN_SPACES" antes da coluna da esquerda
                output.append(" ".repeat(LEFT_MARGIN_SPACES))
                      .append(left)
                      .append(" ".repeat(COLUMN_GAP_SPACES))
                      .append(right)
                      .append("\n"); 
            }
        }
    }

    private static List<String> createEmptySkillBlock() {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < SKILL_BLOCK_LINES; i++) {
            lines.add("");
        }
        return lines;
    }

    private static String padToColumnWidth(String text) {
        int currentWidth = calculateTextWidth(text);
        int remainingWidth = COLUMN_WIDTH_PIXELS - currentWidth;

        if (remainingWidth <= 0) return text + "§r";

        int spaces = Math.round(remainingWidth / 4.0f);
        return text + "§r" + " ".repeat(spaces);
    }

    private static int calculateTextWidth(String text) {
        int width = 0;
        boolean bold = false;

        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);

            if (character == '§' && i + 1 < text.length()) {
                char formattingCode = Character.toLowerCase(text.charAt(++i));
                if (formattingCode == 'l') {
                    bold = true;
                } else if ("0123456789abcdefkr".indexOf(formattingCode) != -1) {
                    bold = false;
                }
                continue;
            }

            int characterWidth = getCharacterWidth(character);
            if (bold && character != ' ') {
                characterWidth++;
            }
            width += characterWidth;
        }

        return width;
    }

    private static int getCharacterWidth(char character) {
        return switch (character) {
            case ' ' -> 4;
            case 'i', '!', '.', ',', ':', ';', '|', '\'' -> 2;
            case 'l', '`' -> 3;
            case 'I', 't', '[', ']', '"' -> 4;
            case 'f', 'k', '(', ')', '{', '}', '<', '>' -> 5;
            case '@' -> 7;
            case '█', '▒', '⭐', '↳' -> 9;
            default -> 6;
        };
    }

    private static String createXpLine(DatabaseManager.SkillData skill) {
        int xpForCurrentLevel = XPManager.getExperienceForLevel(skill.level());
        int xpToNextLevel = Math.max(1, XPManager.getExperienceForLevel(skill.level() + 1) - xpForCurrentLevel);
        int progressToNextLevel = Math.max(0, Math.min(skill.xp() - xpForCurrentLevel, xpToNextLevel));

        return String.format(Locale.US, "§7XP §f%s§8 / §f%s", 
                numberFormat.format(progressToNextLevel), 
                numberFormat.format(xpToNextLevel));
    }

    private static List<String> getSkillBuffLines(Skills skill, int level) {
        StringBuilder buffInfo = new StringBuilder();
        appendSkillBuffInfo(buffInfo, skill, level);

        List<String> lines = new ArrayList<>();
        String result = buffInfo.toString();

        if (!result.isEmpty()) {
            for (String line : result.split("\n")) {
                if (!line.isEmpty()) lines.add(line);
            }
        }
        return lines;
    }

    private static void appendSkillBuffInfo(StringBuilder skillInfo, Skills skill, int level) {
        int safeLevel = Math.max(0, Math.min(level, XPManager.getMaxLevel()));

        switch (skill) {
            case AGILITY -> {
                float movementBonus = safeLevel * 0.3f;

                if (movementBonus > 0.0f) {
                    skillInfo.append(String.format(Locale.US,
                            "   §8↳ §bMovement Speed: §a+%.1f%%\n", movementBonus));
                }
            }
            case MINING, WOODCUTTING, EXCAVATING -> {
                float speedBonus = safeLevel * GatheringSpeed.BONUS_PER_LEVEL * 100.0f;
                float durabilityChance = Math.min(safeLevel * GatheringSpeed.BONUS_PER_LEVEL, 0.99f) * 100.0f;

                if (speedBonus > 0.0f) {
                    skillInfo.append(String.format(Locale.US,
                            "   §8↳ §bBlock Breaking Speed: §a+%.0f%%\n", speedBonus));
                }

                if (durabilityChance > 0.0f) {
                    skillInfo.append(String.format(Locale.US,
                            "   §8↳ §bDurability Protection: §a%.0f%%\n", durabilityChance));
                }

                if (skill == Skills.MINING || skill == Skills.WOODCUTTING) {
                    float duplicationChance = Math.min((safeLevel / 10) * 0.05f, 0.45f) * 100.0f;

                    if (duplicationChance > 0.0f) {
                        skillInfo.append(String.format(Locale.US,
                                "   §8↳ §bLoot Duplication: §a%.0f%%\n", duplicationChance));
                    }
                }

                if (skill == Skills.MINING) {
                    int additionalBlocks = VeinMining.getAdditionalBlockLimit(safeLevel);

                    if (additionalBlocks > 0) {
                        skillInfo.append(String.format(Locale.US,
                                "   §8↳ §bShift Vein Miner: §a x%d\n",
                                additionalBlocks));
                    }
                }
            }
            case FISHING -> {
                String rank = safeLevel >= 99 ? "Grandmaster" : safeLevel >= 75 ? "Expert" : safeLevel >= 50 ? "Artisan" : safeLevel >= 25 ? "Journeyman" : "Novice";
                String rankColor = safeLevel >= 99 ? "§6" : safeLevel >= 75 ? "§5" : safeLevel >= 50 ? "§9" : safeLevel >= 25 ? "§a" : "§7";

                skillInfo.append("   §8↳ §bRank: ").append(rankColor).append(rank).append("\n");

                if (ConfigManager.isFishingSpeedBonusEnabled()) {
                    float waitReduction = (1.0f - Math.max(0.20f, 1.0f - (safeLevel * 0.005f))) * 100.0f;

                    if (waitReduction > 0.0f) {
                        skillInfo.append(String.format(Locale.US,
                                "   §8↳ §bBite Wait Reduction: §a%.1f%%\n", waitReduction));
                    }
                } else {
                    skillInfo.append("   §8↳ §bBite Speed: §cDisabled\n");
                }
            }
            case DEFENSE -> {
                float damageReduction = safeLevel * 0.3f;
                float armorProtection = safeLevel * 1.0f;
                float knockbackResistance = safeLevel * 0.5f;

                if (damageReduction > 0.0f) {
                    skillInfo.append(String.format(Locale.US,
                            "   §8↳ §cDamage Reduction: §a%.1f%%\n", damageReduction));
                }

                if (armorProtection > 0.0f) {
                    skillInfo.append(String.format(Locale.US,
                            "   §8↳ §cArmor Durability Protection: §a%.0f%%\n", armorProtection));
                }

                if (knockbackResistance > 0.0f) {
                    skillInfo.append(String.format(Locale.US,
                            "   §8↳ §cKnockback Resistance: §a%.1f%%\n", knockbackResistance));
                }
            }
            case SLAYING -> {
                float damageBonus = safeLevel * Slaying.DAMAGE_PER_LEVEL * 100.0f;
                float criticalChance = Math.min(safeLevel * Slaying.CRITICAL_CHANCE_PER_LEVEL, 0.495f) * 100.0f;

                if (damageBonus > 0.0f) {
                    skillInfo.append(String.format(Locale.US,
                            "   §8↳ §cSword Damage: §a+%.1f%%\n", damageBonus));
                }

                if (criticalChance > 0.0f) {
                    skillInfo.append(String.format(Locale.US,
                            "   §8↳ §cCritical Chance: §a%.1f%%\n", criticalChance));
                }

                if (safeLevel >= 99) {
                    skillInfo.append("   §8↳ §cLife Steal II: §a60% chance to heal half a heart\n");
                } else if (safeLevel >= 50) {
                    skillInfo.append("   §8↳ §cLife Steal I: §a30% chance to heal half a heart\n");
                }
            }
            case RANGED -> {
                float damageBonus = safeLevel * Ranged.DAMAGE_PER_LEVEL * 100.0f;

                float doubleShotChance = Math.min(
                        safeLevel / 50.0f,
                        1.0f
                ) * 100.0f;

                float tripleShotChance = safeLevel <= 50
                        ? 0.0f
                        : Math.min((safeLevel - 50) / 49.0f, 1.0f) * 100.0f;

                if (damageBonus > 0.0f) {
                    skillInfo.append(String.format(Locale.US,
                            "   §8↳ §cProjectile Damage: §a+%.1f%%\n", damageBonus));
                }

                if (safeLevel > 0) {
                    skillInfo.append(String.format(Locale.US,
                            "   §8↳ §cDouble Shot Chance: §a%.1f%%\n", doubleShotChance));

                    skillInfo.append(String.format(Locale.US,
                            "   §8↳ §cTriple Shot Chance: §a%.1f%%\n", tripleShotChance));
                }
            }
            case SMITHING -> {
                float repairEfficiency = safeLevel >= 99 ? 1.00f : safeLevel >= 75 ? 0.55f : safeLevel >= 50 ? 0.45f : safeLevel >= 25 ? 0.35f : 0.25f;
                float netheriteBonus = (ConfigManager.getSmithingMultiplier(safeLevel) - 1.0f) * 100.0f;

                skillInfo.append(String.format(Locale.US,
                        "   §8↳ §6Repair Efficiency: §a%.0f%%\n", repairEfficiency * 100.0f))
                         .append("   §8↳ §6Anvil Durability: §aNo degradation\n");

                if (netheriteBonus > 0.0f) {
                    skillInfo.append(String.format(Locale.US,
                            "   §8↳ §6Netherite Durability: §a+%.1f%%\n", netheriteBonus));
                }
            }
            case COOKING -> {
                float foodBonus = (ConfigManager.getCookingMultiplier(safeLevel) - 1.0f) * 100.0f;

                if (foodBonus > 0.0f) {
                    skillInfo.append(String.format(Locale.US,
                            "   §8↳ §6Food Nutrition & Saturation: §a%+.1f%%\n", foodBonus));
                }
            }
            case CRAFTING -> {
                float durabilityBonus = (ConfigManager.getCraftingDurabilityMultiplier(safeLevel) - 1.0f) * 100.0f;
                float recoveryChance = ConfigManager.getCraftingRecoveryChance(safeLevel) * 100.0f;

                if (durabilityBonus > 0.0f) {
                    skillInfo.append(String.format(Locale.US,
                            "   §8↳ §6Crafted Item Durability: §a+%.1f%%\n", durabilityBonus));
                }

                if (recoveryChance > 0.0f) {
                    skillInfo.append(String.format(Locale.US,
                            "   §8↳ §6Material Recovery: §a%.1f%%\n", recoveryChance));
                }
            }
            default -> {}
        }
    }

    private static String createProgressBar(DatabaseManager.SkillData skill) {
        if (skill.level() >= XPManager.getMaxLevel()) {
            return "§f" + "█".repeat(XP_BAR_LENGTH);
        }

        int xpForCurrentLevel = XPManager.getExperienceForLevel(skill.level());
        int xpToNextLevel = Math.max(1, XPManager.getExperienceForLevel(skill.level() + 1) - xpForCurrentLevel);
        int progressToNextLevel = Math.max(0, Math.min(skill.xp() - xpForCurrentLevel, xpToNextLevel));

        int filled = Math.max(0, Math.min((int) ((double) progressToNextLevel / xpToNextLevel * XP_BAR_LENGTH), XP_BAR_LENGTH));
        int empty = XP_BAR_LENGTH - filled;

        return "§a" + "█".repeat(filled) + "§8" + "▒".repeat(empty);
    }
}
