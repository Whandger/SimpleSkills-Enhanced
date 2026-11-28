package com.github.ob_yekt.simpleskills.abyss;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Renders the 3D petrified stone shell over the Samurai's golden armor.
 * As health drops across 75%, 50%, and 25% thresholds, cutout fractures open in the stone shell
 * to reveal the royal golden armor and straw kasa underneath, until the shell shatters completely at Stage 3.
 */
public class AbyssalSamuraiStoneShellLayer
        extends RenderLayer<AbyssalSamuraiRenderState, AbyssalSamuraiModel> {
    private static final Identifier STONE_STAGE_0 =
            Identifier.fromNamespaceAndPath("simpleskills", "textures/entity/abyssal_samurai_stone_0.png");
    private static final Identifier STONE_STAGE_1 =
            Identifier.fromNamespaceAndPath("simpleskills", "textures/entity/abyssal_samurai_stone_1.png");
    private static final Identifier STONE_STAGE_2 =
            Identifier.fromNamespaceAndPath("simpleskills", "textures/entity/abyssal_samurai_stone_2.png");

    private final AbyssalSamuraiModel shellModel;

    public AbyssalSamuraiStoneShellLayer(
            RenderLayerParent<AbyssalSamuraiRenderState, AbyssalSamuraiModel> parent,
            EntityModelSet modelSet) {
        super(parent);
        this.shellModel = new AbyssalSamuraiModel(modelSet.bakeLayer(AbyssalSamuraiModel.SHELL_LAYER_LOCATION));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                       AbyssalSamuraiRenderState state, float yRot, float xRot) {
        int stage = state.petrificationStage;
        if (stage >= 3) {
            // Stone shell has completely shattered away: 100% golden samurai armor is exposed.
            return;
        }

        Identifier shellTexture = switch (stage) {
            case 0 -> STONE_STAGE_0;
            case 1 -> STONE_STAGE_1;
            default -> STONE_STAGE_2;
        };

        this.shellModel.setupAnim(state);
        collector.order(1).submitModel(
                this.shellModel,
                state,
                poseStack,
                RenderTypes.entityCutout(shellTexture),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                -1,
                null,
                state.outlineColor
        );
    }
}
