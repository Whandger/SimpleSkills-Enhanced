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

/**
 * Draws the attack effects in a frame whose origin is the Colossus base (the feet) and whose units are
 * model units, which are world blocks divided by the renderer's 1.65 scale.
 *
 * Every world-space quantity (mouth, aim point, landing point) is converted into that frame with the
 * inverse of the layer's basis. The frame origin is the base itself: the renderer shifts the model down
 * by 1.501 units so the feet touch the ground, and this layer shifts it back by the same amount. An earlier
 * version subtracted an extra offset here, which pushed every beam and orb about 2.5 blocks above the boss.
 */
public final class CrimsonColossusAttackLayer
        extends RenderLayer<CrimsonColossusRenderState, CrimsonColossusModel> {

    public CrimsonColossusAttackLayer(
            RenderLayerParent<CrimsonColossusRenderState, CrimsonColossusModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                       CrimsonColossusRenderState state, float yRot, float xRot) {
        final CrimsonColossusEntity.AttackType type = state.attackType;
        if (type == CrimsonColossusEntity.AttackType.NONE) return;

        poseStack.pushPose();
        poseStack.translate(0.0F, 1.501F, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(-(180.0F - state.bodyRot))));

        final float age = state.attackTick + state.partialTick;
        final int duration = type.duration;
        final int impactTick = state.impactTick;

        Matrix3f toLayer = new Matrix3f(poseStack.last().pose()).invert();
        Vec3 worldForward = Vec3.directionFromRotation(0.0F, state.bodyRot);
        Vec3 forwardLocal = transformDirection(toLayer, worldForward).normalize();

        Vec3 mouthWorld = CrimsonColossusEntity.mouthOffset(worldForward);
        Vec3 mouthLocal = transformDirection(toLayer, mouthWorld);
        Vec3 aimLocal = transformDirection(toLayer, state.targetOffset.subtract(mouthWorld));
        Vec3 targetLocal = transformDirection(toLayer, state.targetOffset);

        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (matrix, vertices) -> {
            switch (type) {
                case CRYSTAL_LASER -> CrimsonColossusLaserVfx.draw(
                        matrix.pose(), vertices, forwardLocal, mouthLocal, aimLocal, age, duration);
                case SEISMIC_SLAM -> CrimsonColossusSlamVfx.draw(
                        matrix.pose(), vertices, forwardLocal, age, duration);
                case CRYSTAL_BARRAGE -> CrimsonColossusShardVfx.draw(
                        matrix.pose(), vertices, forwardLocal, targetLocal, age, duration);
                case COLOSSUS_LEAP -> CrimsonColossusLeapShockwaveVfx.draw(
                        matrix.pose(), vertices, forwardLocal, targetLocal, impactTick, age, duration);
                default -> {
                }
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
