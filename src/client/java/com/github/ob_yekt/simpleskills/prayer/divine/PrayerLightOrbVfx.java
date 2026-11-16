package com.github.ob_yekt.simpleskills.prayer.divine;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerLightOrbVfx {
    private PrayerLightOrbVfx() {}

    // 1. LIGHT_ORB — Selo rúnico sagrado no chão e coroa de runas de luz ao redor da visão do jogador.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 5, 8);
        if (fade <= 0.01f) return;

        double t = clamp01(age / Math.max(1.0, duration));

        // 1. Inscrição progressiva das runas (0% -> 32%)
        double inscribe = PrayerVfxPrimitives.smooth(0.0, 0.32, t);
        // 2. Pulso suave de ativação da bênção (35% -> 80%)
        double bless = Math.sin(clamp01((t - 0.30) / 0.55) * Math.PI);

        // ------------------------------------------------------------------
        // 1. SELO RÚNICO DIVINO NO CHÃO (Estrela sagrada + glifos ao redor)
        // ------------------------------------------------------------------
        Vec3 ground = new Vec3(0.0, 0.03, 0.0);
        double groundR = 1.45 * (0.35 + 0.65 * inscribe);
        double slowSpin = age * 0.018;

        // Anéis externo e interno do selo
        PrayerVfxPrimitives.ringPlane(m, out, ground, PrayerVfxPrimitives.UP,
                groundR, 0.022, slowSpin,
                1.0f, 0.86f, 0.38f, (float) (0.85 * inscribe) * fade);
        PrayerVfxPrimitives.ringPlane(m, out, ground.add(0, 0.005, 0), PrayerVfxPrimitives.UP,
                groundR * 0.72, 0.014, -slowSpin * 1.2,
                1.0f, 0.95f, 0.65f, (float) (0.75 * inscribe) * fade);

        // Estrela sagrada de 8 pontas (dois quadrados entrelaçados no chão)
        if (inscribe > 0.15) {
            float starAlpha = (float) (PrayerVfxPrimitives.smooth(0.15, 0.45, t) * (0.68 + bless * 0.25)) * fade;
            double starR = groundR * 0.96;
            drawSacredPolygon(m, out, ground.add(0, 0.008, 0), starR, 4, slowSpin, 0.015,
                    1.0f, 0.90f, 0.48f, starAlpha);
            drawSacredPolygon(m, out, ground.add(0, 0.008, 0), starR, 4, slowSpin + Math.PI / 4.0, 0.015,
                    1.0f, 0.96f, 0.75f, starAlpha);
        }

        // 8 Glifos rúnicos gravados entre os dois anéis do chão
        for (int g = 0; g < 8; g++) {
            double ang = g * (Math.PI * 2.0 / 8.0) + slowSpin + Math.PI / 8.0;
            double glyphR = groundR * 0.84;
            Vec3 glyphCenter = new Vec3(Math.cos(ang) * glyphR, 0.045, Math.sin(ang) * glyphR);
            float glyphAlpha = (float) (inscribe * (0.72 + bless * 0.28)) * fade;
            drawGroundRune(m, out, glyphCenter, ang, 0.16 * inscribe, glyphAlpha);
        }

        // ------------------------------------------------------------------
        // 2. COROA DE RUNAS FLUTUANTES PERTO DA VISÃO DO PLAYER (Altura dos olhos)
        // ------------------------------------------------------------------
        // Raio de 1.18 blocos: fica visível na periferia do campo de visão sem tampar a tela
        double eyeY = 1.48 + Math.sin(age * 0.08) * 0.03;
        double haloR = 1.18;
        double haloSpin = -age * 0.022;

        // Fio de luz dourado sutil conectando as runas na altura da visão
        PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, eyeY, 0), PrayerVfxPrimitives.UP,
                haloR, 0.008, haloSpin,
                1.0f, 0.92f, 0.56f, (float) (0.42 * inscribe) * fade);

        // 6 Runas sagradas verticais flutuando ao redor da cabeça/visão do jogador
        int eyeRunes = 6;
        for (int r = 0; r < eyeRunes; r++) {
            double ang = r * (Math.PI * 2.0 / eyeRunes) + haloSpin;
            double bob = Math.sin(age * 0.12 + r * 1.4) * 0.04;
            Vec3 runePos = new Vec3(Math.cos(ang) * haloR, eyeY + bob, Math.sin(ang) * haloR);

            // Cada runa olha para o player (tangente perpendicular ao raio)
            Vec3 tangent = new Vec3(-Math.sin(ang), 0.0, Math.cos(ang));
            float runeAlpha = (float) (inscribe * (0.78 + bless * 0.22)) * fade;
            drawFloatingRune(m, out, runePos, tangent, 0.16 * inscribe, r, runeAlpha);
        }

        // ------------------------------------------------------------------
        // 3. PARTÍCULAS ASCENDENTES DE LUZ (Subindo das runas do chão até a visão)
        // ------------------------------------------------------------------
        for (int p = 0; p < 12; p++) {
            double cycle = ((age * 0.035) + (p / 12.0)) % 1.0;
            double ang = p * (Math.PI * 2.0 / 12.0) + slowSpin;
            double rad = groundR * 0.85 * (1.0 - cycle * 0.25);

            Vec3 pos = new Vec3(
                    Math.cos(ang) * rad,
                    0.08 + cycle * 1.75,
                    Math.sin(ang) * rad
            );
            float pAlpha = (float) Math.sin(cycle * Math.PI) * 0.72f * fade;
            PrayerVfxPrimitives.crystal(m, out, pos, PrayerVfxPrimitives.UP,
                    0.085, 0.018, 1.0f, 0.95f, 0.68f, pAlpha);
        }
    }

    /** Desenha um polígono regular de luz no plano XZ (usado para a estrela sagrada no chão). */
    private static void drawSacredPolygon(Matrix4fc m, VertexConsumer out, Vec3 center, double radius,
                                          int sides, double rotation, double thickness,
                                          float r, float g, float b, float a) {
        if (a <= 0.01f || radius <= 0.02) return;
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

    /** Desenha um pequeno glifo rúnico (cruz/losango de luz) gravado no chão. */
    private static void drawGroundRune(Matrix4fc m, VertexConsumer out, Vec3 center,
                                       double radialAngle, double scale, float alpha) {
        if (alpha <= 0.01f || scale <= 0.01) return;
        Vec3 radial = new Vec3(Math.cos(radialAngle), 0.0, Math.sin(radialAngle));
        Vec3 tangent = new Vec3(-Math.sin(radialAngle), 0.0, Math.cos(radialAngle));

        // Haste principal apontando para fora + travessa horizontal (símbolo sacro)
        Vec3 pTop = center.add(radial.scale(scale));
        Vec3 pBot = center.subtract(radial.scale(scale));
        Vec3 pLeft = center.add(radial.scale(scale * 0.25)).subtract(tangent.scale(scale * 0.65));
        Vec3 pRight = center.add(radial.scale(scale * 0.25)).add(tangent.scale(scale * 0.65));

        PrayerVfxGeometry.tube(m, out, pBot, pTop, 0.013, 1.0f, 0.96f, 0.72f, alpha);
        PrayerVfxGeometry.tube(m, out, pLeft, pRight, 0.011, 1.0f, 0.88f, 0.45f, alpha * 0.9f);
    }

    /** Desenha uma runa vertical flutuando na linha de visão do jogador com variação de glifo. */
    private static void drawFloatingRune(Matrix4fc m, VertexConsumer out, Vec3 center,
                                         Vec3 tangent, double scale, int variant, float alpha) {
        if (alpha <= 0.01f || scale <= 0.01) return;
        Vec3 up = PrayerVfxPrimitives.UP;

        // Haste vertical luminosa central da runa
        Vec3 top = center.add(up.scale(scale));
        Vec3 bot = center.subtract(up.scale(scale));
        PrayerVfxGeometry.tube(m, out, bot, top, 0.012, 1.0f, 0.98f, 0.80f, alpha);

        // Traços rúnicos variando entre 3 glifos sagrados diferentes
        int glyphType = variant % 3;
        if (glyphType == 0) {
            // Glifo em cruz solar
            Vec3 mid = center.add(up.scale(scale * 0.28));
            PrayerVfxGeometry.tube(m, out, mid.subtract(tangent.scale(scale * 0.65)),
                    mid.add(tangent.scale(scale * 0.65)), 0.010, 1.0f, 0.88f, 0.45f, alpha);
        } else if (glyphType == 1) {
            // Glifo em V ascendente (asas de luz)
            Vec3 mid = center.subtract(up.scale(scale * 0.15));
            PrayerVfxGeometry.tube(m, out, mid, top.subtract(tangent.scale(scale * 0.65)),
                    0.010, 1.0f, 0.88f, 0.45f, alpha);
            PrayerVfxGeometry.tube(m, out, mid, top.add(tangent.scale(scale * 0.65)),
                    0.010, 1.0f, 0.88f, 0.45f, alpha);
        } else {
            // Glifo em losango de clarividência (olho da luz)
            Vec3 left = center.subtract(tangent.scale(scale * 0.55));
            Vec3 right = center.add(tangent.scale(scale * 0.55));
            PrayerVfxGeometry.tube(m, out, top, right, 0.009, 1.0f, 0.90f, 0.50f, alpha);
            PrayerVfxGeometry.tube(m, out, right, bot, 0.009, 1.0f, 0.90f, 0.50f, alpha);
            PrayerVfxGeometry.tube(m, out, bot, left, 0.009, 1.0f, 0.90f, 0.50f, alpha);
            PrayerVfxGeometry.tube(m, out, left, top, 0.009, 1.0f, 0.90f, 0.50f, alpha);
        }
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
