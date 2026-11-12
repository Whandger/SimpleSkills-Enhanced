package com.github.ob_yekt.simpleskills.ui;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.GatheringSpeed;
import com.github.ob_yekt.simpleskills.Slaying;
import com.github.ob_yekt.simpleskills.Ranged;
import com.github.ob_yekt.simpleskills.VeinMining;
import com.github.ob_yekt.simpleskills.TreeFeller;
import com.github.ob_yekt.simpleskills.events.EventHandlers;
import com.github.ob_yekt.simpleskills.excavating.ExcavatingRank;
import com.github.ob_yekt.simpleskills.agility.AgilityRank;
import com.github.ob_yekt.simpleskills.enchanting.EnchantProgression;
import com.github.ob_yekt.simpleskills.enchanting.EnchantingRank;
import com.github.ob_yekt.simpleskills.farming.FarmingPerks;
import com.github.ob_yekt.simpleskills.fishing.FishingConfig;
import com.github.ob_yekt.simpleskills.fishing.FishingProgression;
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

    private static final int COLUMN_WIDTH_PIXELS = 260;
    private static final int COLUMN_GAP_SPACES = 3;

    // NOVA CONSTANTE: Controla o distanciamento da margem esquerda para centralizar o bloco
    private static final int LEFT_MARGIN_SPACES = 14; 

    private static final int XP_BAR_LENGTH = 22;

    private static final int SKILL_BLOCK_LINES = 9;
    private static final int MAX_BUFF_LINES = 6;

    public static void toggleTabMenuVisibility(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.translatable("message.simpleskills.tab.players_only"));
            return;
        }

        UUID playerUuid = player.getUUID();
        boolean isCurrentlyVisible = getVisibility(playerUuid);
        boolean newState = !isCurrentlyVisible;

        setVisibility(playerUuid, newState);

        if (newState) {
            updateTabMenu(player);
            player.sendSystemMessage(Component.translatable("message.simpleskills.tab.hud_enabled"), false);
        } else {
            sendEmptyTabList(player);
            player.sendSystemMessage(Component.translatable("message.simpleskills.tab.hud_disabled"), false);
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
            appendHeader(skillInfo, player);

            boolean isIronman = db.isPlayerInIronmanMode(playerUuidStr);
            int prestige = db.getPrestige(playerUuidStr);

            if (isIronman) {
                skillInfo.append(String.format("§c%s: §a%s\n", tabText(player, "gui.simpleskills.tab.ironman_mode"),
                        tabText(player, "gui.simpleskills.tab.enabled")));
            }
            if (prestige > 0) {
                skillInfo.append(String.format(Locale.US, "§6%s: §e★%d\n",
                        tabText(player, "gui.simpleskills.tab.prestige"), prestige));
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

                List<String> skillBlock = createSkillBlock(skillEnum, skill, player);

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
                     .append(SkillTabTranslations.format(player, "gui.simpleskills.tab.total_level", totalLevels))
                     .append("§6§l⚔ SIMPLE SKILLS ⚔§r");

            player.connection.send(new ClientboundTabListPacket(
                    Component.nullToEmpty(skillInfo.toString()),
                    Component.nullToEmpty("")
            ));

        } catch (Exception e) {
            Simpleskills.LOGGER.error("Failed to update skill menu for player {}: {}", player.getName().getString(), e.getMessage());
            player.sendSystemMessage(Component.translatable("message.simpleskills.tab.load_error"), false);
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

    private static void appendHeader(StringBuilder skillInfo, ServerPlayer player) {
        skillInfo.append("§6§m----------------------------------------------------------§r\n")
                 .append("§6§l⚔ ").append(tabText(player, "gui.simpleskills.tab.skills_overview")).append(" ⚔§r\n")
                 .append("§7").append(tabText(player, "gui.simpleskills.tab.character_progression")).append("§r\n")
                 .append("§6§m----------------------------------------------------------§r\n\n");
    }

    private static List<String> createSkillBlock(Skills skillEnum, DatabaseManager.SkillData skill, ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        boolean isMaxLevel = skill.level() >= XPManager.getMaxLevel();
        String levelColor = isMaxLevel ? "§f" : "§a";

        String skillTitle = tabText(player, "skill.simpleskills.name." + skillEnum.name().toLowerCase(Locale.ROOT));
        String rankTitle = getSkillRankTitle(skillEnum, skill.level(), player);
        if (rankTitle != null) skillTitle += " " + rankTitle;
        lines.add(SkillTabTranslations.format(player, "gui.simpleskills.tab.skill_title",
                skillTitle,
                levelColor, skill.level()));
        lines.add(createProgressBar(skill));
        lines.add(isMaxLevel ? "§f" + tabText(player, "gui.simpleskills.tab.max_level") : createXpLine(skill, player));

        List<String> buffLines = getSkillBuffLines(skillEnum, skill.level(), player);
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

    private static String createXpLine(DatabaseManager.SkillData skill, ServerPlayer player) {
        int xpForCurrentLevel = XPManager.getExperienceForLevel(skill.level());
        int xpToNextLevel = Math.max(1, XPManager.getExperienceForLevel(skill.level() + 1) - xpForCurrentLevel);
        int progressToNextLevel = Math.max(0, Math.min(skill.xp() - xpForCurrentLevel, xpToNextLevel));

        return SkillTabTranslations.format(player, "gui.simpleskills.tab.xp_progress",
                numberFormat.format(progressToNextLevel), 
                numberFormat.format(xpToNextLevel));
    }

    private static String tabText(ServerPlayer player, String key) {
        return SkillTabTranslations.get(player, key);
    }

    /** Returns a localized, rank-colored title suffix for skills with named progression ranks. */
    private static String getSkillRankTitle(Skills skill, int level, ServerPlayer player) {
        int safeLevel = Math.max(1, Math.min(level, XPManager.getMaxLevel()));
        return switch (skill) {
            case AGILITY -> {
                AgilityRank rank = AgilityRank.forLevel(safeLevel);
                yield rankColorForLevel(rank.getMinimumLevel())
                        + tabText(player, "skill.simpleskills.rank.agility." + rank.name().toLowerCase(Locale.ROOT));
            }
            case EXCAVATING -> {
                ExcavatingRank rank = ExcavatingRank.forLevel(safeLevel);
                yield rankColorForLevel(rank.getMinLevel())
                        + tabText(player, "skill.simpleskills.rank.excavating." + rank.name().toLowerCase(Locale.ROOT));
            }
            case ENCHANTING -> {
                EnchantingRank rank = EnchantProgression.getRank(safeLevel);
                yield rankColorForLevel(rank.getMinimumLevel())
                        + tabText(player, "skill.simpleskills.rank.enchanting." + rank.name().toLowerCase(Locale.ROOT));
            }
            case FISHING -> {
                String rankKey = safeLevel >= 99 ? "grandmaster" : safeLevel >= 75 ? "expert"
                        : safeLevel >= 50 ? "artisan" : safeLevel >= 25 ? "journeyman" : "novice";
                int rankLevel = safeLevel >= 99 ? 99 : safeLevel >= 75 ? 75 : safeLevel >= 50 ? 50
                        : safeLevel >= 25 ? 25 : 1;
                yield rankColorForLevel(rankLevel)
                        + tabText(player, "skill.simpleskills.rank.fishing." + rankKey);
            }
            case MINING, WOODCUTTING, FARMING, SLAYING, RANGED, DEFENSE, ALCHEMY, SMITHING, COOKING, CRAFTING, PRAYER -> {
                int rank = Math.min(10, ((safeLevel - 1) / 10) + 1);
                String profession = skill.name().toLowerCase(Locale.ROOT);
                yield rankColorForLevel((rank - 1) * 10 + 1)
                        + tabText(player, "skill.simpleskills.rank." + profession + ".rank_" + rank);
            }
            default -> null;
        };
    }

    private static void appendAgilityAbility(ServerPlayer player, StringBuilder text, String abilityKey,
            String configKey, int fallbackLevel, int skillLevel) {
        int requiredLevel = fallbackLevel;
        try {
            var agilityConfig = ConfigManager.getAgilityConfig();
            if (agilityConfig.has(configKey)) requiredLevel = Math.max(1, agilityConfig.get(configKey).getAsInt());
        } catch (RuntimeException ignored) {
            // Keep the default unlock level if the server config value is invalid.
        }
        text.append("   §8↳ §b").append(tabText(player, "gui.simpleskills.tab." + abilityKey)).append(": ");
        if (skillLevel >= requiredLevel) {
            double multiplier;
            if (abilityKey.equals("agility_dash")) {
                double progress = Math.max(0.0, Math.min(1.0, (skillLevel - 15) / 84.0));
                multiplier = (0.35 + 0.65 * progress) / 0.35;
            } else {
                double progress = Math.max(0.0, Math.min(1.0,
                        (skillLevel - requiredLevel) / (double) Math.max(1, 40 - requiredLevel)));
                multiplier = (0.5 + 0.5 * progress) / 0.5;
            }
            text.append("§e").append(String.format(Locale.US, "%.2fx", multiplier)).append("\n");
        } else {
            text.append(lockedAt(player, requiredLevel)).append("\n");
        }
    }

    private static String lockedAt(ServerPlayer player, int level) {
        return "§c" + tabText(player, "gui.simpleskills.tab.locked") + " §7("
                + tabText(player, "gui.simpleskills.tab.level_abbr") + " " + level + ")";
    }

    private static String toRoman(int level) {
        return switch (level) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> "—";
        };
    }

    private static List<String> getSkillBuffLines(Skills skill, int level, ServerPlayer player) {
        StringBuilder buffInfo = new StringBuilder();
        appendSkillBuffInfo(buffInfo, skill, level, player);

        List<String> lines = new ArrayList<>();
        String result = buffInfo.toString();

        if (!result.isEmpty()) {
            for (String line : result.split("\n")) {
                if (!line.isEmpty()) lines.add(line);
            }
        }
        return lines;
    }

    private static void appendSkillBuffInfo(StringBuilder skillInfo, Skills skill, int level, ServerPlayer player) {
        int safeLevel = Math.max(0, Math.min(level, XPManager.getMaxLevel()));

        switch (skill) {
            case ENCHANTING -> {
                int unlockedLevel = EnchantProgression.getUnlockedEnchantmentLevel(safeLevel);
                skillInfo.append(String.format("   §8↳ §d%s: §f%s\n",
                        tabText(player, "gui.simpleskills.tab.unlocked_enchantment_level"),
                        unlockedLevel == 0 ? tabText(player, "gui.simpleskills.tab.locked") : toRoman(unlockedLevel)));
            }
            case AGILITY -> {
                float movementBonus = safeLevel * 0.3f;

                if (movementBonus > 0.0f) {
                    skillInfo.append(String.format(Locale.US, "   §8↳ §b%s: §a+%.1f%%\n",
                            tabText(player, "gui.simpleskills.tab.movement_speed"), movementBonus));
                }

                appendAgilityAbility(player, skillInfo, "agility_dash", "dash_unlocked_at", 15, safeLevel);
                appendAgilityAbility(player, skillInfo, "agility_double_jump", "double_jump_unlocked_at", 10, safeLevel);
            }
            case MINING, WOODCUTTING, EXCAVATING -> {
                float speedBonus = safeLevel * GatheringSpeed.BONUS_PER_LEVEL * 100.0f;
                float durabilityChance = Math.min(safeLevel * GatheringSpeed.BONUS_PER_LEVEL, 0.99f) * 100.0f;

                if (speedBonus > 0.0f) {
                    skillInfo.append(String.format(Locale.US, "   §8↳ §b%s: §a+%.0f%%\n",
                            tabText(player, "gui.simpleskills.tab.block_breaking_speed"), speedBonus));
                }

                if (durabilityChance > 0.0f) {
                    skillInfo.append(String.format(Locale.US, "   §8↳ §b%s: §a%.0f%%\n",
                            tabText(player, "gui.simpleskills.tab.durability_protection"), durabilityChance));
                }

                if (skill == Skills.MINING || skill == Skills.WOODCUTTING) {
                    float duplicationChance = Math.min((safeLevel / 10) * 0.05f, 0.45f) * 100.0f;

                    if (duplicationChance > 0.0f) {
                        skillInfo.append(String.format(Locale.US, "   §8↳ §b%s: §a%.0f%%\n",
                                tabText(player, "gui.simpleskills.tab.loot_duplication"), duplicationChance));
                    } else {
                        skillInfo.append(String.format("   §8↳ §b%s: %s\n",
                                tabText(player, "gui.simpleskills.tab.loot_duplication"), lockedAt(player, 10)));
                    }
                }

                if (skill == Skills.MINING) {
                    int additionalBlocks = VeinMining.getAdditionalBlockLimit(safeLevel);

                    if (additionalBlocks > 0) {
                        skillInfo.append(String.format(Locale.US, "   §8↳ §b%s: §a x%d\n",
                                tabText(player, "gui.simpleskills.tab.shift_vein_miner"), additionalBlocks));
                    } else {
                        skillInfo.append(String.format("   §8↳ §b%s: %s\n",
                                tabText(player, "gui.simpleskills.tab.shift_vein_miner"), lockedAt(player, 15)));
                    }
                }

                if (skill == Skills.WOODCUTTING) {
                    if (TreeFeller.isUnlocked(safeLevel)) {
                        skillInfo.append(String.format("   §8↳ §b%s: §a%s\n",
                                tabText(player, "gui.simpleskills.tab.shift_tree_feller"), tabText(player, "gui.simpleskills.tab.unlocked")));
                    } else {
                        skillInfo.append(String.format("   §8↳ §b%s: %s\n",
                                tabText(player, "gui.simpleskills.tab.shift_tree_feller"), lockedAt(player, 5)));
                    }
                }

                if (skill == Skills.EXCAVATING) {
                    skillInfo.append(String.format(Locale.US, "   §8↳ §b%s: §a%.2fs\n",
                            tabText(player, "gui.simpleskills.tab.investigation_time"),
                            ExcavatingRank.investigationSeconds(Math.max(1, safeLevel))));
                }
            }
            case FARMING -> {
                // Exactly four benefits. Harvest Bonus and Animal Loot Bonus use the same
                // chance methods the game rolls use, so the menu can never disagree with gameplay.
                float harvestChance = (float) EventHandlers.getHarvestBonusChance(safeLevel) * 100.0f;
                float animalChance = (float) FarmingPerks.animalLootBonusChance(safeLevel) * 100.0f;

                skillInfo.append(String.format(Locale.US, "   §8↳ §6%s: §a+%.0f%%\n",
                        tabText(player, "gui.simpleskills.tab.harvest_bonus"), harvestChance));

                skillInfo.append(String.format(Locale.US, "   §8↳ §6%s: §a+%.0f%%\n",
                        tabText(player, "gui.simpleskills.tab.animal_loot_bonus"), animalChance));

                if (FarmingPerks.isBoneMealMasteryUnlocked(safeLevel)) {
                    String cost = FarmingPerks.boneMealCostLabel(safeLevel);
                    skillInfo.append(cost.equals("FREE")
                            ? String.format("   §8↳ §6%s: §a%s\n", tabText(player, "gui.simpleskills.tab.bone_meal_mastery"),
                                    tabText(player, "gui.simpleskills.tab.free"))
                            : String.format(Locale.US, "   §8↳ §6%s: §a%s §7%s\n",
                                    tabText(player, "gui.simpleskills.tab.bone_meal_mastery"), cost,
                                    tabText(player, "gui.simpleskills.tab.bars")));
                } else {
                    skillInfo.append(String.format(Locale.US, "   §8↳ §6%s: %s\n",
                            tabText(player, "gui.simpleskills.tab.bone_meal_mastery"), lockedAt(player, FarmingPerks.UNLOCK_LEVEL)));
                }

                if (FarmingPerks.isAutoPlantUnlocked(safeLevel)) {
                    skillInfo.append(String.format("   §8↳ §6%s: §a%s\n",
                            tabText(player, "gui.simpleskills.tab.auto_plant"), tabText(player, "gui.simpleskills.tab.unlocked")));
                } else {
                    skillInfo.append(String.format(Locale.US, "   §8↳ §6%s: %s\n",
                            tabText(player, "gui.simpleskills.tab.auto_plant"), lockedAt(player, FarmingPerks.UNLOCK_LEVEL)));
                }
            }
            case FISHING -> {
                if (FishingConfig.isFightEnabled()) {
                    // Real benefits of the fishing minigame (same numbers the fight uses).
                    if (ConfigManager.isFishingSpeedBonusEnabled()) {
                        double attraction = FishingProgression.attractionPercent(safeLevel);
                        if (attraction > 0.0) {
                            skillInfo.append(String.format(Locale.US, "   §8↳ §b%s: §a+%.0f%%\n",
                                    tabText(player, "gui.simpleskills.tab.fish_attraction_speed"), attraction));
                        }
                    } else {
                        skillInfo.append(String.format("   §8↳ §b%s: §c%s\n",
                                tabText(player, "gui.simpleskills.tab.fish_attraction_speed"), tabText(player, "gui.simpleskills.tab.disabled")));
                    }

                    double fatigue = FishingProgression.fatiguePercent(safeLevel);
                    if (fatigue > 0.0) {
                        skillInfo.append(String.format(Locale.US, "   §8↳ §b%s: §a+%.0f%% §7(%s)\n",
                                tabText(player, "gui.simpleskills.tab.fish_fatigue"), fatigue,
                                tabText(player, "gui.simpleskills.tab.tires_faster")));
                    }

                    double tension = FishingProgression.tensionReductionPercent(safeLevel);
                    if (tension > 0.0) {
                        skillInfo.append(String.format(Locale.US, "   §8↳ §b%s: §a-%.0f%% §7(%s)\n",
                                tabText(player, "gui.simpleskills.tab.line_tension"), tension,
                                tabText(player, "gui.simpleskills.tab.builds_slower")));
                    }

                    double recovery = FishingProgression.recoveryPercent(safeLevel);
                    if (recovery > 0.0) {
                        skillInfo.append(String.format(Locale.US, "   §8↳ §b%s: §a+%.0f%% §7(%s)\n",
                                tabText(player, "gui.simpleskills.tab.line_recovery"), recovery,
                                tabText(player, "gui.simpleskills.tab.relaxes_faster")));
                    }
                } else if (ConfigManager.isFishingSpeedBonusEnabled()) {
                    float waitReduction = (1.0f - Math.max(0.20f, 1.0f - (safeLevel * 0.005f))) * 100.0f;

                    if (waitReduction > 0.0f) {
                        skillInfo.append(String.format(Locale.US, "   §8↳ §b%s: §a%.1f%%\n",
                                tabText(player, "gui.simpleskills.tab.bite_wait_reduction"), waitReduction));
                    }
                } else {
                    skillInfo.append(String.format("   §8↳ §b%s: §c%s\n",
                            tabText(player, "gui.simpleskills.tab.bite_speed"), tabText(player, "gui.simpleskills.tab.disabled")));
                }
            }
            case DEFENSE -> {
                float damageReduction = safeLevel * 0.3f;
                float armorProtection = safeLevel * 1.0f;
                float knockbackResistance = safeLevel * 0.5f;

                if (damageReduction > 0.0f) {
                    skillInfo.append(String.format(Locale.US, "   §8↳ §c%s: §a%.1f%%\n",
                            tabText(player, "gui.simpleskills.tab.damage_reduction"), damageReduction));
                }

                if (armorProtection > 0.0f) {
                    skillInfo.append(String.format(Locale.US, "   §8↳ §c%s: §a%.0f%%\n",
                            tabText(player, "gui.simpleskills.tab.armor_durability_protection"), armorProtection));
                }

                if (knockbackResistance > 0.0f) {
                    skillInfo.append(String.format(Locale.US, "   §8↳ §c%s: §a%.1f%%\n",
                            tabText(player, "gui.simpleskills.tab.knockback_resistance"), knockbackResistance));
                }

                int stageLevel = safeLevel >= 99 ? 99 : safeLevel < 10 ? 10 : 10 + ((safeLevel - 10) / 10) * 10;
                int nextStage = safeLevel < 10 ? 10 : safeLevel >= 99 ? 0
                        : Math.min(99, 10 + (((safeLevel - 10) / 10) + 1) * 10);
                if (safeLevel < 10) {
                    skillInfo.append("   §8↳ §c").append(tabText(player, "gui.simpleskills.tab.parry_tier")).append(": ")
                            .append(rankColorForLevel(10)).append(tabText(player, "skill.simpleskills.defense.parry_tier_10"))
                            .append(" ").append(lockedAt(player, 10)).append("\n");
                } else {
                    skillInfo.append("   §8↳ §c").append(tabText(player, "gui.simpleskills.tab.parry_tier")).append(": ")
                            .append(rankColorForLevel(stageLevel)).append(tabText(player, "skill.simpleskills.defense.parry_tier_" + stageLevel))
                            .append(" §a").append(tabText(player, "gui.simpleskills.tab.unlocked")).append("\n");
                }
                if (nextStage > 0 && safeLevel >= 10) {
                    skillInfo.append("   §8↳ §c").append(tabText(player, "gui.simpleskills.tab.next_tier")).append(": ")
                            .append(rankColorForLevel(nextStage)).append(tabText(player, "skill.simpleskills.defense.parry_tier_" + nextStage))
                            .append(" ").append(lockedAt(player, nextStage)).append("\n");
                }
                String projectileLabel = tabText(player, "gui.simpleskills.tab.projectile_reflection");
                if (safeLevel >= 30) {
                    skillInfo.append("   §8↳ §c").append(projectileLabel).append(": §a")
                            .append(tabText(player, "gui.simpleskills.tab.unlocked")).append("\n");
                } else {
                    skillInfo.append("   §8↳ §c").append(projectileLabel).append(": ").append(rankColorForLevel(30))
                            .append(lockedAt(player, 30)).append("\n");
                }
            }
            case SLAYING -> {
                float damageBonus = safeLevel * Slaying.DAMAGE_PER_LEVEL * 100.0f;
                float criticalChance = Math.min(safeLevel * Slaying.CRITICAL_CHANCE_PER_LEVEL, 0.495f) * 100.0f;

                if (damageBonus > 0.0f) {
                    skillInfo.append(String.format(Locale.US, "   §8↳ §c%s: §a+%.1f%%\n",
                            tabText(player, "gui.simpleskills.tab.sword_damage"), damageBonus));
                }

                if (criticalChance > 0.0f) {
                    skillInfo.append(String.format(Locale.US, "   §8↳ §c%s: §a%.1f%%\n",
                            tabText(player, "gui.simpleskills.tab.critical_chance"), criticalChance));
                }

                if (safeLevel >= 99) {
                    skillInfo.append("   §8↳ §c").append(tabText(player, "gui.simpleskills.tab.life_steal_ii"))
                            .append(": ").append(tabText(player, "gui.simpleskills.tab.life_steal_ii_description")).append("\n");
                } else if (safeLevel >= 50) {
                    skillInfo.append("   §8↳ §c").append(tabText(player, "gui.simpleskills.tab.life_steal_i"))
                            .append(": ").append(tabText(player, "gui.simpleskills.tab.life_steal_i_description")).append("\n");
                    skillInfo.append(String.format("   §8↳ §c%s: %s\n",
                            tabText(player, "gui.simpleskills.tab.life_steal_ii"), lockedAt(player, 99)));
                } else {
                    skillInfo.append(String.format("   §8↳ §c%s: %s\n",
                            tabText(player, "gui.simpleskills.tab.life_steal_i"), lockedAt(player, 50)));
                    skillInfo.append(String.format("   §8↳ §c%s: %s\n",
                            tabText(player, "gui.simpleskills.tab.life_steal_ii"), lockedAt(player, 99)));
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
                    skillInfo.append(String.format(Locale.US, "   §8↳ §c%s: §a+%.1f%%\n",
                            tabText(player, "gui.simpleskills.tab.projectile_damage"), damageBonus));
                }

                if (safeLevel > 0) {
                    skillInfo.append(String.format(Locale.US, "   §8↳ §c%s: §a%.1f%%\n",
                            tabText(player, "gui.simpleskills.tab.double_shot_chance"), doubleShotChance));

                    skillInfo.append(String.format(Locale.US, "   §8↳ §c%s: §a%.1f%%\n",
                            tabText(player, "gui.simpleskills.tab.triple_shot_chance"), tripleShotChance));
                }
            }
            case SMITHING -> {
                float repairEfficiency = safeLevel >= 99 ? 1.00f : safeLevel >= 75 ? 0.55f : safeLevel >= 50 ? 0.45f : safeLevel >= 25 ? 0.35f : 0.25f;
                float netheriteBonus = (ConfigManager.getSmithingMultiplier(safeLevel) - 1.0f) * 100.0f;

                skillInfo.append(String.format(Locale.US, "   §8↳ §6%s: §a%.0f%%\n",
                        tabText(player, "gui.simpleskills.tab.repair_efficiency"), repairEfficiency * 100.0f))
                         .append(String.format("   §8↳ §6%s: §a%s\n",
                                 tabText(player, "gui.simpleskills.tab.anvil_durability"),
                                 tabText(player, "gui.simpleskills.tab.no_degradation")));

                if (netheriteBonus > 0.0f) {
                    skillInfo.append(String.format(Locale.US, "   §8↳ §6%s: §a+%.1f%%\n",
                            tabText(player, "gui.simpleskills.tab.netherite_durability"), netheriteBonus));
                }
            }
            case COOKING -> {
                float foodBonus = (ConfigManager.getCookingMultiplier(safeLevel) - 1.0f) * 100.0f;

                if (foodBonus > 0.0f) {
                    skillInfo.append(String.format(Locale.US, "   §8↳ §6%s: §a%+.1f%%\n",
                            tabText(player, "gui.simpleskills.tab.food_nutrition_saturation"), foodBonus));
                }
            }
            case CRAFTING -> {
                float durabilityBonus = (ConfigManager.getCraftingDurabilityMultiplier(safeLevel) - 1.0f) * 100.0f;
                float recoveryChance = ConfigManager.getCraftingRecoveryChance(safeLevel) * 100.0f;

                if (durabilityBonus > 0.0f) {
                    skillInfo.append(String.format(Locale.US, "   §8↳ §6%s: §a+%.1f%%\n",
                            tabText(player, "gui.simpleskills.tab.crafted_item_durability"), durabilityBonus));
                } else {
                    skillInfo.append(String.format("   §8↳ §6%s: %s\n",
                            tabText(player, "gui.simpleskills.tab.crafted_item_durability"), lockedAt(player, 25)));
                }

                if (recoveryChance > 0.0f) {
                    skillInfo.append(String.format(Locale.US, "   §8↳ §6%s: §a%.1f%%\n",
                            tabText(player, "gui.simpleskills.tab.material_recovery"), recoveryChance));
                } else {
                    skillInfo.append(String.format("   §8↳ §6%s: %s\n",
                            tabText(player, "gui.simpleskills.tab.material_recovery"), lockedAt(player, 25)));
                }
            }
            default -> {}
        }
    }

    /** Shared TAB rank palette: ranks in the same level band use the same color in every skill. */
    private static String rankColorForLevel(int level) {
        if (level <= 10) return "§7";
        if (level <= 20) return "§e";
        if (level <= 30) return "§a";
        if (level <= 40) return "§3";
        if (level <= 50) return "§6";
        if (level <= 60) return "§b";
        if (level <= 70) return "§9";
        if (level <= 80) return "§5";
        if (level <= 90) return "§d";
        return "§c";
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
