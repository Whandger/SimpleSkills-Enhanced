package com.github.ob_yekt.simpleskills.prayer.eletro;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerStormJudgmentVfx {
    private PrayerStormJudgmentVfx() {}

    // 25. STORM_JUDGMENT — a celestial electric seal focuses into a single broad, branching verdict.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        // Fade suave apenas nas pontas extremas para garantir zero corte brusco
        float fade = PrayerVfxPrimitives.fade(age, duration, 6, 8);
        double progress = age / (double) duration;

        // 1. Fases da animação:
        // Carga (0.0 -> 0.32) | Queda (0.32 -> 0.42) | Impacto (0.40 -> 0.72) | Dissipação/Saída (0.72 -> 1.0)
        double charge = PrayerVfxPrimitives.smooth(0.0, 0.32, progress);
        double strikeDrop = PrayerVfxPrimitives.smooth(0.32, 0.42, progress);
        double impact = PrayerVfxPrimitives.smooth(0.40, 0.48, progress);
        double aftershock = PrayerVfxPrimitives.smooth(0.42, 0.85, progress);
        double dissipate = PrayerVfxPrimitives.smooth(0.72, 1.0, progress);

        // Fator de sobrevivência (1.0 durante o pico, cai suavemente para 0.0 na saída)
        double alive = 1.0 - dissipate;

        // Pulso elétrico de alta frequência (diminui conforme o efeito perde energia na saída)
        float flicker = 0.85f + 0.15f * (float) Math.sin(age * 1.9) * (float) alive;
        float strikePulse = (float) (impact * (1.0 - aftershock * 0.45) * alive) * flicker;

        Vec3 crown = new Vec3(0, 14, 0);
        Vec3 ground = new Vec3(0, 0.12, 0);

        // =========================================================================
        // FASE 1: O SELO CELESTIAL (COROA) - Expande na carga, contrai/desfaz na saída
        // =========================================================================
        if (alive > 0.001) {
            // Na saída, os anéis externos expandem levemente enquanto somem e os internos colapsam
            double outerCollapse = 1.0 - Math.pow(dissipate, 1.6);
            double innerCollapse = 1.0 - PrayerVfxPrimitives.smooth(0.70, 0.94, progress);

            double sealScale = ((0.3 + charge * 0.85) - (impact * 0.25)) * outerCollapse;
            double innerScale = ((0.3 + charge * 0.85) - (impact * 0.25)) * innerCollapse;

            // A rotação acelera na carga e desacelera na saída como um giroscópio perdendo força
            double spinBoost = age * (0.03 + charge * 0.08 * alive);

            // Anel externo principal (Azul Elétrico Profundo)
            PrayerVfxPrimitives.ringPlane(m, out, crown, PrayerVfxPrimitives.UP, 3.8 * sealScale,
                    0.065 * outerCollapse, spinBoost, 0.28f, 0.68f, 1.0f,
                    (float) (0.9 * fade * outerCollapse));

            // Anéis giroscópicos internos (perdem o alinhamento e fecham antes do anel externo)
            if (innerCollapse > 0.01) {
                double tiltFactor = (1.0 - (charge * 0.65)) + (dissipate * 0.8);
                PrayerVfxPrimitives.ringPlane(m, out, crown, new Vec3(1.0 * tiltFactor, 0.35, 0.2 * tiltFactor),
                        2.7 * innerScale, 0.04 * innerCollapse, -spinBoost * 1.4, 0.75f, 0.92f, 1.0f,
                        (float) (0.92 * fade * innerCollapse));
                PrayerVfxPrimitives.ringPlane(m, out, crown, new Vec3(0.2 * tiltFactor, 0.35, 1.0 * tiltFactor),
                        1.9 * innerScale, 0.03 * innerCollapse, spinBoost * 1.9, 0.45f, 0.82f, 1.0f,
                        (float) (0.85 * fade * innerCollapse));

                // Halo de runas girando no centro do selo
                PrayerVfxPrimitives.runeHalo(m, out, crown, 1.25 * innerScale, -age * 0.045,
                        (float) (fade * innerCollapse), 0.78f, 0.93f, 1.0f);
            }

            // Núcleo de energia condensada na coroa (implodindo no final)
            double coreScale = Math.max(0.0, 1.0 - PrayerVfxPrimitives.smooth(0.75, 0.96, progress));
            if (coreScale > 0.01) {
                double coreRadius = (0.25 + charge * 0.45 - impact * 0.2) * flicker * coreScale;
                PrayerVfxGeometry.ellipsoid(m, out, crown, new Vec3(coreRadius, coreRadius * 1.4, coreRadius), 10, 14,
                        0.70f, 0.92f, 1.0f, (float) (0.88 * fade * coreScale), age * 0.18);
            }

            // Arcos elétricos internos carregando a coroa (param antes da dissipação)
            double arcStrength = charge * (1.0 - PrayerVfxPrimitives.smooth(0.65, 0.82, progress));
            if (arcStrength > 0.05) {
                int crownArcs = 5;
                for (int i = 0; i < crownArcs; i++) {
                    double arcAngle = (i * (Math.PI * 2 / crownArcs)) + age * 0.09;
                    Vec3 rimPoint = crown.add(
                            Math.cos(arcAngle) * (3.5 * sealScale),
                            Math.sin(age * 0.2 + i) * 0.25,
                            Math.sin(arcAngle) * (3.5 * sealScale)
                    );
                    PrayerVfxPrimitives.bolt(m, out, crown, rimPoint, i * 317L + (long) (age * 0.8), 5, 0.22,
                            (float) (0.75 * arcStrength * fade));
                }
            }
        }

        // =========================================================================
        // FASE 2: O JULGAMENTO (PILAR DE RAIO DESCENDO E SE RETRAINDO NA SAÍDA)
        // =========================================================================
        double beamRetract = PrayerVfxPrimitives.smooth(0.72, 0.92, progress);
        double beamAlive = 1.0 - beamRetract;

        if (strikeDrop > 0.001 && beamAlive > 0.001) {
            // Na entrada desce de Y=14 até Y=0.12; na saída se recolhe de volta para o céu (Y=0.12 -> Y=14)
            double currentTipY = 14.0 - (14.0 - 0.12) * strikeDrop * beamAlive;
            Vec3 boltTip = new Vec3(0, currentTipY, 0);

            // O pilar afina progressivamente até virar um fio fino antes de desaparecer
            double thicknessDecay = Math.pow(beamAlive, 1.3);

            // Aura externa larga (Azul Ciano Translúcido)
            double outerRadiusTop = 0.75 * flicker * thicknessDecay;
            double outerRadiusBot = 0.32 * flicker * thicknessDecay;
            PrayerVfxGeometry.taperedTube(m, out, crown, boltTip,
                    outerRadiusTop, outerRadiusBot, 0.18f, 0.62f, 1.0f,
                    (float) (0.30 * fade * (1.0 - aftershock * 0.3) * beamAlive));

            // Núcleo interno incandescente (Branco-Azulado Intenso)
            double innerRadiusTop = 0.26 * flicker * thicknessDecay;
            double innerRadiusBot = 0.11 * flicker * thicknessDecay;
            PrayerVfxGeometry.taperedTube(m, out, crown, boltTip,
                    innerRadiusTop, innerRadiusBot, 0.82f, 0.96f, 1.0f,
                    (float) (0.95 * fade * beamAlive));

            // 3 Raios serrilhados envolvendo o pilar central (se dissipam um pouco antes do núcleo)
            double helixAlive = 1.0 - PrayerVfxPrimitives.smooth(0.68, 0.86, progress);
            if (helixAlive > 0.01) {
                for (int helix = 0; helix < 3; helix++) {
                    double offsetAngle = (helix * Math.PI * 2 / 3.0) + age * 0.35;
                    double spiralRadius = 0.35 * helixAlive;
                    Vec3 startOffset = crown.add(Math.cos(offsetAngle) * spiralRadius, 0, Math.sin(offsetAngle) * spiralRadius);
                    Vec3 endOffset = boltTip.add(Math.cos(offsetAngle + 1.5) * (spiralRadius * 0.7), 0, Math.sin(offsetAngle + 1.5) * (spiralRadius * 0.7));

                    PrayerVfxPrimitives.bolt(m, out, startOffset, endOffset,
                            helix * 733L + (long) (age * 0.75), 10, 0.38 * helixAlive,
                            (float) (0.92 * fade * helixAlive));
                }
            }
        }

        // =========================================================================
        // FASE 3: IMPACTO E RAMIFICAÇÕES NO SOLO (ENCOLHEM NA SAÍDA)
        // =========================================================================
        double groundDissipate = PrayerVfxPrimitives.smooth(0.70, 0.96, progress);
        double groundAlive = 1.0 - groundDissipate;

        if (impact > 0.001 && groundAlive > 0.001) {
            // Esfera de flash no ponto de impacto (encolhe até sumir no chão)
            double flashSize = (1.1 - aftershock * 0.5) * flicker * groundAlive;
            if (flashSize > 0.02) {
                PrayerVfxGeometry.ellipsoid(m, out, ground.add(0, 0.2 * groundAlive, 0),
                        new Vec3(flashSize, flashSize * 0.65, flashSize), 8, 12,
                        0.80f, 0.95f, 1.0f, (float) (strikePulse * 0.85 * fade * groundAlive), -age * 0.15);
            }

            // 10 Ramificações de raios rasteiros que avançam no impacto e recuam na saída
            int branchCount = 10;
            for (int branch = 0; branch < branchCount; branch++) {
                double angle = branch * (Math.PI * 2 / branchCount) + (branch % 2 == 0 ? 0.15 : -0.15) + age * 0.012;
                double reach = (1.8 + (branch % 3) * 0.85) * (0.4 + impact * 0.6 + aftershock * 0.3) * groundAlive;
                double arcHeight = ((branch % 3 == 0) ? 0.45 : 0.12) * groundAlive;

                Vec3 end = ground.add(
                        Math.cos(angle) * reach,
                        arcHeight * (1.0 - aftershock * 0.5),
                        Math.sin(angle) * reach
                );

                PrayerVfxPrimitives.bolt(m, out, ground, end,
                        branch * 449L + (long) (age * 0.6), 7, 0.22 * groundAlive,
                        (float) (0.95 * fade * (1.0 - aftershock * 0.4) * groundAlive));
            }

            // Onda de choque primária (rápida e fina)
            double shockwave1 = 0.3 + aftershock * 4.2;
            float alpha1 = (float) Math.max(0, (1.0 - aftershock) * 0.9) * fade;
            if (alpha1 > 0.01f) {
                PrayerVfxPrimitives.ringPlane(m, out, ground, PrayerVfxPrimitives.UP,
                        shockwave1, 0.055 * (1.0 - aftershock * 0.7), age * 0.08,
                        0.55f, 0.88f, 1.0f, alpha1);
            }

            // Onda de choque secundária (mais lenta e profunda, acompanha a dissipação final)
            double shockwave2 = 0.2 + aftershock * 2.8;
            float alpha2 = (float) Math.max(0, (1.0 - aftershock * 0.85) * 0.65 * groundAlive) * fade;
            if (alpha2 > 0.01f) {
                PrayerVfxPrimitives.ringPlane(m, out, ground.add(0, 0.08, 0), PrayerVfxPrimitives.UP,
                        shockwave2, 0.035 * groundAlive, -age * 0.05,
                        0.25f, 0.65f, 1.0f, alpha2);
            }
        }
    }
}
