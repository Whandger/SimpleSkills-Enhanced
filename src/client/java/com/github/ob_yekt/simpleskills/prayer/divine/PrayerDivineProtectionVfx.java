package com.github.ob_yekt.simpleskills.prayer.divine;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerDivineProtectionVfx {
    private PrayerDivineProtectionVfx() {}

    // 5. DIVINE_PROTECTION — Santuário angelical com auréola serafínica,
    // quatro asas de luz envolventes, selo sagrado no solo e dissipação suave.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        double safeDuration = Math.max(1.0, duration);
        double t = Math.max(0.0, Math.min(1.0, age / safeDuration));

        // Entrada rápida (0% -> 18%) e saída longa e gradual (55% -> 100%)
        double fadeIn = PrayerVfxPrimitives.smooth(0.0, 0.18, t);
        double fadeOutRaw = 1.0 - PrayerVfxPrimitives.smooth(0.55, 1.0, t);
        double fadeOut = fadeOutRaw * fadeOutRaw; // Curva suave que some aos poucos até 0
        float fade = (float) (fadeIn * fadeOut);
        if (fade <= 0.005f) return;

        // Abertura inicial + leve recolhimento enquanto dissipa no final
        double build = PrayerVfxPrimitives.smooth(0.0, 0.22, t) * (0.78 + 0.22 * fadeOutRaw);
        double breathe = Math.sin(age * 0.12);
        double pulse = 1.0 + breathe * 0.025;

        Vec3 center = new Vec3(0.0, 1.05, 0.0);
        Vec3 ground = new Vec3(0.0, 0.03, 0.0);
        Vec3 haloPos = new Vec3(0.0, 2.42 + breathe * 0.04 + (1.0 - fadeOutRaw) * 0.18, 0.0);

        // ------------------------------------------------------------------
        // 1. CÚPULA ETÉREA CELESTIAL (Leitura clara de escudo ativo)
        // ------------------------------------------------------------------
        PrayerVfxGeometry.ellipsoid(m, out, center,
                new Vec3(0.95 * pulse * build, 1.32 * pulse * build, 0.95 * pulse * build),
                12, 18, 0.85f, 0.94f, 1.0f, 0.065f * fade, age * 0.015);

        // Anéis orbitais delicados em ouro e luz branca
        PrayerVfxPrimitives.ringPlane(m, out, center, PrayerVfxPrimitives.UP,
                1.04 * pulse * build, 0.016, age * 0.022,
                1.0f, 0.90f, 0.50f, 0.52f * fade);

        Vec3 tiltA = PrayerVfxPrimitives.safeNormalize(new Vec3(0.32, 1.0, 0.15), PrayerVfxPrimitives.UP);
        Vec3 tiltB = PrayerVfxPrimitives.safeNormalize(new Vec3(-0.32, 1.0, -0.15), PrayerVfxPrimitives.UP);
        PrayerVfxPrimitives.ringPlane(m, out, center, tiltA,
                1.06 * pulse * build, 0.012, -age * 0.028,
                1.0f, 0.97f, 0.82f, 0.45f * fade);
        PrayerVfxPrimitives.ringPlane(m, out, center, tiltB,
                1.06 * pulse * build, 0.012, age * 0.028,
                0.86f, 0.95f, 1.0f, 0.45f * fade);

        // ------------------------------------------------------------------
        // 2. AURÉOLA SERAFÍNICA E COLUNA DE GRAÇA SUPERIOR
        // ------------------------------------------------------------------
        double haloR = 0.56 * build * (1.0 + breathe * 0.03);
        PrayerVfxPrimitives.ringPlane(m, out, haloPos, PrayerVfxPrimitives.UP,
                haloR, 0.024, age * 0.04,
                1.0f, 0.88f, 0.36f, 0.95f * fade);
        PrayerVfxPrimitives.ringPlane(m, out, haloPos.add(0, 0.05, 0), PrayerVfxPrimitives.UP,
                haloR * 0.72, 0.014, -age * 0.06,
                1.0f, 0.99f, 0.85f, 0.90f * fade);
        PrayerVfxPrimitives.runeHalo(m, out, haloPos.add(0, 0.02, 0),
                haloR * 0.86, -age * 0.035, fade * 0.9f, 1.0f, 0.92f, 0.54f);

        // Feixe suave de luz descendo da auréola sobre o personagem
        PrayerVfxGeometry.taperedTube(m, out, haloPos, ground,
                0.48 * build, 0.82 * build,
                1.0f, 0.95f, 0.72f, 0.06f * fade);

        // ------------------------------------------------------------------
        // 3. QUATRO ASAS ANGELICAIS DE LUZ ENVOLVENTES
        // ------------------------------------------------------------------
        double wingOpen = PrayerVfxPrimitives.smooth(0.0, 0.25, t) * (0.65 + 0.35 * fadeOutRaw);
        double flap = Math.sin(age * 0.14) * 0.08;

        // Par Superior (Asas principais majestosas abraçando os flancos)
        drawAngelWing(m, out, new Vec3(0.0, 1.48, -0.22), -1.0, true, wingOpen, flap, fade);
        drawAngelWing(m, out, new Vec3(0.0, 1.48, -0.22),  1.0, true, wingOpen, flap, fade);

        // Par Inferior (Asas secundárias cruzando e protegendo a parte baixa)
        drawAngelWing(m, out, new Vec3(0.0, 1.08, -0.20), -1.0, false, wingOpen, -flap * 0.8, fade);
        drawAngelWing(m, out, new Vec3(0.0, 1.08, -0.20),  1.0, false, wingOpen, -flap * 0.8, fade);

        // ------------------------------------------------------------------
        // 4. SELO SAGRADO NO SOLO + ESCUDOS RÚNICOS FLUTUANTES
        // ------------------------------------------------------------------
        double groundR = 1.18 * build;
        PrayerVfxPrimitives.ringPlane(m, out, ground, PrayerVfxPrimitives.UP,
                groundR, 0.024, age * 0.02,
                1.0f, 0.84f, 0.32f, 0.82f * fade);
        PrayerVfxPrimitives.ringPlane(m, out, ground.add(0, 0.01, 0), PrayerVfxPrimitives.UP,
                groundR * 0.78, 0.014, -age * 0.03,
                1.0f, 0.98f, 0.80f, 0.78f * fade);
        PrayerVfxPrimitives.runeHalo(m, out, ground.add(0, 0.015, 0),
                groundR * 0.88, age * 0.025, fade * 0.85f, 1.0f, 0.90f, 0.48f);

        // Estrela de 6 pontas (dois triângulos entrelaçados) no chão
        if (build > 0.25) {
            double starR = groundR * 0.92;
            drawSacredTriangle(m, out, ground.add(0, 0.012, 0), starR, age * 0.02, 0.014, fade * 0.65f);
            drawSacredTriangle(m, out, ground.add(0, 0.012, 0), starR, age * 0.02 + Math.PI / 3.0, 0.014, fade * 0.65f);
        }

        // 4 Pequenos selos rúnicos orbitando como escudos leves
        for (int i = 0; i < 4; i++) {
            double ang = i * (Math.PI * 0.5) + age * 0.03;
            double orbitR = 1.08 * build;
            double bobY = 1.05 + Math.sin(age * 0.14 + i * 1.5) * 0.14;
            Vec3 shieldPos = new Vec3(Math.cos(ang) * orbitR, bobY, Math.sin(ang) * orbitR);

            PrayerVfxPrimitives.runeHalo(m, out, shieldPos, 0.17 * build, -ang * 2.0,
                    fade * 0.85f, 1.0f, 0.94f, 0.62f);
        }

        // ------------------------------------------------------------------
        // 5. PLUMAS E PARTÍCULAS DE LUZ ASCENDENTES
        // ------------------------------------------------------------------
        drawRisingAngelFeathers(m, out, build, age, fade);
    }

    private static void drawAngelWing(Matrix4fc m, VertexConsumer out, Vec3 shoulderOrigin,
                                      double side, boolean upperPair,
                                      double open, double flap, float fade) {
        if (open <= 0.02 || fade <= 0.005f) return;

        int feathers = upperPair ? 9 : 7;
        double baseSpan = (upperPair ? 1.48 : 1.12) * open;
        double baseHeight = upperPair ? 0.68 : -0.45;
        double wrapForward = upperPair ? 0.58 : 0.72;

        Vec3 rootStart = shoulderOrigin.add(side * 0.14, 0.0, 0.0);
        Vec3 archMid = shoulderOrigin.add(
                side * (0.62 * open),
                (upperPair ? 0.34 : 0.06) + flap * 0.25,
                0.08 * open
        );
        Vec3 archTip = shoulderOrigin.add(
                side * (0.98 * open),
                (upperPair ? 0.52 : -0.08) + flap * 0.45,
                (0.42 + flap * 0.2) * open
        );

        PrayerVfxGeometry.taperedTube(m, out, rootStart, archMid,
                0.042, 0.028, 1.0f, 0.96f, 0.76f, 0.85f * fade);
        PrayerVfxGeometry.taperedTube(m, out, archMid, archTip,
                0.028, 0.010, 1.0f, 0.99f, 0.88f, 0.80f * fade);

        for (int i = 0; i < feathers; i++) {
            double t = i / (double) (feathers - 1);
            Vec3 anchor = i < feathers / 2
                    ? rootStart.lerp(archMid, t * 2.0)
                    : archMid.lerp(archTip, (t - 0.5) * 2.0);

            double sweepAngle = (0.25 + t * 1.15 + flap) * open;
            double featherLen = baseSpan * (0.52 + 0.58 * Math.sin(t * Math.PI * 0.82));

            double dx = side * Math.sin(sweepAngle) * featherLen * 0.88;
            double dy = (baseHeight * (1.0 - t * 0.75) - t * (upperPair ? 0.62 : 0.55) + flap * 0.35) * open;
            double dz = (Math.cos(sweepAngle) * 0.35 + Math.sin(t * Math.PI) * wrapForward) * featherLen * 0.75;

            Vec3 tip = anchor.add(dx, dy, dz);

            PrayerVfxGeometry.taperedTube(m, out, anchor, tip,
                    0.052, 0.006, 1.0f, 0.88f, 0.42f, 0.32f * fade);
            PrayerVfxGeometry.taperedTube(m, out, anchor, tip,
                    0.022, 0.002, 1.0f, 0.99f, 0.92f, 0.88f * fade);
        }
    }

    private static void drawRisingAngelFeathers(Matrix4fc m, VertexConsumer out,
                                                double build, float age, float fade) {
        int count = 12;
        for (int i = 0; i < count; i++) {
            double cycle = ((age * 0.022) + (i / (double) count)) % 1.0;
            double lifeAlpha = Math.sin(cycle * Math.PI);
            float pAlpha = (float) (lifeAlpha * 0.85 * fade);
            if (pAlpha <= 0.005f) continue;

            double ang = i * 2.399963 + age * 0.025 + Math.sin(cycle * Math.PI * 2.0) * 0.35;
            double r = (0.55 + (i % 4) * 0.14) * build;
            double y = 0.18 + cycle * 2.15;

            Vec3 pos = new Vec3(Math.cos(ang) * r, y, Math.sin(ang) * r);
            double sway = Math.sin(age * 0.15 + i) * 0.35;
            Vec3 axis = new Vec3(Math.cos(ang + 1.5) * sway, 1.0, Math.sin(ang + 1.5) * sway).normalize();

            PrayerVfxPrimitives.crystal(m, out, pos, axis,
                    0.16 * build, 0.026 * build,
                    1.0f, 0.96f, 0.76f, pAlpha);
        }
    }

    private static void drawSacredTriangle(Matrix4fc m, VertexConsumer out, Vec3 center,
                                           double radius, double rotation, double thickness, float alpha) {
        if (alpha <= 0.005f) return;
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
