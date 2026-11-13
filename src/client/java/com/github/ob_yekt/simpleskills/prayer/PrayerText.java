package com.github.ob_yekt.simpleskills.prayer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Reads Prayer UI strings directly from the mod's lang resources, with an English fallback. */
final class PrayerText {
    private static final Map<String, JsonObject> LANGUAGES = new ConcurrentHashMap<>();

    private PrayerText() {}

    static Component text(String key, Object... args) {
        String locale = Minecraft.getInstance().getLanguageManager().getSelected();
        String value = lookup(locale, key);
        if (value == null) value = lookup("en_us", key);
        if (value == null) return Component.literal(key);

        for (Object arg : args) {
            String replacement = arg instanceof Component component ? component.getString() : String.valueOf(arg);
            int placeholder = value.indexOf("%s");
            if (placeholder < 0) placeholder = value.indexOf("%d");
            if (placeholder >= 0) value = value.substring(0, placeholder) + replacement + value.substring(placeholder + 2);
        }
        return Component.literal(value);
    }

    private static String lookup(String locale, String key) {
        JsonObject translations = LANGUAGES.computeIfAbsent(locale, PrayerText::readLanguage);
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
}
