package com.github.ob_yekt.simpleskills.fishing;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/** A registered aquatic species that can participate in the Fishing minigame. */
public final class FishSpecies {
    private static final Map<EntityType<?>, FishSpecies> BY_TYPE = new LinkedHashMap<>();

    public static final FishSpecies COD = register(builder("cod", EntityTypes.COD, () -> Items.COD)
            .fight(1.00, 1.00).xp(1.00).minLevel(1)
            .size(new FishSizeModel.Profile(35, 110, 1.0e-5, 3.0))
            .spawn(FishSpawnProfile.of(2, 4, 10, false,
                    FishHabitat.RIVER, 0.55, FishHabitat.WARM_OCEAN, 0.80,
                    FishHabitat.TEMPERATE_OCEAN, 1.0, FishHabitat.COLD_OCEAN, 0.85)));

    public static final FishSpecies SALMON = register(builder("salmon", EntityTypes.SALMON, () -> Items.SALMON)
            .fight(1.20, 0.85).xp(1.15).minLevel(1)
            .size(new FishSizeModel.Profile(45, 110, 1.1e-5, 3.0))
            .spawn(FishSpawnProfile.of(2, 5, 10, false,
                    FishHabitat.RIVER, 1.0, FishHabitat.COLD_FRESHWATER, 0.8,
                    FishHabitat.TEMPERATE_FRESHWATER, 0.25, FishHabitat.COLD_OCEAN, 0.30)));

    public static final FishSpecies TROPICAL_FISH = register(builder("tropical_fish", EntityTypes.TROPICAL_FISH, () -> Items.TROPICAL_FISH)
            .fight(0.85, 1.15).xp(1.30).minLevel(1)
            .size(new FishSizeModel.Profile(6, 25, 2.5e-5, 3.0))
            .spawn(FishSpawnProfile.of(2, 3, 12, false, FishHabitat.WARM_OCEAN, 1.25))
            .variant(mob -> mob instanceof TropicalFishVariantAccess access ? access.simpleskills$getVariant() : 0,
                    (mob, variant) -> {
                        if (mob instanceof TropicalFishVariantAccess access) access.simpleskills$setVariant(variant);
                    }));

    public static final FishSpecies PUFFERFISH = register(builder("pufferfish", EntityTypes.PUFFERFISH, () -> Items.PUFFERFISH)
            .fight(1.10, 0.90).xp(1.50).minLevel(25)
            .size(new FishSizeModel.Profile(15, 45, 5.0e-5, 3.0))
            .spawn(FishSpawnProfile.of(3, 5, 5, false,
                    FishHabitat.WARM_OCEAN, 0.75, FishHabitat.TEMPERATE_OCEAN, 0.25)));

    private final String id;
    private final EntityType<? extends Mob> entityType;
    private final Supplier<Item> item;
    private final double pullStrength;
    private final double fatigueFactor;
    private final double xpMultiplier;
    private final int defaultMinLevel;
    private final FishSizeModel.Profile size;
    private final FishSpawnProfile spawnProfile;
    private final boolean ambientSpawnable;
    private final @Nullable MouthAnchor mouthAnchor;
    private final @Nullable ToIntFunction<Mob> variantGetter;
    private final @Nullable VariantSetter variantSetter;

    /** Position of the mouth relative to the entity's width and height. */
    public record MouthAnchor(double forwardWidth, double verticalHeight) {
        public MouthAnchor {
            if (!Double.isFinite(forwardWidth) || !Double.isFinite(verticalHeight)
                    || forwardWidth < 0.0 || forwardWidth > 2.0
                    || verticalHeight < 0.0 || verticalHeight > 1.5) {
                throw new IllegalArgumentException("Invalid fish mouth anchor");
            }
        }
    }

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
        this.spawnProfile = b.spawnProfile;
        this.ambientSpawnable = b.ambientSpawnable;
        this.mouthAnchor = b.mouthAnchor;
        this.variantGetter = b.variantGetter;
        this.variantSetter = b.variantSetter;
    }

    public static Builder builder(String id, EntityType<? extends Mob> entityType, Supplier<Item> item) {
        return new Builder(id, entityType, item);
    }

    public static synchronized FishSpecies register(Builder builder) {
        FishSpecies species = new FishSpecies(builder);
        if (BY_TYPE.containsKey(species.entityType)) {
            throw new IllegalStateException("Fish species already registered for " + species.entityType);
        }
        for (FishSpecies other : BY_TYPE.values()) {
            if (other.id.equals(species.id)) throw new IllegalStateException("Fish species id already in use: " + species.id);
        }
        BY_TYPE.put(species.entityType, species);
        return species;
    }

    public static @Nullable FishSpecies of(Entity entity) {
        return BY_TYPE.get(entity.getType());
    }

    public static Collection<FishSpecies> all() {
        return Collections.unmodifiableCollection(BY_TYPE.values());
    }

    /** Picks a species using habitat/depth weights. The predicate is used for level and population rules. */
    public static @Nullable FishSpecies pickWeighted(FishHabitat habitat, int depth,
                                                      DoubleSupplier random, Predicate<FishSpecies> eligible) {
        List<Map.Entry<FishSpecies, Double>> candidates = new ArrayList<>();
        double total = 0.0;
        for (FishSpecies species : BY_TYPE.values()) {
            if (!eligible.test(species)) continue;
            double weight = species.spawnProfile.weightFor(habitat, depth)
                    * FishingConfig.spawnWeightMultiplier(species);
            if (weight <= 0.0) continue;
            candidates.add(Map.entry(species, weight));
            total += weight;
        }
        if (total <= 0.0) return null;
        double roll = Math.max(0.0, Math.min(Math.nextDown(1.0), random.getAsDouble())) * total;
        for (Map.Entry<FishSpecies, Double> candidate : candidates) {
            roll -= candidate.getValue();
            if (roll < 0.0) return candidate.getKey();
        }
        return candidates.get(candidates.size() - 1).getKey();
    }

    public String configKey() { return id; }
    public EntityType<? extends Mob> entityType() { return entityType; }
    public Item item() { return item.get(); }
    public double pullStrength() { return pullStrength; }
    public double fatigueFactor() { return fatigueFactor; }
    public double xpMultiplier() { return xpMultiplier; }
    public int defaultMinLevel() { return defaultMinLevel; }
    public FishSizeModel.Profile sizeProfile() { return size; }
    public FishSpawnProfile spawnProfile() { return spawnProfile; }
    public boolean isAmbientSpawnable() { return ambientSpawnable; }
    public @Nullable MouthAnchor mouthAnchor() { return mouthAnchor; }
    public String descriptionId() { return entityType.getDescriptionId(); }
    public boolean hasVariant() { return variantGetter != null; }
    public int variantOf(Mob fish) { return variantGetter == null ? 0 : variantGetter.applyAsInt(fish); }
    public void applyVariant(Mob fish, int variant) { if (variantSetter != null) variantSetter.set(fish, variant); }

    @Override public String toString() { return id; }

    public static final class Builder {
        private final String id;
        private final EntityType<? extends Mob> entityType;
        private final Supplier<Item> item;
        private double pull = 1.0;
        private double fatigue = 1.0;
        private double xp = 1.0;
        private int minLevel = 1;
        private FishSizeModel.Profile size = new FishSizeModel.Profile(20, 60, 1.0e-5, 3.0);
        private FishSpawnProfile spawnProfile = FishSpawnProfile.of(1, 2, 8, false, FishHabitat.TEMPERATE_FRESHWATER, 1.0);
        private boolean ambientSpawnable;
        private @Nullable MouthAnchor mouthAnchor;
        private @Nullable ToIntFunction<Mob> variantGetter;
        private @Nullable VariantSetter variantSetter;

        private Builder(String id, EntityType<? extends Mob> entityType, Supplier<Item> item) {
            this.id = id;
            this.entityType = entityType;
            this.item = item;
        }

        public Builder fight(double pullStrength, double fatigueFactor) { this.pull = pullStrength; this.fatigue = fatigueFactor; return this; }
        public Builder xp(double multiplier) { this.xp = multiplier; return this; }
        public Builder minLevel(int level) { this.minLevel = level; return this; }
        public Builder size(FishSizeModel.Profile profile) { this.size = profile; return this; }
        public Builder spawn(FishSpawnProfile profile) { this.spawnProfile = profile; return this; }
        /** Whether the separate ambient spawner may naturally add this custom species near players. */
        public Builder ambientSpawn(boolean enabled) { this.ambientSpawnable = enabled; return this; }
        public Builder mouth(double forwardWidth, double verticalHeight) { this.mouthAnchor = new MouthAnchor(forwardWidth, verticalHeight); return this; }
        public Builder variant(ToIntFunction<Mob> getter, VariantSetter setter) { this.variantGetter = getter; this.variantSetter = setter; return this; }
    }
}
