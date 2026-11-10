package com.github.ob_yekt.simpleskills.enchanting;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Configurable, non-invasive enchantment caps. This class never changes registry definitions. */
public final class EnchantmentLimits {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("simpleskills").resolve("enchantment_limits.json");
    private static final Map<String, Integer> DEFAULT_LIMITS = createDefaults();
    private static final Map<String, Integer> LIMITS = new HashMap<>(DEFAULT_LIMITS);

    private EnchantmentLimits() {}

    public static synchronized void reload() {
        LIMITS.clear();
        LIMITS.putAll(DEFAULT_LIMITS);
        try {
            Files.createDirectories(FILE.getParent());
            if (!Files.exists(FILE)) {
                saveDefaults();
                return;
            }
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            JsonObject configured = root.has("limits") && root.get("limits").isJsonObject()
                    ? root.getAsJsonObject("limits") : new JsonObject();
            for (Map.Entry<String, JsonElement> entry : configured.entrySet()) {
                if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber()) {
                    Simpleskills.LOGGER.warn("Ignoring non-numeric enchantment cap for {}", entry.getKey());
                    continue;
                }
                int cap = entry.getValue().getAsInt();
                if (cap < 1 || cap > 255) {
                    Simpleskills.LOGGER.warn("Ignoring out-of-range enchantment cap {} for {} (expected 1-255)", cap, entry.getKey());
                    continue;
                }
                LIMITS.put(normalize(entry.getKey()), cap);
            }
            // Keep the file forward-compatible when new built-in defaults are introduced.
            for (Map.Entry<String, Integer> entry : DEFAULT_LIMITS.entrySet()) {
                String id = entry.getKey();
                if (!configured.has(id)) {
                    configured.addProperty(id, entry.getValue());
                    continue;
                }
                JsonElement existing = configured.get(id);
                if (!existing.isJsonPrimitive() || !existing.getAsJsonPrimitive().isNumber()) {
                    configured.addProperty(id, entry.getValue());
                    continue;
                }
                int existingCap = existing.getAsInt();
                if (existingCap < 1 || existingCap > 255) {
                    configured.addProperty(id, entry.getValue());
                }
            }
            root.add("limits", configured);
            Files.writeString(FILE, GSON.toJson(root));
            Simpleskills.LOGGER.info("Loaded {} enchantment caps from {}", LIMITS.size(), FILE.getFileName());
        } catch (Exception exception) {
            Simpleskills.LOGGER.error("Could not load enchantment_limits.json; using built-in defaults", exception);
        }
    }

    private static void saveDefaults() throws IOException {
        JsonObject root = new JsonObject();
        JsonObject limits = new JsonObject();
        DEFAULT_LIMITS.forEach(limits::addProperty);
        root.addProperty("_comment", "Maximum supported Rune Enchanting level per enchantment. Values are configurable from 1 to 255.");
        root.add("limits", limits);
        Files.writeString(FILE, GSON.toJson(root));
    }

    /** Unknown/third-party enchantments retain their own vanilla max level as a safe fallback. */
    public static int getMaxLevel(String enchantmentId, int vanillaMaxLevel) {
        if (enchantmentId == null) return Math.max(1, vanillaMaxLevel);
        return LIMITS.getOrDefault(normalize(enchantmentId), Math.max(1, vanillaMaxLevel));
    }

    public static int getMaxLevel(String enchantmentId) {
        return LIMITS.getOrDefault(normalize(enchantmentId), 1);
    }

    /** True when the enchantment has an explicit built-in or user-configured cap. */
    public static boolean hasConfiguredLimit(String enchantmentId) {
        return enchantmentId != null && LIMITS.containsKey(normalize(enchantmentId));
    }

    public static Map<String, Integer> snapshot() {
        return Collections.unmodifiableMap(new HashMap<>(LIMITS));
    }

    private static String normalize(String id) {
        String value = id.trim().toLowerCase();
        return value.contains(":") ? value : "minecraft:" + value;
    }

    private static Map<String, Integer> createDefaults() {
        Map<String, Integer> caps = new HashMap<>();
        // General scalable enchantments. These caps are balance defaults, not registry max-level changes.
        for (String id : new String[]{"protection", "fire_protection", "blast_protection", "projectile_protection",
                "sharpness", "smite", "bane_of_arthropods", "impaling", "power", "density", "efficiency",
                "fortune", "looting", "unbreaking", "sweeping_edge", "luck_of_the_sea", "lure", "respiration",
                "piercing", "soul_speed", "knockback", "punch", "fire_aspect", "loyalty", "riptide", "lunge",
                "frost_walker", "thorns", "breach", "quick_charge", "wind_burst", "feather_falling"}) {
            caps.put("minecraft:" + id, 10);
        }
        // Provisional balance/technical caps from the approved design; all are editable in JSON.
        put(caps, 3, "depth_strider");
        put(caps, 5, "swift_sneak", "riptide", "lunge", "knockback", "punch", "wind_burst");
        put(caps, 6, "lure");
        put(caps, 7, "thorns", "breach", "feather_falling");
        put(caps, 4, "quick_charge", "frost_walker");
        // These have no meaningful scalable effect and remain level I.
        put(caps, 1, "silk_touch", "mending", "infinity", "flame", "channeling", "aqua_affinity",
                "multishot", "binding_curse", "vanishing_curse");
        return caps;
    }

    private static void put(Map<String, Integer> map, int cap, String... ids) {
        for (String id : ids) map.put("minecraft:" + id, cap);
    }
}
