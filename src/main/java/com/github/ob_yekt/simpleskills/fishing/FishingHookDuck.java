package com.github.ob_yekt.simpleskills.fishing;

/**
 * Implemented by {@code FishingHookFightMixin} so the fishing code can flip the synced
 * "biting" flag of a {@link net.minecraft.world.entity.projectile.FishingHook} (the vanilla bobber
 * dip animation) without reflection.
 */
public interface FishingHookDuck {
    void simpleskills$setBiting(boolean biting);
}
