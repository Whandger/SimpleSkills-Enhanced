package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {

    private boolean customLootEnabled = true;

    /** Vanilla structure-reference radius, in chunks. */
    private static final int VANILLA_REFERENCE_RADIUS = 8;

    /**
     * Default radius used by the Abyss. It is the distance, in chunks, that a structure start
     * can be seen from. The Abyss bridge can only span what this radius reaches, which is
     * 16 blocks per chunk on each side of its start.
     */
    private static final int DEFAULT_REFERENCE_RADIUS = 18;

    /** Upper clamp for the config value. The cost grows with the square of the radius. */
    private static final int MAX_CONFIGURED_RADIUS = 64;

    private static volatile int abyssStructureReferenceRadius = DEFAULT_REFERENCE_RADIUS;

    /**
     * Radius, in chunks, used for both the structure-reference scan and the STRUCTURE_STARTS
     * dependency of the chunk pyramid. Both apply to every Overworld chunk, so raising it
     * increases generation cost for the whole world, not only for the Abyss.
     *
     * <p>Config keys in config/simpleskills/config.json:
     * <ul>
     *   <li>{@code abyss_structure_reference_radius} (int, 8-64, default 18)</li>
     *   <li>{@code abyss_city_extended_structure_references} (boolean, legacy: true = 18, false = 8)</li>
     * </ul>
     */
    public static int getAbyssStructureReferenceRadius() {
        return abyssStructureReferenceRadius;
    }

    @Override
    public void onLoad(String mixinPackage) {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("simpleskills");
        Path configPath = configDir.resolve("config.json");

        if (Files.exists(configPath)) {
            try (FileReader reader = new FileReader(configPath.toFile())) {
                JsonObject json = new Gson().fromJson(reader, JsonObject.class);
                if (json != null) {
                    if (json.has("custom_fishing_loot_enabled")) {
                        customLootEnabled = json.get("custom_fishing_loot_enabled").getAsBoolean();
                    } else if (json.has("features")) {
                        JsonObject features = json.getAsJsonObject("features");
                        if (features != null && features.has("custom_fishing_loot_enabled")) {
                            customLootEnabled = features.get("custom_fishing_loot_enabled").getAsBoolean();
                        }
                    }

                    if (json.has("abyss_city_extended_structure_references")) {
                        boolean extended = json.get("abyss_city_extended_structure_references").getAsBoolean();
                        abyssStructureReferenceRadius = extended
                                ? DEFAULT_REFERENCE_RADIUS
                                : VANILLA_REFERENCE_RADIUS;
                    }

                    if (json.has("abyss_structure_reference_radius")) {
                        int requested = json.get("abyss_structure_reference_radius").getAsInt();
                        abyssStructureReferenceRadius = Math.max(
                                VANILLA_REFERENCE_RADIUS,
                                Math.min(MAX_CONFIGURED_RADIUS, requested));
                    }
                }
                Simpleskills.LOGGER.info("Loaded custom_fishing_loot_enabled: {}", customLootEnabled);
            } catch (Exception e) {
                Simpleskills.LOGGER.error("Error reading config.json in mixin plugin: {}", e.getMessage());
            }
        } else {
            Simpleskills.LOGGER.warn("config.json not found; using default SimpleSkills mixin settings");
        }

        int radius = getAbyssStructureReferenceRadius();
        Simpleskills.LOGGER.info(
                "Abyss structure reference radius: {} chunks (bridge reach per side: {} blocks)",
                radius,
                radius * 16 - 24
        );
        if (radius > DEFAULT_REFERENCE_RADIUS) {
            Simpleskills.LOGGER.warn(
                    "Structure reference radius {} is above the default {}. Generation cost grows with the square of the radius for the whole Overworld.",
                    radius, DEFAULT_REFERENCE_RADIUS
            );
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.equals("com.github.ob_yekt.simpleskills.mixin.FISHING.FishingBobberLootMixin")) {
            return customLootEnabled;
        }
        return true;
    }

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() { return null; }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
