package com.github.ob_yekt.simpleskills.prayer.unholy;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerSoulHarvestVfx {
    private PrayerSoulHarvestVfx() {}

    // 16. SOUL_HARVEST — Almas espectrais emergem ao redor e espiralam suavemente para o peito do conjurador.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        double safeDuration = Math.max(1.0, duration);
        double t = clamp01(age / safeDuration);

        double fadeIn = PrayerVfxPrimitives.smooth(0.0, 0.14, t);
        double fadeOutRaw = 1.0 - PrayerVfxPrimitives.smooth(0.66, 1.0, t);
        double fadeOut = fadeOutRaw * fadeOutRaw;
        float fade = (float) (fadeIn * fadeOut);
        if (fade <= 0.005f) return;

        double open = PrayerVfxPrimitives.smooth(0.0, 0.20, t) * (0.85 + 0.15 * fadeOutRaw);
        Vec3 ground = new Vec3(0.0, 0.04, 0.0);
        Vec3 chestTarget = new Vec3(0.0, 1.15, 0.0);

        // ------------------------------------------------------------------
        // 1. SELO DISCRETO NO SOLO
        // ------------------------------------------------------------------
        double sigilRadius = 2.4 * open;
        PrayerVfxPrimitives.circleSigil(m, out, ground, sigilRadius,
                -age * 0.015, 0.58f * fade, 0.52f, 0.14f, 0.82f);
        PrayerVfxPrimitives.ringPlane(m, out, ground.add(0, 0.015, 0), PrayerVfxPrimitives.UP,
                sigilRadius * 0.56, 0.016, age * 0.024,
                0.55f, 0.88f, 0.96f, 0.45f * fade);

        // ------------------------------------------------------------------
        // 2. ALMAS ESPECTRAIS ESPIRALANDO COM CAUDA FLUIDA
        // ------------------------------------------------------------------
        int soulCount = 7;
        double absorbPulse = 0.0;

        for (int soul = 0; soul < soulCount; soul++) {
            double cycleSpeed = 38.0;
            double rawProgress = (age / cycleSpeed + (soul / (double) soulCount) * 1.14) % 1.0;
            double pull = rawProgress * rawProgress;

            if (rawProgress > 0.88) {
                absorbPulse = Math.max(absorbPulse, Math.sin(((rawProgress - 0.88) / 0.12) * Math.PI));
            }

            Vec3 posNow = evalSoulPos(soul, age, pull, sigilRadius * 0.95, chestTarget);
            double lifeEnv = Math.sin(Math.min(1.0, rawProgress * 4.5) * (Math.PI * 0.5))
                           * (1.0 - PrayerVfxPrimitives.smooth(0.90, 1.0, rawProgress));
            double scale = (0.19 + (1.0 - pull) * 0.10) * lifeEnv * open;
            float soulAlpha = fade * (float) (Math.sin(rawProgress * Math.PI) * 0.85 + 0.15);

            if (scale <= 0.005 || soulAlpha <= 0.005f) continue;

            double facing = Math.atan2(chestTarget.z - posNow.z, chestTarget.x - posNow.x);

            // Silhueta espectral + brilho suave ciano-violeta
            PrayerVfxPrimitives.soulFigure(m, out, posNow, scale, facing, soulAlpha);
            PrayerVfxGeometry.ellipsoid(m, out, posNow,
                    new Vec3(scale * 0.42, scale * 0.62, scale * 0.42),
                    6, 8, 0.58f, 0.88f, 1.0f, 0.32f * soulAlpha, facing);

            // Cauda curva fina acompanhando a espiral
            int tailSegments = 4;
            Vec3 prevTail = posNow;
            for (int seg = 1; seg <= tailSegments; seg++) {
                double pastRaw = Math.max(0.0, rawProgress - seg * 0.042);
                double pastPull = pastRaw * pastRaw;
                Vec3 tailPt = evalSoulPos(soul, age - seg * 1.6f, pastPull, sigilRadius * 0.95, chestTarget);

                float segFrac = 1.0f - (seg - 1) / (float) tailSegments;
                double rStart = 0.07 * scale * 4.2 * segFrac;
                double rEnd = 0.07 * scale * 4.2 * Math.max(0.04f, segFrac - (1.0f / tailSegments));

                PrayerVfxGeometry.taperedTube(m, out, prevTail, tailPt, rStart, rEnd,
                        0.56f, 0.24f, 0.92f, 0.58f * soulAlpha * segFrac);
                prevTail = tailPt;
            }
        }

        // ------------------------------------------------------------------
        // 3. PEQUENO NÚCLEO DE ABSORÇÃO NO PEITO
        // ------------------------------------------------------------------
        double coreRadius = (0.28 + Math.sin(age * 0.14) * 0.03 + absorbPulse * 0.12) * open;
        float coreAlpha = (float) (0.34 + absorbPulse * 0.36) * fade;

        PrayerVfxGeometry.wireSphere(m, out, chestTarget, coreRadius,
                12, age * 0.06, 0.64f, 0.22f, 0.92f, coreAlpha, 0.012);
        PrayerVfxGeometry.ellipsoid(m, out, chestTarget,
                new Vec3(coreRadius * 0.45, coreRadius * 0.45, coreRadius * 0.45),
                8, 10, 0.68f, 0.92f, 1.0f, (float) (0.38 + absorbPulse * 0.32) * fade, -age * 0.08);
    }

    private static Vec3 evalSoulPos(int soul, float age, double pull, double maxRadius, Vec3 target) {
        double dir = (soul % 2 == 0) ? 1.0 : -1.0;
        double spinBoost = pull * 3.2 * dir;
        double angle = soul * (Math.PI * 2.0 / 7.0) + age * (0.04 * dir) + spinBoost;

        double radius = maxRadius * (1.0 - pull) + 0.12;
        double startY = 0.20 + (soul % 4) * 0.45 + Math.sin(age * 0.1 + soul) * 0.10;
        double y = startY + (target.y - startY) * pull;

        return new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
