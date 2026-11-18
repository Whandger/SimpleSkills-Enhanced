package com.github.ob_yekt.simpleskills.runes.armor;

import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.EquipmentPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.BODY;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.HEAD;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.LEFT_ARM;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.LEG;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.RIGHT_ARM;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.drawRuneCircle;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.drawTiltedEllipse;

public final class ArcaneArmorVfx {
    private ArcaneArmorVfx() {}

    public static void submit(SubmitNodeCollector collector, PoseStack poseStack,
                              EquipmentPart part, double time, int seed) {
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, vertices) ->
                draw(pose.pose(), vertices, part, time, seed));
    }

    private static void draw(Matrix4fc matrix, VertexConsumer out, EquipmentPart part,
                             double time, int seed) {
        double pulse = 0.78 + 0.20 * Math.sin(time * 1.35 + seed * 0.01);
        float r = 0.76f, g = 0.36f, b = 1.0f;

        switch (part) {
            case HELMET -> {
                double bob = Math.sin(time * 0.16) * 0.014;
                Vec3 aboveHead = new Vec3(0.0, HEAD.minY() - 0.11 + bob, 0.0);
                drawRuneCircle(matrix, out, aboveHead, new Vec3(0, -1, 0), new Vec3(0, 0, -1),
                        0.26, time * 0.045, r, g, b, (float) pulse, 0.0024);
            }
            case CHEST_TORSO -> {
                Vec3 behindBack = new Vec3(0.0, 0.34, BODY.maxZ() + 0.05);
                drawRuneCircle(matrix, out, behindBack, new Vec3(0, 0, 1), new Vec3(0, -1, 0),
                        0.31, time * 0.038, r, g, b, (float) pulse, 0.0026);
            }
            case ARM_RIGHT -> {
                Vec3 rightShoulder = new Vec3(RIGHT_ARM.minX() - 0.035, 0.18, 0.0);
                drawRuneCircle(matrix, out, rightShoulder, new Vec3(-1, 0, 0), new Vec3(0, -1, 0),
                        0.155, time * 0.055, r, g, b, (float) pulse, 0.0020);
            }
            case ARM_LEFT -> {
                Vec3 leftShoulder = new Vec3(LEFT_ARM.maxX() + 0.035, 0.18, 0.0);
                drawRuneCircle(matrix, out, leftShoulder, new Vec3(1, 0, 0), new Vec3(0, -1, 0),
                        0.155, -time * 0.055, r, g, b, (float) pulse, 0.0020);
            }
            case KNEE_RIGHT, KNEE_LEFT -> {
                Vec3 kneeFront = new Vec3(0.0, 0.36, LEG.minZ() - 0.032);
                double dir = part == EquipmentPart.KNEE_RIGHT ? 1.0 : -1.0;
                drawRuneCircle(matrix, out, kneeFront, new Vec3(0, 0, -1), new Vec3(0, -1, 0),
                        0.135, dir * time * 0.06, r, g, b, (float) pulse, 0.0019);
            }
            case LEG_WAIST -> {
                Vec3 waist = new Vec3(0.0, 0.68, 0.0);
                drawTiltedEllipse(matrix, out, waist, 0.28, 0.17, time * 0.05, 0.0, 0.0,
                        r, g, b, (float) (0.55 * pulse), 28, 0.0016, true);
            }
            case BOOT_RIGHT, BOOT_LEFT -> {
                Vec3 ankle = new Vec3(0.0, 0.64, 0.0);
                drawTiltedEllipse(matrix, out, ankle, 0.175, 0.175, time * 0.06, 0.0, 0.0,
                        r, g, b, (float) (0.65 * pulse), 24, 0.0016, true);
            }
            default -> {
            }
        }
    }
}
