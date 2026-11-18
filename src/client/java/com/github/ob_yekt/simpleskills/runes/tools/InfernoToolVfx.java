package com.github.ob_yekt.simpleskills.runes.tools;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.runes.RuneEquipmentVfxRenderTypes;
import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.ItemGeometryInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

import java.util.List;

import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.fract;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.lerp;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.lerpVec;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.mix;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.orthogonalVector;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.safeNormalize;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.texturedRibbonSegment;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.unit;

public final class InfernoToolVfx {
    private InfernoToolVfx() {}

    public static void submit(SubmitNodeCollector collector, PoseStack poseStack,
                              ItemGeometryInfo info, double time, int seed) {
        collector.submitCustomGeometry(poseStack, RuneEquipmentVfxRenderTypes.flame(), (pose, vertices) ->
                drawRibbons(pose.pose(), vertices, info, time, seed));
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, vertices) ->
                drawTonguesAndEmbers(pose.pose(), vertices, info, time, seed));
    }

    private static void drawRibbons(Matrix4fc matrix, VertexConsumer out, ItemGeometryInfo info,
                                    double time, int seed) {
        List<Vec3> head = info.headSamples();
        if (head.isEmpty()) return;

        int tongues = 14;
        double scroll = time * 0.20;
        double zOut = info.halfDepth() + 0.003;

        for (int tIdx = 0; tIdx < tongues; tIdx++) {
            long tSeed = mix(seed * 89L + tIdx * 0x9E3779B97F4A7C15L);
            int sampleIdx = (int) ((tIdx + 0.5) / tongues * head.size()) % head.size();
            double zSign = (tIdx % 2 == 0) ? 1.0 : -1.0;
            Vec3 origin = head.get(sampleIdx).add(0, 0, zSign * zOut);

            double outX = (unit(tSeed, 1) - 0.5) * 0.036;
            double outY = 0.038 + unit(tSeed, 2) * 0.034;
            double outZ = zSign * (0.004 + unit(tSeed, 3) * 0.008);
            Vec3 lickDir = new Vec3(outX, outY, outZ);

            int segments = 7;
            Vec3 prevCenter = null;
            Vec3 prevSideA = null;
            Vec3 prevSideB = null;
            double prevWidth = 0;
            double prevV = 0;
            float prevR = 1.0f, prevG = 0.96f, prevB = 0.65f, prevAlpha = 0.0f;

            for (int i = 0; i <= segments; i++) {
                double f = i / (double) segments;
                double profile = Math.sin(Math.PI * Math.pow(f, 0.52)) * (1.0 - 0.3 * f);
                double wave = Math.sin(time * 3.1 + f * 7.0 + tIdx * 2.3) * 0.010 * f;

                Vec3 pos = origin.add(lickDir.scale(f)).add(wave, 0, -wave * 0.5);
                double flicker = 0.78 + 0.22 * Math.sin(time * 4.2 + tIdx * 3.1 + f * 11.0);
                double width = Math.max(0.001, profile * 0.015 * flicker);
                double v = fract(f * 1.45 - scroll + tIdx * 0.31);

                float r = 1.0f;
                float g = (float) lerp(0.95, 0.22, Math.pow(f, 0.72));
                float b = (float) lerp(0.62, 0.02, Math.min(1.0, f * 1.8));
                float alpha = (float) (profile * (0.76 + 0.22 * flicker));

                Vec3 tangent = (prevCenter != null)
                        ? safeNormalize(pos.subtract(prevCenter), new Vec3(0, 1, 0))
                        : new Vec3(0, 1, 0);
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

    private static void drawTonguesAndEmbers(Matrix4fc matrix, VertexConsumer out, ItemGeometryInfo info,
                                             double time, int seed) {
        List<Vec3> head = info.headSamples();
        if (head.isEmpty()) return;

        double zOut = info.halfDepth() + 0.003;
        int plumes = 16;
        for (int i = 0; i < plumes; i++) {
            long pSeed = mix(seed * 131L + i * 509L);
            double cycle = time * 0.44 + i * 0.17 + unit(pSeed, 1);
            double life = fract(cycle);
            int idx = (int) ((i + 0.5) / plumes * head.size()) % head.size();
            double zSign = (i % 2 == 0) ? 1.0 : -1.0;
            Vec3 base = head.get(idx).add(0, 0, zSign * zOut);

            double height = (0.028 + unit(pSeed, 2) * 0.026) * (0.76 + 0.24 * Math.sin(time * 4.6 + i * 2.1));
            double swayX = Math.sin(time * 3.3 + i * 1.9) * 0.009 + (unit(pSeed, 4) - 0.5) * 0.012;
            double swayZ = zSign * (0.004 + unit(pSeed, 5) * 0.005) + Math.cos(time * 3.0 + i * 2.3) * 0.004;

            Vec3 mid = base.add(swayX * 0.5, height * 0.52, swayZ * 0.5);
            Vec3 tip = base.add(swayX * 1.25, height, swayZ * 1.25);

            double radius = 0.0068 + unit(pSeed, 3) * 0.0028;
            PrayerVfxGeometry.taperedTube(matrix, out, base, mid, radius, radius * 0.70,
                    1.0f, 0.44f, 0.04f, 0.68f);
            PrayerVfxGeometry.taperedTube(matrix, out, mid, tip, radius * 0.70, 0.0004,
                    0.94f, 0.15f, 0.02f, 0.58f);
            PrayerVfxGeometry.taperedTube(matrix, out, base, lerpVec(base, tip, 0.58),
                    radius * 0.46, 0.0003,
                    1.0f, 0.95f, 0.54f, 0.90f);

            Vec3 sparkFrom = tip.add(swayX * life * 1.3, life * 0.038, swayZ * life * 1.3);
            Vec3 sparkTo = sparkFrom.add(swayX * 0.2, 0.011, swayZ * 0.2);
            float sparkAlpha = (float) (Math.sin(Math.PI * life) * 0.92);
            PrayerVfxGeometry.taperedTube(matrix, out, sparkFrom, sparkTo,
                    0.0015, 0.0002, 1.0f, 0.84f, 0.22f, sparkAlpha);
        }
    }
}
