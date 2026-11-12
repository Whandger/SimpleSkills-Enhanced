package com.github.ob_yekt.simpleskills.ui;

import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.agility.AgilityRank;
import com.github.ob_yekt.simpleskills.enchanting.EnchantingRank;
import com.github.ob_yekt.simpleskills.excavating.ExcavatingRank;
import com.github.ob_yekt.simpleskills.managers.XPManager;
import com.github.ob_yekt.simpleskills.managers.DatabaseManager;
import com.github.ob_yekt.simpleskills.managers.NamePrefixManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Builds the unlocked, selectable profession-title list and its clickable chat menu. */
public final class RankTitleManager {
    public record TitleOption(String id, Skills skill, int requiredLevel, String translationKey) {}

    private static final List<Skills> TEN_RANK_SKILLS = List.of(
            Skills.MINING, Skills.WOODCUTTING, Skills.FARMING, Skills.SLAYING, Skills.RANGED,
            Skills.DEFENSE, Skills.ALCHEMY, Skills.SMITHING, Skills.COOKING, Skills.CRAFTING, Skills.PRAYER);
    private static final int[] FISHING_RANK_LEVELS = {1, 25, 50, 75, 99};
    private static final String[] FISHING_RANK_KEYS = {"novice", "journeyman", "artisan", "expert", "grandmaster"};

    private RankTitleManager() {}

    public static void registerPayloads() {
        PayloadTypeRegistry.serverboundPlay().register(RankTitleMenuActionPayload.TYPE, RankTitleMenuActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RankTitleMenuDataPayload.TYPE, RankTitleMenuDataPayload.CODEC);
    }

    public static void registerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(RankTitleMenuActionPayload.TYPE, (payload, context) ->
                context.server().execute(() -> handleAction(context.player(), payload)));
    }

    private static void handleAction(ServerPlayer player, RankTitleMenuActionPayload payload) {
        if (player == null || payload == null) return;
        DatabaseManager db = DatabaseManager.getInstance();
        switch (payload.action()) {
            case RankTitleMenuActionPayload.OPEN -> sendMenu(player, 0);
            case RankTitleMenuActionPayload.SELECT -> {
                if (payload.titleId() == null || payload.titleId().length() > 64) {
                    sendMenu(player, 3);
                    return;
                }
                TitleOption option = find(payload.titleId());
                if (option == null) {
                    sendMenu(player, 3);
                    return;
                }
                if (!isUnlocked(player, option)) {
                    sendMenu(player, 4);
                    return;
                }
                db.setSelectedTitleKey(player.getStringUUID(), option.id());
                NamePrefixManager.updatePlayerNameDecorations(player);
                sendMenu(player, 1);
            }
            case RankTitleMenuActionPayload.CLEAR -> {
                db.setSelectedTitleKey(player.getStringUUID(), null);
                NamePrefixManager.updatePlayerNameDecorations(player);
                sendMenu(player, 2);
            }
            default -> { }
        }
    }

    private static void sendMenu(ServerPlayer player, int status) {
        if (!ServerPlayNetworking.canSend(player, RankTitleMenuDataPayload.TYPE)) return;
        JsonArray rows = new JsonArray();
        for (TitleOption option : allOptions()) {
            if (!isUnlocked(player, option)) continue;
            String skill = option.skill().name().toLowerCase(Locale.ROOT);
            JsonObject row = new JsonObject();
            row.addProperty("id", option.id());
            row.addProperty("skill", skill);
            row.addProperty("skill_name", SkillTabTranslations.get(player, "skill.simpleskills.name." + skill));
            row.addProperty("name", getLocalizedTitle(player, option));
            row.addProperty("level", option.requiredLevel());
            rows.add(row);
        }
        JsonObject labels = new JsonObject();
        for (String label : List.of("title", "hint", "professions", "unlocked_count", "rank", "level", "equipped",
                "none", "clear", "close", "waiting", "equipped_status", "cleared_status", "invalid", "locked", "empty")) {
            String key = "gui.simpleskills.rank_titles." + label;
            labels.addProperty(label, SkillTabTranslations.get(player, key));
        }
        String rowsJson = rows.toString();
        String labelsJson = labels.toString();
        if (rowsJson.length() > 24_000 || labelsJson.length() > 8_000) return;
        String selected = DatabaseManager.getInstance().getSelectedTitleKey(player.getStringUUID());
        ServerPlayNetworking.send(player, new RankTitleMenuDataPayload(
                rowsJson, labelsJson, selected == null ? "" : selected, status));
    }

    public static String getLocalizedTitle(ServerPlayer player, TitleOption option) {
        return option == null ? "" : SkillTabTranslations.get(player, option.translationKey());
    }

    public static List<TitleOption> allOptions() {
        List<TitleOption> options = new ArrayList<>();
        for (Skills skill : Skills.values()) {
            String profession = skill.name().toLowerCase(Locale.ROOT);
            if (TEN_RANK_SKILLS.contains(skill)) {
                addTenRanks(options, skill, profession);
            } else if (skill == Skills.AGILITY) {
                AgilityRank[] ranks = AgilityRank.values();
                for (int i = 0; i < ranks.length; i++) {
                    AgilityRank rank = ranks[i];
                    add(options, skill, i + 1, rank.getMinimumLevel(),
                            "skill.simpleskills.rank.agility." + rank.name().toLowerCase(Locale.ROOT));
                }
            } else if (skill == Skills.EXCAVATING) {
                ExcavatingRank[] ranks = ExcavatingRank.values();
                for (int i = 0; i < ranks.length; i++) {
                    ExcavatingRank rank = ranks[i];
                    add(options, skill, i + 1, rank.getMinLevel(),
                            "skill.simpleskills.rank.excavating." + rank.name().toLowerCase(Locale.ROOT));
                }
            } else if (skill == Skills.ENCHANTING) {
                EnchantingRank[] ranks = EnchantingRank.values();
                for (int i = 0; i < ranks.length; i++) {
                    EnchantingRank rank = ranks[i];
                    add(options, skill, i + 1, rank.getMinimumLevel(),
                            "skill.simpleskills.rank.enchanting." + rank.name().toLowerCase(Locale.ROOT));
                }
            } else if (skill == Skills.FISHING) {
                for (int i = 0; i < FISHING_RANK_LEVELS.length; i++) {
                    add(options, skill, i + 1, FISHING_RANK_LEVELS[i],
                            "skill.simpleskills.rank.fishing." + FISHING_RANK_KEYS[i]);
                }
            }
        }
        return List.copyOf(options);
    }

    private static void addTenRanks(List<TitleOption> options, Skills skill, String profession) {
        for (int rank = 1; rank <= 10; rank++) {
            int requiredLevel = rank == 1 ? 1 : (rank - 1) * 10;
            add(options, skill, rank, requiredLevel,
                    "skill.simpleskills.rank." + profession + ".rank_" + rank);
        }
    }

    private static void add(List<TitleOption> options, Skills skill, int rank, int requiredLevel, String translationKey) {
        options.add(new TitleOption(skill.name().toLowerCase(Locale.ROOT) + "_rank_" + rank,
                skill, requiredLevel, translationKey));
    }

    public static TitleOption find(String id) {
        if (id == null) return null;
        return allOptions().stream().filter(option -> option.id().equals(id)).findFirst().orElse(null);
    }

    public static boolean isUnlocked(ServerPlayer player, TitleOption option) {
        return option != null && XPManager.getSkillLevel(player.getStringUUID(), option.skill()) >= option.requiredLevel();
    }

    public static void showMenu(ServerPlayer player) {
        sendMenu(player, 0);
    }
}
