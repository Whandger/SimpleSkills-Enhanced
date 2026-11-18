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

import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.boundsCenter;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.fract;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.lerp;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.mix;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.orthogonalVector;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.safeNormalize;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.texturedRibbonSegment;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.unit;

public final class ShadowArmorVfx {
    private ShadowArmorVfx() {}

    public static void submit(SubmitNodeCollector collector, PoseStack poseStack,
                              Bounds bounds, double time, int seed) {
        collector.submitCustomGeometry(poseStack, RuneEquipmentVfxRenderTypes.shadow(), (pose, vertices) ->
                drawShadowSmokeWisps(pose.pose(), vertices, bounds, time, seed));
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, vertices) ->
                drawVoidTendrilsAndEmbers(pose.pose(), vertices, bounds, time, seed));
    }

    /**
     * Volutas densas de fumaça sombria que envolvem por fora da armadura,
     * sobem (-Y) e se desprendem suavemente no ar ao redor da silhueta.
     */
    private static void drawShadowSmokeWisps(Matrix4fc matrix, VertexConsumer out, Bounds bounds,
                                             double time, int seed) {
        boolean isLargePiece = bounds.height() > 0.45;
        int strands = isLargePiece ? 5 : 4;
        double pulse = 0.72 + 0.22 * Math.sin(time * 0.42 + seed * 0.013);

        double centerX = (bounds.minX() + bounds.maxX()) * 0.5;
        double centerZ = (bounds.minZ() + bounds.maxZ()) * 0.5;

        // Raio ligeiramente maior que a caixa da peça para nunca ser engolido pela armadura
        double baseRx = bounds.width() * 0.55 + 0.026;
        double baseRz = bounds.depth() * 0.55 + 0.026;

        // No espaço do modelo de entidade, maxY é a base e minY é o topo (-Y sobe)
        double startY = bounds.maxY() + 0.01;
        double endY = bounds.minY() - 0.075;

        for (int strand = 0; strand < strands; strand++) {
            long strandSeed = mix(seed * 211L + strand * 811L);
            double phase = time * 0.095 + strand * (Math.PI * 2.0 / strands) + unit(strandSeed, 1) * 2.5;
            double spiralTurns = 2.6 + unit(strandSeed, 2) * 1.1;
            int segments = 16;

            Vec3 prevCenter = null;
            Vec3 prevSideA = null;
            Vec3 prevSideB = null;
            double prevWidth = 0;
            double prevV = 0;
            float prevR = 0.12f, prevG = 0.02f, prevB = 0.26f, prevAlpha = 0.0f;

            for (int i = 0; i <= segments; i++) {
                double t = i / (double) segments;
                double envelope = Math.pow(Math.sin(Math.PI * Math.pow(t, 0.72)), 0.85);

                // Abre levemente para fora conforme sobe, como fumaça sombria escapando da armadura
                double flare = 1.0 + 0.30 * Math.pow(t, 1.35);
                double wobble = Math.sin(time * 0.35 + t * 5.0 + strand * 1.7) * 0.16;
                double angle = phase + t * spiralTurns + wobble;

                double x = centerX + Math.cos(angle) * baseRx * flare;
                double z = centerZ + Math.sin(angle) * baseRz * flare;
                double y = lerp(startY, endY, t);
                Vec3 center = new Vec3(x, y, z);

                double baseWidth = Math.max(0.028, Math.min(bounds.width(), bounds.height()) * 0.24);
                double width = envelope * baseWidth * (0.80 + 0.20 * Math.sin(time * 0.55 + strand * 2.1 + t * 6.0));
                double v = fract(t * 1.5 - time * 0.032 + strand * 0.29);

                // Gradiente sombrio: preto-índigo profundo na base -> violeta abissal no corpo -> escuro na ponta
                float r = (float) lerp(0.08, 0.38, envelope);
                float g = (float) lerp(0.01, 0.07, envelope);
                float b = (float) lerp(0.20, 0.72, envelope);
                float alpha = (float) Math.min(0.95, envelope * pulse * 0.92);

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
                            prevWidth * 0.78, width * 0.78, prevV, v,
                            prevR * 0.65f, prevG * 0.65f, prevB * 0.80f, prevAlpha * 0.78f,
                            r * 0.65f, g * 0.65f, b * 0.80f, alpha * 0.78f);
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

    /**
     * Filamentos sinuosos de energia do vazio contornando a armadura por fora
     * e fagulhas sombrias subindo ao redor.
     */
    private static void drawVoidTendrilsAndEmbers(Matrix4fc matrix, VertexConsumer out, Bounds bounds,
                                                  double time, int seed) {
        double scale = bounds.strokeScale();
        Vec3 center = boundsCenter(bounds);
        double baseRx = bounds.width() * 0.56 + 0.030;
        double baseRz = bounds.depth() * 0.56 + 0.030;

        boolean isLargePiece = bounds.height() > 0.45;
        int tendrils = isLargePiece ? 5 : 4;

        // 1. Filamentos energéticos violeta-magenta serpenteando pelo contorno externo
        for (int f = 0; f < tendrils; f++) {
            long fSeed = mix(seed * 41L + f * 613L);
            double baseAngle = time * 0.11 + f * (Math.PI * 2.0 / tendrils) + unit(fSeed, 1) * 3.0;
            double twist = (f % 2 == 0 ? 1.0 : -1.0) * (1.8 + unit(fSeed, 2) * 0.9);
            double yBottom = lerp(bounds.maxY(), bounds.minY(), 0.05 + unit(fSeed, 3) * 0.25);
            double yTop = bounds.minY() - (0.04 + unit(fSeed, 4) * 0.05);

            int steps = 10;
            Vec3 prev = null;
            double prevR = 0.0;

            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                double env = Math.sin(Math.PI * Math.pow(t, 0.75));
                double flare = 1.02 + 0.32 * Math.pow(t, 1.4);
                double ang = baseAngle + t * twist + Math.sin(time * 0.30 + t * 4.5 + f) * 0.18;

                Vec3 pos = new Vec3(
                        center.x + Math.cos(ang) * baseRx * flare,
                        lerp(yBottom, yTop, t),
                        center.z + Math.sin(ang) * baseRz * flare);

                double radius = (0.0046 * scale) * env * (1.0 - 0.35 * t);

                if (prev != null && radius > 0.0001) {
                    // Aura violeta profunda
                    PrayerVfxGeometry.taperedTube(matrix, out, prev, pos,
                            prevR, radius,
                            0.46f, 0.10f, 0.92f, (float) (0.58 * env));
                    // Núcleo fino lilás-orquídea brilhante
                    PrayerVfxGeometry.taperedTube(matrix, out, prev, pos,
                            prevR * 0.38, radius * 0.38,
                            0.84f, 0.48f, 1.00f, (float) (0.82 * env));
                }

                prev = pos;
                prevR = radius;
            }
        }

        // 2. Fagulhas/brasas do vazio subindo suavemente ao redor da armadura
        int embers = isLargePiece ? 8 : 5;
        for (int i = 0; i < embers; i++) {
            long eSeed = mix(seed * 131L + i * 487L);
            double life = fract(time * 0.028 + i * (1.0 / embers) + unit(eSeed, 1));
            double fade = Math.sin(Math.PI * life);
            if (fade < 0.06) continue;

            double angle = unit(eSeed, 2) * Math.PI * 2.0 + time * 0.04 + life * 0.8;
            double radial = 0.96 + unit(eSeed, 3) * 0.26 + life * 0.16;
            double startY = lerp(bounds.maxY() - 0.02, bounds.minY() + 0.04, unit(eSeed, 4));
            double riseY = -life * (0.12 + unit(eSeed, 5) * 0.08);

            Vec3 pos = new Vec3(
                    center.x + Math.cos(angle) * baseRx * radial,
                    startY + riseY,
                    center.z + Math.sin(angle) * baseRz * radial);

            double r = (0.0042 + unit(eSeed, 6) * 0.0020) * scale * fade;
            // Halo roxo escuro
            PrayerVfxGeometry.ellipsoid(matrix, out, pos, new Vec3(r * 1.4, r * 2.2, r * 1.4),
                    4, 6, 0.42f, 0.08f, 0.88f, (float) (0.55 * fade), 0.0);
            // Núcleo lilás intenso
            PrayerVfxGeometry.ellipsoid(matrix, out, pos, new Vec3(r * 0.65, r * 1.1, r * 0.65),
                    4, 6, 0.88f, 0.56f, 1.00f, (float) (0.90 * fade), 0.0);
        }
    }
}
