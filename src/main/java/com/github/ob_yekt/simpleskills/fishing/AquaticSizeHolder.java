package com.github.ob_yekt.simpleskills.fishing;

/**
 * Implemented by custom aquatic entities to synchronize their individual visual/body scale
 * from the server to tracking clients.
 */
public interface AquaticSizeHolder {
    float simpleskills$getAquaticScale();

    void simpleskills$setAquaticScale(float scale);
}
