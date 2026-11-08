package com.github.ob_yekt.simpleskills.fishing;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

/**
 * The individual size of one fish: weight in kilograms and length in centimetres.
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

    /** Rolls a new individual for the species. */
    public static FishAttributes generate(FishSpecies species, RandomSource random) {
        FishSizeModel.Result result = FishSizeModel.generate(species.sizeProfile(), random::nextDouble, random::nextGaussian);
        return new FishAttributes(result.weightKg(), result.lengthCm());
    }

    /**
     * The stored attributes of a fish. A fish that has not been ticked yet gets them rolled (and stored)
     * on the spot, so asking never returns a value that changes later. {@code null} for unsupported mobs.
     */
    public static @Nullable FishAttributes of(Mob fish) {
        if (!(fish instanceof FishAttributeHolder holder)) return null;
        FishAttributes stored = holder.simpleskills$getFishAttributes();
        if (stored != null) return stored;
        FishSpecies species = FishSpecies.of(fish);
        if (species == null || fish.level().isClientSide()) return null;
        FishAttributes rolled = generate(species, fish.getRandom());
        holder.simpleskills$setFishAttributes(rolled);
        return rolled;
    }
}
