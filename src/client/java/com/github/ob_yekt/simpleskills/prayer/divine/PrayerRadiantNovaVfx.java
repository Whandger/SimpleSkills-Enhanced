package com.github.ob_yekt.simpleskills.prayer.divine;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerRadiantNovaVfx {
    private PrayerRadiantNovaVfx() {}

    // 8. RADIANT_NOVA — Uma estrela divina comprime energia em singularidade e detona
    // em uma supernova sagrada com raios solares 3D, ondas de choque e coroa no solo.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        double safeDuration = Math.max(1.0, duration);
        double t = Math.max(0.0, Math.min(1.0, age / safeDuration));

        double fadeIn = PrayerVfxPrimitives.smooth(0.0, 0.08, t);
        double fadeOutRaw = 1.0 - PrayerVfxPrimitives.smooth(0.62, 1.0, t);
        double fadeOut = fadeOutRaw * fadeOutRaw;
        float fade = (float) (fadeIn * fadeOut);
        if (fade <= 0.005f) return;

        Vec3 center = new Vec3(0.0, 1.15, 0.0);
        Vec3 ground = new Vec3(0.0, 0.04, 0.0);

        // Ponto de detonação em 24% da duração
        double detonateT = 0.24;
        double maxRadius = 6.2;

        // ------------------------------------------------------------------
        // FASE 1: COMPRESSÃO ESTELAR / SINGULARIDADE (0% -> 26%)
        // ------------------------------------------------------------------
        if (t <= detonateT + 0.04) {
            double charge = Math.min(1.0, t / detonateT);
            double chargeEase = 1.0 - Math.pow(1.0 - charge, 2.0);
            // Colapso rápido no último instante antes de explodir
            double collapse = t > detonateT - 0.05
                    ? 1.0 - PrayerVfxPrimitives.smooth(detonateT - 0.05, detonateT + 0.02, t) * 0.72
                    : 1.0;

            double coreR = (0.72 - chargeEase * 0.36) * collapse;
            float chargeAlpha = (float) (fadeIn * (t > detonateT ? (1.0 - (t - detonateT) / 0.04) : 1.0));

            // Núcleo solar comprimindo
            PrayerVfxGeometry.ellipsoid(m, out, center,
                    new Vec3(coreR, coreR, coreR), 12, 18,
                    1.0f, 0.98f, 0.85f, 0.96f * chargeAlpha, age * 0.18);
            PrayerVfxGeometry.ellipsoid(m, out, center,
                    new Vec3(coreR * 1.55, coreR * 1.55, coreR * 1.55), 10, 16,
                    1.0f, 0.80f, 0.24f, 0.42f * chargeAlpha, -age * 0.14);

            // Anéis astrolábicos fechando sobre a estrela
            double ringR = (2.4 - chargeEase * 1.75) * collapse;
            PrayerVfxPrimitives.ringPlane(m, out, center, PrayerVfxPrimitives.UP,
                    ringR, 0.026, age * 0.14,
                    1.0f, 0.86f, 0.32f, 0.88f * chargeAlpha);
            PrayerVfxPrimitives.ringPlane(m, out, center, new Vec3(0.4, 1.0, 0.2).normalize(),
                    ringR * 0.85, 0.020, -age * 0.18,
                    1.0f, 0.96f, 0.70f, 0.82f * chargeAlpha);
            PrayerVfxPrimitives.runeHalo(m, out, center, ringR * 0.92, age * 0.12,
                    chargeAlpha, 1.0f, 0.90f, 0.45f);

            // 16 Filamentos de luz sendo sugados para o centro
            for (int i = 0; i < 16; i++) {
                Vec3 dir = fibonacciSphereDir(i, 16, age * 0.03);
                double outerDist = 3.2 * (1.0 - chargeEase * 0.78);
                double innerDist = coreR * 0.9;
                if (outerDist > innerDist + 0.1) {
                    Vec3 pOuter = center.add(dir.scale(outerDist));
                    Vec3 pInner = center.add(dir.scale(innerDist));
                    PrayerVfxGeometry.taperedTube(m, out, pOuter, pInner,
                            0.012, 0.055,
                            1.0f, 0.92f, 0.52f, (float) (chargeEase * 0.85 * chargeAlpha));
                }
            }
        }

        // ------------------------------------------------------------------
        // FASE 2: DETONAÇÃO DE SUPERNOVA RADIANTE (24% -> 100%)
        // ------------------------------------------------------------------
        if (t >= detonateT) {
            double bt = (t - detonateT) / (1.0 - detonateT); // 0 -> 1 pós-detonação
            // Curva explosiva: sai violentamente no início e desacelera suavemente
            double blastEase = 1.0 - Math.pow(1.0 - bt, 3.4);
            double waveEase = 1.0 - Math.pow(1.0 - bt, 2.4);
            float blastFade = (float) (Math.pow(1.0 - bt, 1.25) * fade);

            // 1. Clarão cegante inicial (Starburst 3D)
            if (bt < 0.28) {
                float flashA = (float) Math.pow(1.0 - (bt / 0.28), 1.5) * fade;
                double flashScale = 1.2 + (bt / 0.28) * 5.2;
                PrayerVfxPrimitives.starburst(m, out, center, PrayerVfxPrimitives.UP,
                        flashScale, flashA, 1.0f, 0.99f, 0.88f);
                PrayerVfxPrimitives.starburst(m, out, center, new Vec3(1, 0.2, 0.3).normalize(),
                        flashScale * 0.85, flashA * 0.9f, 1.0f, 0.90f, 0.45f);
            }

            // 2. Esfera volumétrica da Nova expandindo em 3 camadas
            double shellR = 0.35 + blastEase * maxRadius;
            float shellAlpha = (float) Math.pow(1.0 - bt, 1.65) * fade;

            if (shellAlpha > 0.005f) {
                // Aura externa dourado-solar
                PrayerVfxGeometry.ellipsoid(m, out, center,
                        new Vec3(shellR, shellR * 0.92, shellR), 14, 22,
                        1.0f, 0.76f, 0.20f, 0.22f * shellAlpha, age * 0.02);
                // Camada intermediária brilhante
                PrayerVfxGeometry.ellipsoid(m, out, center,
                        new Vec3(shellR * 0.76, shellR * 0.72, shellR * 0.76), 12, 18,
                        1.0f, 0.92f, 0.52f, 0.34f * shellAlpha, -age * 0.03);
                // Núcleo branco incandescente remanescente
                double coreRemnant = Math.max(0.08, (1.15 - bt * 1.05));
                PrayerVfxGeometry.ellipsoid(m, out, center,
                        new Vec3(coreRemnant, coreRemnant, coreRemnant), 10, 16,
                        1.0f, 0.99f, 0.90f, 0.90f * shellAlpha, age * 0.08);
            }

            // 3. 24 Raios Solares Perfurantes (God Rays) disparando em 3D
            int rayCount = 24;
            for (int i = 0; i < rayCount; i++) {
                Vec3 dir = fibonacciSphereDir(i, rayCount, 0.45);
                double lenFactor = 0.78 + (i % 4) * 0.14;
                double tipDist = (0.5 + blastEase * (maxRadius * 1.18)) * lenFactor;
                double tailDist = Math.max(0.15, tipDist * (0.22 + bt * 0.58));

                if (tipDist > tailDist + 0.05) {
                    Vec3 tail = center.add(dir.scale(tailDist));
                    Vec3 tip = center.add(dir.scale(tipDist));
                    double thickness = (0.14 - bt * 0.09) * (i % 3 == 0 ? 1.35 : 0.9);

                    // Aura dourada do raio solar
                    PrayerVfxGeometry.taperedTube(m, out, tail, tip,
                            thickness * 1.8, 0.01,
                            1.0f, 0.80f, 0.24f, 0.38f * blastFade);
                    // Feixe interno branco-dourado
                    PrayerVfxGeometry.taperedTube(m, out, tail, tip,
                            thickness * 0.75, 0.004,
                            1.0f, 0.98f, 0.84f, 0.92f * blastFade);
                }
            }

            // 4. Três ondas de choque orbitais no ar
            for (int w = 0; w < 3; w++) {
                double wR = 0.4 + waveEase * (maxRadius * (1.05 - w * 0.14));
                Vec3 normal = w == 0
                        ? PrayerVfxPrimitives.UP
                        : (w == 1 ? new Vec3(0.85, 0.35, 0.15).normalize()
                                  : new Vec3(-0.25, 0.35, 0.90).normalize());
                double ringThick = (0.11 - w * 0.022) * (1.0 - bt * 0.55);

                PrayerVfxPrimitives.ringPlane(m, out, center, normal,
                        wR, Math.max(0.012, ringThick),
                        age * (w % 2 == 0 ? 0.04 : -0.05),
                        1.0f, 0.88f + w * 0.04f, 0.42f + w * 0.15f,
                        0.85f * blastFade);
            }

            // 5. Onda de choque no solo + Estrela Solar de 12 pontas varrendo o chão
            double groundWaveR = 0.5 + waveEase * (maxRadius * 1.08);
            PrayerVfxPrimitives.ringPlane(m, out, ground, PrayerVfxPrimitives.UP,
                    groundWaveR, 0.14 * (1.0 - bt * 0.5), age * 0.03,
                    1.0f, 0.78f, 0.22f, 0.88f * blastFade);
            PrayerVfxPrimitives.ringPlane(m, out, ground.add(0, 0.02, 0), PrayerVfxPrimitives.UP,
                    groundWaveR * 0.78, 0.05, -age * 0.05,
                    1.0f, 0.97f, 0.76f, 0.85f * blastFade);
            PrayerVfxPrimitives.runeHalo(m, out, ground.add(0, 0.015, 0),
                    groundWaveR * 0.62, age * 0.03, blastFade * 0.85f,
                    1.0f, 0.90f, 0.46f);

            // Estrela solar de 12 pontas desenhada no chão expandindo
            double starR = groundWaveR * 0.92;
            for (int tri = 0; tri < 3; tri++) {
                drawSacredSquare(m, out, ground.add(0, 0.015, 0), starR,
                        age * 0.02 + tri * (Math.PI / 6.0), 0.022 * (1.0 - bt * 0.5),
                        0.68f * blastFade);
            }

            // 6. Estilhaços de luz estelar arremessados pela explosão
            for (int s = 0; s < 18; s++) {
                Vec3 dir = fibonacciSphereDir(s + 7, 18, 1.1);
                double dist = 0.8 + blastEase * (maxRadius * (0.75 + (s % 4) * 0.12));
                Vec3 pos = center.add(dir.scale(dist));
                double crystalLen = 0.48 * (1.0 - bt * 0.65);

                PrayerVfxPrimitives.crystal(m, out, pos, dir,
                        crystalLen, 0.085 * (1.0 - bt * 0.5),
                        1.0f, 0.90f, 0.45f, 0.90f * blastFade);
            }
        }
    }

    private static Vec3 fibonacciSphereDir(int index, int total, double yawOffset) {
        double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));
        double y = 1.0 - (2.0 * (index + 0.5) / total);
        double radiusAtY = Math.sqrt(Math.max(0.0, 1.0 - y * y));
        double theta = goldenAngle * index + yawOffset;
        return new Vec3(Math.cos(theta) * radiusAtY, y, Math.sin(theta) * radiusAtY).normalize();
    }

    private static void drawSacredSquare(Matrix4fc m, VertexConsumer out, Vec3 center,
                                         double radius, double rotation, double thickness, float alpha) {
        if (alpha <= 0.005f || radius <= 0.05) return;
        Vec3[] pts = new Vec3[4];
        for (int i = 0; i < 4; i++) {
            double a = rotation + i * (Math.PI * 0.5);
            pts[i] = center.add(Math.cos(a) * radius, 0.0, Math.sin(a) * radius);
        }
        for (int i = 0; i < 4; i++) {
            PrayerVfxGeometry.tube(m, out, pts[i], pts[(i + 1) % 4],
                    thickness, 1.0f, 0.92f, 0.50f, alpha);
        }
    }
}
