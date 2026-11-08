package com.github.ob_yekt.simpleskills.fishing;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * A fish species that can take part in a fishing session, and the registry of all of them.
 *
 * <p>The four vanilla fish are registered here. Other mods (or this mod's own integrations) can add
 * more through {@link #register(Builder)} without touching the fishing manager: an entity is only
 * ever treated as a fish when its entity type was registered, so no other aquatic creature is
 * caught by accident.</p>
 *
 * <p>A species defines the reward (item), the fight parameters (pull / fatigue), the XP multiplier,
 * the minimum Fishing level and the size profile (length and weight ranges) used to give every
 * individual fish its own size.</p>
 */
public final class FishSpecies {
    private static final Map<EntityType<?>, FishSpecies> BY_TYPE = new LinkedHashMap<>();

    public static final FishSpecies COD = register(builder("cod", EntityTypes.COD, () -> Items.COD)
            .fight(1.00, 1.00).xp(1.00).minLevel(1)
            .size(new FishSizeModel.Profile(35, 110, 1.0e-5, 3.0)));
    public static final FishSpecies SALMON = register(builder("salmon", EntityTypes.SALMON, () -> Items.SALMON)
            .fight(1.20, 0.85).xp(1.15).minLevel(1)
            .size(new FishSizeModel.Profile(45, 110, 1.1e-5, 3.0)));
    public static final FishSpecies TROPICAL_FISH = register(builder("tropical_fish", EntityTypes.TROPICAL_FISH, () -> Items.TROPICAL_FISH)
            .fight(0.85, 1.15).xp(1.30).minLevel(1)
            .size(new FishSizeModel.Profile(6, 25, 2.5e-5, 3.0))
            .variant(mob -> mob instanceof TropicalFishVariantAccess access ? access.simpleskills$getVariant() : 0,
                    (mob, variant) -> {
                        if (mob instanceof TropicalFishVariantAccess access) access.simpleskills$setVariant(variant);
                    }));
    public static final FishSpecies PUFFERFISH = register(builder("pufferfish", EntityTypes.PUFFERFISH, () -> Items.PUFFERFISH)
            .fight(1.10, 0.90).xp(1.50).minLevel(25)
            .size(new FishSizeModel.Profile(15, 45, 5.0e-5, 3.0)));

    private final String id;
    private final EntityType<? extends Mob> entityType;
    private final Supplier<Item> item;
    private final double pullStrength;
    private final double fatigueFactor;
    private final double xpMultiplier;
    private final int defaultMinLevel;
    private final FishSizeModel.Profile size;
    private final @Nullable ToIntFunction<Mob> variantGetter;
    private final @Nullable VariantSetter variantSetter;

    /** Writes a variant (for example the tropical fish pattern) into an entity. */
    @FunctionalInterface
    public interface VariantSetter {
        void set(Mob mob, int variant);
    }

    private FishSpecies(Builder b) {
        this.id = b.id;
        this.entityType = b.entityType;
        this.item = b.item;
        this.pullStrength = b.pull;
        this.fatigueFactor = b.fatigue;
        this.xpMultiplier = b.xp;
        this.defaultMinLevel = b.minLevel;
        this.size = b.size;
        this.variantGetter = b.variantGetter;
        this.variantSetter = b.variantSetter;
    }

    // ---- Registry ---------------------------------------------------------------------------------

    /**
     * Starts the definition of a species. {@code id} is the key used in {@code fishing_species_min_level}
     * (use a unique, lower-case id such as {@code "mymod_trout"}).
     */
    public static Builder builder(String id, EntityType<? extends Mob> entityType, Supplier<Item> item) {
        return new Builder(id, entityType, item);
    }

    /** Registers a species (call it during mod initialization). Registering a type twice is an error. */
    public static synchronized FishSpecies register(Builder builder) {
        FishSpecies species = new FishSpecies(builder);
        if (BY_TYPE.containsKey(species.entityType)) {
            throw new IllegalStateException("Fish species already registered for " + species.entityType);
        }
        for (FishSpecies other : BY_TYPE.values()) {
            if (other.id.equals(species.id)) {
                throw new IllegalStateException("Fish species id already in use: " + species.id);
            }
        }
        BY_TYPE.put(species.entityType, species);
        return species;
    }

    /** Species of an entity, or {@code null} when it is not a registered (supported) fish. */
    public static @Nullable FishSpecies of(Entity entity) {
        return BY_TYPE.get(entity.getType());
    }

    public static Collection<FishSpecies> all() {
        return Collections.unmodifiableCollection(BY_TYPE.values());
    }

    // ---- Data ---------------------------------------------------------------------------------------

    public String configKey() { return id; }
    public EntityType<? extends Mob> entityType() { return entityType; }
    /** The item the player receives for catching this species. */
    public Item item() { return item.get(); }
    /** Multiplier on how hard the fish pulls (line tension). */
    public double pullStrength() { return pullStrength; }
    /** Multiplier on how fast the fish tires (higher = tires faster). */
    public double fatigueFactor() { return fatigueFactor; }
    public double xpMultiplier() { return xpMultiplier; }
    public int defaultMinLevel() { return defaultMinLevel; }
    public FishSizeModel.Profile sizeProfile() { return size; }

    /** Translation key of the species name (as shown by the vanilla entity). */
    public String descriptionId() {
        return entityType.getDescriptionId();
    }

    public boolean hasVariant() {
        return variantGetter != null;
    }

    /** Variant of an individual fish (for example the packed tropical fish pattern), 0 if none. */
    public int variantOf(Mob fish) {
        return variantGetter == null ? 0 : variantGetter.applyAsInt(fish);
    }

    public void applyVariant(Mob fish, int variant) {
        if (variantSetter != null) {
            variantSetter.set(fish, variant);
        }
    }

    @Override
    public String toString() {
        return id;
    }

    // ---- Builder ------------------------------------------------------------------------------------

    public static final class Builder {
        private final String id;
        private final EntityType<? extends Mob> entityType;
        private final Supplier<Item> item;
        private double pull = 1.0;
        private double fatigue = 1.0;
        private double xp = 1.0;
        private int minLevel = 1;
        private FishSizeModel.Profile size = new FishSizeModel.Profile(20, 60, 1.0e-5, 3.0);
        private @Nullable ToIntFunction<Mob> variantGetter;
        private @Nullable VariantSetter variantSetter;

        private Builder(String id, EntityType<? extends Mob> entityType, Supplier<Item> item) {
            this.id = id;
            this.entityType = entityType;
            this.item = item;
        }

        /** Fight parameters: tension growth multiplier and fatigue gain multiplier (1.0 = like a cod). */
        public Builder fight(double pullStrength, double fatigueFactor) {
            this.pull = pullStrength;
            this.fatigue = fatigueFactor;
            return this;
        }

        public Builder xp(double multiplier) {
            this.xp = multiplier;
            return this;
        }

        public Builder minLevel(int level) {
            this.minLevel = level;
            return this;
        }

        /** Length / weight ranges of the species. */
        public Builder size(FishSizeModel.Profile profile) {
            this.size = profile;
            return this;
        }

        /** Species with visual variants (for example tropical fish patterns). */
        public Builder variant(ToIntFunction<Mob> getter, VariantSetter setter) {
            this.variantGetter = getter;
            this.variantSetter = setter;
            return this;
        }
    }
}
