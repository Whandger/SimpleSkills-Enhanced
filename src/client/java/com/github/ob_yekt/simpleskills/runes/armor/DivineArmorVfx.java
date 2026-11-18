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

import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.BODY;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.HEAD;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.boundsCenter;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.drawStarFlare;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.drawTiltedEllipse;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.lerp;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.lerpVec;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.mix;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.unit;

public final class DivineArmorVfx {
    private DivineArmorVfx() {}

    public static void submit(SubmitNodeCollector collector, PoseStack poseStack,
                              EquipmentPart part, Bounds bounds, double time, int seed) {
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, vertices) ->
                draw(pose.pose(), vertices, part, bounds, time, seed));
    }

    private static void draw(Matrix4fc matrix, VertexConsumer out, EquipmentPart part,
                             Bounds bounds, double time, int seed) {
        double pulse = 0.78 + 0.20 * Math.sin(time * 0.85 + seed * 0.01);

        switch (part) {
            case HELMET -> {
                double bob = Math.sin(time * 0.14) * 0.012;
                Vec3 haloCenter = new Vec3(0.0, HEAD.minY() - 0.12 + bob, 0.0);
                drawTiltedEllipse(matrix, out, haloCenter, 0.23, 0.23,
                        time * 0.04, 0.0, 0.0,
                        1.0f, 0.80f, 0.24f, (float) (0.88 * pulse), 36, 0.0042, false);
                drawTiltedEllipse(matrix, out, haloCenter, 0.23, 0.23,
                        -time * 0.04, 0.0, 0.0,
                        1.0f, 0.97f, 0.78f, (float) (0.96 * pulse), 36, 0.0018, false);
            }
            case CHEST_TORSO -> {
                drawAngelWings(matrix, out, time, pulse);
                drawGoldenSparkles(matrix, out, bounds, time, seed, 5, pulse);
            }
            case ARM_RIGHT, ARM_LEFT, KNEE_RIGHT, KNEE_LEFT, LEG_WAIST, BOOT_RIGHT, BOOT_LEFT -> {
                drawGoldenSparkles(matrix, out, bounds, time, seed, 3, pulse);
            }
            default -> {
            }
        }
    }

    private static void drawAngelWings(Matrix4fc matrix, VertexConsumer out, double time, double pulse) {
        double flap = Math.sin(time * 0.14) * 0.035;

        for (int side = -1; side <= 1; side += 2) {
            Vec3 root = new Vec3(side * 0.075, 0.14, BODY.maxZ() + 0.025);
            Vec3 shoulder = new Vec3(side * 0.24, 0.02, 0.22 + flap * 0.5);
            Vec3 elbow = new Vec3(side * 0.46, -0.07, 0.28 + flap * 1.1);
            Vec3 wingTip = new Vec3(side * 0.66, 0.04, 0.33 + flap * 1.6);

            PrayerVfxGeometry.taperedTube(matrix, out, root, shoulder, 0.013, 0.011,
                    1.0f, 0.86f, 0.44f, (float) (0.85 * pulse));
            PrayerVfxGeometry.taperedTube(matrix, out, shoulder, elbow, 0.011, 0.008,
                    1.0f, 0.92f, 0.64f, (float) (0.88 * pulse));
            PrayerVfxGeometry.taperedTube(matrix, out, elbow, wingTip, 0.008, 0.002,
                    1.0f, 0.97f, 0.82f, (float) (0.90 * pulse));

            int primaryFeathers = 7;
            for (int f = 0; f < primaryFeathers; f++) {
                double t = (f + 0.5) / primaryFeathers;
                Vec3 attach = sampleWingArch(root, shoulder, elbow, wingTip, t);
                double featherLen = lerp(0.18, 0.34, Math.sin(Math.PI * (0.25 + 0.70 * t)));
                double splayX = side * lerp(0.03, 0.16, t);
                double splayZ = lerp(0.02, 0.07, t) + flap * (0.6 + t);

                Vec3 tip = attach.add(splayX, featherLen, splayZ);
                PrayerVfxGeometry.taperedTube(matrix, out, attach, tip, 0.014, 0.0012,
                        1.0f, 0.80f, 0.30f, (float) (0.62 * pulse));
                PrayerVfxGeometry.taperedTube(matrix, out, attach, lerpVec(attach, tip, 0.92),
                        0.0065, 0.0006,
                        1.0f, 0.97f, 0.82f, (float) (0.92 * pulse));
            }

            int secondaryFeathers = 5;
            for (int f = 0; f < secondaryFeathers; f++) {
                double t = (f + 0.5) / secondaryFeathers * 0.78;
                Vec3 attach = sampleWingArch(root, shoulder, elbow, wingTip, t).add(0, 0.015, 0.012);
                double featherLen = lerp(0.11, 0.19, t);
                Vec3 tip = attach.add(side * lerp(0.02, 0.09, t), featherLen, 0.025 + flap * 0.5);
                PrayerVfxGeometry.taperedTube(matrix, out, attach, tip, 0.011, 0.001,
                        1.0f, 0.90f, 0.56f, (float) (0.76 * pulse));
            }
        }
    }

    private static Vec3 sampleWingArch(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        if (t < 0.33) return lerpVec(p0, p1, t / 0.33);
        if (t < 0.67) return lerpVec(p1, p2, (t - 0.33) / 0.34);
        return lerpVec(p2, p3, (t - 0.67) / 0.33);
    }

    private static void drawGoldenSparkles(Matrix4fc matrix, VertexConsumer out, Bounds bounds,
                                           double time, int seed, int count, double pulse) {
        Vec3 center = boundsCenter(bounds);
        double rx = Math.max(0.05, bounds.width() * 0.65);
        double rz = Math.max(0.05, bounds.depth() * 0.72);

        for (int i = 0; i < count; i++) {
            long flareSeed = mix(seed * 59L + i * 409L);
            double twinkle = Math.pow(0.5 + 0.5 * Math.sin(time * 0.18 + i * 1.9 + unit(flareSeed, 1) * 6.0), 2.0);
            if (twinkle < 0.12) continue;

            double angle = time * 0.035 + i * (Math.PI * 2.0 / count) + unit(flareSeed, 2) * 3.0;
            Vec3 flarePos = new Vec3(
                    center.x + Math.cos(angle) * rx,
                    lerp(bounds.minY(), bounds.maxY(), 0.15 + unit(flareSeed, 3) * 0.70),
                    center.z + Math.sin(angle) * rz);

            drawStarFlare(matrix, out, flarePos, 0.016 + 0.010 * twinkle,
                    1.0f, 0.92f, 0.56f, (float) (0.90 * twinkle * pulse));
        }
    }
}
