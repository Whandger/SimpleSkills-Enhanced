package com.github.ob_yekt.simpleskills.runes.tools;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.runes.RuneEquipmentVfxRenderTypes;
import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.ItemCategory;
import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.ItemGeometryInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

import java.util.List;

import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.drawOctahedralNode;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.drawRuneCircle;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.drawStarFlare;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.fract;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.lerp;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.lerpVec;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.mix;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.orthogonalVector;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.safeNormalize;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.texturedRibbonSegment;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.unit;

public final class ArcaneToolVfx {
    private ArcaneToolVfx() {}

    public static void submit(SubmitNodeCollector collector, PoseStack poseStack,
                              ItemGeometryInfo info, double time, int seed) {
        boolean isSword = info.category() == ItemCategory.SWORD;

        // 1. Corrente sutil de energia arcana em espiral ao redor da lâmina/cabeça
        collector.submitCustomGeometry(poseStack, RuneEquipmentVfxRenderTypes.flame(), (pose, vertices) ->
                drawArcaneEnergySpiral(pose.pose(), vertices, info, time, seed, isSword));

        // 2. Círculo rúnico duplo na guarda, runas flutuantes variadas e cristais orbitando por fora
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, vertices) ->
                drawRunesAndCrystals(pose.pose(), vertices, info, time, seed, isSword));
    }

    /**
     * Duas fitas finas e translúcidas de energia violeta-magenta espiralando
     * do selo rúnico até a ponta da lâmina (ou ao redor da cabeça da ferramenta).
     */
    private static void drawArcaneEnergySpiral(Matrix4fc matrix, VertexConsumer out, ItemGeometryInfo info,
                                               double time, int seed, boolean isSword) {
        Vec3 guard = info.guardPoint();
        Vec3 bladeStart = info.bladeStart();
        Vec3 bladeTip = info.bladeTip();
        Vec3 bladeVec = bladeTip.subtract(bladeStart);
        double bladeLen = Math.max(0.12, bladeVec.length());
        Vec3 bladeAxis = bladeVec.scale(1.0 / bladeLen);
        Vec3 perpXY = new Vec3(-bladeAxis.y, bladeAxis.x, 0.0).normalize();
        Vec3 perpZ = new Vec3(0.0, 0.0, 1.0);

        Vec3 start = isSword ? guard : lerpVec(guard, bladeStart, 0.5);
        Vec3 end = isSword ? bladeTip.add(bladeAxis.scale(0.02)) : bladeStart.add(bladeAxis.scale(bladeLen * 0.65));

        int strands = 2;
        double spin = time * 0.16;
        double scroll = time * 0.08;

        for (int s = 0; s < strands; s++) {
            double phaseOffset = s * Math.PI + unit(seed, 5 + s) * 2.0;
            int segments = 22;

            Vec3 prevCenter = null;
            Vec3 prevSideA = null;
            Vec3 prevSideB = null;
            double prevWidth = 0;
            double prevV = 0;
            float prevR = 0.58f, prevG = 0.22f, prevB = 1.0f, prevAlpha = 0.0f;

            for (int i = 0; i <= segments; i++) {
                double f = i / (double) segments;
                double envelope = Math.sin(Math.PI * Math.pow(f, 0.72));
                double angle = phaseOffset + f * Math.PI * 3.2 - spin;

                double radXY = (isSword ? lerp(0.095, 0.032, f) : 0.085) * (0.85 + 0.15 * envelope);
                double radZ = info.halfDepth() + 0.012;

                Vec3 pos = lerpVec(start, end, f)
                        .add(perpXY.scale(Math.cos(angle) * radXY))
                        .add(perpZ.scale(Math.sin(angle) * radZ));

                double width = Math.max(0.001, envelope * 0.014);
                double v = fract(f * 2.0 - scroll + s * 0.5);

                float r = (float) lerp(0.55, 0.88, envelope);
                float g = (float) lerp(0.18, 0.46, envelope);
                float b = 1.0f;
                float alpha = (float) (envelope * 0.42);

                Vec3 tangent = (prevCenter != null)
                        ? safeNormalize(pos.subtract(prevCenter), bladeAxis)
                        : bladeAxis;
                Vec3 sideA = orthogonalVector(tangent);
                Vec3 sideB = tangent.cross(sideA).normalize();

                if (prevCenter != null) {
                    texturedRibbonSegment(matrix, out, prevCenter, pos, prevSideA, sideA,
                            prevWidth, width, prevV, v,
                            prevR, prevG, prevB, prevAlpha, r, g, b, alpha);
                    texturedRibbonSegment(matrix, out, prevCenter, pos, prevSideB, sideB,
                            prevWidth * 0.75, width * 0.75, prevV, v,
                            prevR, prevG, prevB, prevAlpha * 0.75f, r, g, b, alpha * 0.75f);
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

    private static void drawRunesAndCrystals(Matrix4fc matrix, VertexConsumer out, ItemGeometryInfo info,
                                             double time, int seed, boolean isSword) {
        double pulse = 0.82 + 0.18 * Math.sin(time * 0.32 + seed * 0.01);

        Vec3 guard = info.guardPoint();
        Vec3 bladeStart = info.bladeStart();
        Vec3 bladeTip = info.bladeTip();
        Vec3 bladeVec = bladeTip.subtract(bladeStart);
        double bladeLen = Math.max(0.12, bladeVec.length());
        Vec3 bladeAxis = bladeVec.scale(1.0 / bladeLen);
        Vec3 perpXY = new Vec3(-bladeAxis.y, bladeAxis.x, 0.0).normalize();
        Vec3 perpZ = new Vec3(0.0, 0.0, 1.0);

        // 1. Selo rúnico duplo largo o suficiente para envolver por fora da guarda/pescoço do item
        double outerRadius = isSword ? 0.165 : 0.145;
        double innerRadius = isSword ? 0.128 : 0.112;
        Vec3 innerCenter = guard.add(bladeAxis.scale(0.024));

        drawRuneCircle(matrix, out, guard, bladeAxis, perpZ,
                outerRadius, time * 0.06,
                0.76f, 0.34f, 1.0f, (float) (0.92 * pulse), 0.0018);
        drawRuneCircle(matrix, out, innerCenter, bladeAxis, perpZ,
                innerRadius, -time * 0.09,
                0.92f, 0.64f, 1.0f, (float) (0.78 * pulse), 0.0013);

        // 2. 10 runas arcanas variadas flutuando ao longo das bordas externas da lâmina ou cabeça da ferramenta
        List<Vec3> head = info.headSamples();
        double faceZ = info.halfDepth() + 0.009;
        int glyphCount = 10;

        for (int i = 0; i < glyphCount; i++) {
            long gSeed = mix(seed * 59L + i * 373L);
            double life = fract(time * 0.026 + i * (1.0 / glyphCount) + unit(gSeed, 1));
            double fade = Math.sin(Math.PI * life);
            if (fade < 0.06) continue;

            double side = (i % 2 == 0) ? 1.0 : -1.0;
            double zSign = ((i / 2) % 2 == 0) ? 1.0 : -1.0;
            double drift = life * 0.032;
            double bob = Math.sin(time * 0.16 + i * 1.9) * 0.007;

            Vec3 anchor;
            if (isSword || head.isEmpty()) {
                double t = 0.08 + ((i + 0.5) / glyphCount) * 0.84;
                // Fica por fora da borda da lâmina para destacar sobre o fundo
                double edgeOffset = lerp(0.072, 0.044, t) + bob;
                anchor = lerpVec(bladeStart, bladeTip, t)
                        .add(bladeAxis.scale(drift))
                        .add(perpXY.scale(side * edgeOffset))
                        .add(0, 0, zSign * faceZ);
            } else {
                int idx = (int) ((i + 0.5) / glyphCount * head.size()) % head.size();
                Vec3 sample = head.get(idx);
                Vec3 outward = safeNormalize(sample.subtract(guard), bladeAxis);
                anchor = sample
                        .add(outward.scale(0.032 + drift + bob))
                        .add(0, 0, zSign * faceZ);
            }

            double glyphSize = 0.025 * (0.82 + 0.18 * fade);
            float alpha = (float) (0.94 * pulse * fade);
            drawArcaneGlyph(matrix, out, anchor, bladeAxis, perpXY,
                    glyphSize, i + seed, alpha);
        }

        // 3. 12 cristais octaédricos orbitando por fora da silhueta da lâmina/cabeça
        int motes = 12;
        for (int i = 0; i < motes; i++) {
            double frac = (i + 0.5) / motes;
            double ang = time * 0.14 + i * (Math.PI * 2.0 / motes) * 2.0;
            double orbitZ = info.halfDepth() + 0.015;

            Vec3 orb;
            if (isSword || head.isEmpty()) {
                Vec3 spinePt = lerpVec(bladeStart, bladeTip, frac);
                double orbitXY = lerp(0.084, 0.048, frac);
                orb = spinePt
                        .add(perpXY.scale(Math.cos(ang) * orbitXY))
                        .add(perpZ.scale(Math.sin(ang) * orbitZ));
            } else {
                int idx = (int) (frac * head.size()) % head.size();
                Vec3 sample = head.get(idx);
                Vec3 outward = safeNormalize(sample.subtract(guard), bladeAxis);
                Vec3 tangent = new Vec3(-outward.y, outward.x, 0.0).normalize();
                orb = sample
                        .add(outward.scale(0.028 + Math.cos(ang) * 0.024))
                        .add(tangent.scale(Math.sin(ang * 0.7) * 0.018))
                        .add(perpZ.scale(Math.sin(ang) * orbitZ));
            }

            drawOctahedralNode(matrix, out, orb, 0.0092,
                    0.82f, 0.50f, 1.0f, (float) (0.92 * pulse));
            drawStarFlare(matrix, out, orb, 0.016,
                    0.96f, 0.84f, 1.0f, (float) (0.55 * pulse));
        }
    }

    /**
     * Desenha 8 símbolos rúnicos arcanos distintos e bem legíveis,
     * com núcleo lilás claro e brilho violeta ao redor.
     */
    private static void drawArcaneGlyph(Matrix4fc matrix, VertexConsumer out, Vec3 center,
                                        Vec3 upDir, Vec3 rightDir, double size, int variant,
                                        float alpha) {
        double stroke = 0.0014;
        float r = 0.92f, g = 0.68f, b = 1.0f;

        Vec3 top = center.add(upDir.scale(size));
        Vec3 bot = center.subtract(upDir.scale(size));
        Vec3 left = center.subtract(rightDir.scale(size * 0.68));
        Vec3 right = center.add(rightDir.scale(size * 0.68));

        Vec3 topL = top.subtract(rightDir.scale(size * 0.62));
        Vec3 topR = top.add(rightDir.scale(size * 0.62));
        Vec3 botL = bot.subtract(rightDir.scale(size * 0.62));
        Vec3 botR = bot.add(rightDir.scale(size * 0.62));
        Vec3 midUp = center.add(upDir.scale(size * 0.35));
        Vec3 midDn = center.subtract(upDir.scale(size * 0.35));

        switch (Math.abs(variant) % 8) {
            case 0 -> { // Othala (losango superior com pernas cruzadas)
                line(matrix, out, top, left, stroke, r, g, b, alpha);
                line(matrix, out, top, right, stroke, r, g, b, alpha);
                line(matrix, out, left, botR, stroke, r, g, b, alpha);
                line(matrix, out, right, botL, stroke, r, g, b, alpha);
            }
            case 1 -> { // Dagaz (ampulheta / laço arcano)
                line(matrix, out, topL, botL, stroke, r, g, b, alpha);
                line(matrix, out, topR, botR, stroke, r, g, b, alpha);
                line(matrix, out, topL, botR, stroke, r, g, b, alpha);
                line(matrix, out, topR, botL, stroke, r, g, b, alpha);
            }
            case 2 -> { // Algiz (tridente rúnico)
                line(matrix, out, bot, top, stroke, r, g, b, alpha);
                line(matrix, out, center, topL, stroke, r, g, b, alpha);
                line(matrix, out, center, topR, stroke, r, g, b, alpha);
            }
            case 3 -> { // Ansuz (haste com duas bandeiras diagonais)
                line(matrix, out, botL, topL, stroke, r, g, b, alpha);
                line(matrix, out, topL, midUp.add(rightDir.scale(size * 0.65)), stroke, r, g, b, alpha);
                line(matrix, out, midUp.subtract(rightDir.scale(size * 0.62)), midDn.add(rightDir.scale(size * 0.65)), stroke, r, g, b, alpha);
            }
            case 4 -> { // Thurisaz (haste com triângulo lateral)
                line(matrix, out, botL, topL, stroke, r, g, b, alpha);
                line(matrix, out, midUp.subtract(rightDir.scale(size * 0.62)), right, stroke, r, g, b, alpha);
                line(matrix, out, right, midDn.subtract(rightDir.scale(size * 0.62)), stroke, r, g, b, alpha);
            }
            case 5 -> { // Ingwaz (losango cruzado)
                line(matrix, out, midUp, left, stroke, r, g, b, alpha);
                line(matrix, out, midUp, right, stroke, r, g, b, alpha);
                line(matrix, out, midDn, left, stroke, r, g, b, alpha);
                line(matrix, out, midDn, right, stroke, r, g, b, alpha);
                line(matrix, out, bot, top, stroke * 0.85, r, g, b, alpha);
            }
            case 6 -> { // Berkano (duplo triângulo na haste)
                line(matrix, out, botL, topL, stroke, r, g, b, alpha);
                line(matrix, out, topL, midUp.add(rightDir.scale(size * 0.55)), stroke, r, g, b, alpha);
                line(matrix, out, midUp.add(rightDir.scale(size * 0.55)), center.subtract(rightDir.scale(size * 0.62)), stroke, r, g, b, alpha);
                line(matrix, out, center.subtract(rightDir.scale(size * 0.62)), midDn.add(rightDir.scale(size * 0.55)), stroke, r, g, b, alpha);
                line(matrix, out, midDn.add(rightDir.scale(size * 0.55)), botL, stroke, r, g, b, alpha);
            }
            default -> { // Eihwaz (haste vertical com ganchos opostos)
                line(matrix, out, bot, top, stroke, r, g, b, alpha);
                line(matrix, out, top, midUp.add(rightDir.scale(size * 0.62)), stroke, r, g, b, alpha);
                line(matrix, out, bot, midDn.subtract(rightDir.scale(size * 0.62)), stroke, r, g, b, alpha);
            }
        }
    }

    private static void line(Matrix4fc matrix, VertexConsumer out, Vec3 a, Vec3 b,
                             double stroke, float r, float g, float bl, float alpha) {
        PrayerVfxGeometry.taperedTube(matrix, out, a, b, stroke, stroke, r, g, bl, alpha);
    }
}
