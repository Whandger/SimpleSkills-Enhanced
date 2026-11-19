package com.github.ob_yekt.simpleskills.ui;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.fishing.FishingConfig;
import com.github.ob_yekt.simpleskills.managers.ConfigManager;
import com.github.ob_yekt.simpleskills.managers.DatabaseManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntToDoubleFunction;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Serves the Skill Overview screen (opened with the O key).
 * The server only sends raw data: levels, XP, the unlock levels and the per-level values of the
 * server config tables. Every text, percentage and formula is calculated by the client screen.
 */
public final class SkillOverviewManager {

    private static final int MAX_TABLE_LEVEL = 99;

    private SkillOverviewManager() {}

    public static void registerPayloads() {
        PayloadTypeRegistry.serverboundPlay().register(SkillOverviewRequestPayload.TYPE, SkillOverviewRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SkillOverviewDataPayload.TYPE, SkillOverviewDataPayload.CODEC);
    }

    public static void registerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(SkillOverviewRequestPayload.TYPE, (payload, context) ->
                context.server().execute(() -> send(context.player())));
    }

    public static void send(ServerPlayer player) {
        if (player == null || !ServerPlayNetworking.canSend(player, SkillOverviewDataPayload.TYPE)) return;
        try {
            ServerPlayNetworking.send(player, new SkillOverviewDataPayload(buildSnapshotJson(player)));
        } catch (Exception e) {
            Simpleskills.LOGGER.error("Failed to build skill overview for {}: {}",
                    player.getName().getString(), e.getMessage());
            player.sendSystemMessage(Component.translatable("message.simpleskills.tab.load_error"), false);
        }
    }

    /** Raw snapshot: no text, no percentages, no formulas. */
    public static String buildSnapshotJson(ServerPlayer player) {
        String uuid = player.getStringUUID();
        DatabaseManager db = DatabaseManager.getInstance();
        if (db.getAllSkills(uuid).isEmpty()) {
            db.initializePlayer(uuid);
        }
        Map<String, DatabaseManager.SkillData> skills = db.getAllSkills(uuid);

        JsonObject root = new JsonObject();
        root.addProperty("ironman", db.isPlayerInIronmanMode(uuid));
        root.addProperty("prestige", db.getPrestige(uuid));

        JsonArray skillArray = new JsonArray();
        for (Skills skill : Skills.values()) {
            DatabaseManager.SkillData data = skills.getOrDefault(skill.getId(), new DatabaseManager.SkillData(0, 1));
            JsonObject entry = new JsonObject();
            entry.addProperty("id", skill.name().toLowerCase(Locale.ROOT));
            entry.addProperty("level", data.level());
            entry.addProperty("xp", data.xp());
            skillArray.add(entry);
        }
        root.add("skills", skillArray);

        JsonObject agility = ConfigManager.getAgilityConfig();
        JsonObject config = new JsonObject();
        config.addProperty("fishing_fight", FishingConfig.isFightEnabled());
        config.addProperty("fishing_speed_bonus", ConfigManager.isFishingSpeedBonusEnabled());
        config.addProperty("dash_unlocked_at", intOption(agility, "dash_unlocked_at", 15));
        config.addProperty("double_jump_unlocked_at", intOption(agility, "double_jump_unlocked_at", 10));
        config.add("smithing", table(ConfigManager::getSmithingMultiplier));
        config.add("cooking", table(ConfigManager::getCookingMultiplier));
        config.add("crafting_durability", table(ConfigManager::getCraftingDurabilityMultiplier));
        config.add("crafting_recovery", table(ConfigManager::getCraftingRecoveryChance));
        root.add("config", config);

        return root.toString();
    }

    private static JsonArray table(IntToDoubleFunction valueForLevel) {
        JsonArray array = new JsonArray();
        for (int level = 0; level <= MAX_TABLE_LEVEL; level++) {
            array.add(valueForLevel.applyAsDouble(level));
        }
        return array;
    }

    private static int intOption(JsonObject object, String key, int fallback) {
        try {
            if (object != null && object.has(key)) {
                return Math.max(1, object.get(key).getAsInt());
            }
        } catch (RuntimeException ignored) {
            // Invalid value in the config file: keep the default unlock level.
        }
        return fallback;
    }
}
