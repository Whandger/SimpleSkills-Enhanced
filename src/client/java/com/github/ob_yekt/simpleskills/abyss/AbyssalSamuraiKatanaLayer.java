package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaAnimationLibrary;
import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaClientStanceState;
import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaStanceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Renders the exact player katana item models on the Samurai's custom rig.
 * Sheathed states use the combined sheathed model; transitional/drawn states split
 * the saya and blade and animate both from the player's shared JSON clip library.
 */
public final class AbyssalSamuraiKatanaLayer
        extends RenderLayer<AbyssalSamuraiRenderState, AbyssalSamuraiModel> {
    /** Exact same item-model offsets and scale used by KatanaRenderUtil for players. */
    private static final float ITEM_TO_GEO_SCALE = 2.08F;
    private static final float GUARD_Y = 5.196F;
    private static final float KOIGUCHI_Y = 16.8F;

    public AbyssalSamuraiKatanaLayer(
            RenderLayerParent<AbyssalSamuraiRenderState, AbyssalSamuraiModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                       AbyssalSamuraiRenderState state, float yRot, float xRot) {
        if (state.isInvisible || !state.hasKatana || state.katanaPose == null) return;

        KatanaClientStanceState.RenderPose pose = state.katanaPose;
        int stance = pose.stance();
        boolean useSheathedModel = stance == KatanaStanceManager.SHEATHED
                || KatanaStanceManager.isSheathedGripStance(stance);

        if (useSheathedModel) {
            submitSayaModel(poseStack, collector, packedLight,
                    state.katanaSheathedRenderState, pose, state.outlineColor);
            return;
        }

        // During draw/sheath the blade and saya separate and move independently, just
        // like they do in the player's KatanaPlayerRenderLayer.
        submitSayaModel(poseStack, collector, packedLight,
                state.katanaSayaRenderState, pose, state.outlineColor);
        if (state.katanaRenderState.isEmpty()) return;
        submitBladeModel(poseStack, collector, packedLight, state, pose, state.outlineColor);
    }

    private void submitSayaModel(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                                 net.minecraft.client.renderer.item.ItemStackRenderState itemState,
                                 KatanaClientStanceState.RenderPose pose, int outlineColor) {
        if (itemState.isEmpty()) return;

        poseStack.pushPose();
        this.getParentModel().translateToKatanaAnimationFrame(poseStack);
        // This is intentionally identical to KatanaRenderUtil.submit(..., "saya", ...).
        KatanaAnimationLibrary.applyBedrock(poseStack, pose, "saya");
        poseStack.scale(ITEM_TO_GEO_SCALE, ITEM_TO_GEO_SCALE, ITEM_TO_GEO_SCALE);
        poseStack.translate(0.0F, -(KOIGUCHI_Y - 8.0F) / 16.0F, 0.0F);
        itemState.submit(poseStack, collector, packedLight, OverlayTexture.NO_OVERLAY, outlineColor);
        poseStack.popPose();
    }

    private void submitBladeModel(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                                  AbyssalSamuraiRenderState state,
                                  KatanaClientStanceState.RenderPose pose, int outlineColor) {
        if (state.katanaRenderState.isEmpty()) return;

        poseStack.pushPose();
        this.getParentModel().translateToKatanaAnimationFrame(poseStack);
        // This is intentionally identical to KatanaRenderUtil.submit(..., "katana", ...).
        KatanaAnimationLibrary.applyBedrock(poseStack, pose, "katana");
        poseStack.scale(ITEM_TO_GEO_SCALE, ITEM_TO_GEO_SCALE, ITEM_TO_GEO_SCALE);
        poseStack.translate(0.0F, (GUARD_Y - 8.0F) / 16.0F, 0.0F);
        poseStack.rotateDegrees(Axis.ZP, 180.0F);
        state.katanaRenderState.submit(
                poseStack, collector, packedLight, OverlayTexture.NO_OVERLAY, outlineColor);
        poseStack.popPose();
    }
}
