package com.github.ob_yekt.simpleskills.mixin.DEFENSE;

/**
 * Legacy placeholder retained so projects that overlay this patch do not keep
 * compiling the previous ItemStack constructor injection.
 *
 * Glowstone shield blocking is now registered directly through the delayed
 * BLOCKS_ATTACKS component in GlowstoneShields.registerShield(). This class
 * is intentionally not a Mixin and must not be listed in simpleskills.mixins.json.
 */
@Deprecated(forRemoval = true)
public final class GlowstoneShieldBlocksMixin {
    private GlowstoneShieldBlocksMixin() {
    }
}
