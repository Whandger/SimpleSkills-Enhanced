package com.github.ob_yekt.simpleskills.runes.armor;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.Bounds;
import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.EquipmentPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.boundsCenter;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.drawSnowflakeStar;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.lerpVec;

public final class FrostArmorVfx {
    private FrostArmorVfx() {}

    public static void submit(SubmitNodeCollector collector, PoseStack poseStack,
                              EquipmentPart part, Bounds bounds, double time, int seed) {
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, vertices) ->
                draw(pose.pose(), vertices, part, bounds, time, seed));
    }

    private static void draw(Matrix4fc matrix, VertexConsumer out, EquipmentPart part,
                             Bounds bounds, double time, int seed) {
        double pulse = 0.76 + 0.20 * Math.sin(time * 0.9 + seed * 0.02);

        switch (part) {
            case HELMET -> {
                for (int side = -1; side <= 1; side += 2) {
                    Vec3 p0 = new Vec3(side * 0.245, -0.34, -0.02);
                    Vec3 p1 = new Vec3(side * 0.345, -0.40, -0.055);
                    Vec3 p2 = new Vec3(side * 0.405, -0.52, -0.075);
                    Vec3 p3 = new Vec3(side * 0.375, -0.65, -0.055);

                    drawIceHornSegment(matrix, out, p0, p1, 0.036, 0.026, pulse);
                    drawIceHornSegment(matrix, out, p1, p2, 0.026, 0.015, pulse);
                    drawIceHornSegment(matrix, out, p2, p3, 0.015, 0.001, pulse);

                    Vec3 barbBase = new Vec3(side * 0.245, -0.30, 0.025);
                    Vec3 barbTip = new Vec3(side * 0.340, -0.34, 0.055);
                    drawIceCrystalSpike(matrix, out, barbBase, barbTip, 0.018, pulse);
                }
            }
            case ARM_RIGHT -> {
                drawIceCrystalSpike(matrix, out,
                        new Vec3(-0.18, -0.06, 0.00), new Vec3(-0.37, -0.21, 0.00), 0.026, pulse);
                drawIceCrystalSpike(matrix, out,
                        new Vec3(-0.17, -0.02, -0.065), new Vec3(-0.31, -0.12, -0.115), 0.019, pulse);
                drawIceCrystalSpike(matrix, out,
                        new Vec3(-0.17, -0.02, 0.065), new Vec3(-0.31, -0.12, 0.115), 0.019, pulse);
                drawIceCrystalSpike(matrix, out,
                        new Vec3(-0.18, 0.04, 0.00), new Vec3(-0.30, 0.00, 0.00), 0.016, pulse);
            }
            case ARM_LEFT -> {
                drawIceCrystalSpike(matrix, out,
                        new Vec3(0.18, -0.06, 0.00), new Vec3(0.37, -0.21, 0.00), 0.026, pulse);
                drawIceCrystalSpike(matrix, out,
                        new Vec3(0.17, -0.02, -0.065), new Vec3(0.31, -0.12, -0.115), 0.019, pulse);
                drawIceCrystalSpike(matrix, out,
                        new Vec3(0.17, -0.02, 0.065), new Vec3(0.31, -0.12, 0.115), 0.019, pulse);
                drawIceCrystalSpike(matrix, out,
                        new Vec3(0.18, 0.04, 0.00), new Vec3(0.30, 0.00, 0.00), 0.016, pulse);
            }
            case KNEE_RIGHT, KNEE_LEFT -> {
                drawIceCrystalSpike(matrix, out,
                        new Vec3(0.0, 0.35, -0.14), new Vec3(0.0, 0.42, -0.28), 0.022, pulse);
                drawIceCrystalSpike(matrix, out,
                        new Vec3(-0.055, 0.34, -0.135), new Vec3(-0.085, 0.40, -0.225), 0.015, pulse);
                drawIceCrystalSpike(matrix, out,
                        new Vec3(0.055, 0.34, -0.135), new Vec3(0.085, 0.40, -0.225), 0.015, pulse);
            }
            case CHEST_TORSO, LEG_WAIST, BOOT_RIGHT, BOOT_LEFT -> {
                Vec3 c = boundsCenter(bounds);
                for (int i = 0; i < 3; i++) {
                    double ang = time * 0.06 + i * (Math.PI * 2.0 / 3.0) + seed * 0.01;
                    Vec3 pos = c.add(
                            Math.cos(ang) * bounds.width() * 0.56,
                            Math.sin(time * 0.09 + i * 2.1) * bounds.height() * 0.25,
                            Math.sin(ang) * bounds.depth() * 0.62);
                    drawSnowflakeStar(matrix, out, pos, 0.014, time * 0.08 + i, pulse);
                }
            }
            default -> {
            }
        }
    }

    private static void drawIceHornSegment(Matrix4fc matrix, VertexConsumer out, Vec3 from, Vec3 to,
                                           double r0, double r1, double pulse) {
        PrayerVfxGeometry.taperedTube(matrix, out, from, to, r0, r1,
                0.34f, 0.84f, 1.0f, (float) (0.76 * pulse));
        PrayerVfxGeometry.taperedTube(matrix, out, from, to, r0 * 0.46, r1 * 0.46,
                0.90f, 0.98f, 1.0f, (float) (0.94 * pulse));
    }

    private static void drawIceCrystalSpike(Matrix4fc matrix, VertexConsumer out, Vec3 base, Vec3 tip,
                                            double baseRadius, double pulse) {
        PrayerVfxGeometry.taperedTube(matrix, out, base, tip, baseRadius, 0.0005,
                0.32f, 0.82f, 1.0f, (float) (0.78 * pulse));
        PrayerVfxGeometry.taperedTube(matrix, out, base, lerpVec(base, tip, 0.82),
                baseRadius * 0.44, 0.0004,
                0.92f, 0.99f, 1.0f, (float) (0.95 * pulse));
    }
}
