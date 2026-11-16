package com.github.ob_yekt.simpleskills.enchanting;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Reads Rune Infusion UI text directly from the mod's language resources. */
final class RuneInfusionText {
    private static final Map<String, JsonObject> LANGUAGES = new ConcurrentHashMap<>();

    private RuneInfusionText() {}

    static Component text(String key, Object... args) {
        String locale = Minecraft.getInstance().getLanguageManager().getSelected()
                .toLowerCase(java.util.Locale.ROOT).replace('-', '_');
        String value = lookup(locale, key);
        if (value == null) value = lookup(languageFallback(locale), key);
        if (value == null) value = lookup("en_us", key);
        if (value == null) return Component.literal(key);

        for (Object arg : args) {
            String replacement = arg instanceof Component component ? component.getString() : String.valueOf(arg);
            int placeholder = value.indexOf("%s");
            if (placeholder < 0) placeholder = value.indexOf("%d");
            if (placeholder < 0) break;
            value = value.substring(0, placeholder) + replacement + value.substring(placeholder + 2);
        }
        return Component.literal(value.replace("%%", "%"));
    }

    private static String lookup(String locale, String key) {
        JsonObject translations = LANGUAGES.computeIfAbsent(locale, RuneInfusionText::readLanguage);
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

    private static String languageFallback(String locale) {
        if (locale.startsWith("pt_")) return "pt_br";
        if (locale.startsWith("es_")) return "es_es";
        if (locale.startsWith("fr_")) return "fr_fr";
        if (locale.startsWith("ar_")) return "ar_sa";
        return "en_us";
    }
}
