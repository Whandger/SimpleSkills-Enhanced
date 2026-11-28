package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.abyss.client.CrimsonColossusFeedback;
import com.github.ob_yekt.simpleskills.abyss.client.CrimsonColossusModel;
import com.github.ob_yekt.simpleskills.abyss.client.CrimsonColossusRenderer;
import com.github.ob_yekt.simpleskills.abyss.entity.CrimsonColossusEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

/** Client registration for the Crimson Colossus model, emissive pass and four attack effects. */
public final class CrimsonColossusClient {
    private CrimsonColossusClient() { }

    public static void register() {
        ModelLayerRegistry.registerModelLayer(
                CrimsonColossusModel.LAYER_LOCATION,
                CrimsonColossusModel::createBodyLayer
        );
        EntityRendererRegistry.register(CrimsonColossusEntity.TYPE, CrimsonColossusRenderer::new);
        ClientTickEvents.END_CLIENT_TICK.register(CrimsonColossusFeedback::tick);
    }
}
