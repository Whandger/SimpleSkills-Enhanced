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

import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.clamp;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.fract;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.lerp;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.mix;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.orthogonalVector;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.safeNormalize;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.texturedRibbonSegment;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.unit;

public final class FrostToolVfx {
    private FrostToolVfx() {}

    public static void submit(SubmitNodeCollector collector, PoseStack poseStack,
                              ItemGeometryInfo info, double time, int seed) {
        collector.submitCustomGeometry(poseStack, RuneEquipmentVfxRenderTypes.shadow(), (pose, vertices) ->
                drawColdMist(pose.pose(), vertices, info, time, seed));
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, vertices) ->
                draw(pose.pose(), vertices, info, time, seed));
    }

    private static void draw(Matrix4fc matrix, VertexConsumer out, ItemGeometryInfo info,
                             double time, int seed) {
        List<Vec3> all = info.allSamples();
        List<Vec3> head = info.headSamples();
        if (all.isEmpty()) return;

        double pulse = 0.78 + 0.20 * Math.sin(time * 0.9 + seed * 0.02);
        Vec3 bladeAxis = safeNormalize(info.bladeTip().subtract(info.bladeStart()), new Vec3(1, 1, 0));
        Vec3 frontEdgeDir = new Vec3(-bladeAxis.y, bladeAxis.x, 0.0).normalize();

        // 1. 40 pequenos flocos de neve finos (6 pontas afiladas) na frente e em ambas as faces do equipamento
        int flakes = 40;
        for (int i = 0; i < flakes; i++) {
            long pSeed = mix(seed * 67L + i * 331L);
            double life = fract(time * 0.032 + i * (1.0 / flakes) + unit(pSeed, 1));

            List<Vec3> pool = (!head.isEmpty() && i % 3 == 0) ? head : all;
            int idx = (int) (unit(pSeed, 2) * pool.size()) % pool.size();
            Vec3 origin = pool.get(idx);

            double zSign = (i % 2 == 0) ? 1.0 : -1.0;
            // Nasce levemente à frente do fio/borda e é levado pelo vento para trás ao longo do ciclo
            double windSweep = (0.018 - life * 0.042) + (unit(pSeed, 5) - 0.5) * 0.016;
            double driftX = Math.sin(time * 0.08 + i * 2.0) * 0.014 + (unit(pSeed, 3) - 0.5) * 0.018;
            double driftY = 0.010 - life * 0.048;
            double driftZ = zSign * (info.halfDepth() + 0.007 + unit(pSeed, 4) * 0.018);

            Vec3 pos = origin
                    .add(frontEdgeDir.scale(windSweep))
                    .add(driftX, driftY, driftZ);

            double fade = Math.sin(Math.PI * life);
            double radius = (0.0036 + unit(pSeed, 6) * 0.0018) * (0.75 + 0.25 * fade);
            drawFineSnowflake(matrix, out, pos, radius, time * 0.10 + i, 0.94 * pulse * fade);
        }

        // 2. 32 micro-cristais de gelo cintilando bem próximos à superfície e frente do equipamento
        int specks = 32;
        for (int i = 0; i < specks; i++) {
            long sSeed = mix(seed * 97L + i * 733L);
            double life = fract(time * 0.044 + i * (1.0 / specks) + unit(sSeed, 1));

            List<Vec3> pool = (!head.isEmpty() && i % 3 == 0) ? head : all;
            int idx = (int) ((i + 0.5) / specks * pool.size()) % pool.size();
            double zSign = ((i / 2) % 2 == 0) ? 1.0 : -1.0;
            double windSweep = (0.014 - life * 0.032) + (unit(sSeed, 4) - 0.5) * 0.014;

            Vec3 pos = pool.get(idx)
                    .add(frontEdgeDir.scale(windSweep))
                    .add(
                            (unit(sSeed, 2) - 0.5) * 0.018,
                            0.007 - life * 0.034,
                            zSign * (info.halfDepth() + 0.005 + unit(sSeed, 3) * 0.012));

            double fade = Math.sin(Math.PI * life);
            double r = (0.0013 + unit(sSeed, 5) * 0.0006) * fade;
            drawTinyIceShard(matrix, out, pos, r, 0.92 * pulse * fade);
        }
    }

    /** Desenha um floco de neve de 6 pontas bem fino e afilado, sem barra grossa no eixo Z. */
    private static void drawFineSnowflake(Matrix4fc matrix, VertexConsumer out, Vec3 center,
                                          double radius, double spin, double alpha) {
        double centerThickness = Math.max(0.00024, radius * 0.075);
        double tipThickness = 0.00010;
        float a = (float) alpha;

        for (int k = 0; k < 6; k++) {
            double ang = spin + k * (Math.PI / 3.0);
            Vec3 tip = center.add(Math.cos(ang) * radius, Math.sin(ang) * radius, 0.0);
            PrayerVfxGeometry.taperedTube(matrix, out, center, tip,
                    centerThickness, tipThickness,
                    0.82f, 0.97f, 1.0f, a);
        }
    }

    /** Desenha um micro-cristal de gelo em formato de losango afilado. */
    private static void drawTinyIceShard(Matrix4fc matrix, VertexConsumer out, Vec3 center,
                                         double radius, double alpha) {
        double h = radius * 1.45;
        float a = (float) alpha;
        PrayerVfxGeometry.taperedTube(matrix, out, center, center.add(0, h, 0),
                radius, 0.0001, 0.76f, 0.96f, 1.0f, a);
        PrayerVfxGeometry.taperedTube(matrix, out, center, center.add(0, -h, 0),
                radius, 0.0001, 0.76f, 0.96f, 1.0f, a);
    }

    /**
     * Rajadas longas de névoa gelada que vêm pela frente do equipamento,
     * contornam as faces em 3D e escapam para trás.
     */
    private static void drawColdMist(Matrix4fc matrix, VertexConsumer out, ItemGeometryInfo info,
                                     double time, int seed) {
        List<Vec3> all = info.allSamples();
        if (all.isEmpty()) return;

        Vec3 bladeAxis = safeNormalize(info.bladeTip().subtract(info.bladeStart()), new Vec3(1, 1, 0));
        // frontDir aponta para a frente/fio do equipamento; -frontDir aponta para trás
        Vec3 frontDir = new Vec3(-bladeAxis.y, bladeAxis.x, 0.0).normalize();

        int wisps = 10;
        double zOut = info.halfDepth() + 0.006;

        for (int w = 0; w < wisps; w++) {
            long wSeed = mix(seed * 179L + w * 463L);
            int idx = (int) ((w + 0.5) / wisps * all.size()) % all.size();
            Vec3 anchor = all.get(idx);

            double zSign = (w % 2 == 0) ? 1.0 : -1.0;
            boolean wrapFrontEdge = (w % 2 == 0);
            double phase = time * 0.16 + w * 1.7 + unit(wSeed, 1) * 6.0;
            double alongDrift = (unit(wSeed, 2) - 0.45) * 0.055;
            double depthSpread = zOut + unit(wSeed, 3) * 0.008;

            int segments = 20;
            Vec3 prevPos = null;
            Vec3 prevSideA = null;
            Vec3 prevSideB = null;
            double prevWidth = 0;
            double prevV = 0;
            float prevAlpha = 0.0f;

            for (int i = 0; i <= segments; i++) {
                double t = i / (double) segments;
                double envelope = Math.pow(Math.sin(Math.PI * t), 0.72);

                // Deslocamento contínuo da frente (+frontDir) para trás (-frontDir) do equipamento
                double frontToBack = lerp(0.075, -0.095, t);
                double arcOutward = Math.sin(Math.PI * t) * 0.016;

                // Transição suave em Z para contornar o equipamento de um lado para o outro
                double wrapWindow = wrapFrontEdge
                        ? clamp((t - 0.12) / 0.36, 0.0, 1.0)  // Contorna cedo pela borda da frente
                        : clamp((t - 0.52) / 0.36, 0.0, 1.0); // Passa pela face frontal e contorna por trás
                double smoothWrap = wrapWindow * wrapWindow * (3.0 - 2.0 * wrapWindow);
                double currentZ = lerp(zSign * depthSpread, -zSign * (depthSpread + 0.006), smoothWrap);

                double windRipple = Math.sin(t * 5.0 - phase) * 0.013 * Math.sin(Math.PI * t);
                double zWave = Math.cos(t * 4.2 - phase * 0.85) * 0.006 * Math.sin(Math.PI * t);

                Vec3 pos = anchor
                        .add(frontDir.scale(frontToBack + (wrapFrontEdge ? arcOutward : -arcOutward * 0.4)))
                        .add(bladeAxis.scale(t * alongDrift + windRipple))
                        .add(0, -t * 0.022, currentZ + zWave);

                double width = envelope * 0.015;
                double v = fract(t * 2.5 - time * 0.075 + w * 0.25);
                float alpha = (float) (envelope * 0.40);

                Vec3 tangent = (prevPos != null)
                        ? safeNormalize(pos.subtract(prevPos), frontDir.scale(-1.0))
                        : frontDir.scale(-1.0);
                Vec3 sideA = orthogonalVector(tangent);
                Vec3 sideB = tangent.cross(sideA).normalize();

                if (prevPos != null) {
                    texturedRibbonSegment(matrix, out, prevPos, pos, prevSideA, sideA,
                            prevWidth, width, prevV, v,
                            0.72f, 0.94f, 1.0f, prevAlpha,
                            0.90f, 0.99f, 1.0f, alpha);
                    texturedRibbonSegment(matrix, out, prevPos, pos, prevSideB, sideB,
                            prevWidth * 0.75, width * 0.75, prevV, v,
                            0.72f, 0.94f, 1.0f, prevAlpha * 0.75f,
                            0.90f, 0.99f, 1.0f, alpha * 0.75f);
                }

                prevPos = pos;
                prevSideA = sideA;
                prevSideB = sideB;
                prevWidth = width;
                prevV = v;
                prevAlpha = alpha;
            }
        }
    }
}