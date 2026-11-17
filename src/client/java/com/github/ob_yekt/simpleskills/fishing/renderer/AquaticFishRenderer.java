package com.github.ob_yekt.simpleskills.fishing.renderer;

import com.github.ob_yekt.simpleskills.fishing.FishingFishEntity;
import com.github.ob_yekt.simpleskills.fishing.model.AquaticEntityModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Renderer shared by catchable fish; each species has its own layer, texture and shadow size. */
public final class AquaticFishRenderer extends MobRenderer<FishingFishEntity, LivingEntityRenderState, AquaticEntityModel> {
    private final Identifier texture;

    public AquaticFishRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer,
                               AquaticEntityModel.Shape shape, String texturePath, float shadowRadius) {
        super(context, new AquaticEntityModel(context.bakeLayer(layer), shape), shadowRadius);
        this.texture = Identifier.fromNamespaceAndPath("simpleskills", "textures/" + texturePath);
    }

    @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(LivingEntityRenderState state) { return texture; }

@Override
    protected void setupRotations(LivingEntityRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        float amp = state.isInWater ? 1.0F : 1.35F;
        float speed = state.isInWater ? 1.0F : 1.75F;
        poseStack.rotateDegrees(Axis.YP, amp * 4.2F * Mth.sin(speed * 0.6F * state.ageInTicks));
        if (!state.isInWater) {
            poseStack.translate(0.18F, 0.08F, 0.0F);
            poseStack.rotateDegrees(Axis.ZP, 90.0F);
        }
    }
}
