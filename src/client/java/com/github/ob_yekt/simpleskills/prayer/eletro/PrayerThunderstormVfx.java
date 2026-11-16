package com.github.ob_yekt.simpleskills.prayer.eletro;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerThunderstormVfx {
    private PrayerThunderstormVfx() {}

    // 24. THUNDERSTORM — a massive dark storm cell builds overhead, unleashes all bolts, and disperses outward into the wind.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 8, 12);
        double progress = age / (double) duration;

        // Formação (0.0 -> 0.18) | Fim dos raios (~0.74) | Dispersão lateral da nuvem (0.74 -> 1.0)
        double build = PrayerVfxPrimitives.smooth(0.0, 0.18, progress);
        double dissipate = PrayerVfxPrimitives.smooth(0.74, 1.0, progress);
        double cloudFade = build * (1.0 - dissipate);

        if (cloudFade <= 0.001) return;

        double cloudHeight = 12.5;
        double areaRadius = 7.2;

        // =========================================================================
        // 1. NUVEM DE TEMPESTADE (SE ESPALHA PARA OS LADOS NA SAÍDA SEM ENCOLHER)
        // =========================================================================
        int baseLobes = 14;
        for (int lobe = 0; lobe < baseLobes; lobe++) {
            double ringRatio = (lobe % 2 == 0) ? 0.85 : 0.48;
            double angle = (lobe * Math.PI * 2 / baseLobes) + age * (0.012 * (1.0 - dissipate * 0.5));

            // Na saída, os lóbulos derivam para longe do centro (espalhando para os lados)
            double lateralDrift = dissipate * (3.2 + (lobe % 3) * 0.85);
            double dist = areaRadius * ringRatio * (0.65 + 0.35 * build) + lateralDrift;
            double waveY = Math.sin(age * 0.07 + lobe * 1.3) * 0.28 * (1.0 - dissipate);

            Vec3 center = new Vec3(
                    Math.cos(angle) * dist,
                    cloudHeight + waveY + (dissipate * 0.55),
                    Math.sin(angle) * dist
            );

            // Mantém o tamanho formado (build) e estica horizontalmente enquanto achata ao se desfazer
            double baseSize = (2.2 + (lobe % 3) * 0.55) * build;
            double spreadWidth = baseSize * (1.0 + dissipate * 0.45);
            double flattenHeight = baseSize * 0.38 * (1.0 - dissipate * 0.45);

            PrayerVfxGeometry.ellipsoid(m, out, center,
                    new Vec3(spreadWidth, flattenHeight, spreadWidth * 0.88),
                    8, 12, 0.08f, 0.12f, 0.21f, (float) (0.82 * fade * cloudFade), angle);
        }

        int topLobes = 8;
        for (int top = 0; top < topLobes; top++) {
            double angle = (top * Math.PI * 2 / topLobes) - age * 0.008;
            double lateralDrift = dissipate * (2.6 + (top % 2) * 0.9);
            double dist = areaRadius * 0.42 * (0.6 + 0.4 * build) + lateralDrift;

            Vec3 center = new Vec3(
                    Math.cos(angle) * dist,
                    cloudHeight + 1.35 + Math.cos(age * 0.06 + top) * 0.22 * (1.0 - dissipate) + (dissipate * 0.7),
                    Math.sin(angle) * dist
            );

            double baseSize = (2.6 + (top % 2) * 0.6) * build;
            double spreadWidth = baseSize * (1.0 + dissipate * 0.4);
            double flattenHeight = baseSize * 0.46 * (1.0 - dissipate * 0.5);

            PrayerVfxGeometry.ellipsoid(m, out, center,
                    new Vec3(spreadWidth, flattenHeight, spreadWidth * 0.92),
                    8, 12, 0.05f, 0.08f, 0.15f, (float) (0.74 * fade * cloudFade), -angle);
        }

        // =========================================================================
        // 2. CORRENTES DE VENTO E ARCOS ELÉTRICOS INTRA-NUVEM (PARAM ANTES DA NUVEM SUMIR)
        // =========================================================================
        double spin = age * (0.022 * (1.0 - dissipate * 0.7));
        // Os anéis de vento também abrem para os lados enquanto somem
        PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, cloudHeight - 0.55, 0), PrayerVfxPrimitives.UP,
                areaRadius * (0.92 * build + dissipate * 0.35), 0.055 * cloudFade, spin,
                0.16f, 0.32f, 0.55f, (float) (0.45 * fade * cloudFade));

        PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, cloudHeight - 0.35, 0), PrayerVfxPrimitives.UP,
                areaRadius * (0.52 * build + dissipate * 0.28), 0.04 * cloudFade, -spin * 1.6,
                0.24f, 0.46f, 0.72f, (float) (0.42 * fade * cloudFade));

        // Arcos internos cessam completamente aos 72% da duração, antes da nuvem dissipar
        double arcIntensity = build * (1.0 - PrayerVfxPrimitives.smooth(0.58, 0.72, progress));
        if (arcIntensity > 0.02) {
            int strandCount = 10;
            for (int strand = 0; strand < strandCount; strand++) {
                double angle = strand * (Math.PI * 2 / strandCount) + age * 0.025;
                double r1 = areaRadius * (0.25 + (strand % 3) * 0.25) * build;
                double r2 = areaRadius * (0.35 + ((strand + 1) % 3) * 0.24) * build;

                Vec3 from = new Vec3(Math.cos(angle) * r1, cloudHeight - 0.15 + (strand % 2) * 0.25, Math.sin(angle) * r1);
                Vec3 to = new Vec3(Math.cos(angle + 0.65) * r2, cloudHeight - 0.3, Math.sin(angle + 0.65) * r2);

                PrayerVfxPrimitives.bolt(m, out, from, to,
                        strand * 331L + (long) (age * 0.7), 6, 0.24, (float) (0.75 * fade * arcIntensity));
            }
        }

        // =========================================================================
        // 3. DESCARGAS DE RAIOS (ENCERRAM COMPLETAMENTE ANTES DA NUVEM SE DISSIPAR)
        // =========================================================================
        int totalStrikes = 10;
        double strikeDuration = 11.0;
        // Garante que até o último raio termine sua animação por volta de 74% da duração total
        double latestEnd = duration * 0.74;
        double firstStart = 6.0;
        double lastStart = Math.max(firstStart + 1.0, latestEnd - strikeDuration);
        double interval = (lastStart - firstStart) / Math.max(1, totalStrikes - 1);

        for (int strike = 0; strike < totalStrikes; strike++) {
            double startAge = firstStart + strike * interval;
            double phase = (age - startAge) / strikeDuration;

            if (phase <= 0.0 || phase >= 1.0) continue;

            double drop = PrayerVfxPrimitives.smooth(0.0, 0.20, phase);
            double impactLife = 1.0 - PrayerVfxPrimitives.smooth(0.55, 1.0, phase);

            double strobe = 0.72 + 0.28 * Math.sin(age * 2.6 + strike * 1.7);
            double strikeAlpha = fade * impactLife * strobe;

            if (strikeAlpha <= 0.01) continue;

            double angle = strike * 2.39996 + 0.4;
            double distFromCenter = 1.2 + ((strike * 37) % 10) / 10.0 * (areaRadius - 1.6);

            Vec3 ground = new Vec3(Math.cos(angle) * distFromCenter, 0.1, Math.sin(angle) * distFromCenter);
            Vec3 cloudOrigin = new Vec3(
                    Math.cos(angle + 0.15) * (distFromCenter * 0.85),
                    cloudHeight - 0.25,
                    Math.sin(angle + 0.15) * (distFromCenter * 0.85)
            );

            Vec3 currentTip = cloudOrigin.lerp(ground, drop);
            long boltSeed = strike * 1301L + (long) (age * 0.9);

            // 3.1. Feixe de 3 Raios Entrelaçados
            PrayerVfxPrimitives.bolt(m, out, cloudOrigin, currentTip,
                    boltSeed, 18, 0.55, (float) strikeAlpha);
            PrayerVfxPrimitives.bolt(m, out, cloudOrigin, currentTip,
                    boltSeed + 99L, 14, 0.32, (float) (strikeAlpha * 0.9));
            PrayerVfxPrimitives.bolt(m, out, cloudOrigin, currentTip,
                    boltSeed + 271L, 10, 0.16, (float) (strikeAlpha * 0.85));

            // 3.2. Raízes elétricas na saída da nuvem
            for (int root = 0; root < 3; root++) {
                double rAngle = angle + (root - 1) * 1.1 + age * 0.04;
                Vec3 rootStart = cloudOrigin.add(Math.cos(rAngle) * 1.3, 0.2, Math.sin(rAngle) * 1.3);
                Vec3 rootJoin = cloudOrigin.lerp(currentTip, 0.14);
                PrayerVfxPrimitives.bolt(m, out, rootStart, rootJoin,
                        boltSeed + root * 211L, 5, 0.18, (float) (strikeAlpha * 0.65));
            }

            // 3.3. Duas Ramificações Secundárias no Ar
            if (drop > 0.35) {
                Vec3 midHigh = cloudOrigin.lerp(ground, 0.35);
                Vec3 forkEnd1 = cloudOrigin.lerp(ground, 0.78).add(
                        Math.cos(angle + 1.15) * 1.9,
                        0.0,
                        Math.sin(angle + 1.15) * 1.9
                );
                PrayerVfxPrimitives.bolt(m, out, midHigh, forkEnd1,
                        boltSeed + 477L, 9, 0.32, (float) (strikeAlpha * 0.8));
            }

            if (drop > 0.62) {
                Vec3 midLow = cloudOrigin.lerp(ground, 0.62);
                Vec3 forkEnd2 = ground.add(
                        Math.cos(angle - 1.25) * 1.5,
                        0.6 * (1.0 - drop * 0.4),
                        Math.sin(angle - 1.25) * 1.5
                );
                PrayerVfxPrimitives.bolt(m, out, midLow, forkEnd2,
                        boltSeed + 853L, 7, 0.24, (float) (strikeAlpha * 0.72));
            }

            // 3.4. Efeitos de Impacto no Solo
            if (drop > 0.85) {
                double groundProgress = PrayerVfxPrimitives.smooth(0.20, 1.0, phase);

                double shockRadius = 0.35 + groundProgress * 2.6;
                PrayerVfxPrimitives.ringPlane(m, out, ground, PrayerVfxPrimitives.UP,
                        shockRadius, 0.045 * impactLife, age * 0.1,
                        0.55f, 0.88f, 1.0f, (float) (0.9 * strikeAlpha));

                double flashRadius = 0.52 * impactLife;
                PrayerVfxGeometry.ellipsoid(m, out, ground.add(0, 0.12, 0),
                        new Vec3(flashRadius, flashRadius * 0.55, flashRadius), 6, 10,
                        0.82f, 0.96f, 1.0f, (float) (0.88 * strikeAlpha), age * 0.2);

                for (int spark = 0; spark < 6; spark++) {
                    double sAngle = spark * (Math.PI * 2 / 6.0) + strike + age * 0.05;
                    double sReach = (0.7 + groundProgress * 1.7) * impactLife;
                    Vec3 sparkEnd = ground.add(Math.cos(sAngle) * sReach, 0.08 + (spark % 2) * 0.12, Math.sin(sAngle) * sReach);
                    PrayerVfxPrimitives.bolt(m, out, ground, sparkEnd,
                            (strike * 6 + spark) * 521L + (long) age, 5, 0.18, (float) (0.9 * strikeAlpha));
                }
            }
        }
    }
}
