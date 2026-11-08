package com.github.ob_yekt.simpleskills.fishing;

/** Implemented by {@code TropicalFishVariantMixin}: access to the (private) packed variant of a tropical fish. */
public interface TropicalFishVariantAccess {
    int simpleskills$getVariant();

    void simpleskills$setVariant(int packedVariant);
}
