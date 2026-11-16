package com.github.ob_yekt.simpleskills.prayer.eletro;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerChainLightningVfx {
    private PrayerChainLightningVfx() {}

    // 23. CHAIN_LIGHTNING — an instantaneous bolt strikes the primary target and immediately forks into rapid branching jumps.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 1, 3);
        double progress = age / (double) duration;

        double strobe = 0.82 + 0.18 * Math.sin(age * 3.8);
        long seedBase = 0xC4A1L + (long) (age * 1.2);

        double dist = destination.length();
        Vec3 dir = dist > 0.001 ? destination.scale(1.0 / dist) : new Vec3(0, 0, 1);

        // =========================================================================
        // 1. DISPARO PRINCIPAL (ESTALA NO ALVO EM 0.05 E SOME EM 0.12)
        // =========================================================================
        double mainHead = PrayerVfxPrimitives.smooth(0.00, 0.05, progress);
        double mainFlashOut = 1.0 - PrayerVfxPrimitives.smooth(0.05, 0.13, progress);

        if (mainHead > 0.001 && mainFlashOut > 0.001) {
            Vec3 currentEnd = destination.scale(mainHead);
            float mainAlpha = (float) (fade * mainFlashOut * strobe);

            PrayerVfxPrimitives.bolt(m, out, Vec3.ZERO, currentEnd,
                    seedBase, 18, 0.34, mainAlpha);
            PrayerVfxPrimitives.bolt(m, out, Vec3.ZERO, currentEnd,
                    seedBase + 131L, 12, 0.18, (float) (mainAlpha * 0.85));

            // Estalo na origem
            for (int c = 0; c < 3; c++) {
                double cAngle = c * (Math.PI * 2 / 3.0) + age * 0.35;
                Vec3 sparkTip = dir.scale(0.65).add(
                        Math.cos(cAngle) * 0.45,
                        Math.sin(cAngle) * 0.45,
                        Math.sin(cAngle + 1.0) * 0.45
                );
                PrayerVfxPrimitives.bolt(m, out, Vec3.ZERO, sparkTip,
                        seedBase + c * 97L, 4, 0.12, mainAlpha * 0.85f);
            }
        }

        // =========================================================================
        // 2. IMPACTO PRIMÁRIO NO ALVO PRINCIPAL
        // =========================================================================
        double primaryHitIn = PrayerVfxPrimitives.smooth(0.03, 0.06, progress);
        double primaryHitOut = 1.0 - PrayerVfxPrimitives.smooth(0.08, 0.28, progress);
        double primaryImpactLife = primaryHitIn * primaryHitOut;

        if (primaryImpactLife > 0.01) {
            float hitAlpha = (float) (fade * primaryImpactLife * strobe);

            double coreSize = 0.28 * primaryImpactLife;
            PrayerVfxGeometry.ellipsoid(m, out, destination,
                    new Vec3(coreSize, coreSize * 1.25, coreSize), 8, 12,
                    0.82f, 0.96f, 1.0f, 0.92f * hitAlpha, age * 0.4);

            PrayerVfxPrimitives.ringPlane(m, out, destination, dir,
                    0.2 + (1.0 - primaryHitOut) * 1.15,
                    0.032 * primaryImpactLife, age * 0.18,
                    0.48f, 0.85f, 1.0f, 0.85f * hitAlpha);
        }

        // =========================================================================
        // 3. SALTOS EM CADEIA QUASE INSTANTÂNEOS (DISPARAM ASSIM QUE O PRIMEIRO BATE)
        // =========================================================================
        int hopCount = 4;
        Vec3 previousNode = destination.add(0, 0.25, 0);
        double baseYaw = Math.atan2(dir.z, dir.x);

        for (int hop = 0; hop < hopCount; hop++) {
            // Todos disparam logo após o impacto primário (0.04 -> 0.055 -> 0.07 -> 0.085)
            double hopStart = 0.04 + hop * 0.015;
            double hopHit = hopStart + 0.035;
            double hopOut = 1.0 - PrayerVfxPrimitives.smooth(hopHit + 0.02, hopHit + 0.14, progress);

            double headProgress = PrayerVfxPrimitives.smooth(hopStart, hopHit, progress);

            double zigzag = (hop % 2 == 0 ? 1.0 : -1.0) * (0.68 + hop * 0.12);
            double hopAngle = baseYaw + zigzag + Math.sin(age * 0.02 + hop) * 0.08;
            double hopDist = 1.95 + (hop % 2) * 0.55;
            double hopHeight = ((hop % 2 == 0) ? 0.42 : -0.35);

            Vec3 nextNode = previousNode.add(
                    Math.cos(hopAngle) * hopDist,
                    hopHeight,
                    Math.sin(hopAngle) * hopDist
            );

            // Raio estala instantaneamente e apaga com flash (sem ficar arrastando)
            if (headProgress > 0.001 && hopOut > 0.001) {
                Vec3 arcEnd = previousNode.lerp(nextNode, headProgress);

                double chainDecay = 1.0 - (hop * 0.12);
                float hopAlpha = (float) (fade * hopOut * chainDecay * strobe);
                int segments = Math.max(7, 13 - hop * 2);
                double jitter = 0.24 * chainDecay;

                PrayerVfxPrimitives.bolt(m, out, previousNode, arcEnd,
                        seedBase + (hop + 1) * 739L, segments, jitter, hopAlpha);

                if (hop < 2) {
                    PrayerVfxPrimitives.bolt(m, out, previousNode, arcEnd,
                            seedBase + (hop + 1) * 1103L, segments - 2, jitter * 0.55, hopAlpha * 0.7f);
                }
            }

            // Flash e faíscas nos alvos atingidos (persistem um pouco mais que o raio para marcar onde bateu)
            double nodeFlashIn = PrayerVfxPrimitives.smooth(hopHit - 0.01, hopHit + 0.01, progress);
            double nodeFlashOut = 1.0 - PrayerVfxPrimitives.smooth(hopHit + 0.04, hopHit + 0.26, progress);
            double nodeLife = nodeFlashIn * nodeFlashOut;

            if (nodeLife > 0.01) {
                double chainDecay = 1.0 - (hop * 0.12);
                float nodeAlpha = (float) (fade * nodeLife * chainDecay * strobe);
                double sphereRadius = (0.16 + nodeLife * 0.14) * chainDecay;

                PrayerVfxGeometry.wireSphere(m, out, nextNode, sphereRadius, 8,
                        age * 0.22 + hop, 0.58f, 0.88f, 1.0f,
                        nodeAlpha, 0.014 * chainDecay);

                for (int s = 0; s < 2; s++) {
                    double sAng = hopAngle + (s == 0 ? 1.4 : -1.4) + age * 0.08;
                    double sparkReach = (0.2 + (1.0 - nodeFlashOut) * 0.55) * chainDecay;
                    Vec3 sparkOut = nextNode.add(
                            Math.cos(sAng) * sparkReach,
                            (s == 0 ? 0.3 : -0.25) * sparkReach,
                            Math.sin(sAng) * sparkReach
                    );
                    PrayerVfxPrimitives.bolt(m, out, nextNode, sparkOut,
                            seedBase + hop * 313L + s * 59L, 4, 0.11, nodeAlpha * 0.85f);
                }
            }

            previousNode = nextNode;
        }
    }
}
