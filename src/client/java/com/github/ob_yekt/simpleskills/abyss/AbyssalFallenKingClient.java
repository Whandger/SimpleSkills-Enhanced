package com.github.ob_yekt.simpleskills.abyss;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

/** Client registration for the custom 3D Abyssal Fallen King model and renderer. */
public final class AbyssalFallenKingClient {
    private AbyssalFallenKingClient() {
    }

    public static void register() {
        ModelLayerRegistry.registerModelLayer(
                AbyssalFallenKingModel.LAYER_LOCATION,
                AbyssalFallenKingModel::createBodyLayer
        );
        EntityRendererRegistry.register(AbyssalFallenKingEntity.TYPE, AbyssalFallenKingRenderer::new);
    }
}
