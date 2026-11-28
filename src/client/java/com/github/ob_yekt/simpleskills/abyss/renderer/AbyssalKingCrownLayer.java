package com.github.ob_yekt.simpleskills.abyss.renderer;

import com.github.ob_yekt.simpleskills.abyss.model.AbyssalFallenKingModel;
import com.github.ob_yekt.simpleskills.abyss.model.AbyssalFallenKingRenderState;
import com.github.ob_yekt.simpleskills.abyss.model.AbyssalKingCrownModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

/** Renders a separate, fully 3D gold crown aligned to the animated skeleton head. */
public final class AbyssalKingCrownLayer extends RenderLayer<AbyssalFallenKingRenderState, AbyssalFallenKingModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "simpleskills", "textures/entity/abyssal_fallen_king_crown.png");

    private final AbyssalKingCrownModel crown;

    public AbyssalKingCrownLayer(RenderLayerParent<AbyssalFallenKingRenderState, AbyssalFallenKingModel> parent,
                                AbyssalKingCrownModel crown) {
        super(parent);
        this.crown = crown;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       AbyssalFallenKingRenderState state, float yRot, float xRot) {
        this.crown.copyHeadPose(this.getParentModel().getHead());
        renderColoredCutoutModel(this.crown, TEXTURE, poseStack, collector, light, state, 0xFFFFFFFF, 0);
    }
}
