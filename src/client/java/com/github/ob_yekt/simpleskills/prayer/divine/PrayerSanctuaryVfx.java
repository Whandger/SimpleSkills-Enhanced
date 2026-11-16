package com.github.ob_yekt.simpleskills.prayer.divine;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerSanctuaryVfx {
    private PrayerSanctuaryVfx() {}

    // 6. SANCTUARY — Território consagrado amplo em estilo catedral celestial com selo octogonal,
    // 8 pilares de luz ligados por arcos sagrados, relicário central e motas ascendentes.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        double safeDuration = Math.max(1.0, duration);
        double t = Math.max(0.0, Math.min(1.0, age / safeDuration));

        // Entrada solene (0% -> 14%) e dissipação gradual no final (70% -> 100%)
        double fadeIn = PrayerVfxPrimitives.smooth(0.0, 0.14, t);
        double fadeOutRaw = 1.0 - PrayerVfxPrimitives.smooth(0.70, 1.0, t);
        double fadeOut = fadeOutRaw * fadeOutRaw;
        float fade = (float) (fadeIn * fadeOut);
        if (fade <= 0.005f) return;

        double expansion = PrayerVfxPrimitives.smooth(0.0, 0.18, t) * (0.86 + 0.14 * fadeOutRaw);
        double maxRadius = 5.8;
        double radius = maxRadius * expansion;
        double rot = age * 0.005;

        Vec3 ground = new Vec3(0.0, 0.03, 0.0);

        // ------------------------------------------------------------------
        // 1. SELO DE CATEDRAL NO SOLO + BARREIRA DE PERÍMETRO
        // ------------------------------------------------------------------
        PrayerVfxPrimitives.circleSigil(m, out, ground, radius, rot, fade * 0.92f,
                1.0f, 0.84f, 0.30f);

        PrayerVfxPrimitives.ringPlane(m, out, ground.add(0, 0.015, 0), PrayerVfxPrimitives.UP,
                radius * 1.02, 0.055, -rot * 1.5,
                1.0f, 0.95f, 0.65f, 0.85f * fade);

        PrayerVfxPrimitives.ringPlane(m, out, ground.add(0, 0.01, 0), PrayerVfxPrimitives.UP,
                radius * 0.64, 0.032, rot * 2.0,
                1.0f, 0.86f, 0.36f, 0.78f * fade);

        PrayerVfxPrimitives.ringPlane(m, out, ground.add(0, 0.01, 0), PrayerVfxPrimitives.UP,
                radius * 0.36, 0.022, -rot * 2.5,
                1.0f, 0.94f, 0.60f, 0.70f * fade);

        // Estrela de 8 pontas inscrita no chão (dois quadrados entrelaçados)
        if (expansion > 0.25) {
            double starR = radius * 0.94;
            drawSacredSquare(m, out, ground.add(0, 0.012, 0), starR, rot, 0.026, 0.68f * fade);
            drawSacredSquare(m, out, ground.add(0, 0.012, 0), starR, rot + Math.PI * 0.25, 0.026, 0.68f * fade);
        }

        // Cortina de luz translúcida marcando a borda do santuário
        double wallHeight = 1.35 * expansion;
        if (wallHeight > 0.05) {
            PrayerVfxGeometry.taperedTube(m, out, ground, ground.add(0, wallHeight, 0),
                    radius * 0.99, radius * 1.02,
                    1.0f, 0.88f, 0.42f, 0.07f * fade);
        }

        // ------------------------------------------------------------------
        // 2. OITO PILARES DE LUZ E ARCOS DE CATEDRAL CONECTADOS
        // ------------------------------------------------------------------
        int pillarCount = 8;
        Vec3[] pillarTops = new Vec3[pillarCount];
        double[] pillarGrowths = new double[pillarCount];
        double pillarRingR = radius * 0.91;

        for (int i = 0; i < pillarCount; i++) {
            double angle = i * (Math.PI * 2.0 / pillarCount) + rot;
            double staggerStart = i * 0.015;
            double pGrow = PrayerVfxPrimitives.smooth(staggerStart, staggerStart + 0.16, t) * fadeOutRaw;
            pillarGrowths[i] = pGrow;

            Vec3 base = new Vec3(Math.cos(angle) * pillarRingR, 0.05, Math.sin(angle) * pillarRingR);
            double breathe = 0.94 + 0.06 * Math.sin(age * 0.12 + i * 0.9);
            double height = (0.45 + pGrow * 2.75) * breathe;
            Vec3 top = base.add(0.0, height, 0.0);
            pillarTops[i] = top;

            if (pGrow <= 0.01) continue;

            // Aura dourada externa do pilar
            PrayerVfxGeometry.taperedTube(m, out, base, top,
                    0.20 * pGrow, 0.06 * pGrow,
                    1.0f, 0.80f, 0.26f, 0.34f * fade);
            // Núcleo branco-perolado brilhante do pilar
            PrayerVfxGeometry.taperedTube(m, out, base, top,
                    0.085 * pGrow, 0.024 * pGrow,
                    1.0f, 0.98f, 0.84f, 0.86f * fade);

            // Base luminosa do pilar no chão
            PrayerVfxPrimitives.ringPlane(m, out, base, PrayerVfxPrimitives.UP,
                    0.42 * pGrow, 0.028, -age * 0.04 + i,
                    1.0f, 0.86f, 0.35f, 0.78f * fade);

            // Orbe e auréola rúnica flutuando no topo de cada pilar
            Vec3 crownPos = top.add(0.0, 0.22 * pGrow, 0.0);
            PrayerVfxGeometry.ellipsoid(m, out, crownPos,
                    new Vec3(0.15 * pGrow, 0.22 * pGrow, 0.15 * pGrow), 8, 12,
                    1.0f, 0.97f, 0.78f, 0.92f * fade, age * 0.10 + i);
            PrayerVfxPrimitives.runeHalo(m, out, crownPos, 0.30 * pGrow,
                    age * 0.04 + i, fade * 0.85f, 1.0f, 0.88f, 0.42f);
        }

        // Arcos luminosos conectando o topo dos 8 pilares (coroa de catedral)
        for (int i = 0; i < pillarCount; i++) {
            int next = (i + 1) % pillarCount;
            double archFactor = Math.min(pillarGrowths[i], pillarGrowths[next]);
            if (archFactor <= 0.15) continue;

            Vec3 a = pillarTops[i];
            Vec3 b = pillarTops[next];
            Vec3 mid = a.lerp(b, 0.5).add(0.0, 0.52 * archFactor, 0.0);

            drawArchCurve(m, out, a, mid, b, 10, 0.022 * archFactor, 0.68f * fade);
        }

        // ------------------------------------------------------------------
        // 3. ALTAR / RELICÁRIO CENTRAL E COLUNA DE GRAÇA CELESTIAL
        // ------------------------------------------------------------------
        double altarGrow = PrayerVfxPrimitives.smooth(0.04, 0.22, t) * fadeOutRaw;
        if (altarGrow > 0.02) {
            // Degraus rúnicos na base central
            for (int tier = 0; tier < 3; tier++) {
                double r = (1.05 - tier * 0.24) * altarGrow;
                double y = 0.06 + tier * 0.16;
                PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, y, 0), PrayerVfxPrimitives.UP,
                        r, 0.032, age * (tier % 2 == 0 ? 0.025 : -0.03),
                        1.0f, 0.84f, 0.32f, 0.82f * fade);
            }

            // Relicário flutuante no coração do santuário
            double bob = Math.sin(age * 0.11) * 0.09;
            Vec3 relicPos = new Vec3(0.0, 1.25 + bob, 0.0);

            // Cristal sagrado vertical (duas pirâmides unidas)
            PrayerVfxPrimitives.crystal(m, out, relicPos.subtract(0, 0.36 * altarGrow, 0),
                    PrayerVfxPrimitives.UP, 0.72 * altarGrow, 0.23 * altarGrow,
                    1.0f, 0.96f, 0.72f, 0.92f * fade);
            PrayerVfxPrimitives.crystal(m, out, relicPos.add(0, 0.36 * altarGrow, 0),
                    new Vec3(0, -1, 0), 0.72 * altarGrow, 0.23 * altarGrow,
                    1.0f, 0.84f, 0.32f, 0.85f * fade);

            // Anéis astrolábicos orbitando o relicário
            Vec3 tilt1 = new Vec3(0.45, 1.0, 0.20).normalize();
            Vec3 tilt2 = new Vec3(-0.45, 1.0, -0.20).normalize();
            PrayerVfxPrimitives.ringPlane(m, out, relicPos, tilt1,
                    0.58 * altarGrow, 0.018, age * 0.05,
                    1.0f, 0.95f, 0.65f, 0.85f * fade);
            PrayerVfxPrimitives.ringPlane(m, out, relicPos, tilt2,
                    0.58 * altarGrow, 0.018, -age * 0.05,
                    0.88f, 0.96f, 1.0f, 0.80f * fade);

            // Coluna suave de luz descendo do céu sobre o altar
            PrayerVfxGeometry.taperedTube(m, out, new Vec3(0, 5.5, 0), ground,
                    0.35 * altarGrow, 0.95 * altarGrow,
                    1.0f, 0.94f, 0.68f, 0.075f * fade);

            // Filamentos de bênção ligando o relicário ao topo de cada pilar
            for (int i = 0; i < pillarCount; i++) {
                if (pillarGrowths[i] <= 0.2) continue;
                PrayerVfxGeometry.taperedTube(m, out, relicPos, pillarTops[i],
                        0.010, 0.018,
                        1.0f, 0.92f, 0.56f, 0.26f * fade * (float) pillarGrowths[i]);
            }
        }

        // ------------------------------------------------------------------
        // 4. MOTAS DE LUZ ASCENDENTES DENTRO DO SANTUÁRIO
        // ------------------------------------------------------------------
        drawSanctuaryMotes(m, out, radius, age, fade);
    }

    private static void drawArchCurve(Matrix4fc m, VertexConsumer out, Vec3 p0, Vec3 p1, Vec3 p2,
                                      int segments, double thickness, float alpha) {
        if (alpha <= 0.005f) return;
        Vec3 prev = p0;
        for (int s = 1; s <= segments; s++) {
            double u = s / (double) segments;
            double inv = 1.0 - u;
            Vec3 curr = p0.scale(inv * inv)
                    .add(p1.scale(2.0 * inv * u))
                    .add(p2.scale(u * u));
            PrayerVfxGeometry.tube(m, out, prev, curr, thickness,
                    1.0f, 0.92f, 0.54f, alpha);
            prev = curr;
        }
    }

    private static void drawSanctuaryMotes(Matrix4fc m, VertexConsumer out,
                                           double radius, float age, float fade) {
        if (radius <= 0.2) return;
        int count = 28;
        for (int i = 0; i < count; i++) {
            double cycle = ((age * 0.018) + (i / (double) count)) % 1.0;
            double lifeAlpha = Math.sin(cycle * Math.PI);
            float mAlpha = (float) (lifeAlpha * 0.85 * fade);
            if (mAlpha <= 0.005f) continue;

            double ang = i * 2.399963 + age * 0.015 + Math.sin(cycle * Math.PI * 2.0) * 0.25;
            double r = radius * (0.18 + ((i * 37) % 100) / 130.0);
            double y = 0.14 + cycle * 3.1;

            Vec3 pos = new Vec3(Math.cos(ang) * r, y, Math.sin(ang) * r);
            PrayerVfxPrimitives.crystal(m, out, pos, PrayerVfxPrimitives.UP,
                    0.18, 0.028,
                    1.0f, 0.95f, 0.65f, mAlpha);
        }
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
                    thickness, 1.0f, 0.90f, 0.45f, alpha);
        }
    }
}
