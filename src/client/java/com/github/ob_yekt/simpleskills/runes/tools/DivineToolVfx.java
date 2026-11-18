package com.github.ob_yekt.simpleskills.runes.tools;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
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
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.mix;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.safeNormalize;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.unit;

public final class DivineToolVfx {
    private DivineToolVfx() {}

    public static void submit(SubmitNodeCollector collector, PoseStack poseStack,
                              ItemGeometryInfo info, double time, int seed) {
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, vertices) ->
                draw(pose.pose(), vertices, info, time, seed));
    }

    private static void draw(Matrix4fc matrix, VertexConsumer out, ItemGeometryInfo info,
                             double time, int seed) {
        List<Vec3> head = info.headSamples();
        List<Vec3> pool = head.isEmpty() ? info.allSamples() : head;
        if (pool.isEmpty()) return;

        Vec3 bladeAxis = safeNormalize(info.bladeTip().subtract(info.bladeStart()), new Vec3(1, 1, 0));
        Vec3 perpXY = new Vec3(-bladeAxis.y, bladeAxis.x, 0.0).normalize();
        double zOut = info.halfDepth() + 0.006;

        // 24 pontos de luz dourada assíncronos:
        // ~2.0s brilhando suavemente perto da lâmina -> ~0.4s explodindo em estrela
        int count = 24;
        double cycleRate = 0.021;     // ~2.38s no total (20 ticks/s)
        double explodeStart = 0.82;   // ~1.95s carregando antes de estourar

        for (int i = 0; i < count; i++) {
            long pSeed = mix(seed * 79L + i * 353L);
            double life = fract(time * cycleRate + i * (1.0 / count) + unit(pSeed, 1));

            int idx = (int) (unit(pSeed, 2) * pool.size()) % pool.size();
            Vec3 anchor = pool.get(idx);

            double zSign = (i % 2 == 0) ? 1.0 : -1.0;
            double sideSign = ((i / 2) % 2 == 0) ? 1.0 : -1.0;

            // Posiciona próximo às bordas/frente e nas duas faces da lâmina
            double edgeOffset = sideSign * (0.010 + unit(pSeed, 3) * 0.024);
            double alongOffset = (unit(pSeed, 4) - 0.5) * 0.018;
            double zOffset = zSign * (zOut + unit(pSeed, 5) * 0.012);

            // Flutuação leve enquanto espera para estourar
            double driftProgress = Math.min(life / explodeStart, 1.0);
            double driftAlong = driftProgress * (0.014 + unit(pSeed, 6) * 0.012);
            double driftOut = sideSign * driftProgress * 0.008
                    + Math.sin(time * 0.14 + i * 2.1) * 0.003;

            Vec3 pos = anchor
                    .add(perpXY.scale(edgeOffset + driftOut))
                    .add(bladeAxis.scale(alongOffset + driftAlong))
                    .add(0, 0, zOffset);

            double starTilt = unit(pSeed, 7) * (Math.PI * 0.25);

            if (life < explodeStart) {
                // FASE 1 (0s -> ~2s): Pequeno brilho dourado pulsante
                double t = life / explodeStart;
                double fadeIn = Math.min(1.0, t / 0.12);
                double shimmer = 0.78 + 0.22 * Math.sin(time * 0.35 + i * 2.7);
                // Dá uma leve intensificada nos últimos 0.3s antes da explosão
                double prePop = t > 0.85 ? 1.0 + ((t - 0.85) / 0.15) * 0.45 : 1.0;

                double r = (0.0020 + unit(pSeed, 8) * 0.0010) * fadeIn * prePop;
                float alpha = (float) (fadeIn * shimmer);

                // Halo dourado quente
                PrayerVfxGeometry.ellipsoid(matrix, out, pos, new Vec3(r * 1.55, r * 1.55, r * 0.6),
                        4, 6, 1.00f, 0.80f, 0.22f, alpha * 0.48f, 0.0);
                // Núcleo claro
                PrayerVfxGeometry.ellipsoid(matrix, out, pos, new Vec3(r * 0.72, r * 0.72, r * 0.4),
                        4, 6, 1.00f, 0.96f, 0.68f, alpha * 0.95f, 0.0);

                // Micro-cintilação em cruz bem sutil
                drawDelicateStar(matrix, out, pos, r * 2.6, starTilt, alpha * 0.55f);
            } else {
                // FASE 2 (~2.0s -> ~2.4s): Explosão em estrela
                double burstT = (life - explodeStart) / (1.0 - explodeStart);
                double expand = 1.0 - Math.pow(1.0 - burstT, 2.6);
                double fadeOut = Math.pow(1.0 - burstT, 1.25);

                double maxRadius = 0.020 + unit(pSeed, 8) * 0.010;
                double currentRadius = lerp(0.003, maxRadius, expand);
                double spin = starTilt + burstT * 0.35;

                // Flash central rápido no início do estouro
                if (burstT < 0.45) {
                    double flashFade = 1.0 - (burstT / 0.45);
                    double flashR = 0.0042 * (1.0 - burstT * 0.5);
                    PrayerVfxGeometry.ellipsoid(matrix, out, pos, new Vec3(flashR, flashR, flashR * 0.4),
                            4, 6, 1.00f, 0.99f, 0.85f, (float) (flashFade * 0.95), 0.0);
                }

                // Estrela de 8 pontas (4 raios principais longos + 4 diagonais curtos)
                drawExplosionStar(matrix, out, pos, currentRadius, spin, (float) fadeOut);
            }
        }
    }

    /** Cruz luminosa fina durante a fase de brilho. */
    private static void drawDelicateStar(Matrix4fc matrix, VertexConsumer out, Vec3 center,
                                         double radius, double angle, float alpha) {
        double thick = Math.max(0.00022, radius * 0.07);
        for (int k = 0; k < 4; k++) {
            double a = angle + k * (Math.PI * 0.5);
            Vec3 tip = center.add(Math.cos(a) * radius, Math.sin(a) * radius, 0.0);
            PrayerVfxGeometry.taperedTube(matrix, out, center, tip,
                    thick, 0.00006, 1.00f, 0.92f, 0.52f, alpha);
        }
    }

    /** Estrela pontiaguda de 8 raios (4 principais + 4 diagonais) da explosão. */
    private static void drawExplosionStar(Matrix4fc matrix, VertexConsumer out, Vec3 center,
                                          double radius, double angle, float alpha) {
        double mainThick = Math.max(0.00042, radius * 0.028);
        double diagThick = mainThick * 0.65;

        // 4 pontas principais longas (branco-dourado intenso)
        for (int k = 0; k < 4; k++) {
            double a = angle + k * (Math.PI * 0.5);
            Vec3 tip = center.add(Math.cos(a) * radius, Math.sin(a) * radius, 0.0);
            PrayerVfxGeometry.taperedTube(matrix, out, center, tip,
                    mainThick, 0.00008, 1.00f, 0.96f, 0.70f, alpha * 0.95f);
        }

        // 4 pontas diagonais menores (ouro quente)
        double diagRadius = radius * 0.52;
        for (int k = 0; k < 4; k++) {
            double a = angle + Math.PI * 0.25 + k * (Math.PI * 0.5);
            Vec3 tip = center.add(Math.cos(a) * diagRadius, Math.sin(a) * diagRadius, 0.0);
            PrayerVfxGeometry.taperedTube(matrix, out, center, tip,
                    diagThick, 0.00006, 1.00f, 0.82f, 0.28f, alpha * 0.80f);
        }
    }
}
