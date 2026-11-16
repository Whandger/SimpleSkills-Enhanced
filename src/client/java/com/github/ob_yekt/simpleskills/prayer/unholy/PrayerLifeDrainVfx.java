package com.github.ob_yekt.simpleskills.prayer.unholy;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerLifeDrainVfx {
    private PrayerLifeDrainVfx() {}

    // 12. LIFE_DRAIN — Faixas fluidas de energia carmesim e sombra fluem da vítima
    // em arcos etéreos até o conjurador, alimentando um núcleo vital pulsante.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        double safeDuration = Math.max(1.0, duration);
        double t = Math.max(0.0, Math.min(1.0, age / safeDuration));

        // Entrada suave e dissipação gradual nos últimos 30%
        double fadeIn = PrayerVfxPrimitives.smooth(0.0, 0.12, t);
        double fadeOutRaw = 1.0 - PrayerVfxPrimitives.smooth(0.70, 1.0, t);
        double fadeOut = fadeOutRaw * fadeOutRaw;
        float fade = (float) (fadeIn * fadeOut);
        if (fade <= 0.005f) return;

        // Faixas viajam da vítima até o conjurador nos primeiros 18%
        double reach = PrayerVfxPrimitives.smooth(0.0, 0.18, t);

        double dist = destination.length();
        if (dist < 0.05) return;

        Vec3 toTarget = destination.normalize();
        Vec3[] basis = PrayerVfxPrimitives.basis(toTarget);
        Vec3 side = basis[0];
        Vec3 up = basis[1];

        // ------------------------------------------------------------------
        // 1. CINCO FAIXAS FLUÍDAS DE ENERGIA (VÍTIMA -> CONJURADOR)
        // ------------------------------------------------------------------
        int ribbonCount = 5;
        int segments = 24;
        int activeSegments = Math.max(1, (int) Math.round(segments * reach));

        for (int r = 0; r < ribbonCount; r++) {
            double baseAngle = r * (Math.PI * 2.0 / ribbonCount);
            double spreadRadius = 0.24 + (r % 2) * 0.12;
            double speed = 0.09 + (r % 3) * 0.025;
            double freq = 2.0 + (r % 2) * 0.8;

            Vec3 prev = null;
            for (int i = 0; i <= activeSegments; i++) {
                // flow: 0 = na vítima (destination), 1 = no conjurador (Vec3.ZERO)
                double flow = i / (double) segments;
                double env = Math.sin(flow * Math.PI); // 0 nas pontas, 1 no meio do trajeto

                // Movimento ondulante tipo fita ao vento
                double twist = baseAngle + flow * Math.PI * freq - age * speed;
                double wave = Math.sin(flow * Math.PI * 3.0 - age * 0.18 + r * 1.3) * 0.12;
                double rad = (spreadRadius + wave) * env * (0.8 + 0.2 * fadeOutRaw);

                Vec3 pt = destination.lerp(Vec3.ZERO, flow)
                        .add(side.scale(Math.cos(twist) * rad))
                        .add(up.scale(Math.sin(twist) * rad));

                if (prev != null) {
                    // Pulso de luz correndo pela faixa na direção do player
                    double pulseWave = 0.5 + 0.5 * Math.sin(flow * Math.PI * 4.0 - age * 0.32 + r);
                    double pulseBoost = Math.pow(pulseWave, 2.0);

                    // Faixa fina e elegante (afina nas pontas para entrar limpo no corpo)
                    double thick = (0.014 + 0.012 * env) * (0.75 + 0.35 * pulseBoost) * fadeOutRaw;

                    // Gradiente de cor: roxo-sombrio na vítima -> carmesim -> rubi brilhante no player
                    float f = (float) flow;
                    float red = 0.48f + 0.52f * f;
                    float green = 0.04f + 0.18f * f + (float) (0.18 * pulseBoost);
                    float blue = 0.26f - 0.04f * f + (float) (0.14 * pulseBoost);

                    float baseAlpha = (float) (0.35 + 0.55 * env) * fade;

                    // Aura etérea da faixa
                    PrayerVfxGeometry.tube(m, out, prev, pt, thick * 2.1,
                            red * 0.75f, green * 0.4f, blue * 0.85f, 0.24f * baseAlpha);
                    // Núcleo luminoso da faixa
                    PrayerVfxGeometry.tube(m, out, prev, pt, thick,
                            red, green, blue, (float) (0.68 + 0.30 * pulseBoost) * baseAlpha);
                }
                prev = pt;
            }
        }

        // ------------------------------------------------------------------
        // 2. FAÍSCAS DE ESSÊNCIA DESLIZANDO AO LONGO DAS FAIXAS
        // ------------------------------------------------------------------
        if (reach > 0.35) {
            int sparkCount = 8;
            for (int s = 0; s < sparkCount; s++) {
                int ribbonIdx = s % ribbonCount;
                double baseAngle = ribbonIdx * (Math.PI * 2.0 / ribbonCount);
                double spreadRadius = 0.24 + (ribbonIdx % 2) * 0.12;
                double freq = 2.0 + (ribbonIdx % 2) * 0.8;

                double flow = (s / (double) sparkCount + age * 0.045) % 1.0;
                double env = Math.sin(flow * Math.PI);
                double twist = baseAngle + flow * Math.PI * freq - age * (0.09 + (ribbonIdx % 3) * 0.025);
                double rad = spreadRadius * env;

                Vec3 pos = destination.lerp(Vec3.ZERO, flow)
                        .add(side.scale(Math.cos(twist) * rad))
                        .add(up.scale(Math.sin(twist) * rad));

                float sAlpha = (float) (env * fade * 0.92);
                if (sAlpha <= 0.005f) continue;

                // Pequena lasca de luz alongada na direção do movimento
                PrayerVfxPrimitives.crystal(m, out, pos, toTarget.scale(-1.0),
                        0.18 * fadeOutRaw, 0.024 * fadeOutRaw,
                        1.0f, 0.28f, 0.38f, sAlpha);
            }
        }

        // ------------------------------------------------------------------
        // 3. SELO DE EXTRAÇÃO NA VÍTIMA & ABSORÇÃO NO CONJURADOR
        // ------------------------------------------------------------------
        double targetR = (0.48 + Math.sin(age * 0.18) * 0.03) * fadeOutRaw;
        PrayerVfxPrimitives.ringPlane(m, out, destination, toTarget.scale(-1.0),
                targetR, 0.020, age * 0.06,
                0.78f, 0.08f, 0.24f, 0.82f * fade);
        PrayerVfxPrimitives.runeHalo(m, out, destination,
                targetR * 0.72, -age * 0.05, 0.78f * fade,
                0.96f, 0.22f, 0.34f);

        // No conjurador (Vec3.ZERO): anel e núcleo recebendo as faixas
        double absorbBeat = 0.5 + 0.5 * Math.sin(age * 0.32);
        double casterR = (0.24 + absorbBeat * 0.08) * reach * fadeOutRaw;
        if (casterR > 0.01) {
            PrayerVfxPrimitives.ringPlane(m, out, Vec3.ZERO, toTarget,
                    casterR * 1.45, 0.018, -age * 0.08,
                    0.96f, 0.18f, 0.30f, (float) (0.65 + 0.30 * absorbBeat) * fade);
            PrayerVfxGeometry.ellipsoid(m, out, Vec3.ZERO,
                    new Vec3(casterR * 0.65, casterR * 0.65, casterR * 0.65),
                    8, 10, 1.0f, 0.26f, 0.34f, (float) (0.55 + 0.35 * absorbBeat) * fade, age * 0.12);
        }
    }
}
