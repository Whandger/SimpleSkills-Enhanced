package com.github.ob_yekt.simpleskills.abyss;

import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;

/** Registers the corruption spikes layer on every living-entity renderer. */
public final class CrimsonBlightClient {
    private CrimsonBlightClient() {}

    public static void register() {
        ModelLayerRegistry.registerModelLayer(
                CrimsonBlightModel.LAYER_LOCATION,
                CrimsonBlightModel::createLayer
        );

        LivingEntityRenderLayerRegistrationCallback.EVENT.register(
                (entityType, entityRenderer, registrationHelper, context) -> {
                    if (entityRenderer instanceof LivingEntityRenderer<?, ?, ?> livingRenderer) {
                        registrationHelper.register(
                                new CrimsonBlightLayer(livingRenderer, context.getModelSet())
                        );
                    }
                }
        );
    }
}
