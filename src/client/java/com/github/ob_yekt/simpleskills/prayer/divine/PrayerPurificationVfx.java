package com.github.ob_yekt.simpleskills.prayer.divine;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerPurificationVfx {
    private PrayerPurificationVfx() {}

    // 4. PURIFICATION — Um anel de luz sagrada sobe dos pés até a cabeça varrendo
    // fragmentos sombrios do corpo e transmutando-os em luz pura num selo superior.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        double safeDuration = Math.max(1.0, duration);
        double t = Math.max(0.0, Math.min(1.0, age / safeDuration));

        // Entrada rápida (0% -> 10%) e dissipação suave e gradual (68% -> 100%)
        double fadeIn = PrayerVfxPrimitives.smooth(0.0, 0.10, t);
        double fadeOutRaw = 1.0 - PrayerVfxPrimitives.smooth(0.68, 1.0, t);
        double fadeOut = fadeOutRaw * fadeOutRaw;
        float fade = (float) (fadeIn * fadeOut);
        if (fade <= 0.005f) return;

        // Progresso da varredura de baixo para cima (8% -> 66%)
        double scanProgress = PrayerVfxPrimitives.smooth(0.08, 0.66, t);
        double scanY = 0.08 + scanProgress * 2.45;
        double topSealY = 2.62;

        Vec3 ground = new Vec3(0.0, 0.035, 0.0);
        Vec3 topSealPos = new Vec3(0.0, topSealY, 0.0);

        // ------------------------------------------------------------------
        // 1. SELO DE CONSAGRAÇÃO NO CHÃO E SELO ABSORVEDOR SUPERIOR
        // ------------------------------------------------------------------
        double baseOpen = PrayerVfxPrimitives.smooth(0.0, 0.14, t) * (0.82 + 0.18 * fadeOutRaw);
        double baseR = 1.72 * baseOpen;
        double rot = age * 0.022;

        PrayerVfxPrimitives.circleSigil(m, out, ground, baseR, rot, fade * 0.9f,
                1.0f, 0.85f, 0.34f);
        PrayerVfxPrimitives.ringPlane(m, out, ground.add(0, 0.014, 0), PrayerVfxPrimitives.UP,
                baseR * 0.68, 0.020, -rot * 1.6,
                1.0f, 0.97f, 0.78f, 0.85f * fade);

        if (baseOpen > 0.25) {
            drawSacredTriangle(m, out, ground.add(0, 0.012, 0), baseR * 0.86, rot, 0.016, 0.65f * fade);
            drawSacredTriangle(m, out, ground.add(0, 0.012, 0), baseR * 0.86, rot + Math.PI / 3.0, 0.016, 0.65f * fade);
        }

        // Selo purificador flutuando acima da cabeça (onde as trevas são queimadas)
        double topOpen = PrayerVfxPrimitives.smooth(0.04, 0.20, t) * (0.78 + 0.22 * fadeOutRaw);
        double topR = 0.78 * topOpen;
        PrayerVfxPrimitives.ringPlane(m, out, topSealPos, PrayerVfxPrimitives.UP,
                topR, 0.024, -age * 0.045,
                1.0f, 0.90f, 0.42f, 0.90f * fade);
        PrayerVfxPrimitives.ringPlane(m, out, topSealPos.add(0, 0.04, 0), PrayerVfxPrimitives.UP,
                topR * 0.68, 0.015, age * 0.06,
                1.0f, 0.99f, 0.88f, 0.92f * fade);
        PrayerVfxPrimitives.runeHalo(m, out, topSealPos.add(0, 0.02, 0),
                topR * 0.85, age * 0.035, fade * 0.92f, 1.0f, 0.94f, 0.60f);

        // ------------------------------------------------------------------
        // 2. ANEL DE VARREDURA PURIFICADOR SUBINDO PELO CORPO
        // ------------------------------------------------------------------
        double scanActive = Math.sin(Math.min(1.0, scanProgress / 0.98) * Math.PI * 0.85 + 0.2);
        double scanR = (1.02 + Math.sin(age * 0.28) * 0.05) * baseOpen;
        float scanAlpha = (float) (scanActive * fade);
        Vec3 scanPos = new Vec3(0.0, scanY, 0.0);

        if (scanAlpha > 0.01f) {
            // Anel principal branco-dourado brilhante
            PrayerVfxPrimitives.ringPlane(m, out, scanPos, PrayerVfxPrimitives.UP,
                    scanR, 0.034, age * 0.08,
                    1.0f, 0.99f, 0.86f, 0.96f * scanAlpha);
            // Segundo anel externo dourado fino
            PrayerVfxPrimitives.ringPlane(m, out, scanPos, PrayerVfxPrimitives.UP,
                    scanR * 1.16, 0.016, -age * 0.10,
                    1.0f, 0.84f, 0.30f, 0.80f * scanAlpha);
            // Rastro vertical luminoso logo abaixo do anel de varredura
            double trailBottom = Math.max(0.06, scanY - 0.42);
            PrayerVfxGeometry.taperedTube(m, out,
                    new Vec3(0.0, trailBottom, 0.0), scanPos,
                    scanR * 0.92, scanR * 0.98,
                    1.0f, 0.94f, 0.64f, 0.16f * scanAlpha);
        }

        // 3 Filetes helicoidais ascendentes subindo junto com a varredura
        if (scanProgress > 0.02) {
            for (int h = 0; h < 3; h++) {
                drawPurifyingHelix(m, out, 0.08, scanY, baseOpen, h, age, fade * 0.85f);
            }
        }

        // ------------------------------------------------------------------
        // 3. IMPUREZAS SOMBRIAS SENDO ARRANCADAS E TRANSMUTADAS EM LUZ
        // ------------------------------------------------------------------
        int moteCount = 16;
        for (int i = 0; i < moteCount; i++) {
            // Altura original onde cada fragmento sombrio estava preso no corpo
            double initialY = 0.25 + (i / (double) (moteCount - 1)) * 1.85;
            double baseAngle = i * 2.399963 + age * 0.035;

            if (scanY < initialY) {
                // ANTES DA VARREDURA: fragmento sombrio/impureza agarrada ao corpo
                double wobble = Math.sin(age * 0.22 + i * 1.7) * 0.06;
                double darkR = (0.56 + (i % 3) * 0.12 + wobble) * baseOpen;
                Vec3 darkPos = new Vec3(
                        Math.cos(baseAngle) * darkR,
                        initialY + Math.cos(age * 0.16 + i) * 0.05,
                        Math.sin(baseAngle) * darkR
                );
                Vec3 tangent = new Vec3(-Math.sin(baseAngle), 0.35, Math.cos(baseAngle)).normalize();

                // Cristal sombrio violáceo-escuro
                PrayerVfxPrimitives.crystal(m, out, darkPos, tangent,
                        0.22 * baseOpen, 0.062 * baseOpen,
                        0.22f, 0.10f, 0.32f, 0.85f * fade);
            } else {
                // APÓS O ANEL PASSAR: arrancado para fora, transmutado em luz e sugado ao selo superior
                double localTime = Math.min(1.0, (scanY - initialY) / Math.max(0.45, (topSealY - initialY)));
                double ease = 1.0 - Math.pow(1.0 - localTime, 2.0);

                // Expande para fora no impacto da purificação e depois converge no selo superior
                double outwardArc = Math.sin(localTime * Math.PI) * 0.65;
                double curR = ((0.65 * (1.0 - ease)) + outwardArc + 0.12 * ease) * baseOpen;
                double curY = initialY + (topSealY - initialY) * ease;
                double spiralAngle = baseAngle + localTime * 3.2;

                Vec3 pos = new Vec3(Math.cos(spiralAngle) * curR, curY, Math.sin(spiralAngle) * curR);
                float lifeAlpha = (float) (Math.sin(Math.min(1.0, localTime * 0.92 + 0.08) * Math.PI) * fade);

                if (localTime < 0.28) {
                    // Transição: fragmento sombrio dissolvendo-se em faísca dourada
                    float mix = (float) (localTime / 0.28);
                    float rCol = 0.22f + mix * 0.78f;
                    float gCol = 0.10f + mix * 0.82f;
                    float bCol = 0.32f + mix * 0.28f;
                    PrayerVfxPrimitives.crystal(m, out, pos, PrayerVfxPrimitives.UP,
                            0.20 * (1.0 - mix * 0.3), 0.055 * (1.0 - mix * 0.4),
                            rCol, gCol, bCol, 0.88f * lifeAlpha);
                } else {
                    // Pura luz dourado-branca subindo em direção ao selo superior
                    Vec3 toTop = topSealPos.subtract(pos);
                    Vec3 dir = toTop.lengthSqr() > 1e-4 ? toTop.normalize() : PrayerVfxPrimitives.UP;

                    PrayerVfxPrimitives.crystal(m, out, pos, dir,
                            0.22 * (1.0 - ease * 0.4), 0.032,
                            1.0f, 0.96f, 0.74f, 0.95f * lifeAlpha);
                    // Rastro luminoso sutil
                    PrayerVfxGeometry.taperedTube(m, out,
                            pos.subtract(dir.scale(0.24)), pos,
                            0.005, 0.022,
                            1.0f, 0.88f, 0.42f, 0.55f * lifeAlpha);
                }
            }
        }

        // ------------------------------------------------------------------
        // 4. AURA CORPO PURIFICADO + CLARÃO FINAL NO SELO SUPERIOR
        // ------------------------------------------------------------------
        double bodyGlowAlpha = (0.04 + 0.07 * scanProgress) * fade;
        PrayerVfxGeometry.ellipsoid(m, out, new Vec3(0.0, 1.08, 0.0),
                new Vec3(0.54 * baseOpen, 1.12 * baseOpen, 0.54 * baseOpen),
                10, 16, 1.0f, 0.94f, 0.66f, (float) bodyGlowAlpha, age * 0.04);

        // Pulso limpo no selo superior quando a varredura conclui (~62% -> 88%)
        if (t >= 0.60 && t <= 0.90) {
            double burstT = (t - 0.60) / 0.30;
            float burstAlpha = (float) (Math.sin(burstT * Math.PI) * 0.85 * fade);
            PrayerVfxPrimitives.ringPlane(m, out, topSealPos, PrayerVfxPrimitives.UP,
                    topR + burstT * 1.15, 0.026 * (1.0 - burstT * 0.6), age * 0.06,
                    1.0f, 0.98f, 0.82f, burstAlpha);
        }
    }

    private static void drawPurifyingHelix(Matrix4fc m, VertexConsumer out,
                                           double yStart, double yEnd, double scale,
                                           int index, float age, float fade) {
        if (yEnd - yStart < 0.12 || fade <= 0.005f) return;
        int steps = 18;
        Vec3 prev = null;
        for (int s = 0; s <= steps; s++) {
            double u = s / (double) steps;
            double y = yStart + (yEnd - yStart) * u;
            double ang = age * 0.11 + u * Math.PI * 2.6 + index * (Math.PI * 2.0 / 3.0);
            double r = (0.68 - u * 0.12) * scale;
            Vec3 curr = new Vec3(Math.cos(ang) * r, y, Math.sin(ang) * r);

            if (prev != null) {
                double thick = 0.020 * (0.5 + 0.5 * u);
                PrayerVfxGeometry.tube(m, out, prev, curr, thick * 2.0,
                        1.0f, 0.84f, 0.32f, 0.25f * fade);
                PrayerVfxGeometry.tube(m, out, prev, curr, thick,
                        1.0f, 0.98f, 0.84f, 0.85f * fade);
            }
            prev = curr;
        }
    }

    private static void drawSacredTriangle(Matrix4fc m, VertexConsumer out, Vec3 center,
                                           double radius, double rotation, double thickness, float alpha) {
        if (alpha <= 0.005f || radius <= 0.05) return;
        Vec3[] pts = new Vec3[3];
        for (int i = 0; i < 3; i++) {
            double a = rotation + i * (Math.PI * 2.0 / 3.0);
            pts[i] = center.add(Math.cos(a) * radius, 0.0, Math.sin(a) * radius);
        }
        PrayerVfxGeometry.tube(m, out, pts[0], pts[1], thickness, 1.0f, 0.90f, 0.45f, alpha);
        PrayerVfxGeometry.tube(m, out, pts[1], pts[2], thickness, 1.0f, 0.90f, 0.45f, alpha);
        PrayerVfxGeometry.tube(m, out, pts[2], pts[0], thickness, 1.0f, 0.90f, 0.45f, alpha);
    }
}
