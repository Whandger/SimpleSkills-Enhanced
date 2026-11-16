package com.github.ob_yekt.simpleskills.prayer.divine;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerCelestialWrathVfx {
    private PrayerCelestialWrathVfx() {}

    // Área 30x30 blocos (raio de 15.0 blocos)
    private static final double AREA_RADIUS = 15.0;
    private static final double SKY_HEIGHT = 22.0;
    private static final int PILLAR_COUNT = 16;

    // 10. CELESTIAL_WRATH — Um selo serafínico colossal rasga os céus sobre uma área 30x30,
    // despejando 16 pilares de fogo sagrado seguidos por um julgamento central devastador.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 6, 10);
        if (fade <= 0.01f) return;

        double t = clamp01(age / Math.max(1.0, duration));

        // 1. Abertura do selo no céu e demarcação do solo (0% -> 24%)
        double open = PrayerVfxPrimitives.smooth(0.0, 0.24, t);
        // 2. Dissipação gradual dos céus no final (78% -> 100%)
        double skyClose = 1.0 - PrayerVfxPrimitives.smooth(0.78, 1.0, t);
        double skyScale = open * (0.85 + 0.15 * skyClose);
        float skyAlpha = (float) (open * skyClose) * fade;

        Vec3 sky = new Vec3(0, SKY_HEIGHT, 0);
        Vec3 ground = new Vec3(0, 0.04, 0);

        // ------------------------------------------------------------------
        // 1. SELO CELESTIAL COLOSSAL NO CÉU + ASAS SERAFÍNICAS (30x30)
        // ------------------------------------------------------------------
        if (skyAlpha > 0.01f) {
            double skyR = AREA_RADIUS * 0.96 * skyScale;
            double spin = age * 0.012;

            // Anéis exteriores e interiores do firmamento
            PrayerVfxPrimitives.ringPlane(m, out, sky, PrayerVfxPrimitives.UP,
                    skyR, 0.16, spin,
                    1.0f, 0.74f, 0.20f, 0.85f * skyAlpha);
            PrayerVfxPrimitives.ringPlane(m, out, sky.add(0, -0.15, 0), PrayerVfxPrimitives.UP,
                    skyR * 0.72, 0.10, -spin * 1.3,
                    1.0f, 0.90f, 0.45f, 0.88f * skyAlpha);
            PrayerVfxPrimitives.ringPlane(m, out, sky.add(0, -0.30, 0), PrayerVfxPrimitives.UP,
                    skyR * 0.42, 0.08, spin * 1.8,
                    1.0f, 0.98f, 0.78f, 0.92f * skyAlpha);

            // Anéis astrolábicos inclinados no núcleo celeste
            PrayerVfxPrimitives.ringPlane(m, out, sky, new Vec3(1, 0.22, 0).normalize(),
                    skyR * 0.55, 0.06, -age * 0.025,
                    1.0f, 0.86f, 0.36f, 0.72f * skyAlpha);
            PrayerVfxPrimitives.ringPlane(m, out, sky, new Vec3(0.2, 0.22, 1).normalize(),
                    skyR * 0.55, 0.06, age * 0.028,
                    1.0f, 0.95f, 0.65f, 0.72f * skyAlpha);

            PrayerVfxPrimitives.runeHalo(m, out, sky.add(0, -0.2, 0), 3.6 * skyScale, -spin * 1.5,
                    skyAlpha, 1.0f, 0.92f, 0.55f);

            // Estrela sagrada de 12 pontas inscrita no selo celeste
            if (open > 0.2) {
                double starR = skyR * 0.94;
                for (int tri = 0; tri < 3; tri++) {
                    drawSacredPolygon(m, out, sky.add(0, -0.08, 0), starR, 4,
                            spin + tri * (Math.PI / 6.0), 0.045,
                            1.0f, 0.86f, 0.38f, 0.62f * skyAlpha);
                }
            }

            // 6 Asas Serafínicas gigantes abertas em coroa ao redor do selo
            int wingPairs = 6;
            for (int w = 0; w < wingPairs; w++) {
                double baseAng = w * (Math.PI * 2.0 / wingPairs) + spin * 0.5;
                Vec3 dir = new Vec3(Math.cos(baseAng), 0, Math.sin(baseAng));
                Vec3 perp = new Vec3(-Math.sin(baseAng), 0, Math.cos(baseAng));

                for (int feather = 0; feather < 6; feather++) {
                    double fRatio = feather / 5.0;
                    double reach = (3.5 + fRatio * 9.5) * skyScale;
                    double spread = (fRatio - 0.35) * 3.2 * skyScale;
                    double droop = -0.6 - fRatio * 2.2;

                    Vec3 start = sky.add(dir.scale(2.2 * skyScale)).add(perp.scale(spread * 0.35)).add(0, -0.3, 0);
                    Vec3 tip = sky.add(dir.scale(reach)).add(perp.scale(spread)).add(0, droop, 0);

                    PrayerVfxGeometry.taperedTube(m, out, start, tip,
                            0.18 * skyScale, 0.012,
                            1.0f, 0.88f + (float) fRatio * 0.10f, 0.42f + (float) fRatio * 0.35f,
                            0.65f * skyAlpha);
                }
            }

            // Círculo de julgamento demarcando a borda dos 30x30 blocos no chão
            PrayerVfxPrimitives.ringPlane(m, out, ground, PrayerVfxPrimitives.UP,
                    AREA_RADIUS * open, 0.09, -spin,
                    1.0f, 0.72f, 0.18f, 0.62f * skyAlpha);
            PrayerVfxPrimitives.ringPlane(m, out, ground.add(0, 0.01, 0), PrayerVfxPrimitives.UP,
                    AREA_RADIUS * 0.96 * open, 0.035, spin * 1.5,
                    1.0f, 0.94f, 0.58f, 0.70f * skyAlpha);
        }

        // ------------------------------------------------------------------
        // 2. BOMBARDEIO DE 16 PILARES DE IRA CELESTIAL NA ÁREA 30x30
        // ------------------------------------------------------------------
        for (int i = 0; i < PILLAR_COUNT; i++) {
            double startT = 0.14 + (i / (double) PILLAR_COUNT) * 0.42;
            double strikeDur = 0.05;  // Desce rasgando o céu instantaneamente
            double lingerDur = 0.32;  // Queima no solo e dissipa

            double localAge = t - startT;
            if (localAge < 0.0 || localAge > strikeDur + lingerDur) continue;

            double strike = clamp01(localAge / strikeDur);
            double strikeEase = 1.0 - Math.pow(1.0 - strike, 3.0);
            double dissipate = clamp01((localAge - strikeDur) / lingerDur);
            float pAlpha = (float) Math.pow(1.0 - dissipate, 1.35) * fade;
            if (pAlpha <= 0.01f) continue;

            // Distribuição em espiral áurea cobrindo todo o raio de 14 blocos
            double distNorm = Math.sqrt((i + 0.5) / PILLAR_COUNT);
            double r = 2.6 + distNorm * (AREA_RADIUS - 3.4);
            double ang = i * 2.399963 + 0.7;

            Vec3 impactPos = new Vec3(Math.cos(ang) * r, 0.05, Math.sin(ang) * r);
            Vec3 skyOrigin = impactPos.add(0, SKY_HEIGHT, 0);
            Vec3 tip = skyOrigin.lerp(impactPos, strikeEase);

            double widthMult = (1.0 - dissipate * 0.65) * (1.0 + (1.0 - dissipate) * 0.35);

            // Aura externa dourado-fogo (ira divina)
            PrayerVfxGeometry.taperedTube(m, out, skyOrigin, tip,
                    0.95 * widthMult, 0.65 * widthMult,
                    1.0f, 0.68f, 0.14f, 0.28f * pAlpha);
            // Coluna intermediária dourada
            PrayerVfxGeometry.taperedTube(m, out, skyOrigin, tip,
                    0.48 * widthMult, 0.32 * widthMult,
                    1.0f, 0.88f, 0.38f, 0.68f * pAlpha);
            // Núcleo incandescente branco puro
            PrayerVfxGeometry.taperedTube(m, out, skyOrigin, tip,
                    0.20 * widthMult, 0.14 * widthMult,
                    1.0f, 0.99f, 0.86f, 0.98f * pAlpha);

            // Impacto no solo de cada pilar
            if (strike >= 0.8) {
                double impP = clamp01((localAge - strikeDur * 0.8) / lingerDur);
                double impEase = 1.0 - Math.pow(1.0 - impP, 2.2);
                float impAlpha = (float) Math.pow(1.0 - impP, 1.3) * fade;

                // Clarão no impacto
                if (impP < 0.35) {
                    float starA = (float) (1.0 - impP / 0.35) * fade * 0.92f;
                    PrayerVfxPrimitives.starburst(m, out, impactPos.add(0, 0.4, 0), PrayerVfxPrimitives.UP,
                            0.8 + impP * 3.8, starA, 1.0f, 0.92f, 0.52f);
                }

                // Onda de choque no solo
                PrayerVfxPrimitives.ringPlane(m, out, impactPos, PrayerVfxPrimitives.UP,
                        0.45 + impEase * 3.4, 0.08 * (1.0 - impP * 0.5), age * 0.08 + i,
                        1.0f, 0.78f, 0.24f, 0.85f * impAlpha);

                // Estilhaços de luz sagrada subindo da cratera
                for (int s = 0; s < 5; s++) {
                    double sAng = s * (Math.PI * 2.0 / 5.0) + i;
                    double sDist = 0.35 + impEase * 2.1;
                    double sY = 0.15 + Math.sin(impP * Math.PI) * (1.2 + (s % 2) * 0.6);
                    Vec3 sPos = impactPos.add(Math.cos(sAng) * sDist, sY, Math.sin(sAng) * sDist);
                    Vec3 sAxis = new Vec3(Math.cos(sAng) * 0.35, 1.0, Math.sin(sAng) * 0.35).normalize();

                    PrayerVfxPrimitives.crystal(m, out, sPos, sAxis,
                            0.42 * (1.0 - impP * 0.4), 0.065 * (1.0 - impP * 0.4),
                            1.0f, 0.92f, 0.52f, impAlpha);
                }
            }
        }

        // ------------------------------------------------------------------
        // 3. O JULGAMENTO FINAL — PILAR MONUMENTAL NO CENTRO + ONDA DE 15 BLOCOS
        // ------------------------------------------------------------------
        double finalStart = 0.56;
        if (t >= finalStart) {
            double ft = clamp01((t - finalStart) / (1.0 - finalStart));
            double drop = clamp01(ft / 0.10);
            double dropEase = 1.0 - Math.pow(1.0 - drop, 3.0);
            double linger = clamp01((ft - 0.10) / 0.90);
            double waveEase = 1.0 - Math.pow(1.0 - linger, 2.3);
            float fAlpha = (float) Math.pow(1.0 - linger, 1.25) * fade;

            Vec3 centerGround = new Vec3(0, 0.08, 0);
            Vec3 centerTip = sky.lerp(centerGround, dropEase);
            double colWidth = (1.0 - linger * 0.55) * (1.0 + (1.0 - linger) * 0.4);

            // Coluna central colossal de julgamento
            PrayerVfxGeometry.taperedTube(m, out, sky, centerTip,
                    2.6 * colWidth, 2.1 * colWidth,
                    1.0f, 0.65f, 0.12f, 0.32f * fAlpha);
            PrayerVfxGeometry.taperedTube(m, out, sky, centerTip,
                    1.45 * colWidth, 1.15 * colWidth,
                    1.0f, 0.88f, 0.36f, 0.75f * fAlpha);
            PrayerVfxGeometry.taperedTube(m, out, sky, centerTip,
                    0.68 * colWidth, 0.55 * colWidth,
                    1.0f, 1.0f, 0.90f, 1.0f * fAlpha);

            // Anéis de pressão subindo pelo pilar central
            for (int r = 0; r < 4; r++) {
                double ringY = ((linger * 1.4 + r * 0.25) % 1.0) * (SKY_HEIGHT - 1.0);
                PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, 0.5 + ringY, 0), PrayerVfxPrimitives.UP,
                        (2.4 - (ringY / SKY_HEIGHT) * 0.8) * colWidth, 0.09, age * 0.12 + r,
                        1.0f, 0.94f, 0.62f, 0.78f * fAlpha);
            }

            if (drop >= 0.85) {
                // Clarão apocalíptico central
                if (linger < 0.35) {
                    float starA = (float) (1.0 - linger / 0.35) * fade;
                    PrayerVfxPrimitives.starburst(m, out, new Vec3(0, 1.2, 0), PrayerVfxPrimitives.UP,
                            2.5 + linger * 14.0, starA, 1.0f, 0.95f, 0.65f);
                }

                // Onda de choque final varrendo todos os 15 blocos de raio (30x30)
                double megaWaveR = 1.2 + waveEase * (AREA_RADIUS - 1.0);
                PrayerVfxPrimitives.ringPlane(m, out, centerGround, PrayerVfxPrimitives.UP,
                        megaWaveR, 0.32 * (1.0 - linger * 0.4), age * 0.04,
                        1.0f, 0.72f, 0.18f, 0.90f * fAlpha);
                PrayerVfxPrimitives.ringPlane(m, out, centerGround.add(0, 0.04, 0), PrayerVfxPrimitives.UP,
                        megaWaveR * 0.78, 0.14, -age * 0.06,
                        1.0f, 0.96f, 0.72f, 0.92f * fAlpha);

                // 24 lanças de luz brotando radialmente na frente da onda
                for (int lance = 0; lance < 24; lance++) {
                    double lAng = lance * (Math.PI * 2.0 / 24.0);
                    double lR = megaWaveR * (0.85 + (lance % 3) * 0.07);
                    Vec3 lBase = new Vec3(Math.cos(lAng) * lR, 0.0, Math.sin(lAng) * lR);
                    Vec3 lAxis = new Vec3(Math.cos(lAng) * 0.32, 1.0, Math.sin(lAng) * 0.32).normalize();
                    double lH = (1.6 + (lance % 3) * 0.6) * (1.0 - linger * 0.65);

                    PrayerVfxPrimitives.crystal(m, out, lBase.add(0, lH * 0.45, 0), lAxis,
                            lH, 0.22 * (1.0 - linger * 0.65),
                            1.0f, 0.88f, 0.42f, 0.88f * fAlpha);
                }
            }
        }
    }

    private static void drawSacredPolygon(Matrix4fc m, VertexConsumer out, Vec3 center, double radius,
                                          int sides, double rotation, double thickness,
                                          float r, float g, float b, float a) {
        if (a <= 0.01f || radius <= 0.05) return;
        Vec3 prev = null;
        Vec3 first = null;
        for (int i = 0; i < sides; i++) {
            double ang = rotation + i * (Math.PI * 2.0 / sides);
            Vec3 pt = center.add(Math.cos(ang) * radius, 0.0, Math.sin(ang) * radius);
            if (prev != null) {
                PrayerVfxGeometry.tube(m, out, prev, pt, thickness, r, g, b, a);
            } else {
                first = pt;
            }
            prev = pt;
        }
        if (prev != null && first != null) {
            PrayerVfxGeometry.tube(m, out, prev, first, thickness, r, g, b, a);
        }
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
