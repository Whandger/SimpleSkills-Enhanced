package com.github.ob_yekt.simpleskills.abyss;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

/** Client registrations for the Samurai model, stone shell, emissive layers and renderer. */
public final class AbyssalSamuraiClient {
    private AbyssalSamuraiClient() {
    }

    public static void register() {
        ModelLayerRegistry.registerModelLayer(
                AbyssalSamuraiModel.LAYER_LOCATION,
                AbyssalSamuraiModel::createBodyLayer
        );
        ModelLayerRegistry.registerModelLayer(
                AbyssalSamuraiModel.SHELL_LAYER_LOCATION,
                AbyssalSamuraiModel::createStoneShellLayer
        );
        for (AbyssalSamuraiArmorModel.Piece piece : AbyssalSamuraiArmorModel.Piece.values()) {
            ModelLayerRegistry.registerModelLayer(piece.layerLocation(), piece::createLayer);
        }
        EntityRendererRegistry.register(AbyssalSamuraiEntity.TYPE, AbyssalSamuraiRenderer::new);
    }
}
