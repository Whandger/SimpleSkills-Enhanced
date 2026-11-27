package com.github.ob_yekt.simpleskills.abyss;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Emissive layer for glowing stone fracture lines during Stages 1-2 and glowing
 * samurai eyes + royal chest gems once the golden armor is uncovered in Stage 3.
 */
public class AbyssalSamuraiEmissiveLayer
        extends RenderLayer<AbyssalSamuraiRenderState, AbyssalSamuraiModel> {
    private static final Identifier EMISSIVE_CRACK_1 =
            Identifier.fromNamespaceAndPath("simpleskills", "textures/entity/abyssal_samurai_emissive_crack_1.png");
    private static final Identifier EMISSIVE_CRACK_2 =
            Identifier.fromNamespaceAndPath("simpleskills", "textures/entity/abyssal_samurai_emissive_crack_2.png");
    private static final Identifier EMISSIVE_GOLD =
            Identifier.fromNamespaceAndPath("simpleskills", "textures/entity/abyssal_samurai_emissive_gold.png");

    public AbyssalSamuraiEmissiveLayer(
            RenderLayerParent<AbyssalSamuraiRenderState, AbyssalSamuraiModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                       AbyssalSamuraiRenderState state, float yRot, float xRot) {
        if (state.petrificationStage == 0) {
            return;
        }

        Identifier emissiveTex = switch (state.petrificationStage) {
            case 1 -> EMISSIVE_CRACK_1;
            case 2 -> EMISSIVE_CRACK_2;
            default -> EMISSIVE_GOLD;
        };

        collector.order(2).submitModel(
                this.getParentModel(),
                state,
                poseStack,
                RenderTypes.eyes(emissiveTex),
                0xF000F0,
                OverlayTexture.NO_OVERLAY,
                -1,
                null,
                state.outlineColor
        );
    }
}
