package com.github.ob_yekt.simpleskills.abyss.weapon;

import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.EntityTypes;

/** Registers the extra third-person player layer. */
public final class KatanaClientRendering {
    private KatanaClientRendering() {}

    public static void register() {
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, entityRenderer,
                                                                      registrationHelper, context) -> {
            if (entityType == EntityTypes.PLAYER && entityRenderer instanceof AvatarRenderer<?> avatarRenderer) {
                registrationHelper.register(new KatanaPlayerRenderLayer(avatarRenderer));
            }
        });
    }
}
