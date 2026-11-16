package com.github.ob_yekt.simpleskills.prayer.unholy;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerShadowBoltVfx {
    private PrayerShadowBoltVfx() {}

    // Marcos temporais normalizados (0.0 -> 1.0) sincronizados com PrayerVisualEffects e PrayerSoundtrack
    public static final double IMPACT_T = 0.26;      // 0% a 26%: Voo da orbe
    public static final double DETONATE_T = 0.82;    // 26% a 82%: Buraco negro sugando (4x4)
                                                     // 82% a 100%: Explosão e dano em área
    private static final double SUCTION_RADIUS = 2.15; // Raio visual cobrindo a área 4x4 ao redor do mob

    // 11. SHADOW_BOLT — Orbe sombria que colapsa em singularidade gravitacional e detona.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float total = Math.max(1.0f, duration);
        float fade = PrayerVfxPrimitives.fade(age, duration, 2, 4);
        double progress = Math.min(1.0, Math.max(0.0, age / total));

        Vec3 dir = PrayerVfxPrimitives.safeNormalize(destination, new Vec3(0, 0, -1));
        Vec3[] frame = PrayerVfxPrimitives.basis(dir);

        // =========================================================================
        // FASE 1: PROJÉTIL (Orbe densa viajando até o alvo)
        // =========================================================================
        if (progress < IMPACT_T) {
            double travel = PrayerVfxPrimitives.smooth(0.0, IMPACT_T, progress);
            Vec3 orbPos = Vec3.ZERO.lerp(destination, travel);

            // Núcleo abissal + gaiola externa violeta
            PrayerVfxGeometry.wireSphere(m, out, orbPos, 0.22, 14, -age * 0.45,
                    0.03f, 0.0f, 0.06f, 0.98f * fade, 0.035);
            PrayerVfxGeometry.wireSphere(m, out, orbPos, 0.34, 14, age * 0.35,
                    0.62f, 0.12f, 0.98f, 0.85f * fade, 0.022);

            // 4 fitas espiraladas formando a cauda da orbe
            for (int ribbon = 0; ribbon < 4; ribbon++) {
                double baseAngle = ribbon * (Math.PI * 0.5) + age * 0.5;
                Vec3 prev = orbPos.add(dir.scale(0.25));
                for (int step = 1; step <= 8; step++) {
                    double t = step / 8.0;
                    double radius = 0.48 * Math.sin(t * Math.PI);
                    double twist = baseAngle + t * Math.PI * 2.2;
                    Vec3 point = orbPos.subtract(dir.scale(t * 1.4))
                            .add(frame[0].scale(Math.cos(twist) * radius))
                            .add(frame[1].scale(Math.sin(twist) * radius));
                    PrayerVfxGeometry.tube(m, out, prev, point, 0.045 * (1.0 - t),
                            0.45f, 0.08f, 0.85f, (float) (0.85 * (1.0 - t * 0.5)) * fade);
                    prev = point;
                }
            }
            return;
        }

        // =========================================================================
        // FASE 2: BURACO NEGRO (Sucção 4x4 e colapso gravitacional)
        // =========================================================================
        if (progress < DETONATE_T) {
            double bhPhase = (progress - IMPACT_T) / (DETONATE_T - IMPACT_T);

            // Abre rápido no impacto e comprime violentamente nos últimos 22% antes de explodir
            double expand = PrayerVfxPrimitives.smooth(0.0, 0.15, bhPhase);
            double compress = PrayerVfxPrimitives.smooth(0.78, 1.0, bhPhase);
            double coreScale = expand * (1.0 - compress * 0.85);

            // 1. Horizonte de Eventos: vazio central + corona brilhante
            PrayerVfxGeometry.wireSphere(m, out, destination, 0.45 * coreScale, 18, -age * 0.4,
                    0.02f, 0.0f, 0.04f, 0.98f * fade, 0.05);
            PrayerVfxGeometry.wireSphere(m, out, destination, 0.60 * coreScale, 18, age * 0.55,
                    0.72f, 0.22f, 1.0f, 0.90f * fade, 0.025);

            // 2. Disco de Acreção principal + anel inclinado (lente gravitacional)
            PrayerVfxGeometry.torus(m, out, destination, 1.15 * coreScale, 0.045 * coreScale,
                    32, age * 0.6, 0.78f, 0.30f, 1.0f, 0.85f * fade);
            drawTiltedRing(m, out, destination, 0.95 * coreScale, 0.03 * coreScale,
                    28, -age * 0.75, 0.48, 0.92f, 0.65f, 1.0f, 0.75f * fade);

            // 3. Correntes de sucção espiralando de toda a área 4x4 para o centro
            int tendrils = 8;
            for (int i = 0; i < tendrils; i++) {
                double flowOffset = frac(bhPhase * 2.2 + (i / (double) tendrils));
                double startRadius = SUCTION_RADIUS * (1.0 - flowOffset * 0.35) * (1.0 - compress * 0.5);
                double baseAngle = i * (Math.PI * 2.0 / tendrils) + age * 0.32;
                double yOffset = Math.sin(i * 1.7 + age * 0.2) * 0.45 * (1.0 - compress);

                Vec3 prev = null;
                for (int step = 0; step <= 8; step++) {
                    double t = step / 8.0; // 0 = borda externa (4x4), 1 = núcleo
                    double r = startRadius * Math.pow(1.0 - t, 1.35) + (0.25 * coreScale * t);
                    double theta = baseAngle + t * Math.PI * 1.85;
                    double y = yOffset * (1.0 - t);

                    Vec3 point = destination.add(
                            Math.cos(theta) * r,
                            y,
                            Math.sin(theta) * r
                    );

                    if (prev != null) {
                        float heat = (float) t;
                        float red = 0.28f + 0.62f * heat;
                        float green = 0.04f + 0.46f * heat;
                        float blue = 0.58f + 0.42f * heat;
                        float alpha = (float) Math.sin(t * Math.PI) * 0.85f * fade;

                        PrayerVfxGeometry.tube(m, out, prev, point, 0.042 * (1.0 - t * 0.65),
                                red, green, blue, alpha);
                    }
                    prev = point;
                }
            }
            return;
        }

        // =========================================================================
        // FASE 3: DETONAÇÃO (Explosão após o colapso — momento do dano em área)
        // =========================================================================
        double boom = (progress - DETONATE_T) / (1.0 - DETONATE_T);
        double boomEase = PrayerVfxPrimitives.smooth(0.0, 1.0, boom);
        float alpha = (float) (1.0 - boomEase) * fade;

        // Flash central branco-violeta que expande e dissipa
        PrayerVfxGeometry.wireSphere(m, out, destination, 0.18 + boomEase * 1.15, 18, age * 0.8,
                0.95f, 0.78f, 1.0f, alpha, 0.04 * (1.0 - boomEase * 0.5));

        // Onda de choque horizontal cobrindo o raio 4x4
        PrayerVfxGeometry.torus(m, out, destination, 0.25 + boomEase * 2.15,
                0.14 * (1.0 - boomEase * 0.75), 36, age,
                0.78f, 0.28f, 1.0f, alpha);

        // Segunda onda de choque inclinada para dar volume 3D na explosão
        drawTiltedRing(m, out, destination, 0.20 + boomEase * 1.85,
                0.08 * (1.0 - boomEase * 0.75), 28, -age, -0.55,
                0.55f, 0.10f, 0.95f, alpha * 0.8f);

        // 12 estilhaços de sombra disparando radialmente para fora
        for (int shard = 0; shard < 12; shard++) {
            double yaw = shard * (Math.PI * 2.0 / 12.0) + 0.25;
            double pitch = ((shard % 3) - 1) * 0.42;
            Vec3 rayDir = new Vec3(
                    Math.cos(yaw) * Math.cos(pitch),
                    Math.sin(pitch),
                    Math.sin(yaw) * Math.cos(pitch)
            );
            Vec3 start = destination.add(rayDir.scale(0.25 + boomEase * 0.7));
            Vec3 end = destination.add(rayDir.scale(0.45 + boomEase * 2.2));
            PrayerVfxGeometry.tube(m, out, start, end, 0.045 * (1.0 - boomEase),
                    0.85f, 0.45f, 1.0f, alpha);
        }
    }

    private static void drawTiltedRing(Matrix4fc m, VertexConsumer out, Vec3 center,
                                       double radius, double thickness, int segments,
                                       double spin, double tiltRad,
                                       float r, float g, float b, float a) {
        if (radius <= 0.001 || a <= 0.001f) return;
        double cosTilt = Math.cos(tiltRad);
        double sinTilt = Math.sin(tiltRad);

        Vec3 first = null;
        Vec3 prev = null;
        for (int i = 0; i < segments; i++) {
            double angle = spin + (i * Math.PI * 2.0 / segments);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Vec3 point = center.add(x, z * sinTilt, z * cosTilt);

            if (prev != null) {
                PrayerVfxGeometry.tube(m, out, prev, point, thickness, r, g, b, a);
            } else {
                first = point;
            }
            prev = point;
        }
        if (prev != null && first != null) {
            PrayerVfxGeometry.tube(m, out, prev, first, thickness, r, g, b, a);
        }
    }

    private static double frac(double x) {
        return x - Math.floor(x);
    }
}
