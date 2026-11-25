package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Adds rare Abyss ores, crystal corruption, and solid-block speleothem formations to new Overworld chunks. The ruined castle is placed by its structure set. */
public final class AbyssWorldgen {
    public static final int MIN_Y = -256;
    public static final int MAX_Y = -80;

    private AbyssWorldgen() {}

    public static boolean isAbyssY(int y) {
        return y >= MIN_Y && y <= MAX_Y;
    }

    public static boolean intersectsAbyss(int minY, int maxY) {
        return maxY >= MIN_Y && minY <= MAX_Y;
    }

    /** The Abyss is an Overworld layer, not a rule for other dimensions. */
    public static boolean isOverworld(LevelAccessor level) {
        if (level instanceof ServerLevel serverLevel) {
            return Level.OVERWORLD.equals(serverLevel.dimension());
        }
        if (level instanceof ServerLevelAccessor serverLevelAccessor) {
            return Level.OVERWORLD.equals(serverLevelAccessor.getLevel().dimension());
        }
        if (level instanceof Level world) {
            return Level.OVERWORLD.equals(world.dimension());
        }
        return false;
    }

    private static ResourceKey<PlacedFeature> placed(String path) {
        return ResourceKey.create(Registries.PLACED_FEATURE,
                Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, path));
    }

    public static final ResourceKey<PlacedFeature> ABYSSAL_DIAMOND = placed("abyssal_diamond_ore");
    public static final ResourceKey<PlacedFeature> ABYSSAL_EMERALD = placed("abyssal_emerald_ore");
    public static final ResourceKey<PlacedFeature> ABYSSAL_RUNE = placed("abyssal_rune_ore");
    public static final ResourceKey<PlacedFeature> TRANSCENDENT = placed("transcendent_ore");
    public static final ResourceKey<PlacedFeature> AMETHYST_CORRUPTION = placed("amethyst_corruption");
    public static final ResourceKey<PlacedFeature> FROSTED_ABYSSAL_POCKET = placed("frosted_abyssal_pocket");
    public static final ResourceKey<PlacedFeature> ABYSSAL_SPELEOTHEMS = placed("abyssal_speleothem_clusters");
    public static final ResourceKey<PlacedFeature> ABYSSAL_MONUMENTAL_SPELEOTHEMS = placed("abyssal_monumental_speleothems");
    public static final ResourceKey<PlacedFeature> ABYSSAL_COLOSSAL_SPELEOTHEMS = placed("abyssal_colossal_speleothems");

    private static boolean registered;

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        // Structure and piece types must be registered before worldgen data is read.
        AbyssalCastleStructure.register();
        // The Abyss is an endgame layer: only diamond, emerald, rune, and Transcendent ores spawn here.
        add(ABYSSAL_DIAMOND);
        add(ABYSSAL_EMERALD);
        add(ABYSSAL_RUNE);
        add(TRANSCENDENT);
        add(AMETHYST_CORRUPTION);
        addDecoration(FROSTED_ABYSSAL_POCKET);
        addDecoration(ABYSSAL_SPELEOTHEMS);
        addDecoration(ABYSSAL_MONUMENTAL_SPELEOTHEMS);
        addDecoration(ABYSSAL_COLOSSAL_SPELEOTHEMS);
        Simpleskills.LOGGER.info("Registered The Abyss ore, amethyst, cavern formations, and the ruined castle structure.");
    }

    private static void add(ResourceKey<PlacedFeature> feature) {
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES, feature);
    }

    private static void addDecoration(ResourceKey<PlacedFeature> feature) {
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_DECORATION, feature);
    }
}
