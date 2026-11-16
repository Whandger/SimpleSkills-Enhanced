package com.github.ob_yekt.simpleskills.prayer.unholy;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerDarkPactVfx {
    private PrayerDarkPactVfx() {}

    // 17. DARK_PACT — Selo de contrato carmesim no chão, 4 obeliscos com correntes de elos reais
    // presas ao peito do conjurador e troca ritualística de vitalidade por poder sombrio.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        double safeDuration = Math.max(1.0, duration);
        double t = Math.max(0.0, Math.min(1.0, age / safeDuration));

        // Entrada firme (0% -> 14%) e dissipação suave no final (68% -> 100%)
        double fadeIn = PrayerVfxPrimitives.smooth(0.0, 0.14, t);
        double fadeOutRaw = 1.0 - PrayerVfxPrimitives.smooth(0.68, 1.0, t);
        double fadeOut = fadeOutRaw * fadeOutRaw;
        float fade = (float) (fadeIn * fadeOut);
        if (fade <= 0.005f) return;

        double open = PrayerVfxPrimitives.smooth(0.0, 0.20, t) * (0.84 + 0.16 * fadeOutRaw);
        double pulse = Math.sin(age * 0.16);

        Vec3 ground = new Vec3(0.0, 0.04, 0.0);
        Vec3 chest = new Vec3(0.0, 1.08, 0.0);
        Vec3 crownPos = new Vec3(0.0, 2.28 + Math.sin(age * 0.10) * 0.04, 0.0);

        // ------------------------------------------------------------------
        // 1. SELO DE CONTRATO CARMESIM NO CHÃO
        // ------------------------------------------------------------------
        double sealR = 2.15 * open;
        double rot = -age * 0.016;

        PrayerVfxPrimitives.circleSigil(m, out, ground, sealR, rot,
                0.86f * fade, 0.62f, 0.05f, 0.18f);
        PrayerVfxPrimitives.ringPlane(m, out, ground.add(0, 0.015, 0), PrayerVfxPrimitives.UP,
                sealR * 0.68, 0.024, -rot * 1.6,
                0.92f, 0.14f, 0.28f, 0.88f * fade);
        PrayerVfxPrimitives.ringPlane(m, out, ground.add(0, 0.012, 0), PrayerVfxPrimitives.UP,
                sealR * 1.03, 0.018, rot * 0.8,
                0.22f, 0.02f, 0.08f, 0.85f * fade);

        // Hexagrama de contrato inscrito no selo
        if (open > 0.25) {
            drawContractTriangle(m, out, ground.add(0, 0.012, 0), sealR * 0.90, rot, 0.018, 0.72f * fade);
            drawContractTriangle(m, out, ground.add(0, 0.012, 0), sealR * 0.90, rot + Math.PI, 0.018, 0.72f * fade);
        }

        // ------------------------------------------------------------------
        // 2. 4 MONÓLITOS RÚNICOS + CORRENTES DE ELOS PRESAS AO CONJURADOR
        // ------------------------------------------------------------------
        int pillarCount = 4;
        double chainReach = PrayerVfxPrimitives.smooth(0.10, 0.30, t) * fadeOutRaw;

        for (int i = 0; i < pillarCount; i++) {
            double ang = i * (Math.PI * 0.5) - age * 0.020;
            double orbitR = 1.68 * open;
            double bob = Math.sin(age * 0.12 + i * 1.6) * 0.08;
            double y = 0.82 + (i % 2) * 0.36 + bob;

            Vec3 stonePos = new Vec3(Math.cos(ang) * orbitR, y, Math.sin(ang) * orbitR);
            Vec3 inward = chest.subtract(stonePos).normalize();

            // Monólito sombrio duplo (cristal superior e inferior)
            PrayerVfxPrimitives.crystal(m, out, stonePos.subtract(0, 0.22 * open, 0),
                    PrayerVfxPrimitives.UP, 0.44 * open, 0.13 * open,
                    0.14f, 0.02f, 0.07f, 0.94f * fade);
            PrayerVfxPrimitives.crystal(m, out, stonePos.add(0, 0.22 * open, 0),
                    new Vec3(0, -1, 0), 0.36 * open, 0.13 * open,
                    0.58f, 0.06f, 0.18f, 0.90f * fade);

            // Halo rúnico carmesim girando atrás da pedra
            PrayerVfxPrimitives.runeHalo(m, out, stonePos, 0.28 * open,
                    age * 0.05 + i, 0.90f * fade, 0.88f, 0.12f, 0.26f);

            // Corrente real de elos ligando o monólito ao peito do conjurador
            if (chainReach > 0.05) {
                Vec3 chainTarget = stonePos.lerp(chest, chainReach);
                drawLinkChain(m, out, stonePos.add(inward.scale(0.12)), chainTarget,
                        7, age, i, fade);
            }
        }

        // ------------------------------------------------------------------
        // 3. CORAÇÃO DO PACTO NO PEITO E COROA SOMBRIA ACIMA DA CABEÇA
        // ------------------------------------------------------------------
        double heartScale = (0.34 + 0.04 * pulse) * open;

        // Aura sombria envolvente no tronco
        PrayerVfxGeometry.ellipsoid(m, out, chest,
                new Vec3(0.48 * open, 0.82 * open, 0.48 * open),
                10, 14, 0.06f, 0.01f, 0.04f, 0.24f * fade, age * 0.03);

        // Núcleo rubi pulsante no peito + gaiola sombria
        PrayerVfxGeometry.ellipsoid(m, out, chest,
                new Vec3(heartScale * 0.55, heartScale * 0.65, heartScale * 0.55),
                8, 12, 0.88f, 0.08f, 0.22f, 0.88f * fade, -age * 0.08);
        PrayerVfxGeometry.wireSphere(m, out, chest, heartScale * 1.25,
                12, age * 0.05, 0.45f, 0.04f, 0.16f, 0.78f * fade, 0.014);

        // Coroa de pacto flutuando sobre a cabeça
        double crownScale = PrayerVfxPrimitives.smooth(0.08, 0.26, t) * (0.8 + 0.2 * fadeOutRaw);
        PrayerVfxPrimitives.ringPlane(m, out, crownPos, PrayerVfxPrimitives.UP,
                0.46 * crownScale, 0.018, age * 0.04,
                0.85f, 0.10f, 0.24f, 0.88f * fade);
        PrayerVfxPrimitives.runeHalo(m, out, crownPos.add(0, 0.03, 0),
                0.34 * crownScale, -age * 0.05, 0.85f * fade, 0.42f, 0.04f, 0.14f);

        // ------------------------------------------------------------------
        // 4. TROCA DO PACTO: GOTAS DE SANGUE DESCENDO & PODER SOMBRIO SUBINDO
        // ------------------------------------------------------------------
        drawPactExchange(m, out, sealR, chest, open, age, fade);
    }

    /** Desenha uma corrente feita de elos alternados com leve catenária (gravidade). */
    private static void drawLinkChain(Matrix4fc m, VertexConsumer out,
                                      Vec3 from, Vec3 to, int links,
                                      float age, int seed, float fade) {
        Vec3 span = to.subtract(from);
        double dist = span.length();
        if (dist < 0.15 || fade <= 0.005f) return;

        Vec3 dir = span.normalize();
        Vec3[] basis = PrayerVfxPrimitives.basis(dir);
        Vec3 side = basis[0];
        Vec3 up = basis[1];

        for (int i = 0; i < links; i++) {
            double u = (i + 0.5) / links;
            // Leve curvatura para baixo (peso da corrente) + vibração de tensão
            double sag = Math.sin(u * Math.PI) * 0.14;
            double vibrate = Math.sin(age * 0.35 + i * 1.4 + seed) * 0.015;
            Vec3 linkCenter = from.lerp(to, u)
                    .subtract(0.0, sag, 0.0)
                    .add(side.scale(vibrate));

            // Alterna a orientação de cada elo em 90 graus como uma corrente real
            Vec3 linkNormal = (i % 2 == 0) ? side : up;
            double linkRadius = (dist / links) * 0.62;

            // Elo metálico sombrio com brilho carmesim interno
            PrayerVfxPrimitives.ringPlane(m, out, linkCenter, linkNormal,
                    linkRadius, 0.020, 0.0,
                    0.18f, 0.02f, 0.08f, 0.92f * fade);
            PrayerVfxPrimitives.ringPlane(m, out, linkCenter, linkNormal,
                    linkRadius * 0.72, 0.009, 0.0,
                    0.82f, 0.10f, 0.24f, 0.75f * fade);
        }
    }

    /** Gotas carmesins descem do peito para o selo; chamas sombrias sobem do selo para o corpo. */
    private static void drawPactExchange(Matrix4fc m, VertexConsumer out,
                                         double sealR, Vec3 chest,
                                         double open, float age, float fade) {
        if (open <= 0.1) return;

        // 6 Gotas de vitalidade caindo em espiral do peito para o selo (o tributo)
        for (int i = 0; i < 6; i++) {
            double cycle = ((age * 0.028) + (i / 6.0)) % 1.0;
            double ease = cycle * cycle;
            float a = (float) (Math.sin(cycle * Math.PI) * 0.92 * fade);
            if (a <= 0.005f) continue;

            double ang = i * (Math.PI / 3.0) + cycle * 2.2 + age * 0.03;
            double r = 0.22 + ease * (sealR * 0.62);
            double y = chest.y * (1.0 - ease) + 0.08 * ease;

            Vec3 pos = new Vec3(Math.cos(ang) * r, y, Math.sin(ang) * r);
            PrayerVfxPrimitives.crystal(m, out, pos, new Vec3(0, -1, 0),
                    0.18, 0.038, 0.95f, 0.10f, 0.22f, a);
        }

        // 8 Filamentos de poder abissal subindo do selo para envolver o conjurador (a recompensa)
        for (int i = 0; i < 8; i++) {
            double cycle = ((age * 0.024) + (i / 8.0) + 0.35) % 1.0;
            float a = (float) (Math.sin(cycle * Math.PI) * 0.85 * fade);
            if (a <= 0.005f) continue;

            double ang = i * (Math.PI * 0.25) - cycle * 2.8 - age * 0.025;
            double r = (sealR * 0.78) * (1.0 - cycle * 0.55);
            double y = 0.08 + cycle * 2.15;

            Vec3 pos = new Vec3(Math.cos(ang) * r, y, Math.sin(ang) * r);
            PrayerVfxPrimitives.crystal(m, out, pos, PrayerVfxPrimitives.UP,
                    0.22, 0.032, 0.35f, 0.03f, 0.14f, a);
            PrayerVfxGeometry.ellipsoid(m, out, pos, new Vec3(0.035, 0.055, 0.035),
                    5, 7, 0.85f, 0.12f, 0.28f, a * 0.85f, ang);
        }
    }

    private static void drawContractTriangle(Matrix4fc m, VertexConsumer out, Vec3 center,
                                             double radius, double rotation, double thickness, float alpha) {
        if (alpha <= 0.005f || radius <= 0.05) return;
        Vec3[] pts = new Vec3[3];
        for (int i = 0; i < 3; i++) {
            double a = rotation + i * (Math.PI * 2.0 / 3.0);
            pts[i] = center.add(Math.cos(a) * radius, 0.0, Math.sin(a) * radius);
        }
        PrayerVfxGeometry.tube(m, out, pts[0], pts[1], thickness, 0.78f, 0.08f, 0.22f, alpha);
        PrayerVfxGeometry.tube(m, out, pts[1], pts[2], thickness, 0.78f, 0.08f, 0.22f, alpha);
        PrayerVfxGeometry.tube(m, out, pts[2], pts[0], thickness, 0.78f, 0.08f, 0.22f, alpha);
    }
}
