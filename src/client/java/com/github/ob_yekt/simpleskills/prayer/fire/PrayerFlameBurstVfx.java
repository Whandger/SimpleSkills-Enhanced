package com.github.ob_yekt.simpleskills.prayer.fire;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerFlameBurstVfx {
    private PrayerFlameBurstVfx() {}

    // 27. FLAME_BURST — Estalo inicial e parede contínua de fogo com halo de bloom rasante ao solo.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        double life = Math.max(8.0, duration * 0.78);
        if (age < 0.0f || age > life) return;

        double p = age / life; // 0.0 -> 1.0
        double travel = 1.0 - Math.pow(1.0 - p, 2.05);
        float alpha = (float) (PrayerVfxPrimitives.smooth(0.0, 0.08, p) * Math.pow(1.0 - p, 1.5));
        if (alpha <= 0.01f) return;

        // ------------------------------------------------------------------
        // 1. ESTALO DE IGNIÇÃO RENTE AO CHÃO + FLASH DE BLOOM INICIAL
        // ------------------------------------------------------------------
        if (age < 2.5f) {
            float snap = 1.0f - (age / 2.5f);
            Vec3 snapPos = new Vec3(0, 0.18, 0);
            PrayerVfxPrimitives.starburst(m, out, snapPos, PrayerVfxPrimitives.UP,
                    0.22 + (1.0 - snap) * 0.65, snap * snap, 1.0f, 0.94f, 0.65f);
            // Halo suave do estalo
            PrayerVfxGeometry.ellipsoid(m, out, snapPos, new Vec3(0.45, 0.25, 0.45),
                    6, 8, 1.0f, 0.55f, 0.10f, 0.22f * snap * snap, age * 0.4);
        }

        // ------------------------------------------------------------------
        // 2. CORTINA CONTÍNUA DE FOGO + CAMADAS DE BLOOM (GRADIENTE SUAVE)
        // ------------------------------------------------------------------
        int segments = 36;
        double frontRadius = 0.22 + travel * 2.95;
        double trailRadius = Math.max(0.05, frontRadius - (0.55 + Math.sin(p * Math.PI) * 0.45) * (1.0 - p * 0.4));
        double maxCrest = (0.35 + Math.sin(p * Math.PI) * 0.55) * (1.0 - p * 0.45);

        Vec3[] floorPts = new Vec3[segments];
        Vec3[] midPts = new Vec3[segments];
        Vec3[] crestPts = new Vec3[segments];

        // Pontos exclusivos para o bloom (mais largos e altos, mas com alpha = 0 nas pontas)
        Vec3[] bloomInnerFloor = new Vec3[segments];
        Vec3[] bloomOuterFloor = new Vec3[segments];
        Vec3[] bloomTopPts = new Vec3[segments];

        for (int i = 0; i < segments; i++) {
            double angle = i * (Math.PI * 2.0 / segments);

            double radialWave = 1.0
                    + 0.14 * Math.sin(angle * 5.0 + age * 0.35)
                    + 0.08 * Math.cos(angle * 9.0 - age * 0.50);

            double flameWave = 0.45
                    + 0.35 * Math.sin(angle * 7.0 - age * 0.65)
                    + 0.20 * Math.cos(angle * 13.0 + age * 0.85);
            flameWave = Math.max(0.18, flameWave);

            Vec3 radial = new Vec3(Math.cos(angle), 0, Math.sin(angle));

            double rBack = trailRadius * radialWave;
            double rFront = frontRadius * radialWave;
            double rMid = rBack + (rFront - rBack) * 0.62;

            double hMid = maxCrest * flameWave * 0.52;
            double hTop = maxCrest * flameWave;

            floorPts[i] = new Vec3(radial.x * rBack, 0.03, radial.z * rBack);
            midPts[i] = new Vec3(radial.x * rFront, 0.04 + hMid, radial.z * rFront);
            crestPts[i] = new Vec3(radial.x * rMid, 0.05 + hTop, radial.z * rMid);

            // Extensões suaves do bloom (sem arestas duras)
            double rBloomIn = Math.max(0.02, rBack - 0.38);
            double rBloomOut = rFront + 0.42;
            bloomInnerFloor[i] = new Vec3(radial.x * rBloomIn, 0.015, radial.z * rBloomIn);
            bloomOuterFloor[i] = new Vec3(radial.x * rBloomOut, 0.015, radial.z * rBloomOut);
            bloomTopPts[i] = new Vec3(radial.x * rMid, 0.12 + hTop * 1.65, radial.z * rMid);
        }

        for (int i = 0; i < segments; i++) {
            int next = (i + 1) % segments;

            // --- BLOOM A: Halo de luz espalhado no chão (desaparece até alpha 0 nas bordas) ---
            drawGradientQuad(m, out,
                    bloomInnerFloor[i], bloomInnerFloor[next], floorPts[next], floorPts[i],
                    1.0f, 0.35f, 0.03f, 0.0f,
                    1.0f, 0.48f, 0.06f, alpha * 0.26f);

            drawGradientQuad(m, out,
                    midPts[i], midPts[next], bloomOuterFloor[next], bloomOuterFloor[i],
                    1.0f, 0.52f, 0.08f, alpha * 0.28f,
                    1.0f, 0.30f, 0.02f, 0.0f);

            // --- BLOOM B: Aura de calor envolvente subindo acima das chamas (alpha -> 0 no topo) ---
            drawGradientQuad(m, out,
                    floorPts[i], floorPts[next], bloomTopPts[next], bloomTopPts[i],
                    1.0f, 0.45f, 0.05f, alpha * 0.24f,
                    1.0f, 0.28f, 0.02f, 0.0f);

            // --- FOGO PRINCIPAL: Base vermelha-laranja -> crista laranja brilhante ---
            drawGradientQuad(m, out,
                    floorPts[i], floorPts[next], midPts[next], midPts[i],
                    1.0f, 0.34f, 0.03f, alpha * 0.72f,
                    1.0f, 0.56f, 0.07f, alpha * 0.80f);

            drawGradientQuad(m, out,
                    midPts[i], midPts[next], crestPts[next], crestPts[i],
                    1.0f, 0.56f, 0.07f, alpha * 0.80f,
                    1.0f, 0.76f, 0.16f, alpha * 0.35f);

            // --- NÚCLEO INCANDESCENTE (Gatilho visual do bloom na frente de onda) ---
            if (p < 0.78) {
                float hotAlpha = alpha * (float) (1.0 - p / 0.78);
                Vec3 hotTopA = floorPts[i].lerp(midPts[i], 0.55);
                Vec3 hotTopB = floorPts[next].lerp(midPts[next], 0.55);

                drawGradientQuad(m, out,
                        floorPts[i], floorPts[next], hotTopB, hotTopA,
                        1.0f, 0.96f, 0.72f, hotAlpha * 0.92f,
                        1.0f, 0.78f, 0.20f, hotAlpha * 0.25f);
            }
        }

        // ------------------------------------------------------------------
        // 3. LÍNGUAS FINAS LAMBENDO AS CRISTAS + FAÍSCAS COM HALO
        // ------------------------------------------------------------------
        Random rng = new Random(2777L);
        for (int t = 0; t < 18; t++) {
            int idx = (t * 2) % segments;
            Vec3 base = midPts[idx];
            Vec3 crest = crestPts[idx];

            double swayX = Math.sin(age * 0.7 + t) * 0.12;
            double swayZ = Math.cos(age * 0.6 + t * 1.3) * 0.12;
            Vec3 tip = crest.add(swayX, 0.14 + rng.nextDouble() * 0.22 * (1.0 - p * 0.5), swayZ);

            // Halo translúcido ao redor da língua de fogo (bloom local)
            PrayerVfxGeometry.taperedTube(m, out, base, tip, 0.085, 0.015,
                    1.0f, 0.42f, 0.04f, alpha * 0.16f);

            // Chama interna nítida
            PrayerVfxGeometry.taperedTube(m, out, base, crest, 0.042, 0.020,
                    1.0f, 0.50f, 0.06f, alpha * 0.78f);
            PrayerVfxGeometry.taperedTube(m, out, crest, tip, 0.020, 0.002,
                    1.0f, 0.85f, 0.28f, alpha * 0.88f);
        }

        // Faíscas baixas correndo na frente da onda com pequeno rastro de bloom
        for (int s = 0; s < 16; s++) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            double speed = 0.95 + rng.nextDouble() * 0.45;
            double yHead = 0.05 + rng.nextDouble() * 0.32 + p * 0.15;
            double yTail = Math.max(0.03, yHead - 0.06);

            Vec3 radial = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            double headR = frontRadius * speed * 1.08;
            double tailR = Math.max(0.08, headR - (0.28 * (1.0 - p * 0.5)));

            Vec3 tail = new Vec3(radial.x * tailR, yTail, radial.z * tailR);
            Vec3 head = new Vec3(radial.x * headR, yHead, radial.z * headR);

            // Glow suave da faísca
            PrayerVfxGeometry.taperedTube(m, out, tail, head, 0.034, 0.006,
                    1.0f, 0.50f, 0.08f, alpha * 0.22f);
            // Núcleo brilhante da faísca
            PrayerVfxGeometry.taperedTube(m, out, tail, head, 0.012, 0.001,
                    1.0f, 0.92f, 0.55f, alpha * 0.95f);
        }
    }

    /** Quad duplo com gradiente de cor e alpha entre a borda inferior (p0-p1) e superior (p2-p3). */
    private static void drawGradientQuad(Matrix4fc m, VertexConsumer out,
                                         Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                                         float r0, float g0, float b0, float a0,
                                         float r1, float g1, float b1, float a1) {
        if (a0 <= 0.004f && a1 <= 0.004f) return;
        // Frente
        emitVertex(m, out, p0, r0, g0, b0, a0);
        emitVertex(m, out, p1, r0, g0, b0, a0);
        emitVertex(m, out, p2, r1, g1, b1, a1);
        emitVertex(m, out, p3, r1, g1, b1, a1);
        // Verso
        emitVertex(m, out, p3, r1, g1, b1, a1);
        emitVertex(m, out, p2, r1, g1, b1, a1);
        emitVertex(m, out, p1, r0, g0, b0, a0);
        emitVertex(m, out, p0, r0, g0, b0, a0);
    }

    private static void emitVertex(Matrix4fc m, VertexConsumer out, Vec3 p,
                                   float r, float g, float b, float a) {
        out.addVertex(m, (float) p.x, (float) p.y, (float) p.z)
           .setColor(r, g, b, a);
    }
}
