package com.github.ob_yekt.simpleskills.abyss;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class AbyssalFallenKingEmissiveLayer extends RenderLayer<AbyssalFallenKingRenderState, AbyssalFallenKingModel> {
    private static final Identifier EMISSIVE_TEXTURE =
            Identifier.fromNamespaceAndPath("simpleskills", "textures/entity/abyssal_fallen_king_emissive.png");

    public AbyssalFallenKingEmissiveLayer(RenderLayerParent<AbyssalFallenKingRenderState, AbyssalFallenKingModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight,
                       AbyssalFallenKingRenderState state, float yRot, float xRot) {
        float pulse;
        int minAlpha;
        if (state.isDying) {
            // Phase 1..5: abyssal glow dims gradually as the King loses strength and crumbles
            float fade = 1.0F - Mth.clamp(state.deathProgress * 1.12F, 0.0F, 1.0F);
            pulse = 0.72F * fade * fade;
            minAlpha = 0;
            if (pulse <= 0.015F) {
                return;
            }
        } else if (state.kingState == AbyssalFallenKingEntity.STATE_SITTING) {
            pulse = 0.25F;
            minAlpha = 40;
        } else {
            pulse = 0.78F + 0.22F * Mth.sin(state.ageInTicks * 0.14F);
            minAlpha = 40;
        }

        int alpha = Mth.clamp((int) (pulse * 255.0F), minAlpha, 255);
        int color = (alpha << 24) | 0x00FFFFFF;

        submitNodeCollector.order(1).submitModel(
                this.getParentModel(),
                state,
                poseStack,
                RenderTypes.eyes(EMISSIVE_TEXTURE),
                0xF000F0,
                OverlayTexture.NO_OVERLAY,
                color,
                null,
                state.outlineColor
        );
    }
}
