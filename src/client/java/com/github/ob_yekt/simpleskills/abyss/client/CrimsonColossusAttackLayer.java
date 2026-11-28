package com.github.ob_yekt.simpleskills.abyss.client;

import com.github.ob_yekt.simpleskills.abyss.client.vfx.CrimsonColossusLaserVfx;
import com.github.ob_yekt.simpleskills.abyss.client.vfx.CrimsonColossusLeapShockwaveVfx;
import com.github.ob_yekt.simpleskills.abyss.client.vfx.CrimsonColossusShardVfx;
import com.github.ob_yekt.simpleskills.abyss.client.vfx.CrimsonColossusSlamVfx;
import com.github.ob_yekt.simpleskills.abyss.entity.CrimsonColossusEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class CrimsonColossusAttackLayer
        extends RenderLayer<CrimsonColossusRenderState, CrimsonColossusModel> {

    public CrimsonColossusAttackLayer(
            RenderLayerParent<CrimsonColossusRenderState, CrimsonColossusModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                       CrimsonColossusRenderState state, float yRot, float xRot) {
        CrimsonColossusEntity.AttackType type = state.attackType;
        if (type == CrimsonColossusEntity.AttackType.NONE) return;

        poseStack.pushPose();

        // Keep the parent basis so the layer translation can be removed when
        // converting the real world-space muzzle into this layer's local space.
        Matrix3f parentBasis = new Matrix3f(poseStack.last().pose());
        Vector3f layerTranslation = parentBasis.transform(new Vector3f(0.0F, 1.501F, 0.0F));

        poseStack.translate(0.0F, 1.501F, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(-(180.0F - state.bodyRot))));

        float age = state.attackTick + state.partialTick;
        int duration = type.duration;
        Vec3 worldForward = Vec3.directionFromRotation(0.0F, state.bodyRot);

        Matrix3f inverseRenderBasis = new Matrix3f(poseStack.last().pose()).invert();
        Vec3 forwardLocal = transformDirection(inverseRenderBasis, worldForward);

        // Match the server-side muzzle in CrimsonColossusEntity#getMouthWorldPosition.
        Vec3 worldMouthOffset = worldForward.scale(1.85D).add(0.0D, 1.75D, 0.0D);
        Vec3 mouthBeforeLayerTranslation = worldMouthOffset.subtract(
                layerTranslation.x, layerTranslation.y, layerTranslation.z);
        Vec3 mouthLocal = transformDirection(inverseRenderBasis, mouthBeforeLayerTranslation);

        // Convert the actual muzzle-to-player vector, not the player's raw offset
        // from the Colossus origin. This keeps the visible beam and server ray aligned.
        Vec3 worldAimVector = state.targetOffset.subtract(worldMouthOffset);
        Vec3 aimVectorLocal = transformDirection(inverseRenderBasis, worldAimVector);

        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (matrix, vertices) -> {
            switch (type) {
                case CRYSTAL_LASER -> CrimsonColossusLaserVfx.draw(
                        matrix.pose(), vertices, forwardLocal, mouthLocal, aimVectorLocal, age, duration);
                case SEISMIC_SLAM -> CrimsonColossusSlamVfx.draw(
                        matrix.pose(), vertices, worldForward, age, duration);
                case CRYSTAL_BARRAGE -> CrimsonColossusShardVfx.draw(
                        matrix.pose(), vertices, worldForward, state.targetOffset, age, duration);
                case COLOSSUS_LEAP -> CrimsonColossusLeapShockwaveVfx.draw(
                        matrix.pose(), vertices, worldForward, age, duration);
                default -> { }
            }
        });

        poseStack.popPose();
    }

    private static Vec3 transformDirection(Matrix3f matrix, Vec3 direction) {
        Vector3f transformed = matrix.transform(new Vector3f(
                (float) direction.x,
                (float) direction.y,
                (float) direction.z
        ));
        return new Vec3(transformed.x, transformed.y, transformed.z);
    }

}
