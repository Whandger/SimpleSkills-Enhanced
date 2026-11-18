package com.github.ob_yekt.simpleskills.runes.tools;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.runes.RuneEquipmentVfxRenderTypes;
import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.Bounds;
import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.ItemGeometryInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.boundsCenter;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.fract;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.lerp;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.mix;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.orthogonalVector;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.safeNormalize;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.texturedRibbonSegment;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.unit;

public final class ShadowToolVfx {
    private ShadowToolVfx() {}

    public static void submit(SubmitNodeCollector collector, PoseStack poseStack,
                              ItemGeometryInfo info, double time, int seed) {
        Bounds bounds = info.bounds();
        collector.submitCustomGeometry(poseStack, RuneEquipmentVfxRenderTypes.shadow(), (pose, vertices) ->
                drawVeil(pose.pose(), vertices, bounds, time, seed));
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, vertices) ->
                drawFilaments(pose.pose(), vertices, bounds, time, seed));
    }

    private static void drawVeil(Matrix4fc matrix, VertexConsumer out, Bounds bounds,
                                 double time, int seed) {
        int strands = 3;
        double pulse = 0.58 + 0.28 * Math.sin(time * 0.46 + seed * 0.013);
        double centerX = (bounds.minX() + bounds.maxX()) * 0.5;
        double centerZ = (bounds.minZ() + bounds.maxZ()) * 0.5;

        for (int strand = 0; strand < strands; strand++) {
            long strandSeed = mix(seed * 211L + strand * 811L);
            double phase = time * 0.085 + strand * (Math.PI * 2.0 / strands) + unit(strandSeed, 1);
            int segments = 16;

            Vec3 prevCenter = null;
            Vec3 prevSideA = null;
            Vec3 prevSideB = null;
            double prevWidth = 0;
            double prevV = 0;
            float prevR = 0.35f, prevG = 0.10f, prevB = 0.65f, prevAlpha = 0.0f;

            for (int i = 0; i <= segments; i++) {
                double t = i / (double) segments;
                double y = lerp(bounds.maxY(), bounds.minY(), t);
                double angle = phase + t * 5.1 + Math.sin(time * 0.55 + t * 4.2) * 0.35;
                double envelope = Math.sin(Math.PI * t);

                double x = centerX + Math.cos(angle) * Math.max(0.016, bounds.width() * 0.52);
                double z = centerZ + Math.sin(angle * 0.85) * Math.max(0.020, bounds.depth() * 0.76);
                Vec3 center = new Vec3(x, y, z);

                double baseWidth = Math.max(0.020, Math.min(bounds.width(), bounds.height()) * 0.26);
                double width = envelope * baseWidth * (0.74 + 0.26 * Math.sin(time * 0.8 + strand * 2.0 + t * 6.5));
                double v = fract(t * 1.4 - time * 0.022 + strand * 0.31);

                float r = (float) lerp(0.18, 0.54, envelope);
                float g = (float) lerp(0.04, 0.14, envelope);
                float b = (float) lerp(0.38, 0.92, envelope);
                float alpha = (float) (envelope * pulse);

                Vec3 tangent = (prevCenter != null)
                        ? safeNormalize(center.subtract(prevCenter), new Vec3(0, -1, 0))
                        : new Vec3(0, -1, 0);
                Vec3 sideA = orthogonalVector(tangent);
                Vec3 sideB = tangent.cross(sideA).normalize();

                if (prevCenter != null) {
                    texturedRibbonSegment(matrix, out, prevCenter, center, prevSideA, sideA,
                            prevWidth, width, prevV, v,
                            prevR, prevG, prevB, prevAlpha, r, g, b, alpha);
                    texturedRibbonSegment(matrix, out, prevCenter, center, prevSideB, sideB,
                            prevWidth * 0.75, width * 0.75, prevV, v,
                            prevR * 0.7f, prevG * 0.7f, prevB * 0.85f, prevAlpha * 0.75f,
                            r * 0.7f, g * 0.7f, b * 0.85f, alpha * 0.75f);
                }

                prevCenter = center;
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

    private static void drawFilaments(Matrix4fc matrix, VertexConsumer out, Bounds bounds,
                                      double time, int seed) {
        double scale = bounds.strokeScale();
        Vec3 center = boundsCenter(bounds);

        for (int filament = 0; filament < 4; filament++) {
            long filamentSeed = mix(seed * 41L + filament * 613L);
            double x = lerp(bounds.minX(), bounds.maxX(), unit(filamentSeed, 1));
            double z = lerp(bounds.minZ(), bounds.maxZ(), unit(filamentSeed, 2));
            double phase = time * 0.48 + unit(filamentSeed, 3) * Math.PI * 2.0;

            Vec3 p0 = new Vec3(x, bounds.maxY(), z);
            Vec3 p1 = new Vec3(
                    x + Math.sin(phase) * 0.032 * scale,
                    lerp(bounds.maxY(), bounds.minY(), 0.35),
                    z + Math.cos(phase) * 0.026 * scale);
            Vec3 p2 = new Vec3(
                    x + Math.sin(phase + 1.4) * 0.048 * scale,
                    lerp(bounds.maxY(), bounds.minY(), 0.72),
                    z + Math.cos(phase + 1.4) * 0.036 * scale);
            Vec3 p3 = new Vec3(
                    x + Math.sin(phase + 2.6) * 0.028 * scale,
                    bounds.minY(),
                    z + Math.cos(phase + 2.6) * 0.024 * scale);

            PrayerVfxGeometry.taperedTube(matrix, out, p0, p1,
                    0.0048 * scale, 0.0032 * scale, 0.24f, 0.06f, 0.52f, 0.38f);
            PrayerVfxGeometry.taperedTube(matrix, out, p1, p2,
                    0.0032 * scale, 0.0018 * scale, 0.48f, 0.16f, 0.88f, 0.48f);
            PrayerVfxGeometry.taperedTube(matrix, out, p2, p3,
                    0.0018 * scale, 0.0004 * scale, 0.72f, 0.34f, 1.0f, 0.34f);
        }

        for (int i = 0; i < 3; i++) {
            double angle = -time * 0.42 + i * (Math.PI * 2.0 / 3.0) + seed * 0.01;
            Vec3 mote = new Vec3(
                    center.x + Math.cos(angle) * Math.max(0.022, bounds.width() * 0.56),
                    center.y + Math.sin(time * 0.65 + i * 2.1) * bounds.height() * 0.28,
                    center.z + Math.sin(angle) * Math.max(0.022, bounds.depth() * 0.64));
            double r = 0.0065 * scale;
            PrayerVfxGeometry.ellipsoid(matrix, out, mote, new Vec3(r, r * 1.4, r),
                    4, 6, 0.62f, 0.26f, 0.98f, 0.68f, angle);
        }
    }
}
