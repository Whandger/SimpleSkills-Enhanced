package com.github.ob_yekt.simpleskills.prayer.ice;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerAbsoluteZeroVfx {
    private PrayerAbsoluteZeroVfx() {}

    // Raio máximo da área de efeito no solo (~11.5 blocos = ~23x23 blocos de cobertura).
    private static final double MAX_RADIUS = 11.5;

    // 35. ABSOLUTE_ZERO — Materializa um cristal monumental que contrai o calor ao redor,
    // atinge a singularidade imóvel e detona em uma onda de congelamento ampla no solo.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 6, 8);
        if (fade <= 0.01f) return;

        double t = clamp01(age / Math.max(1.0, duration));

        // 3 Atos da animação:
        // 1. Formação e sucção térmica (0% -> 42%)
        double charge = PrayerVfxPrimitives.smooth(0.0, 0.42, t);
        // 2. Ponto de singularidade / congelamento absoluto (42% -> 56%)
        double singularity = Math.sin(clamp01((t - 0.36) / 0.24) * Math.PI);
        // 3. Colapso e onda de choque glacial no solo (55% -> 100%)
        double freeze = PrayerVfxPrimitives.smooth(0.54, 0.96, t);
        double expand = 1.0 - Math.pow(1.0 - freeze, 2.4);

        // Escala do cristal central: cresce na carga, pulsa na singularidade e colapsa no impacto
        double collapse = 1.0 - PrayerVfxPrimitives.smooth(0.54, 0.68, t);
        double coreScale = Math.pow(charge, 0.75) * (1.0 + singularity * 0.14) * collapse;

        Vec3 core = new Vec3(0, 3.8 - expand * 0.55, 0);

        // ------------------------------------------------------------------
        // 1. CRISTAL MONUMENTAL CENTRAL (3 camadas concêntricas de gelo puro)
        // ------------------------------------------------------------------
        if (coreScale > 0.02) {
            float flash = (float) singularity * 0.45f;
            // Aura externa translúcida
            PrayerVfxPrimitives.crystal(m, out, core, PrayerVfxPrimitives.UP,
                    4.6 * coreScale, 1.38 * coreScale,
                    0.36f + flash, 0.76f + flash * 0.5f, 1.0f, 0.30f * fade);
            // Camada intermediária cristalina
            PrayerVfxPrimitives.crystal(m, out, core, PrayerVfxPrimitives.UP,
                    3.45 * coreScale, 0.78 * coreScale,
                    0.72f + flash * 0.3f, 0.94f, 1.0f, 0.48f * fade);
            // Núcleo branco-ciano absoluto
            PrayerVfxPrimitives.crystal(m, out, core, PrayerVfxPrimitives.UP,
                    2.15 * coreScale, 0.32 * coreScale,
                    0.95f, 1.0f, 1.0f, 0.92f * fade);

            PrayerVfxGeometry.ellipsoid(m, out, core,
                    new Vec3(0.45 * coreScale, 1.08 * coreScale, 0.45 * coreScale),
                    9, 14, 0.82f, 0.97f, 1.0f, 0.45f * fade, age * 0.015);
        }

        // ------------------------------------------------------------------
        // 2. ANÉIS ASTROLÁBICOS QUE SE CONTRAEM E CONGELAM NO PONTO ZERO
        // ------------------------------------------------------------------
        if (collapse > 0.02) {
            double spinFactor = Math.max(0.05, 1.0 - singularity * 0.95);
            double contract = 1.0 - charge * 0.32;

            for (int orbit = 0; orbit < 3; orbit++) {
                Vec3 normal = orbit == 0
                        ? PrayerVfxPrimitives.UP
                        : (orbit == 1 ? new Vec3(1, 0.22, 0).normalize() : new Vec3(0.25, 0.18, 1).normalize());

                double radius = (3.2 + orbit * 1.15) * contract * (1.0 + expand * 0.65);
                double spin = age * (orbit % 2 == 0 ? 0.022 : -0.028) * spinFactor;
                float ringAlpha = (float) (0.76 * charge * collapse) * fade;

                PrayerVfxPrimitives.ringPlane(m, out, core, normal,
                        radius, 0.042, spin,
                        0.55f + (float) singularity * 0.35f,
                        0.88f + (float) singularity * 0.12f,
                        1.0f, ringAlpha);
            }
        }

        // ------------------------------------------------------------------
        // 3. CRISTAIS SATÉLITES: SUGADOS DE LONGE PARA DENTRO E DEPOIS ESTILHAÇADOS
        // ------------------------------------------------------------------
        for (int shard = 0; shard < 28; shard++) {
            double goldenAngle = shard * 2.399963;
            double spin = age * (shard % 2 == 0 ? 0.014 : -0.018) * (1.0 - singularity * 0.9);
            double angle = goldenAngle + spin;

            // Puxa calor de até ~11 blocos de distância até orbitar perto do núcleo
            double outerR = 8.4 + (shard % 4) * 1.05;
            double innerR = 2.0 + (shard % 3) * 0.42;
            double currentR = outerR + (innerR - outerR) * charge;

            // Após o colapso, arremessa os estilhaços até a borda da área de efeito
            double shatterOut = expand * (6.8 + (shard % 4) * 1.35);
            double radius = currentR + shatterOut;

            double baseY = Math.sin(goldenAngle * 1.7) * (2.2 - charge * 0.95);
            double fallY = -2.1 * freeze * freeze;

            Vec3 point = core.add(
                    Math.cos(angle) * radius,
                    baseY + fallY,
                    Math.sin(angle) * radius
            );

            Vec3 toCore = PrayerVfxPrimitives.safeNormalize(core.subtract(point), PrayerVfxPrimitives.UP);
            Vec3 axis = freeze <= 0.01
                    ? toCore
                    : PrayerVfxPrimitives.safeNormalize(
                            toCore.add(Math.sin(age * 0.25 + shard), -freeze * 1.4, Math.cos(age * 0.25 + shard)),
                            PrayerVfxPrimitives.UP);

            double shardScale = Math.pow(charge, 0.6) * (1.0 - freeze * 0.75);
            float shardAlpha = (float) (0.84 * (1.0 - freeze * 0.7)) * fade;

            if (shardScale > 0.04 && shardAlpha > 0.01f) {
                PrayerVfxPrimitives.crystal(m, out, point, axis,
                        (0.95 + (shard % 3) * 0.26) * shardScale,
                        0.19 * shardScale,
                        0.58f, 0.89f, 1.0f, shardAlpha);
            }
        }

        // ------------------------------------------------------------------
        // 4. DETONAÇÃO DO ZERO ABSOLUTO E CONGELAMENTO AMPLO DO CHÃO
        // ------------------------------------------------------------------
        if (freeze > 0.0) {
            float impactAlpha = (float) Math.pow(1.0 - freeze, 1.15) * fade;
            Vec3 ground = new Vec3(0, 0.04, 0);

            // A) Clarão estelar no ponto onde o cristal colapsou
            if (freeze < 0.35) {
                float flashP = (float) (freeze / 0.35);
                float flashA = (1.0f - flashP) * (1.0f - flashP) * fade;
                PrayerVfxPrimitives.starburst(m, out, core, PrayerVfxPrimitives.UP,
                        1.2 + flashP * 7.5, flashA, 0.88f, 0.97f, 1.0f);
            }

            // B) Onda de choque glacial varrendo toda a área
            double waveR = 0.5 + expand * MAX_RADIUS;
            PrayerVfxPrimitives.ringPlane(m, out, ground, PrayerVfxPrimitives.UP,
                    waveR, 0.22 * (1.0 - freeze * 0.4), age * 0.03,
                    0.55f, 0.88f, 1.0f, 0.85f * impactAlpha);
            PrayerVfxPrimitives.ringPlane(m, out, ground.add(0, 0.02, 0), PrayerVfxPrimitives.UP,
                    waveR * 0.74, 0.09, -age * 0.05,
                    0.92f, 0.98f, 1.0f, 0.90f * impactAlpha);

            // C) 48 estalagmites de gelo brotando em 6 anéis concêntricos até ~10.8 blocos de raio
            int spikeCount = 48;
            for (int spike = 0; spike < spikeCount; spike++) {
                int ringIdx = spike % 6;
                double distNorm = 0.16 + ringIdx * 0.156; // 16%, 31.6%, 47.2%, 62.8%, 78.4%, 94%
                double spikeR = distNorm * MAX_RADIUS;

                double localGrow = PrayerVfxPrimitives.smooth(distNorm * 0.68, distNorm * 0.68 + 0.22, expand);
                if (localGrow <= 0.02) continue;

                double ang = spike * (Math.PI * 2.0 / spikeCount) + ringIdx * 0.42;
                Vec3 base = new Vec3(Math.cos(ang) * spikeR, 0.0, Math.sin(ang) * spikeR);
                Vec3 tiltAxis = new Vec3(Math.cos(ang) * 0.28, 1.0, Math.sin(ang) * 0.28).normalize();

                double h = (0.95 + (spike % 3) * 0.45) * localGrow * (1.0 - freeze * 0.35);
                double w = (0.18 + (spike % 2) * 0.06) * localGrow * (1.0 - freeze * 0.35);

                PrayerVfxPrimitives.crystal(m, out, base.add(0, h * 0.42, 0), tiltAxis,
                        h, w, 0.52f, 0.86f, 1.0f, 0.88f * impactAlpha);
            }
        }
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
