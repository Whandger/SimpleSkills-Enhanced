package com.github.ob_yekt.simpleskills.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/** Reads the player's locale for the text-only TAB overlay, which is sent as a literal component. */
final class SkillTabTranslations {
    private static final Map<String, Map<String, String>> CACHE = new ConcurrentHashMap<>();

    private SkillTabTranslations() {}

    static String get(ServerPlayer player, String key) {
        String locale = normalize(player.clientInformation().language());
        String value = CACHE.computeIfAbsent(locale, SkillTabTranslations::load).get(key);
        if (value == null) {
            String languageFallback = languageFallback(locale);
            if (languageFallback != null) {
                value = CACHE.computeIfAbsent(languageFallback, SkillTabTranslations::load).get(key);
            }
        }
        if (value == null) {
            value = CACHE.computeIfAbsent("en_us", SkillTabTranslations::load).get(key);
        }
        return value != null ? value : key;
    }

    static String format(ServerPlayer player, String key, Object... args) {
        return String.format(Locale.US, get(player, key), args);
    }

    private static Map<String, String> load(String locale) {
        String path = "/assets/simpleskills/lang/" + locale + ".json";
        try (InputStream stream = SkillTabTranslations.class.getResourceAsStream(path)) {
            if (stream == null) return Map.of();
            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, String> result = new java.util.HashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                    result.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
            return Map.copyOf(result);
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private static String normalize(String locale) {
        return locale.toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static String languageFallback(String locale) {
        if (locale.equals("en_us")) return null;
        String language = locale.split("_", 2)[0];
        return switch (language) {
            case "pt" -> "pt_br";
            case "es" -> "es_es";
            case "fr" -> "fr_fr";
            case "ar" -> "ar_sa";
            default -> "en_us";
        };
    }
}
