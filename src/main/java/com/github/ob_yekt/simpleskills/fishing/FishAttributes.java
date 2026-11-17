package com.github.ob_yekt.simpleskills.fishing;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

/**
 * The individual size of one aquatic animal: weight in kilograms and length in centimetres.
 *
 * <p>Rolled exactly once, the first time a fish of a registered species is ticked on the server (so
 * natural fish and fish added by the ecosystem use the same path), then stored on the entity and saved
 * with it. The fight difficulty, the XP and the catch screen all read these stored values; nothing
 * rolls them again.</p>
 */
public record FishAttributes(float weightKg, float lengthCm) {

    /** NBT keys written on the fish entity. */
    public static final String NBT_WEIGHT = "simpleskills_fish_weight_kg";
    public static final String NBT_LENGTH = "simpleskills_fish_length_cm";

    public boolean isValid() {
        return weightKg > 0.0F && lengthCm > 0.0F && Float.isFinite(weightKg) && Float.isFinite(lengthCm);
    }

    /** Rolls a new individual for a registered catchable species. */
    public static FishAttributes generate(FishSpecies species, RandomSource random) {
        return generate(species.sizeProfile(), random);
    }

    /** Rolls a new individual from any aquatic size profile, including whales and orcas. */
    public static FishAttributes generate(FishSizeModel.Profile profile, RandomSource random) {
        FishSizeModel.Result result = FishSizeModel.generate(profile, random::nextDouble, random::nextGaussian);
        return new FishAttributes(result.weightKg(), result.lengthCm());
    }

    /** Returns the size profile used by this mob, or null when it is not part of the aquatic expansion. */
    public static FishSizeModel.@Nullable Profile profileFor(Mob mob) {
        FishSpecies species = FishSpecies.of(mob);
        if (species != null) return species.sizeProfile();
        if (mob instanceof MarineMammalEntity mammal) return mammal.sizeProfile();
        return null;
    }

    /**
     * Final synchronized scale: species baseline multiplied by the individual's length/weight ratio.
     * Fish are kept between 0.72x and 1.48x; marine mammals use the same individual variation on top
     * of a much larger species baseline.
     */
    public static float visualScaleFor(Mob mob, FishAttributes attributes) {
        FishSizeModel.Profile profile = profileFor(mob);
        if (profile == null || attributes == null || !attributes.isValid()) return 1.0F;

        double individualScale = FishSizeModel.visualScale(profile, attributes.weightKg());
        double speciesScale = mob instanceof MarineMammalEntity mammal
                ? mammal.getBaseVisualScale()
                : 1.0D;
        return (float) (speciesScale * individualScale);
    }

    /**
     * The stored attributes of an aquatic mob. An entity that has not been ticked yet gets them rolled
     * (and stored) on the spot, so asking never returns a value that changes later. {@code null} for unsupported mobs.
     */
    public static @Nullable FishAttributes of(Mob fish) {
        if (!(fish instanceof FishAttributeHolder holder)) return null;
        FishAttributes stored = holder.simpleskills$getFishAttributes();
        if (stored != null) return stored;
        FishSizeModel.Profile profile = profileFor(fish);
        if (profile == null || fish.level().isClientSide()) return null;
        FishAttributes rolled = generate(profile, fish.getRandom());
        holder.simpleskills$setFishAttributes(rolled);
        return rolled;
    }
}
