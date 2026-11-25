package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class AbyssalFallenKingRenderer
        extends MobRenderer<AbyssalFallenKingEntity, AbyssalFallenKingRenderState, AbyssalFallenKingModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("simpleskills", "textures/entity/abyssal_fallen_king.png");
    private static final float BASE_SUSPENSION_HEIGHT = 0.72F;

    public AbyssalFallenKingRenderer(EntityRendererProvider.Context context) {
        super(context, new AbyssalFallenKingModel(context.bakeLayer(AbyssalFallenKingModel.LAYER_LOCATION)), 0.75F);
        this.addLayer(new AbyssalFallenKingEmissiveLayer(this));
    }

    @Override
    public AbyssalFallenKingRenderState createRenderState() {
        return new AbyssalFallenKingRenderState();
    }

    @Override
    public void extractRenderState(AbyssalFallenKingEntity entity, AbyssalFallenKingRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.kingState = entity.getKingState();
        state.sittingIdleAnimationState.copyFrom(entity.sittingIdleAnimationState);
        state.awakeningAnimationState.copyFrom(entity.awakeningAnimationState);
        state.idleAnimationState.copyFrom(entity.idleAnimationState);
        state.walkAnimationState.copyFrom(entity.walkAnimationState);
        state.attackAnimationState.copyFrom(entity.attackAnimationState);
        state.deathAnimationState.copyFrom(entity.deathAnimationState);

        if (state.kingState == AbyssalFallenKingEntity.STATE_SITTING) {
            state.awakeningProgress = 0.0F;
            state.suspensionHeight = BASE_SUSPENSION_HEIGHT + 0.07F * Mth.sin(state.ageInTicks * 0.055F);
        } else if (state.kingState == AbyssalFallenKingEntity.STATE_AWAKENING) {
            float totalMs = AbyssalFallenKingEntity.AWAKENING_DURATION_TICKS * 50.0F;
            float elapsedMs = state.awakeningAnimationState.getTimeInMillis(state.ageInTicks);
            float progress = Mth.clamp(elapsedMs / Math.max(1.0F, totalMs), 0.0F, 1.0F);
            state.awakeningProgress = progress;

            // Remains suspended during instability/rupture, then descends smoothly to the dais floor
            double descend = PrayerVfxPrimitives.smooth(0.55, 1.0, progress);
            state.suspensionHeight = (float) (BASE_SUSPENSION_HEIGHT * (1.0 - descend));
        } else {
            state.awakeningProgress = 1.0F;
            state.suspensionHeight = 0.0F;
        }
    }

    @Override
    public void submit(AbyssalFallenKingRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        if (state.suspensionHeight != 0.0F) {
            poseStack.translate(0.0F, state.suspensionHeight, 0.0F);
        }

        super.submit(state, poseStack, collector, camera);

        if (state.kingState != AbyssalFallenKingEntity.STATE_ACTIVE) {
            float age = state.ageInTicks;
            int kingState = state.kingState;
            float ruptureProgress = state.awakeningProgress;
            collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (matrix, vertices) ->
                    AbyssalKingPrisonVfx.draw(matrix.pose(), vertices, age, kingState, ruptureProgress));
        }

        poseStack.popPose();
    }

    @Override
    protected void scale(AbyssalFallenKingRenderState state, PoseStack poseStack) {
        // Imposing royal stature (~2.7 blocks tall)
        poseStack.scale(1.18F, 1.18F, 1.18F);
    }

    @Override
    public Identifier getTextureLocation(AbyssalFallenKingRenderState state) {
        return TEXTURE;
    }
}
