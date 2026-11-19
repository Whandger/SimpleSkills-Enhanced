package com.github.ob_yekt.simpleskills;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/** Reads Skill Overview text from the mod's language files, in the client's own language. */
final class SkillOverviewText {
    private static final Map<String, JsonObject> LANGUAGES = new ConcurrentHashMap<>();

    private SkillOverviewText() {}

    /** Raw translated text (same keys and fallbacks the TAB used). */
    static String get(String key) {
        String value = lookup(key);
        return value != null ? value : key;
    }

    /** Translated text with Java format placeholders, formatted with US locale. */
    static String format(String key, Object... args) {
        return String.format(Locale.US, get(key), args);
    }

    private static String lookup(String key) {
        String locale = Minecraft.getInstance().getLanguageManager().getSelected()
                .toLowerCase(Locale.ROOT).replace('-', '_');
        String value = read(locale, key);
        if (value == null) value = read(fallback(locale), key);
        if (value == null) value = read("en_us", key);
        return value;
    }

    private static String read(String locale, String key) {
        JsonObject translations = LANGUAGES.computeIfAbsent(locale, SkillOverviewText::readLanguage);
        return translations.has(key) && translations.get(key).isJsonPrimitive()
                ? translations.get(key).getAsString() : null;
    }

    private static JsonObject readLanguage(String locale) {
        Identifier resourceId = Identifier.fromNamespaceAndPath("simpleskills", "lang/" + locale + ".json");
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(resourceId).orElse(null);
            if (resource == null) return new JsonObject();
            try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception ignored) {
            return new JsonObject();
        }
    }

    private static String fallback(String locale) {
        if (locale.startsWith("pt_")) return "pt_br";
        if (locale.startsWith("es_")) return "es_es";
        if (locale.startsWith("fr_")) return "fr_fr";
        if (locale.startsWith("ar_")) return "ar_sa";
        return "en_us";
    }
}
