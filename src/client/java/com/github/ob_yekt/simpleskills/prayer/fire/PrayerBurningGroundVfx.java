package com.github.ob_yekt.simpleskills.prayer.fire;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerBurningGroundVfx {
    private PrayerBurningGroundVfx() {}

    // 28. BURNING_GROUND — Piso em brasa viva com bloom suave nas bordas, brasas subindo e resfriamento gradual.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        if (age < 0.0f || age >= duration) return;

        // Fade-in rápido (4 ticks) e fade-out longo (últimos 35% da duração, mínimo 12 ticks)
        double fadeOutTicks = Math.max(12.0, duration * 0.35);
        double fadeIn = PrayerVfxPrimitives.smooth(0.0, 4.0, age);
        double fadeOutRaw = 1.0 - PrayerVfxPrimitives.smooth(Math.max(4.0, duration - fadeOutTicks), duration, age);
        double fadeOut = Math.pow(fadeOutRaw, 1.8); // Desaparecimento suave e progressivo
        float fade = (float) (fadeIn * fadeOut);
        if (fade <= 0.005f) return;

        // Expansão rápida no início + retração gradual das bordas conforme esfria
        double expansion = 1.0 - Math.pow(1.0 - Math.min(1.0, age / 8.0), 2.2);
        double coolShrink = 0.72 + 0.28 * fadeOutRaw;
        double radius = 3.1 * expansion * coolShrink;
        if (radius <= 0.05) return;

        float emberPulse = 0.85f + 0.15f * (float) Math.sin(age * 0.28);

        // ------------------------------------------------------------------
        // 1. PISO EM BRASA + BLOOM SUAVE NAS BORDAS
        // ------------------------------------------------------------------
        int segments = 36;
        Vec3 center = new Vec3(0, 0.025, 0);
        Vec3[] innerRing = new Vec3[segments];
        Vec3[] outerRing = new Vec3[segments];
        Vec3[] bloomRing = new Vec3[segments];

        for (int i = 0; i < segments; i++) {
            double angle = i * (Math.PI * 2.0 / segments);
            double wave = 1.0
                    + 0.10 * Math.sin(angle * 5.0 + age * 0.06)
                    + 0.06 * Math.cos(angle * 8.0 - age * 0.09);

            Vec3 radial = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            double rIn = radius * 0.42 * wave;
            double rOut = radius * 0.94 * wave;
            double rBloom = radius * 1.18 * wave;

            innerRing[i] = new Vec3(radial.x * rIn, 0.025, radial.z * rIn);
            outerRing[i] = new Vec3(radial.x * rOut, 0.020, radial.z * rOut);
            bloomRing[i] = new Vec3(radial.x * rBloom, 0.012, radial.z * rBloom);
        }

        for (int i = 0; i < segments; i++) {
            int next = (i + 1) % segments;

            // Núcleo central de calor intenso (brasa viva)
            drawGradientTri(m, out, center, innerRing[i], innerRing[next],
                    1.0f, 0.46f, 0.06f, 0.42f * fade * emberPulse,
                    0.85f, 0.22f, 0.02f, 0.30f * fade);

            // Anel principal de solo queimado (vermelho-escuro -> laranja na borda)
            drawGradientQuad(m, out,
                    innerRing[i], innerRing[next], outerRing[next], outerRing[i],
                    0.85f, 0.22f, 0.02f, 0.30f * fade,
                    1.0f, 0.38f, 0.04f, 0.36f * fade * emberPulse);

            // Bloom externo no chão (desaparece suavemente até alpha = 0)
            drawGradientQuad(m, out,
                    outerRing[i], outerRing[next], bloomRing[next], bloomRing[i],
                    1.0f, 0.38f, 0.04f, 0.36f * fade * emberPulse,
                    1.0f, 0.22f, 0.01f, 0.0f);
        }

        // ------------------------------------------------------------------
        // 2. BRASAS FLUTUANDO PARA CIMA NA ÁREA EM BRASA
        // ------------------------------------------------------------------
        Random rng = new Random(2828L);
        int emberCount = 24;
        for (int e = 0; e < emberCount; e++) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            double distFactor = Math.sqrt(rng.nextDouble()) * 0.95;
            double riseSpeed = 0.045 + rng.nextDouble() * 0.045;
            double cycleLen = 14.0 + rng.nextDouble() * 10.0;
            double offset = rng.nextDouble() * cycleLen;

            double localAge = (age + offset) % cycleLen;
            double ep = localAge / cycleLen;
            float eAlpha = fade * (float) Math.sin(Math.PI * ep);
            if (eAlpha <= 0.01f) continue;

            double r = radius * distFactor;
            double driftX = Math.sin(localAge * 0.35 + e) * 0.14;
            double driftZ = Math.cos(localAge * 0.30 + e * 1.7) * 0.14;
            double y = 0.06 + localAge * riseSpeed * 1.8;

            Vec3 tail = new Vec3(Math.cos(angle) * r + driftX, y, Math.sin(angle) * r + driftZ);
            Vec3 head = tail.add(0, 0.09, 0);

            PrayerVfxGeometry.taperedTube(m, out, tail, head, 0.026, 0.004,
                    1.0f, 0.45f, 0.05f, eAlpha * 0.25f);
            PrayerVfxGeometry.taperedTube(m, out, tail, head, 0.010, 0.001,
                    1.0f, 0.88f, 0.35f, eAlpha * 0.95f);
        }
    }

    private static void drawGradientQuad(Matrix4fc m, VertexConsumer out,
                                         Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                                         float r0, float g0, float b0, float a0,
                                         float r1, float g1, float b1, float a1) {
        if (a0 <= 0.004f && a1 <= 0.004f) return;
        emitVertex(m, out, p0, r0, g0, b0, a0);
        emitVertex(m, out, p1, r0, g0, b0, a0);
        emitVertex(m, out, p2, r1, g1, b1, a1);
        emitVertex(m, out, p3, r1, g1, b1, a1);

        emitVertex(m, out, p3, r1, g1, b1, a1);
        emitVertex(m, out, p2, r1, g1, b1, a1);
        emitVertex(m, out, p1, r0, g0, b0, a0);
        emitVertex(m, out, p0, r0, g0, b0, a0);
    }

    private static void drawGradientTri(Matrix4fc m, VertexConsumer out,
                                        Vec3 center, Vec3 p1, Vec3 p2,
                                        float rC, float gC, float bC, float aC,
                                        float rE, float gE, float bE, float aE) {
        if (aC <= 0.004f && aE <= 0.004f) return;
        emitVertex(m, out, center, rC, gC, bC, aC);
        emitVertex(m, out, p1, rE, gE, bE, aE);
        emitVertex(m, out, p2, rE, gE, bE, aE);
        emitVertex(m, out, center, rC, gC, bC, aC);

        emitVertex(m, out, center, rC, gC, bC, aC);
        emitVertex(m, out, p2, rE, gE, bE, aE);
        emitVertex(m, out, p1, rE, gE, bE, aE);
        emitVertex(m, out, center, rC, gC, bC, aC);
    }

    private static void emitVertex(Matrix4fc m, VertexConsumer out, Vec3 p,
                                   float r, float g, float b, float a) {
        out.addVertex(m, (float) p.x, (float) p.y, (float) p.z)
           .setColor(r, g, b, a);
    }
}
