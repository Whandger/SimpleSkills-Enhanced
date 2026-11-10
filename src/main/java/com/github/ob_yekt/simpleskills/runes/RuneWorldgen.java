package com.github.ob_yekt.simpleskills.runes;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Adds the data-driven Rune ore features to newly generated Overworld chunks. */
public final class RuneWorldgen {
    private RuneWorldgen() {}

    public static final ResourceKey<PlacedFeature> RUNE_ORE_PLACED = ResourceKey.create(
            Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "rune_ore"));
    public static final ResourceKey<PlacedFeature> DEEPSLATE_RUNE_ORE_PLACED = ResourceKey.create(
            Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "deepslate_rune_ore"));

    private static boolean registered;

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES, RUNE_ORE_PLACED);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES, DEEPSLATE_RUNE_ORE_PLACED);
        Simpleskills.LOGGER.info("Registered Rune ore world generation in Overworld biomes.");
    }
}
