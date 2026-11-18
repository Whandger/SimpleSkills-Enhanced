package com.github.ob_yekt.simpleskills.runes.armor;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.runes.RuneEquipmentVfxRenderTypes;
import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.Bounds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.fract;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.lerp;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.lerpVec;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.mix;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.orthogonalVector;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.safeNormalize;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.texturedRibbonSegment;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.unit;

public final class InfernoArmorVfx {
    private InfernoArmorVfx() {}

    public static void submit(SubmitNodeCollector collector, PoseStack poseStack,
                              Bounds bounds, double time, int seed) {
        collector.submitCustomGeometry(poseStack, RuneEquipmentVfxRenderTypes.flame(), (pose, vertices) ->
                drawRibbons(pose.pose(), vertices, bounds, time, seed));
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, vertices) ->
                drawTonguesAndEmbers(pose.pose(), vertices, bounds, time, seed));
    }

    private static void drawRibbons(Matrix4fc matrix, VertexConsumer out, Bounds bounds,
                                    double time, int seed) {
        int tongues = bounds.height() > 0.55 ? 6 : 4;
        double scroll = time * 0.14;
        double centerX = (bounds.minX() + bounds.maxX()) * 0.5;
        double centerZ = (bounds.minZ() + bounds.maxZ()) * 0.5;

        for (int tIdx = 0; tIdx < tongues; tIdx++) {
            long tSeed = mix(seed * 73L + tIdx * 0x9E3779B97F4A7C15L);
            double angle = tIdx * (Math.PI * 2.0 / tongues) + unit(tSeed, 1) * 0.9;
            double startT = unit(tSeed, 2) * 0.45;
            double yBottom = lerp(bounds.maxY(), bounds.minY(), startT);
            double flameHeight = bounds.height() * (0.48 + unit(tSeed, 3) * 0.36);

            double rx = Math.max(0.02, bounds.width() * 0.48);
            double rz = Math.max(0.02, bounds.depth() * 0.52);

            int segments = 10;
            Vec3 prevCenter = null;
            Vec3 prevSideA = null;
            Vec3 prevSideB = null;
            double prevWidth = 0;
            double prevV = 0;
            float prevR = 1.0f, prevG = 0.95f, prevB = 0.6f, prevAlpha = 0.0f;

            for (int i = 0; i <= segments; i++) {
                double f = i / (double) segments;
                double profile = Math.sin(Math.PI * Math.pow(f, 0.55)) * (1.0 - 0.35 * f);
                double swayX = Math.sin(time * 2.3 + f * 6.5 + tIdx * 2.1) * 0.022 * f;
                double swayZ = Math.cos(time * 1.9 + f * 5.8 + tIdx * 1.7) * 0.022 * f;

                double x = centerX + Math.cos(angle + f * 0.6) * rx * (1.0 - f * 0.25) + swayX;
                double z = centerZ + Math.sin(angle + f * 0.6) * rz * (1.0 - f * 0.25) + swayZ;
                double y = yBottom - f * flameHeight;
                Vec3 pos = new Vec3(x, y, z);

                double flicker = 0.78 + 0.22 * Math.sin(time * 3.4 + tIdx * 2.9 + f * 10.0);
                double width = Math.max(0.001, profile * Math.min(bounds.width(), bounds.depth()) * 0.28 * flicker);
                double v = fract(f * 1.4 - scroll + tIdx * 0.27);

                float r = 1.0f;
                float g = (float) lerp(0.95, 0.24, Math.pow(f, 0.75));
                float b = (float) lerp(0.62, 0.02, Math.min(1.0, f * 1.8));
                float alpha = (float) (profile * (0.68 + 0.28 * flicker));

                Vec3 tangent = (prevCenter != null)
                        ? safeNormalize(pos.subtract(prevCenter), new Vec3(0, -1, 0))
                        : new Vec3(0, -1, 0);
                Vec3 sideA = orthogonalVector(tangent);
                Vec3 sideB = tangent.cross(sideA).normalize();

                if (prevCenter != null) {
                    texturedRibbonSegment(matrix, out, prevCenter, pos, prevSideA, sideA,
                            prevWidth, width, prevV, v,
                            prevR, prevG, prevB, prevAlpha, r, g, b, alpha);
                    texturedRibbonSegment(matrix, out, prevCenter, pos, prevSideB, sideB,
                            prevWidth * 0.85, width * 0.85, prevV, v,
                            prevR, prevG, prevB, prevAlpha * 0.85f, r, g, b, alpha * 0.85f);
                }

                prevCenter = pos;
                prevSideA = sideA;
                prevSideB = sideB;
                prevWidth = width;
                prevV = v;
                prevR = r;
                prevG = g;
                prevB = b;
                prevAlpha = alpha;
            }
        }
    }

    private static void drawTonguesAndEmbers(Matrix4fc matrix, VertexConsumer out, Bounds bounds,
                                             double time, int seed) {
        double scale = bounds.strokeScale();
        double centerX = (bounds.minX() + bounds.maxX()) * 0.5;
        double centerZ = (bounds.minZ() + bounds.maxZ()) * 0.5;
        int tongues = bounds.height() > 0.55 ? 7 : 5;

        for (int i = 0; i < tongues; i++) {
            long pSeed = mix(seed * 131L + i * 509L);
            double angle = i * (Math.PI * 2.0 / tongues) + unit(pSeed, 1) * 0.8;
            double yStart = lerp(bounds.maxY(), bounds.minY(), unit(pSeed, 2) * 0.55);
            Vec3 base = new Vec3(
                    centerX + Math.cos(angle) * bounds.width() * 0.46,
                    yStart,
                    centerZ + Math.sin(angle) * bounds.depth() * 0.50);

            double flicker = 0.78 + 0.22 * Math.sin(time * 4.2 + i * 2.4);
            double height = (0.11 + unit(pSeed, 3) * 0.09) * scale * flicker;
            double swayX = Math.sin(time * 2.7 + i * 1.8) * 0.022 * scale;
            double swayZ = Math.cos(time * 2.4 + i * 2.2) * 0.022 * scale;

            Vec3 mid = base.add(swayX * 0.5, -height * 0.52, swayZ * 0.5);
            Vec3 tip = base.add(swayX * 1.3, -height, swayZ * 1.3);
            double radius = 0.017 * scale;

            PrayerVfxGeometry.taperedTube(matrix, out, base, mid, radius, radius * 0.68,
                    1.0f, 0.44f, 0.05f, 0.58f);
            PrayerVfxGeometry.taperedTube(matrix, out, mid, tip, radius * 0.68, 0.0006,
                    0.92f, 0.15f, 0.02f, 0.48f);
            PrayerVfxGeometry.taperedTube(matrix, out, base, lerpVec(base, tip, 0.56),
                    radius * 0.46, 0.0005,
                    1.0f, 0.94f, 0.54f, 0.86f);

            double life = fract(time * 0.32 + i * 0.19 + unit(pSeed, 4));
            Vec3 emberFrom = tip.add(swayX * life * 1.4, -life * 0.09 * scale, swayZ * life * 1.4);
            Vec3 emberTo = emberFrom.add(0, -0.024 * scale, 0);
            float alpha = (float) (Math.sin(Math.PI * life) * 0.88);
            PrayerVfxGeometry.taperedTube(matrix, out, emberFrom, emberTo,
                    0.0028 * scale, 0.0004 * scale,
                    1.0f, 0.82f, 0.20f, alpha);
        }
    }
}
