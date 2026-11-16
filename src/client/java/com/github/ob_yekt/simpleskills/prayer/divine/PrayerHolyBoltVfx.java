package com.github.ob_yekt.simpleskills.prayer.divine;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerHolyBoltVfx {
    private PrayerHolyBoltVfx() {}

    // 3. HOLY_BOLT — esfera de energia sagrada pulsante cercada por brilhinhos cintilantes e rastro estelar.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 2, 4);
        double travel = PrayerVfxPrimitives.smooth(2, duration * 0.56, age);
        Vec3 direction = PrayerVfxPrimitives.safeNormalize(destination.subtract(Vec3.ZERO), new Vec3(0, 0, -1));
        Vec3 center = Vec3.ZERO.lerp(destination, travel);
        Vec3[] frame = PrayerVfxPrimitives.basis(direction);

        // 1. Núcleo interno intenso e pulsante
        double pulse = Math.sin(age * 0.85);
        double coreRadius = 0.16 + pulse * 0.02;
        drawEnergySphere(m, out, center, coreRadius, age * 0.22,
                1.0f, 0.98f, 0.84f, 0.95f * fade);

        // 2. Aura externa de energia dourada translúcida
        double auraRadius = 0.27 - pulse * 0.025;
        drawEnergySphere(m, out, center, auraRadius, -age * 0.15,
                1.0f, 0.82f, 0.24f, 0.42f * fade);

        // 3. Anéis finos de energia orbitando a esfera
        Vec3 tiltNormal1 = PrayerVfxPrimitives.safeNormalize(
                direction.scale(0.65).add(frame[0].scale(0.75)), PrayerVfxPrimitives.UP);
        Vec3 tiltNormal2 = PrayerVfxPrimitives.safeNormalize(
                direction.scale(0.65).subtract(frame[1].scale(0.75)), PrayerVfxPrimitives.UP);

        PrayerVfxPrimitives.ringPlane(m, out, center, tiltNormal1, 0.34, 0.011,
                age * 0.45, 1.0f, 0.88f, 0.35f, 0.78f * fade);
        PrayerVfxPrimitives.ringPlane(m, out, center, tiltNormal2, 0.31, 0.009,
                -age * 0.52, 1.0f, 0.96f, 0.68f, 0.72f * fade);

        // 4. Brilhinhos orbitando ao redor da bola de energia
        drawOrbitingSparkles(m, out, center, age, fade);

        // 5. Rastro de brilhinhos ficando para trás durante o trajeto
        if (travel > 0.05) {
            drawTrailSparkles(m, out, center, direction, frame, travel, age, fade);
        }

        // 6. Explosão de luz e brilhinhos no impacto
        if (travel > 0.88) {
            double impact = PrayerVfxPrimitives.smooth(0.88, 1.0, travel);
            float impactAlpha = (float) ((1.0 - impact) * fade);

            PrayerVfxPrimitives.starburst(m, out, destination, PrayerVfxPrimitives.UP,
                    0.35 + impact * 0.95, 0.92f * impactAlpha, 1.0f, 0.88f, 0.36f);
            PrayerVfxPrimitives.ringPlane(m, out, destination, direction,
                    0.18 + impact * 0.62, 0.02, age * 0.15, 1.0f, 0.97f, 0.72f, 0.85f * impactAlpha);

            drawImpactSparkles(m, out, destination, impact, age, impactAlpha);
        }
    }

    private static void drawOrbitingSparkles(Matrix4fc m, VertexConsumer out, Vec3 center, float age, float fade) {
        int count = 16;
        double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));

        for (int i = 0; i < count; i++) {
            double t = (i + 0.5) / count;
            double y = 1.0 - 2.0 * t;
            double radial = Math.sqrt(Math.max(0.0, 1.0 - y * y));
            double theta = goldenAngle * i + age * (0.14 + (i % 3) * 0.04);

            double orbitRadius = 0.31 + (i % 4) * 0.045 + Math.sin(age * 0.35 + i) * 0.025;
            Vec3 offset = new Vec3(
                    Math.cos(theta) * radial * orbitRadius,
                    y * orbitRadius,
                    Math.sin(theta) * radial * orbitRadius
            );

            // Efeito de piscar (twinkle) independente para cada brilhinho
            double twinkle = 0.5 + 0.5 * Math.sin(age * 0.85 + i * 2.3);
            if (twinkle < 0.15) continue;

            double size = (0.026 + (i % 3) * 0.011) * (0.45 + 0.75 * twinkle);
            float alpha = (float) (twinkle * 0.95 * fade);

            // Alterna entre branco brilhante, dourado claro e ouro quente
            float r = 1.0f;
            float g = (i % 3 == 0) ? 0.99f : (i % 3 == 1 ? 0.91f : 0.80f);
            float b = (i % 3 == 0) ? 0.90f : (i % 3 == 1 ? 0.52f : 0.25f);

            drawSparkle(m, out, center.add(offset), size, age * 0.3 + i, r, g, b, alpha);
        }
    }

    private static void drawTrailSparkles(Matrix4fc m, VertexConsumer out, Vec3 center, Vec3 direction,
                                          Vec3[] frame, double travel, float age, float fade) {
        int count = 14;
        double maxTrailLength = Math.min(1.25, travel * 2.2);

        for (int i = 0; i < count; i++) {
            double progress = (i + 1.0) / count;
            double distBack = progress * maxTrailLength;
            double spiralAngle = i * 1.9 - age * 0.35;
            double spread = 0.05 + progress * 0.18;

            Vec3 radialOffset = frame[0].scale(Math.cos(spiralAngle) * spread)
                    .add(frame[1].scale(Math.sin(spiralAngle) * spread));
            Vec3 pos = center.subtract(direction.scale(distBack)).add(radialOffset);

            double twinkle = 0.55 + 0.45 * Math.sin(age * 0.95 - i * 1.7);
            double size = (0.032 * (1.0 - progress * 0.65)) * twinkle;
            float alpha = (float) ((1.0 - progress) * twinkle * 0.9 * fade);

            if (alpha <= 0.02f) continue;

            drawSparkle(m, out, pos, size, -age * 0.25 + i,
                    1.0f, 0.93f, 0.58f, alpha);
        }
    }

    private static void drawImpactSparkles(Matrix4fc m, VertexConsumer out, Vec3 destination,
                                           double impact, float age, float impactAlpha) {
        int count = 10;
        double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));

        for (int i = 0; i < count; i++) {
            double t = (i + 0.5) / count;
            double y = 1.0 - 2.0 * t;
            double radial = Math.sqrt(Math.max(0.0, 1.0 - y * y));
            double theta = goldenAngle * i;

            double burstDist = 0.2 + impact * (0.55 + (i % 3) * 0.18);
            Vec3 pos = destination.add(
                    Math.cos(theta) * radial * burstDist,
                    y * burstDist,
                    Math.sin(theta) * radial * burstDist
            );

            double size = 0.042 * (1.0 - impact * 0.5);
            drawSparkle(m, out, pos, size, age * 0.4 + i, 1.0f, 0.96f, 0.72f, impactAlpha);
        }
    }

    private static void drawSparkle(Matrix4fc m, VertexConsumer out, Vec3 center, double size, double spin,
                                    float r, float g, float b, float alpha) {
        double thin = size * 0.22;
        double cos = Math.cos(spin);
        double sin = Math.sin(spin);

        Vec3 ax = new Vec3(cos, 0, sin);
        Vec3 ay = new Vec3(0, 1, 0);
        Vec3 az = new Vec3(-sin, 0, cos);

        // 3 losangos cruzados em 3D para que o brilhinho apareça nítido de qualquer ângulo
        drawDiamond(m, out, center, ax, ay, size, thin, r, g, b, alpha);
        drawDiamond(m, out, center, ay, az, size, thin, r, g, b, alpha);
        drawDiamond(m, out, center, az, ax, size, thin, r, g, b, alpha);
    }

    private static void drawDiamond(Matrix4fc m, VertexConsumer out, Vec3 center, Vec3 majorAxis, Vec3 minorAxis,
                                    double majorRadius, double minorRadius,
                                    float r, float g, float b, float alpha) {
        Vec3 p1 = center.add(majorAxis.scale(majorRadius));
        Vec3 p2 = center.add(minorAxis.scale(minorRadius));
        Vec3 p3 = center.subtract(majorAxis.scale(majorRadius));
        Vec3 p4 = center.subtract(minorAxis.scale(minorRadius));
        PrayerVfxGeometry.quad(m, out, p1, p2, p3, p4, r, g, b, alpha);
    }

    private static void drawEnergySphere(Matrix4fc m, VertexConsumer out, Vec3 center, double radius, double spin,
                                         float r, float g, float b, float alpha) {
        int stacks = 6;
        int slices = 10;

        for (int i = 0; i < stacks; i++) {
            double phi0 = Math.PI * i / stacks;
            double phi1 = Math.PI * (i + 1) / stacks;

            for (int j = 0; j < slices; j++) {
                double theta0 = (2.0 * Math.PI * j / slices) + spin;
                double theta1 = (2.0 * Math.PI * (j + 1) / slices) + spin;

                Vec3 v00 = spherePoint(center, radius, phi0, theta0);
                Vec3 v10 = spherePoint(center, radius, phi1, theta0);
                Vec3 v11 = spherePoint(center, radius, phi1, theta1);
                Vec3 v01 = spherePoint(center, radius, phi0, theta1);

                if (i == 0) {
                    PrayerVfxGeometry.triangle(m, out, v00, v10, v11, r, g, b, alpha);
                } else if (i == stacks - 1) {
                    PrayerVfxGeometry.triangle(m, out, v00, v10, v01, r, g, b, alpha);
                } else {
                    PrayerVfxGeometry.quad(m, out, v00, v10, v11, v01, r, g, b, alpha);
                }
            }
        }
    }

    private static Vec3 spherePoint(Vec3 center, double radius, double phi, double theta) {
        double sinPhi = Math.sin(phi);
        double x = radius * sinPhi * Math.cos(theta);
        double y = radius * Math.cos(phi);
        double z = radius * sinPhi * Math.sin(theta);
        return center.add(x, y, z);
    }
}
