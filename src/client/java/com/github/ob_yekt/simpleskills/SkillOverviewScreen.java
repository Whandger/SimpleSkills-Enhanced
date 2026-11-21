package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.agility.AgilityRank;
import com.github.ob_yekt.simpleskills.enchanting.EnchantingRank;
import com.github.ob_yekt.simpleskills.excavating.ExcavatingRank;
import com.github.ob_yekt.simpleskills.ui.SkillOverviewDataPayload;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.github.ob_yekt.simpleskills.prayer.PrayerAbility;
import com.github.ob_yekt.simpleskills.prayer.PrayerScaling;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Skill Overview (key O). The server sends raw data only; this screen performs every calculation
 * and builds every line of text itself. Palette: Rune Infusion (gold frame, dark panels).
 * Bars: Fishing HUD colours.
 */
public final class SkillOverviewScreen extends Screen {
    private static SkillOverviewScreen activeScreen;

    // ----- balance constants, mirrored from the game logic -----
    private static final int MAX_LEVEL = 99;
    private static final double SCALING_A = 50.751;     // XPManager
    private static final double EXPONENT_P = 2.672;     // XPManager
    private static final double OFFSET_K = 0.295;       // XPManager
    private static final float GATHERING_BONUS_PER_LEVEL = 0.01f;   // GatheringSpeed
    private static final float SLAYING_DAMAGE_PER_LEVEL = 0.003f;   // Slaying
    private static final float SLAYING_CRIT_PER_LEVEL = 0.005f;     // Slaying
    private static final float SLAYING_CRIT_CAP = 0.495f;           // Slaying
    private static final float RANGED_DAMAGE_PER_LEVEL = 0.005f;    // Ranged
    private static final int VEIN_FIRST_LEVEL = 15;                 // VeinMining
    private static final int VEIN_INTERVAL = 15;                    // VeinMining
    private static final int VEIN_PER_TIER = 2;                     // VeinMining
    private static final int VEIN_MAX = 12;                         // VeinMining
    private static final int TREE_UNLOCK_LEVEL = 5;                 // TreeFeller
    private static final int FARM_UNLOCK_LEVEL = 5;                 // FarmingPerks
    private static final int FARM_MAX_BONUS_LEVEL = 99;             // FarmingPerks
    private static final int ENCHANT_MIN_LEVEL = 1;                 // EnchantProgression
    private static final int ENCHANT_MAX_LEVEL = 99;                // EnchantProgression
    private static final int ENCHANT_MAX_UNLOCKED = 10;             // EnchantProgression
    private static final int DASH_BASE_LEVEL = 15;
    private static final int DOUBLE_JUMP_BASE_LEVEL = 10;

    private record Config(boolean fishingFight, boolean fishingSpeedBonus, int dashUnlockedAt,
                          int doubleJumpUnlockedAt, double[] smithing, double[] cooking,
                          double[] craftingDurability, double[] craftingRecovery, double[] alchemy) {}

    private record Entry(String id, String name, int level, boolean max, double progress,
                         String xpText, String rank, List<String> buffs, ItemStack icon) {}

    private static final NumberFormat INTEGERS = NumberFormat.getIntegerInstance(Locale.US);

    private static final int WIDTH = 600;
    private static final int HEIGHT = 296;
    private static final int COLUMNS = 3;
    private static final int CARD_W = 88;
    private static final int CARD_H = 40;
    private static final int GAP = 4;
    private static final int GRID_LEFT = 12;
    private static final int GRID_TOP = 62;
    private static final int DETAIL_LEFT = GRID_LEFT + COLUMNS * CARD_W + (COLUMNS - 1) * GAP + 10;
    private static final int DETAIL_W = WIDTH - 12 - DETAIL_LEFT;
    private static final int DETAIL_H = 5 * CARD_H + 4 * GAP;

    // Rune Infusion palette
    private static final int GOLD = 0xFFE9CF7A;
    private static final int GOLD_DARK = 0xFF80612A;
    private static final int FRAME_BG = 0xF217120E;
    private static final int PANEL_BG = 0xFF292018;
    private static final int CARD_BG = 0xFF241C14;
    private static final int CARD_HOVER = 0xFF33281C;
    private static final int CARD_SELECTED = 0xFF3D3020;
    private static final int TEXT = 0xFFF3E8D2;
    private static final int MUTED = 0xFFB7A88D;
    // Fishing HUD bar colours
    private static final int BAR_BORDER = 0xFF000000;
    private static final int BAR_BACKGROUND = 0xFF2B2B2B;
    private static final int BAR_PROGRESS = 0xFFE9CF7A;
    private static final int BAR_XP = 0xFF55E05A;

    /** Legacy colour codes (0-9, a-f). */
    private static final String COLOR_CODES = "0123456789abcdef";

    private final List<Entry> entries = new ArrayList<>();
    private String title = "";
    private String subtitle = "";
    private String total = "";
    private String ironmanText = "";
    private String prestigeText = "";
    private int selected;
    private int left;
    private int top;

    private SkillOverviewScreen(String json) {
        super(Component.empty());
        accept(json);
    }

    public static void showOrUpdate(SkillOverviewDataPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (activeScreen != null && mc.gui.screen() == activeScreen) {
            activeScreen.accept(payload.json());
            return;
        }
        activeScreen = new SkillOverviewScreen(payload.json());
        mc.gui.setScreen(activeScreen);
    }

    // =====================================================================================
    // Data -> screen model (every formula lives below)
    // =====================================================================================

    private void accept(String json) {
        entries.clear();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            boolean ironman = root.has("ironman") && root.get("ironman").getAsBoolean();
            int prestige = root.has("prestige") ? root.get("prestige").getAsInt() : 0;
            Config config = readConfig(root.has("config") ? root.getAsJsonObject("config") : null);

            List<String[]> rawSkills = new ArrayList<>();
            List<Integer> levels = new ArrayList<>();
            List<Integer> xps = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray("skills")) {
                JsonObject skill = element.getAsJsonObject();
                levels.add(skill.get("level").getAsInt());
                xps.add(skill.get("xp").getAsInt());
                rawSkills.add(new String[]{skill.get("id").getAsString()});
            }

            int totalLevels = 0;
            for (int level : levels) totalLevels += level;

            title = SkillOverviewText.get("gui.simpleskills.tab.skills_overview");
            subtitle = SkillOverviewText.get("gui.simpleskills.tab.character_progression");
            total = SkillOverviewText.format("gui.simpleskills.tab.total_level", totalLevels);
            ironmanText = ironman
                    ? SkillOverviewText.get("gui.simpleskills.tab.ironman_mode") + ": "
                      + SkillOverviewText.get("gui.simpleskills.tab.enabled")
                    : "";
            prestigeText = prestige > 0
                    ? String.format(Locale.US, "%s: \u2605%d", SkillOverviewText.get("gui.simpleskills.tab.prestige"), prestige)
                    : "";

            for (int i = 0; i < rawSkills.size(); i++) {
                entries.add(buildEntry(rawSkills.get(i)[0], levels.get(i), xps.get(i), config));
            }
        } catch (RuntimeException ignored) {
            // Malformed snapshot: show an empty screen instead of crashing the client.
        }
        if (selected >= entries.size()) selected = 0;
    }

    private static Config readConfig(JsonObject c) {
        return new Config(
                c != null && c.has("fishing_fight") && c.get("fishing_fight").getAsBoolean(),
                c == null || !c.has("fishing_speed_bonus") || c.get("fishing_speed_bonus").getAsBoolean(),
                c != null && c.has("dash_unlocked_at") ? c.get("dash_unlocked_at").getAsInt() : DASH_BASE_LEVEL,
                c != null && c.has("double_jump_unlocked_at") ? c.get("double_jump_unlocked_at").getAsInt() : DOUBLE_JUMP_BASE_LEVEL,
                table(c, "smithing", 1.0),
                table(c, "cooking", 1.0),
                table(c, "crafting_durability", 1.0),
                table(c, "crafting_recovery", 0.0),
                table(c, "alchemy", 1.0));
    }

    private static double[] table(JsonObject c, String key, double fallback) {
        double[] values = new double[MAX_LEVEL + 1];
        java.util.Arrays.fill(values, fallback);
        if (c != null && c.has(key) && c.get(key).isJsonArray()) {
            JsonArray array = c.getAsJsonArray(key);
            for (int level = 0; level <= MAX_LEVEL && level < array.size(); level++) {
                values[level] = array.get(level).getAsDouble();
            }
        }
        return values;
    }

    private Entry buildEntry(String id, int level, int xp, Config config) {
        boolean max = level >= MAX_LEVEL;
        int base = experienceForLevel(level);
        int toNext = max ? 0 : Math.max(1, experienceForLevel(level + 1) - base);
        int into = max ? 0 : Math.max(0, Math.min(xp - base, toNext));
        double progress = max ? 1.0 : (double) into / toNext;
        String xpText = max
                ? SkillOverviewText.get("gui.simpleskills.tab.max_level")
                : SkillOverviewText.format("gui.simpleskills.tab.xp_progress",
                        INTEGERS.format(into), INTEGERS.format(toNext));
        String name = SkillOverviewText.get("skill.simpleskills.name." + id);
        return new Entry(id, name, level, max, progress, xpText, rankTitle(id, level),
                buffLines(id, level, config), iconFor(id));
    }

    /** Vanilla-style XP curve (same constants as XPManager). */
    private static int experienceForLevel(int level) {
        if (level <= 1) return 0;
        double base = Math.pow(1.0 + OFFSET_K, EXPONENT_P);
        return (int) Math.floor(SCALING_A * (Math.pow((double) level + OFFSET_K, EXPONENT_P) - base));
    }

    private static int clampLevel(int level) {
        return Math.max(0, Math.min(MAX_LEVEL, level));
    }

    // ----- ranks -----

    private static String rankTitle(String id, int level) {
        int safe = Math.max(1, Math.min(MAX_LEVEL, level));
        switch (id) {
            case "agility" -> {
                AgilityRank rank = AgilityRank.forLevel(safe);
                return rankColor(rank.ordinal() * 10 + 1)
                        + SkillOverviewText.get("skill.simpleskills.rank.agility." + lower(rank.name()));
            }
            case "excavating" -> {
                ExcavatingRank rank = ExcavatingRank.forLevel(safe);
                return rankColor(rank.getMinLevel())
                        + SkillOverviewText.get("skill.simpleskills.rank.excavating." + lower(rank.name()));
            }
            case "enchanting" -> {
                EnchantingRank rank = EnchantingRank.forLevel(safe);
                return rankColor(rank.getMinimumLevel())
                        + SkillOverviewText.get("skill.simpleskills.rank.enchanting." + lower(rank.name()));
            }
            case "fishing" -> {
                String key = safe >= 99 ? "grandmaster" : safe >= 75 ? "expert"
                        : safe >= 50 ? "artisan" : safe >= 25 ? "journeyman" : "novice";
                int band = safe >= 99 ? 99 : safe >= 75 ? 75 : safe >= 50 ? 50 : safe >= 25 ? 25 : 1;
                return rankColor(band) + SkillOverviewText.get("skill.simpleskills.rank.fishing." + key);
            }
            default -> {
                int rank = Math.min(10, ((safe - 1) / 10) + 1);
                return rankColor((rank - 1) * 10 + 1)
                        + SkillOverviewText.get("skill.simpleskills.rank." + id + ".rank_" + rank);
            }
        }
    }

    /** Shared palette: ranks in the same level band use the same colour in every skill. */
    private static String rankColor(int level) {
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

    private static String lower(String enumName) {
        return enumName.toLowerCase(Locale.ROOT);
    }

    // ----- effect lines -----

    private static List<String> buffLines(String id, int level, Config cfg) {
        StringBuilder out = new StringBuilder();
        int safe = clampLevel(level);

        switch (id) {
            case "enchanting" -> {
                int unlocked = unlockedEnchantmentLevel(safe);
                out.append(String.format(Locale.US, "   §8↳ §7%s: §f%s\n",
                        SkillOverviewText.get("gui.simpleskills.tab.unlocked_enchantment_level"),
                        unlocked == 0 ? SkillOverviewText.get("gui.simpleskills.tab.locked") : toRoman(unlocked)));
            }
            case "agility" -> {
                float movement = safe * 0.3f;
                if (movement > 0.0f) {
                    out.append(String.format(Locale.US, "   §8↳ §7%s: §a+%.1f%%\n",
                            SkillOverviewText.get("gui.simpleskills.tab.movement_speed"), movement));
                }
                appendAgilityAbility(out, "agility_dash", cfg.dashUnlockedAt(), safe);
                appendAgilityAbility(out, "agility_double_jump", cfg.doubleJumpUnlockedAt(), safe);
            }
            case "mining", "woodcutting", "excavating" -> {
                float speed = safe * GATHERING_BONUS_PER_LEVEL * 100.0f;
                float durability = Math.min(safe * GATHERING_BONUS_PER_LEVEL, 0.99f) * 100.0f;
                if (speed > 0.0f) {
                    out.append(String.format(Locale.US, "   §8↳ §7%s: §a+%.0f%%\n",
                            SkillOverviewText.get("gui.simpleskills.tab.block_breaking_speed"), speed));
                }
                if (durability > 0.0f) {
                    out.append(String.format(Locale.US, "   §8↳ §7%s: §a%.0f%%\n",
                            SkillOverviewText.get("gui.simpleskills.tab.durability_protection"), durability));
                }
                if (id.equals("mining") || id.equals("woodcutting")) {
                    float duplication = Math.min((safe / 10) * 0.05f, 0.45f) * 100.0f;
                    if (duplication > 0.0f) {
                        out.append(String.format(Locale.US, "   §8↳ §7%s: §a%.0f%%\n",
                                SkillOverviewText.get("gui.simpleskills.tab.loot_duplication"), duplication));
                    } else {
                        out.append(String.format("   §8↳ §7%s: %s\n",
                                SkillOverviewText.get("gui.simpleskills.tab.loot_duplication"), lockedAt(10)));
                    }
                }
                if (id.equals("mining")) {
                    int extra = veinMiningBlocks(safe);
                    if (extra > 0) {
                        out.append(String.format(Locale.US, "   §8↳ §7%s: §a x%d\n",
                                SkillOverviewText.get("gui.simpleskills.tab.shift_vein_miner"), extra));
                    } else {
                        out.append(String.format("   §8↳ §7%s: %s\n",
                                SkillOverviewText.get("gui.simpleskills.tab.shift_vein_miner"), lockedAt(VEIN_FIRST_LEVEL)));
                    }
                }
                if (id.equals("woodcutting")) {
                    if (safe >= TREE_UNLOCK_LEVEL) {
                        out.append(String.format("   §8↳ §7%s: §a%s\n",
                                SkillOverviewText.get("gui.simpleskills.tab.shift_tree_feller"),
                                SkillOverviewText.get("gui.simpleskills.tab.unlocked")));
                    } else {
                        out.append(String.format("   §8↳ §7%s: %s\n",
                                SkillOverviewText.get("gui.simpleskills.tab.shift_tree_feller"), lockedAt(TREE_UNLOCK_LEVEL)));
                    }
                }
                if (id.equals("excavating")) {
                    out.append(String.format(Locale.US, "   §8↳ §7%s: §a%.2fs\n",
                            SkillOverviewText.get("gui.simpleskills.tab.investigation_time"),
                            ExcavatingRank.investigationSeconds(Math.max(1, safe))));
                }
            }
            case "farming" -> {
                float harvest = Math.min(safe, FARM_MAX_BONUS_LEVEL);
                float animal = Math.min(Math.max(safe, 0), FARM_MAX_BONUS_LEVEL);
                out.append(String.format(Locale.US, "   §8↳ §7%s: §a+%.0f%%\n",
                        SkillOverviewText.get("gui.simpleskills.tab.harvest_bonus"), harvest));
                out.append(String.format(Locale.US, "   §8↳ §7%s: §a+%.0f%%\n",
                        SkillOverviewText.get("gui.simpleskills.tab.animal_loot_bonus"), animal));

                if (safe >= FARM_UNLOCK_LEVEL) {
                    String cost = boneMealCostLabel(safe);
                    out.append(cost.equals("FREE")
                            ? String.format("   §8↳ §7%s: §a%s\n",
                                    SkillOverviewText.get("gui.simpleskills.tab.bone_meal_mastery"),
                                    SkillOverviewText.get("gui.simpleskills.tab.free"))
                            : String.format(Locale.US, "   §8↳ §7%s: §a%s §7%s\n",
                                    SkillOverviewText.get("gui.simpleskills.tab.bone_meal_mastery"), cost,
                                    SkillOverviewText.get("gui.simpleskills.tab.bars")));
                    out.append(String.format("   §8↳ §7%s: §a%s\n",
                            SkillOverviewText.get("gui.simpleskills.tab.auto_plant"),
                            SkillOverviewText.get("gui.simpleskills.tab.unlocked")));
                } else {
                    out.append(String.format(Locale.US, "   §8↳ §7%s: %s\n",
                            SkillOverviewText.get("gui.simpleskills.tab.bone_meal_mastery"), lockedAt(FARM_UNLOCK_LEVEL)));
                    out.append(String.format(Locale.US, "   §8↳ §7%s: %s\n",
                            SkillOverviewText.get("gui.simpleskills.tab.auto_plant"), lockedAt(FARM_UNLOCK_LEVEL)));
                }
            }
            case "fishing" -> {
                if (cfg.fishingFight()) {
                    if (cfg.fishingSpeedBonus()) {
                        double attraction = attractionPercent(safe);
                        if (attraction > 0.0) {
                            out.append(String.format(Locale.US, "   §8↳ §7%s: §a+%.0f%%\n",
                                    SkillOverviewText.get("gui.simpleskills.tab.fish_attraction_speed"), attraction));
                        }
                    } else {
                        out.append(String.format("   §8↳ §7%s: §c%s\n",
                                SkillOverviewText.get("gui.simpleskills.tab.fish_attraction_speed"),
                                SkillOverviewText.get("gui.simpleskills.tab.disabled")));
                    }
                    double fatigue = fatiguePercent(safe);
                    if (fatigue > 0.0) {
                        out.append(String.format(Locale.US, "   §8↳ §7%s: §a+%.0f%% §7(%s)\n",
                                SkillOverviewText.get("gui.simpleskills.tab.fish_fatigue"), fatigue,
                                SkillOverviewText.get("gui.simpleskills.tab.tires_faster")));
                    }
                    double tension = tensionPercent(safe);
                    if (tension > 0.0) {
                        out.append(String.format(Locale.US, "   §8↳ §7%s: §a-%.0f%% §7(%s)\n",
                                SkillOverviewText.get("gui.simpleskills.tab.line_tension"), tension,
                                SkillOverviewText.get("gui.simpleskills.tab.builds_slower")));
                    }
                    double recovery = recoveryPercent(safe);
                    if (recovery > 0.0) {
                        out.append(String.format(Locale.US, "   §8↳ §7%s: §a+%.0f%% §7(%s)\n",
                                SkillOverviewText.get("gui.simpleskills.tab.line_recovery"), recovery,
                                SkillOverviewText.get("gui.simpleskills.tab.relaxes_faster")));
                    }
                } else if (cfg.fishingSpeedBonus()) {
                    float wait = (1.0f - Math.max(0.20f, 1.0f - (safe * 0.005f))) * 100.0f;
                    if (wait > 0.0f) {
                        out.append(String.format(Locale.US, "   §8↳ §7%s: §a%.1f%%\n",
                                SkillOverviewText.get("gui.simpleskills.tab.bite_wait_reduction"), wait));
                    }
                } else {
                    out.append(String.format("   §8↳ §7%s: §c%s\n",
                            SkillOverviewText.get("gui.simpleskills.tab.bite_speed"),
                            SkillOverviewText.get("gui.simpleskills.tab.disabled")));
                }
            }
            case "defense" -> {
                float damageReduction = safe * 0.3f;
                float armorProtection = safe * 1.0f;
                float knockback = safe * 0.5f;
                if (damageReduction > 0.0f) {
                    out.append(String.format(Locale.US, "   §8↳ §7%s: §a%.1f%%\n",
                            SkillOverviewText.get("gui.simpleskills.tab.damage_reduction"), damageReduction));
                }
                if (armorProtection > 0.0f) {
                    out.append(String.format(Locale.US, "   §8↳ §7%s: §a%.0f%%\n",
                            SkillOverviewText.get("gui.simpleskills.tab.armor_durability_protection"), armorProtection));
                }
                if (knockback > 0.0f) {
                    out.append(String.format(Locale.US, "   §8↳ §7%s: §a%.1f%%\n",
                            SkillOverviewText.get("gui.simpleskills.tab.knockback_resistance"), knockback));
                }

                int stage = safe >= 99 ? 99 : safe < 10 ? 10 : 10 + ((safe - 10) / 10) * 10;
                int next = safe < 10 ? 10 : safe >= 99 ? 0 : Math.min(99, 10 + (((safe - 10) / 10) + 1) * 10);
                String tierLabel = SkillOverviewText.get("gui.simpleskills.tab.parry_tier");
                if (safe < 10) {
                    out.append("   §8↳ §7").append(tierLabel).append(": ")
                       .append(rankColor(10)).append(SkillOverviewText.get("skill.simpleskills.defense.parry_tier_10"))
                       .append(" ").append(lockedAt(10)).append("\n");
                } else {
                    out.append("   §8↳ §7").append(tierLabel).append(": ")
                       .append(rankColor(stage)).append(SkillOverviewText.get("skill.simpleskills.defense.parry_tier_" + stage))
                       .append(" §a").append(SkillOverviewText.get("gui.simpleskills.tab.unlocked")).append("\n");
                }
                if (next > 0 && safe >= 10) {
                    out.append("   §8↳ §7").append(SkillOverviewText.get("gui.simpleskills.tab.next_tier")).append(": ")
                       .append(rankColor(next)).append(SkillOverviewText.get("skill.simpleskills.defense.parry_tier_" + next))
                       .append(" ").append(lockedAt(next)).append("\n");
                }
                String reflection = SkillOverviewText.get("gui.simpleskills.tab.projectile_reflection");
                if (safe >= 30) {
                    out.append("   §8↳ §7").append(reflection).append(": §a")
                       .append(SkillOverviewText.get("gui.simpleskills.tab.unlocked")).append("\n");
                } else {
                    out.append("   §8↳ §7").append(reflection).append(": ")
                       .append(rankColor(30)).append(lockedAt(30)).append("\n");
                }
            }
            case "slaying" -> {
                float damage = safe * SLAYING_DAMAGE_PER_LEVEL * 100.0f;
                float crit = Math.min(safe * SLAYING_CRIT_PER_LEVEL, SLAYING_CRIT_CAP) * 100.0f;
                if (damage > 0.0f) {
                    out.append(String.format(Locale.US, "   §8↳ §7%s: §a+%.1f%%\n",
                            SkillOverviewText.get("gui.simpleskills.tab.sword_damage"), damage));
                }
                if (crit > 0.0f) {
                    out.append(String.format(Locale.US, "   §8↳ §7%s: §a%.1f%%\n",
                            SkillOverviewText.get("gui.simpleskills.tab.critical_chance"), crit));
                }
                if (safe >= 99) {
                    out.append("   §8↳ §7").append(SkillOverviewText.get("gui.simpleskills.tab.life_steal_ii"))
                       .append(": ").append(SkillOverviewText.get("gui.simpleskills.tab.life_steal_ii_description")).append("\n");
                } else if (safe >= 50) {
                    out.append("   §8↳ §7").append(SkillOverviewText.get("gui.simpleskills.tab.life_steal_i"))
                       .append(": ").append(SkillOverviewText.get("gui.simpleskills.tab.life_steal_i_description")).append("\n");
                    out.append(String.format("   §8↳ §7%s: %s\n",
                            SkillOverviewText.get("gui.simpleskills.tab.life_steal_ii"), lockedAt(99)));
                } else {
                    out.append(String.format("   §8↳ §7%s: %s\n",
                            SkillOverviewText.get("gui.simpleskills.tab.life_steal_i"), lockedAt(50)));
                    out.append(String.format("   §8↳ §7%s: %s\n",
                            SkillOverviewText.get("gui.simpleskills.tab.life_steal_ii"), lockedAt(99)));
                }
            }
            case "ranged" -> {
                float damage = safe * RANGED_DAMAGE_PER_LEVEL * 100.0f;
                float doubleShot = Math.min(safe / 50.0f, 1.0f) * 100.0f;
                float tripleShot = safe <= 50 ? 0.0f : Math.min((safe - 50) / 49.0f, 1.0f) * 100.0f;
                if (damage > 0.0f) {
                    out.append(String.format(Locale.US, "   §8↳ §7%s: §a+%.1f%%\n",
                            SkillOverviewText.get("gui.simpleskills.tab.projectile_damage"), damage));
                }
                if (safe > 0) {
                    out.append(String.format(Locale.US, "   §8↳ §7%s: §a%.1f%%\n",
                            SkillOverviewText.get("gui.simpleskills.tab.double_shot_chance"), doubleShot));
                    out.append(String.format(Locale.US, "   §8↳ §7%s: §a%.1f%%\n",
                            SkillOverviewText.get("gui.simpleskills.tab.triple_shot_chance"), tripleShot));
                }
            }
            case "smithing" -> {
                float efficiency = safe >= 99 ? 1.00f : safe >= 75 ? 0.55f : safe >= 50 ? 0.45f : safe >= 25 ? 0.35f : 0.25f;
                float netherite = (float) ((cfg.smithing()[safe] - 1.0) * 100.0);
                out.append(String.format(Locale.US, "   §8↳ §7%s: §a%.0f%%\n",
                        SkillOverviewText.get("gui.simpleskills.tab.repair_efficiency"), efficiency * 100.0f));
                out.append(String.format("   §8↳ §7%s: §a%s\n",
                        SkillOverviewText.get("gui.simpleskills.tab.anvil_durability"),
                        SkillOverviewText.get("gui.simpleskills.tab.no_degradation")));
                if (netherite > 0.0f) {
                    out.append(String.format(Locale.US, "   §8↳ §7%s: §a+%.1f%%\n",
                            SkillOverviewText.get("gui.simpleskills.tab.netherite_durability"), netherite));
                }
            }
            case "cooking" -> {
                float food = (float) ((cfg.cooking()[safe] - 1.0) * 100.0);
                if (food > 0.0f) {
                    out.append(String.format(Locale.US, "   §8↳ §7%s: §a%+.1f%%\n",
                            SkillOverviewText.get("gui.simpleskills.tab.food_nutrition_saturation"), food));
                }
            }
            case "alchemy" -> {
                double current = cfg.alchemy()[safe];
                int next = nextStep(cfg.alchemy(), safe);
                String label = SkillOverviewText.get("gui.simpleskills.tab.alchemy_potion_duration");
                if (current > 1.0) {
                    out.append(String.format(Locale.US, "   §8↳ §7%s: §a+%.0f%%\n", label, (current - 1.0) * 100.0));
                } else {
                    out.append(String.format("   §8↳ §7%s: %s\n", label, lockedAt(next < 0 ? 25 : next)));
                }
                if (next >= 0) {
                    out.append("   §8↳ §7").append(SkillOverviewText.format("gui.simpleskills.tab.next_bonus",
                            next, (cfg.alchemy()[next] - 1.0) * 100.0)).append("\n");
                }
            }
            case "prayer" -> {
                out.append(String.format(Locale.US, "   §8↳ §7%s: §a+%.1f%%\n",
                        SkillOverviewText.get("gui.simpleskills.tab.prayer_damage"),
                        (PrayerScaling.damageScale(safe) - 1.0f) * 100.0f));
                out.append(String.format(Locale.US, "   §8↳ §7%s: §a-%.1f%%\n",
                        SkillOverviewText.get("gui.simpleskills.tab.prayer_cooldown"),
                        (1.0 - PrayerScaling.cooldownMultiplier(safe)) * 100.0));
                out.append(String.format(Locale.US, "   §8↳ §7%s: §a%.2fx\n",
                        SkillOverviewText.get("gui.simpleskills.tab.prayer_xp_per_cast"),
                        PrayerScaling.xpMultiplier(safe)));
                out.append(String.format(Locale.US, "   §8↳ §7%s: §a%s\n",
                        SkillOverviewText.get("gui.simpleskills.tab.prayer_buff_power"),
                        toRoman(PrayerScaling.buffPower(safe) + 1)));
                long unlocked = java.util.Arrays.stream(PrayerAbility.values())
                        .filter(a -> a.isUnlocked(safe)).count();
                out.append(String.format(Locale.US, "   §8↳ §7%s: §a%d§7/%d\n",
                        SkillOverviewText.get("gui.simpleskills.tab.prayer_unlocked"),
                        unlocked, PrayerAbility.values().length));
            }
            case "crafting" -> {
                float durability = (float) ((cfg.craftingDurability()[safe] - 1.0) * 100.0);
                float recovery = (float) (cfg.craftingRecovery()[safe] * 100.0);
                if (durability > 0.0f) {
                    out.append(String.format(Locale.US, "   §8↳ §7%s: §a+%.1f%%\n",
                            SkillOverviewText.get("gui.simpleskills.tab.crafted_item_durability"), durability));
                } else {
                    out.append(String.format("   §8↳ §7%s: %s\n",
                            SkillOverviewText.get("gui.simpleskills.tab.crafted_item_durability"), lockedAt(25)));
                }
                if (recovery > 0.0f) {
                    out.append(String.format(Locale.US, "   §8↳ §7%s: §a%.1f%%\n",
                            SkillOverviewText.get("gui.simpleskills.tab.material_recovery"), recovery));
                } else {
                    out.append(String.format("   §8↳ §7%s: %s\n",
                            SkillOverviewText.get("gui.simpleskills.tab.material_recovery"), lockedAt(25)));
                }
            }
            default -> {
            }
        }

        List<String> lines = new ArrayList<>();
        for (String line : out.toString().split("\n")) {
            // Effect lines are flush with the panel text; the "   " indent is dropped here.
            String trimmed = line.replaceFirst("^\\s+", "");
            if (!trimmed.isEmpty()) lines.add(trimmed);
        }
        return lines;
    }

    private static void appendAgilityAbility(StringBuilder out, String abilityKey, int requiredLevel, int skillLevel) {
        out.append("   §8↳ §7").append(SkillOverviewText.get("gui.simpleskills.tab." + abilityKey)).append(": ");
        if (skillLevel >= requiredLevel) {
            double multiplier;
            if (abilityKey.equals("agility_dash")) {
                double progress = clamp01((skillLevel - DASH_BASE_LEVEL) / 84.0);
                multiplier = (0.35 + 0.65 * progress) / 0.35;
            } else {
                double progress = clamp01((skillLevel - requiredLevel) / (double) Math.max(1, 40 - requiredLevel));
                multiplier = (0.5 + 0.5 * progress) / 0.5;
            }
            out.append("§a").append(String.format(Locale.US, "%.2fx", multiplier)).append("\n");
        } else {
            out.append(lockedAt(requiredLevel)).append("\n");
        }
    }

    /** Next index after {@code level} whose table value differs from the value at {@code level}, or -1. */
    private static int nextStep(double[] table, int level) {
        for (int l = level + 1; l < table.length; l++) {
            if (table[l] != table[level]) return l;
        }
        return -1;
    }

    private static String lockedAt(int level) {
        return "§c" + SkillOverviewText.get("gui.simpleskills.tab.locked") + " §7("
                + SkillOverviewText.get("gui.simpleskills.tab.level_abbr") + " " + level + ")";
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static String toRoman(int level) {
        return switch (level) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> "—";
        };
    }

    // ----- balance formulas (mirrored from the game logic) -----

    private static int unlockedEnchantmentLevel(int professionLevel) {
        if (professionLevel < ENCHANT_MIN_LEVEL) return 0;
        int safe = Math.min(ENCHANT_MAX_LEVEL, professionLevel);
        return Math.min(ENCHANT_MAX_UNLOCKED, (safe / 10) + 1);
    }

    private static int veinMiningBlocks(int level) {
        if (level < VEIN_FIRST_LEVEL) return 0;
        return Math.min((level / VEIN_INTERVAL) * VEIN_PER_TIER, VEIN_MAX);
    }

    /** Fishing level benefits: 0.0 at level 1, 1.0 at level 99. */
    private static double fishingFraction(int level) {
        return Math.max(0.0, Math.min(1.0, (level - 1) / (double) (MAX_LEVEL - 1)));
    }

    private static double attractionPercent(int level) { return 1.5 * fishingFraction(level) * 100.0; }
    private static double fatiguePercent(int level) { return 1.0 * fishingFraction(level) * 100.0; }
    private static double tensionPercent(int level) { return 0.40 * fishingFraction(level) * 100.0; }
    private static double recoveryPercent(int level) { return 1.0 * fishingFraction(level) * 100.0; }

    /** Points: -1 locked, 4 (5-9), 3 (10-14), 2 (15-19), 0 (20+ free). Label: half-bars. */
    private static String boneMealCostLabel(int farmingLevel) {
        if (farmingLevel < FARM_UNLOCK_LEVEL) return "LOCKED";
        int points = farmingLevel < 10 ? 4 : farmingLevel < 15 ? 3 : farmingLevel < 20 ? 2 : 0;
        if (points == 0) return "FREE";
        return (points % 2 == 0)
                ? Integer.toString(points / 2)
                : String.format(Locale.US, "%.1f", points / 2.0);
    }

    // =====================================================================================
    // Rendering and input
    // =====================================================================================

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 4, top - 4, left + WIDTH + 4, top + HEIGHT + 4, 0xCC080706);
        g.fill(left - 3, top - 3, left + WIDTH + 3, top + HEIGHT + 3, GOLD_DARK);
        g.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, GOLD);
        g.fill(left, top, left + WIDTH, top + HEIGHT, FRAME_BG);
        g.fill(left + 6, top + 6, left + WIDTH - 6, top + HEIGHT - 6, PANEL_BG);
        drawCornerGems(g);

        g.centeredText(font, Component.literal(title), left + WIDTH / 2, top + 12, GOLD);
        g.centeredText(font, Component.literal(subtitle), left + WIDTH / 2, top + 24, MUTED);
        g.text(font, Component.literal(total), left + 14, top + 40, TEXT, false);
        String right = ironmanText.isEmpty() ? prestigeText
                : prestigeText.isEmpty() ? ironmanText : ironmanText + "   " + prestigeText;
        if (!right.isEmpty()) {
            g.text(font, Component.literal(right), left + WIDTH - 14 - font.width(right), top + 40, GOLD, false);
        }
        g.fill(left + 12, top + 52, left + WIDTH - 12, top + 53, GOLD_DARK);

        drawGrid(g, mouseX, mouseY);
        drawDetail(g);
    }

    private void drawCornerGems(GuiGraphicsExtractor g) {
        int[][] corners = {
                {left + 3, top + 3}, {left + WIDTH - 3, top + 3},
                {left + 3, top + HEIGHT - 3}, {left + WIDTH - 3, top + HEIGHT - 3}
        };
        for (int[] c : corners) {
            g.fill(c[0] - 2, c[1], c[0] + 3, c[1] + 1, GOLD);
            g.fill(c[0], c[1] - 2, c[0] + 1, c[1] + 3, GOLD);
        }
    }

    private int cardX(int index) { return left + GRID_LEFT + (index % COLUMNS) * (CARD_W + GAP); }
    private int cardY(int index) { return top + GRID_TOP + (index / COLUMNS) * (CARD_H + GAP); }

    private void drawGrid(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            int x = cardX(i);
            int y = cardY(i);
            boolean isSelected = i == selected;
            boolean hover = inside(mouseX, mouseY, x, y, CARD_W, CARD_H);

            g.fill(x - 1, y - 1, x + CARD_W + 1, y + CARD_H + 1, isSelected ? GOLD : GOLD_DARK);
            g.fill(x, y, x + CARD_W, y + CARD_H, isSelected ? CARD_SELECTED : hover ? CARD_HOVER : CARD_BG);
            g.item(entry.icon(), x + 5, y + 6);
            String name = font.plainSubstrByWidth(entry.name(), CARD_W - 28);
            g.text(font, Component.literal(name), x + 24, y + 6, isSelected ? GOLD : TEXT, false);
            String level = entry.max() ? "MAX"
                    : SkillOverviewText.get("gui.simpleskills.tab.level_abbr") + " " + entry.level();
            g.text(font, Component.literal(level), x + 24, y + 17, MUTED, false);
            drawBar(g, x + 4, y + CARD_H - 7, CARD_W - 8, 3, entry.progress(), BAR_PROGRESS);
        }
    }

    private void drawDetail(GuiGraphicsExtractor g) {
        int x = left + DETAIL_LEFT;
        int y = top + GRID_TOP;
        g.fill(x - 1, y - 1, x + DETAIL_W + 1, y + DETAIL_H + 1, GOLD_DARK);
        g.fill(x, y, x + DETAIL_W, y + DETAIL_H, CARD_BG);
        if (entries.isEmpty()) return;

        Entry entry = entries.get(selected);
        g.pose().pushMatrix();
        g.pose().translate(x + 8, y + 8);
        g.pose().scale(3.0f, 3.0f);
        g.item(entry.icon(), 0, 0);
        g.pose().popMatrix();

        g.text(font, Component.literal(font.plainSubstrByWidth(entry.name(), DETAIL_W - 58)),
                x + 58, y + 8, GOLD, false);
        g.text(font, parseLegacy(entry.rank()), x + 58, y + 20, TEXT, false);
        String level = entry.max() ? "MAX"
                : SkillOverviewText.get("gui.simpleskills.tab.level_abbr") + " " + entry.level() + " / " + MAX_LEVEL;
        g.text(font, Component.literal(level), x + 58, y + 32, MUTED, false);

        drawBar(g, x + 8, y + 56, DETAIL_W - 16, 8, entry.progress(), BAR_XP);
        g.text(font, parseLegacy(entry.xpText()), x + 8, y + 68, TEXT, false);
        g.fill(x + 8, y + 82, x + DETAIL_W - 8, y + 83, GOLD_DARK);

        int lineY = y + 90;
        for (String line : entry.buffs()) {
            if (lineY > y + DETAIL_H - 10) break;
            g.text(font, parseLegacy(line), x + 8, lineY, TEXT, false);
            lineY += 11;
        }
    }

    private static void drawBar(GuiGraphicsExtractor g, int x, int y, int w, int h, double progress, int color) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, BAR_BORDER);
        g.fill(x, y, x + w, y + h, BAR_BACKGROUND);
        int filled = (int) Math.round(w * Math.max(0.0, Math.min(1.0, progress)));
        if (filled > 0) g.fill(x, y, x + filled, y + h, color);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static ItemStack iconFor(String id) {
        Item item = switch (id) {
            case "mining" -> Items.IRON_PICKAXE;
            case "woodcutting" -> Items.IRON_AXE;
            case "excavating" -> Items.IRON_SHOVEL;
            case "farming" -> Items.WHEAT;
            case "fishing" -> Items.FISHING_ROD;
            case "defense" -> Items.SHIELD;
            case "slaying" -> Items.DIAMOND_SWORD;
            case "ranged" -> Items.BOW;
            case "enchanting" -> Items.ENCHANTING_TABLE;
            case "alchemy" -> Items.BREWING_STAND;
            case "smithing" -> Items.ANVIL;
            case "cooking" -> Items.SMOKER;
            case "crafting" -> Items.CRAFTING_TABLE;
            case "agility" -> Items.FEATHER;
            case "prayer" -> Items.BELL;
            default -> Items.BOOK;
        };
        return new ItemStack(item);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (int i = 0; i < entries.size(); i++) {
            if (inside(event.x(), event.y(), cardX(i), cardY(i), CARD_W, CARD_H)) {
                selected = i;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        if (activeScreen == this) activeScreen = null;
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Converts legacy § colour codes into a styled component. */
    static Component parseLegacy(String raw) {
        MutableComponent out = Component.empty();
        if (raw == null || raw.isEmpty()) return out;
        StringBuilder buffer = new StringBuilder();
        ChatFormatting color = null;
        boolean bold = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\u00A7' && i + 1 < raw.length()) {
                flush(out, buffer, color, bold);
                char code = Character.toLowerCase(raw.charAt(++i));
                ChatFormatting format = ChatFormatting.getByCode(code);
                if (code == 'r') {
                    color = null;
                    bold = false;
                } else if (format != null && COLOR_CODES.indexOf(code) >= 0) {
                    color = format;
                    bold = false;
                } else if (code == 'l') {
                    bold = true;
                }
            } else {
                buffer.append(c);
            }
        }
        flush(out, buffer, color, bold);
        return out;
    }

    private static void flush(MutableComponent out, StringBuilder buffer, ChatFormatting color, boolean bold) {
        if (buffer.isEmpty()) return;
        Style style = Style.EMPTY.withBold(bold);
        if (color != null) style = style.withColor(color);
        out.append(Component.literal(buffer.toString()).withStyle(style));
        buffer.setLength(0);
    }
}
