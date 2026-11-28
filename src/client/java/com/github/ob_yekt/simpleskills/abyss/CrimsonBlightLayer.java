package com.github.ob_yekt.simpleskills.abyss;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Full-bright crystal spikes that scale around each target's actual render-state hitbox. */
public final class CrimsonBlightLayer<S extends LivingEntityRenderState, M extends EntityModel<S>>
        extends RenderLayer<S, M> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "simpleskills", "textures/entity/crystal_corruption_spikes.png");

    private final CrimsonBlightModel<S> crystalModel;

    public CrimsonBlightLayer(RenderLayerParent<S, M> parent, EntityModelSet modelSet) {
        super(parent);
        this.crystalModel = new CrimsonBlightModel<>(modelSet.bakeLayer(CrimsonBlightModel.LAYER_LOCATION));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                       S state, float yRot, float xRot) {
        if (state.isInvisible || !(state instanceof CrimsonBlightRenderState blightState)) return;
        int amplifier = blightState.simpleskills$getCrimsonBlightLevel();
        if (amplifier < 0) return;

        this.crystalModel.setCorruptionLevel(amplifier);
        this.crystalModel.setupAnim(state);

        // Vanilla-sized reference hitbox = 0.6 wide x 1.8 high.
        // These ratios make the crystal shell envelop small and large mobs alike.
        float widthScale = Mth.clamp(state.boundingBoxWidth / 0.60F, 0.60F, 8.0F);
        float heightScale = Mth.clamp(state.boundingBoxHeight / 2.0F, 0.60F, 8.0F);

        poseStack.pushPose();
        poseStack.scale(widthScale, heightScale, widthScale);
        collector.order(2).submitModel(
                this.crystalModel,
                state,
                poseStack,
                RenderTypes.eyes(TEXTURE),
                0xF000F0,
                OverlayTexture.NO_OVERLAY,
                -1,
                null,
                state.outlineColor
        );
        poseStack.popPose();
    }
}
