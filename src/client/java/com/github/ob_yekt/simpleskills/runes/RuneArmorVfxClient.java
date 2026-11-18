package com.github.ob_yekt.simpleskills.runes;

/**
 * Legacy armor-particle hook intentionally disabled.
 * Rune enchantment visuals are now rendered exclusively by
 * {@link RuneEquipmentVfxRenderer} as custom equipment-attached geometry.
 * This class is retained to avoid deleting a project file; it emits no particles.
 */
@Deprecated(forRemoval = false)
public final class RuneArmorVfxClient {
    private RuneArmorVfxClient() {}

    /** Kept as a compatibility no-op for any existing registration call. */
    public static void register() { }

    /** Kept as a compatibility no-op. */
    public static void reset() { }
}
