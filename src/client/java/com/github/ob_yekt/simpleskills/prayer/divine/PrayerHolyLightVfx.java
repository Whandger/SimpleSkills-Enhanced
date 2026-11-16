package com.github.ob_yekt.simpleskills.prayer.divine;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerHolyLightVfx {
    private PrayerHolyLightVfx() {}

    // 9. HOLY_LIGHT — Pilar de luz sagrada descendo dos céus com coroa celeste no topo,
    // selo cruciforme de purificação no solo, anéis litúrgicos e motas ascendentes.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        double safeDuration = Math.max(1.0, duration);
        double t = Math.max(0.0, Math.min(1.0, age / safeDuration));

        // Entrada serena (0% -> 14%) e dissipação suave e gradual (65% -> 100%)
        double fadeIn = PrayerVfxPrimitives.smooth(0.0, 0.14, t);
        double fadeOutRaw = 1.0 - PrayerVfxPrimitives.smooth(0.65, 1.0, t);
        double fadeOut = fadeOutRaw * fadeOutRaw;
        float fade = (float) (fadeIn * fadeOut);
        if (fade <= 0.005f) return;

        // O pilar desce do céu (y = 6.2) até tocar o chão (y = 0.04) nos primeiros 16%
        double descend = PrayerVfxPrimitives.smooth(0.0, 0.16, t);
        double skyY = 6.2;
        double beamBottomY = skyY - (skyY - 0.04) * descend;

        double breathe = Math.sin(age * 0.12);
        double pulse = (1.0 + breathe * 0.04) * (0.78 + 0.22 * fadeOutRaw);

        Vec3 skyPos = new Vec3(0.0, skyY, 0.0);
        Vec3 beamBottom = new Vec3(0.0, beamBottomY, 0.0);
        Vec3 ground = new Vec3(0.0, 0.04, 0.0);

        // ------------------------------------------------------------------
        // 1. PORTAL CELESTE NO ALTO (Origem da luz)
        // ------------------------------------------------------------------
        double crownScale = PrayerVfxPrimitives.smooth(0.0, 0.12, t) * (0.8 + 0.2 * fadeOutRaw);
        PrayerVfxPrimitives.ringPlane(m, out, skyPos, PrayerVfxPrimitives.UP,
                1.15 * crownScale, 0.032, age * 0.025,
                1.0f, 0.88f, 0.42f, 0.85f * fade);
        PrayerVfxPrimitives.ringPlane(m, out, skyPos.subtract(0, 0.08, 0), PrayerVfxPrimitives.UP,
                0.76 * crownScale, 0.020, -age * 0.035,
                1.0f, 0.98f, 0.82f, 0.88f * fade);
        PrayerVfxPrimitives.runeHalo(m, out, skyPos.subtract(0, 0.04, 0),
                0.95 * crownScale, age * 0.02, fade * 0.9f, 1.0f, 0.92f, 0.56f);

        // ------------------------------------------------------------------
        // 2. COLUNA DE LUZ SAGRADA EM 3 CAMADAS + RAIOS SOLARES OBLÍQUOS
        // ------------------------------------------------------------------
        // Aura externa dourada quente
        PrayerVfxGeometry.taperedTube(m, out, beamBottom, skyPos,
                0.92 * pulse, 0.48 * pulse,
                1.0f, 0.82f, 0.30f, 0.13f * fade);
        // Corpo intermediário perolado
        PrayerVfxGeometry.taperedTube(m, out, beamBottom, skyPos,
                0.42 * pulse, 0.22 * pulse,
                1.0f, 0.95f, 0.72f, 0.28f * fade);
        // Núcleo central branco puro
        PrayerVfxGeometry.taperedTube(m, out, beamBottom, skyPos,
                0.14 * pulse, 0.08 * pulse,
                1.0f, 1.0f, 0.94f, 0.68f * fade);

        // 8 Raios solares diagonais descendo do portal celeste até a base
        for (int ray = 0; ray < 8; ray++) {
            double angle = ray * (Math.PI * 0.25) + age * 0.012;
            double topR = 0.42 * crownScale;
            double botR = (0.85 + (ray % 2) * 0.22) * pulse;
            Vec3 topPt = skyPos.add(Math.cos(angle) * topR, 0.0, Math.sin(angle) * topR);
            Vec3 botPt = beamBottom.add(Math.cos(angle + 0.18) * botR, 0.0, Math.sin(angle + 0.18) * botR);

            PrayerVfxGeometry.taperedTube(m, out, topPt, botPt,
                    0.012, 0.032,
                    1.0f, 0.92f, 0.58f, 0.48f * fade);
        }

        // ------------------------------------------------------------------
        // 3. IMPACTO NO SOLO: SELO SAGRADO, CRUZ DE LUZ E ONDAS DE CURA
        // ------------------------------------------------------------------
        if (descend > 0.75) {
            double groundFactor = PrayerVfxPrimitives.smooth(0.75, 1.0, descend);
            double sigilR = 1.95 * groundFactor * (0.85 + 0.15 * fadeOutRaw);
            float gAlpha = (float) (groundFactor * fade);

            // Clarão suave no ponto onde a luz toca o chão
            PrayerVfxGeometry.ellipsoid(m, out, ground.add(0, 0.15, 0),
                    new Vec3(0.55 * groundFactor, 0.28 * groundFactor, 0.55 * groundFactor),
                    8, 14, 1.0f, 0.98f, 0.84f, 0.45f * gAlpha, age * 0.05);

            PrayerVfxPrimitives.circleSigil(m, out, ground, sigilR, age * 0.01, gAlpha * 0.92f,
                    1.0f, 0.86f, 0.38f);

            PrayerVfxPrimitives.ringPlane(m, out, ground.add(0, 0.015, 0), PrayerVfxPrimitives.UP,
                    sigilR * 0.72, 0.024, -age * 0.02,
                    1.0f, 0.96f, 0.72f, 0.82f * gAlpha);

            // Cruz sagrada de luz inscrita no selo do chão
            drawGroundCross(m, out, ground.add(0, 0.012, 0), sigilR * 0.88, age * 0.01,
                    0.024, 0.72f * gAlpha);

            // Ondas periódicas de cura emanando do centro no chão
            for (int w = 0; w < 2; w++) {
                double waveCycle = ((age * 0.032) + w * 0.5) % 1.0;
                double waveR = 0.35 + waveCycle * (sigilR * 1.15);
                float waveAlpha = (float) (Math.sin(waveCycle * Math.PI) * 0.65 * gAlpha);
                if (waveAlpha > 0.005f) {
                    PrayerVfxPrimitives.ringPlane(m, out, ground.add(0, 0.02, 0), PrayerVfxPrimitives.UP,
                            waveR, 0.03 * (1.0 - waveCycle * 0.5), 0.0,
                            1.0f, 0.92f, 0.54f, waveAlpha);
                }
            }
        }

        // ------------------------------------------------------------------
        // 4. ANÉIS LITÚRGICOS SUSPENSOS E RÚNICOS AO LONGO DO PILAR
        // ------------------------------------------------------------------
        for (int ring = 0; ring < 4; ring++) {
            double targetY = 0.65 + ring * 1.32 + Math.sin(age * 0.09 + ring * 1.2) * 0.14;
            if (targetY < beamBottomY) continue;

            double rScale = (0.52 + ring * 0.12) * pulse;
            double dir = (ring % 2 == 0) ? 1.0 : -1.0;

            PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, targetY, 0), PrayerVfxPrimitives.UP,
                    rScale, 0.020, age * 0.025 * dir,
                    1.0f, 0.89f, 0.48f, 0.82f * fade);

            PrayerVfxPrimitives.runeHalo(m, out, new Vec3(0, targetY + 0.24, 0),
                    rScale * 0.62, -age * 0.022 * dir + ring,
                    0.78f * fade, 1.0f, 0.95f, 0.72f);
        }

        // ------------------------------------------------------------------
        // 5. MOTAS DE LUZ E CURA ASCENDENTES EM ESPIRAL SUAVE
        // ------------------------------------------------------------------
        drawRisingBlessingMotes(m, out, beamBottomY, skyY, pulse, age, fade);
    }

    private static void drawRisingBlessingMotes(Matrix4fc m, VertexConsumer out,
                                                double minY, double maxY,
                                                double pulse, float age, float fade) {
        if (maxY - minY < 0.5) return;
        int count = 18;
        for (int i = 0; i < count; i++) {
            double cycle = ((age * 0.020) + (i / (double) count)) % 1.0;
            double lifeAlpha = Math.sin(cycle * Math.PI);
            float mAlpha = (float) (lifeAlpha * 0.88 * fade);
            if (mAlpha <= 0.005f) continue;

            double y = minY + 0.15 + cycle * (maxY - minY - 0.3);
            double ang = i * 2.399963 + cycle * 2.4 + age * 0.02;
            double r = (0.28 + (i % 4) * 0.14) * pulse;

            Vec3 pos = new Vec3(Math.cos(ang) * r, y, Math.sin(ang) * r);
            PrayerVfxPrimitives.crystal(m, out, pos, PrayerVfxPrimitives.UP,
                    0.18, 0.026,
                    1.0f, 0.97f, 0.74f, mAlpha);
        }
    }

    private static void drawGroundCross(Matrix4fc m, VertexConsumer out, Vec3 center,
                                        double armLength, double rotation, double thickness, float alpha) {
        if (alpha <= 0.005f || armLength <= 0.05) return;
        for (int arm = 0; arm < 2; arm++) {
            double a = rotation + arm * (Math.PI * 0.5);
            Vec3 dir = new Vec3(Math.cos(a), 0.0, Math.sin(a));
            Vec3 p1 = center.subtract(dir.scale(armLength));
            Vec3 p2 = center.add(dir.scale(armLength));
            PrayerVfxGeometry.tube(m, out, p1, p2, thickness,
                    1.0f, 0.94f, 0.58f, alpha);
        }
    }
}
