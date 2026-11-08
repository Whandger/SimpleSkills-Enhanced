package com.github.ob_yekt.simpleskills.fishing;

import org.jspecify.annotations.Nullable;

/** Implemented by {@code MobFishAttributesMixin}: the individual size stored on a fish entity. */
public interface FishAttributeHolder {
    @Nullable FishAttributes simpleskills$getFishAttributes();

    void simpleskills$setFishAttributes(@Nullable FishAttributes attributes);
}
