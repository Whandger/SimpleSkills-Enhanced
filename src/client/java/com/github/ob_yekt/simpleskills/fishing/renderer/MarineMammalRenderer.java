package com.github.ob_yekt.simpleskills.fishing.renderer;

import com.github.ob_yekt.simpleskills.fishing.MarineMammalEntity;
import com.github.ob_yekt.simpleskills.fishing.model.AquaticEntityModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Renderer for the environmental whale and orca entities. */
public final class MarineMammalRenderer extends MobRenderer<MarineMammalEntity, LivingEntityRenderState, AquaticEntityModel> {
    private final Identifier texture;

    public MarineMammalRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer,
                                AquaticEntityModel.Shape shape, String texturePath) {
        super(context, new AquaticEntityModel(context.bakeLayer(layer), shape), 1.1F);
        this.texture = Identifier.fromNamespaceAndPath("simpleskills", "textures/" + texturePath);
    }

    @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(LivingEntityRenderState state) { return texture; }

    @Override
    public void extractRenderState(MarineMammalEntity entity, LivingEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.scale *= entity.simpleskills$getAquaticScale();
    }

    @Override
    protected void setupRotations(LivingEntityRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        poseStack.rotateDegrees(Axis.YP, 1.2F * Mth.sin(0.28F * state.ageInTicks));
    }
}
