package com.github.ob_yekt.simpleskills.fishing;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * The vanilla fish that can take part in a fishing session. The species of the real entity decides
 * the main reward, so a fish that looks like a salmon is always delivered as a salmon.
 */
public enum FishSpecies {
    //   config key       pull  fatigue xp    min level
    COD("cod",            1.00, 1.00,   1.00, 1),
    SALMON("salmon",      1.20, 0.85,   1.15, 1),
    TROPICAL_FISH("tropical_fish", 0.85, 1.15, 1.30, 1),
    PUFFERFISH("pufferfish", 1.10, 0.90, 1.50, 25);

    private final String configKey;
    private final double pullStrength;
    private final double fatigueResistance;
    private final double xpMultiplier;
    private final int defaultMinLevel;

    FishSpecies(String configKey, double pullStrength, double fatigueResistance, double xpMultiplier, int defaultMinLevel) {
        this.configKey = configKey;
        this.pullStrength = pullStrength;
        this.fatigueResistance = fatigueResistance;
        this.xpMultiplier = xpMultiplier;
        this.defaultMinLevel = defaultMinLevel;
    }

    public String configKey() { return configKey; }
    /** Multiplier on how hard the fish pulls (line tension). */
    public double pullStrength() { return pullStrength; }
    /** Multiplier on how fast the fish tires (higher = tires faster). */
    public double fatigueFactor() { return fatigueResistance; }
    public double xpMultiplier() { return xpMultiplier; }
    public int defaultMinLevel() { return defaultMinLevel; }

    /** The item the player receives for catching this species. */
    public Item item() {
        return switch (this) {
            case COD -> Items.COD;
            case SALMON -> Items.SALMON;
            case TROPICAL_FISH -> Items.TROPICAL_FISH;
            case PUFFERFISH -> Items.PUFFERFISH;
        };
    }

    public EntityType<? extends AbstractFish> entityType() {
        return switch (this) {
            case COD -> EntityTypes.COD;
            case SALMON -> EntityTypes.SALMON;
            case TROPICAL_FISH -> EntityTypes.TROPICAL_FISH;
            case PUFFERFISH -> EntityTypes.PUFFERFISH;
        };
    }

    /** Translation key of the species name (as shown by the vanilla entity). */
    public String descriptionId() {
        return entityType().getDescriptionId();
    }

    /** Species of a real fish entity, or {@code null} for fish this system does not handle (modded ones). */
    public static @Nullable FishSpecies of(AbstractFish fish) {
        if (fish instanceof Cod) return COD;
        if (fish instanceof Salmon) return SALMON;
        if (fish instanceof TropicalFish) return TROPICAL_FISH;
        if (fish instanceof Pufferfish) return PUFFERFISH;
        return null;
    }
}
