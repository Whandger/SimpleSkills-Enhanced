package com.github.ob_yekt.simpleskills.abyss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class CrimsonColossusEmissiveLayer
        extends RenderLayer<CrimsonColossusRenderState, CrimsonColossusModel> {
    private static final Identifier EMISSIVE_TEXTURE = Identifier.fromNamespaceAndPath(
            "simpleskills", "textures/entity/boss/crimson_colossus_emissive.png");

    public CrimsonColossusEmissiveLayer(
            RenderLayerParent<CrimsonColossusRenderState, CrimsonColossusModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                       CrimsonColossusRenderState state, float yRot, float xRot) {
        int alpha = Mth.clamp((int) (state.crystalGlowPulse * 255.0F), 0, 255);
        int color = (alpha << 24) | 0x00FFFFFF;
        collector.order(1).submitModel(
                this.getParentModel(), state, poseStack,
                RenderTypes.eyes(EMISSIVE_TEXTURE), 0xF000F0,
                OverlayTexture.NO_OVERLAY, color, null, state.outlineColor
        );
    }
}
