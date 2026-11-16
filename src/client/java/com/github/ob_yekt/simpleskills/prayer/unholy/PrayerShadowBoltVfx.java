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

    // 11. SHADOW_BOLT — Orbe sombria que colapsa em singularidade gravitacional (Gargantua) e detona.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float total = Math.max(1.0f, duration);
        float fade = PrayerVfxPrimitives.fade(age, duration, 2, 5);
        double progress = Math.min(1.0, Math.max(0.0, age / total));

        Vec3 dir = PrayerVfxPrimitives.safeNormalize(destination, new Vec3(0, 0, -1));
        Vec3[] frame = PrayerVfxPrimitives.basis(dir);

        // =========================================================================
        // FASE 1: PROJÉTIL (Singularidade compacta viajando até o alvo)
        // =========================================================================
        if (progress < IMPACT_T) {
            double travel = PrayerVfxPrimitives.smooth(0.0, IMPACT_T, progress);
            Vec3 orbPos = Vec3.ZERO.lerp(destination, travel);

            // Núcleo escuro sólido + anel de fótons + mini disco de acreção girando
            PrayerVfxGeometry.ellipsoid(m, out, orbPos, new Vec3(0.20, 0.20, 0.20), 10, 14,
                    0.01f, 0.0f, 0.02f, 0.99f * fade, -age * 0.45);
            PrayerVfxGeometry.wireSphere(m, out, orbPos, 0.23, 14, age * 0.35,
                    0.85f, 0.55f, 1.0f, 0.92f * fade, 0.016);
            drawTiltedRing(m, out, orbPos, 0.46, 0.024, 24, age * 0.65, 0.35,
                    0.72f, 0.22f, 1.0f, 0.88f * fade);

            // 4 fitas espiraladas formando a cauda gravitacional da orbe
            for (int ribbon = 0; ribbon < 4; ribbon++) {
                double baseAngle = ribbon * (Math.PI * 0.5) + age * 0.55;
                Vec3 prev = orbPos.add(dir.scale(0.18));
                for (int step = 1; step <= 8; step++) {
                    double t = step / 8.0;
                    double radius = 0.44 * Math.sin(t * Math.PI);
                    double twist = baseAngle + t * Math.PI * 2.4;
                    Vec3 point = orbPos.subtract(dir.scale(t * 1.45))
                            .add(frame[0].scale(Math.cos(twist) * radius))
                            .add(frame[1].scale(Math.sin(twist) * radius));
                    PrayerVfxGeometry.tube(m, out, prev, point, 0.042 * (1.0 - t),
                            0.52f, 0.10f, 0.92f, (float) (0.85 * (1.0 - t * 0.5)) * fade);
                    prev = point;
                }
            }
            return;
        }

        // =========================================================================
        // FASE 2: BURACO NEGRO (Horizonte de Eventos, Lente Gravitacional e Sucção 4x4)
        // =========================================================================
        if (progress < DETONATE_T) {
            double bhPhase = (progress - IMPACT_T) / (DETONATE_T - IMPACT_T);

            // Abre rápido no impacto e colapsa violentamente nos últimos 20% antes da supernova
            double expand = PrayerVfxPrimitives.smooth(0.0, 0.14, bhPhase);
            double compress = PrayerVfxPrimitives.smooth(0.80, 1.0, bhPhase);
            double coreScale = expand * (1.0 - compress * 0.88);
            double wobble = 1.0 + Math.sin(age * 0.45) * 0.03;

            double eventHorizonR = 0.44 * coreScale * wobble;

            // 1. HORIZONTE DE EVENTOS (Esfera negra sólida que engole a luz)
            PrayerVfxGeometry.ellipsoid(m, out, destination,
                    new Vec3(eventHorizonR, eventHorizonR, eventHorizonR), 14, 20,
                    0.01f, 0.0f, 0.02f, 1.0f * fade, -age * 0.25);

            // Anel de Fótons (borda ultra-brilhante colada na esfera escura)
            PrayerVfxGeometry.wireSphere(m, out, destination, eventHorizonR * 1.06, 20, age * 0.5,
                    0.95f, 0.76f, 1.0f, 0.95f * fade, 0.015 * coreScale);

            // 2. LENTE GRAVITACIONAL (Arcos dobrados por cima e por baixo do buraco negro)
            double lensTilt = 0.16; // Leve inclinação cinematográfica
            drawTiltedRing(m, out, destination, eventHorizonR * 1.48, 0.036 * coreScale,
                    36, age * 0.75, Math.PI * 0.46,
                    0.96f, 0.80f, 1.0f, 0.92f * fade);
            drawTiltedRing(m, out, destination, eventHorizonR * 1.72, 0.024 * coreScale,
                    36, -age * 0.60, Math.PI * 0.46 + 0.14,
                    0.68f, 0.22f, 1.0f, 0.72f * fade);

            // 3. DISCO DE ACREÇÃO MULTI-CAMADAS (Gradiente térmico: branco-lilás -> violeta -> abissal)
            // Camada interna incandescente (matéria superaquecida caindo no horizonte)
            drawTiltedRing(m, out, destination, eventHorizonR * 1.45, 0.052 * coreScale,
                    40, age * 0.95, lensTilt,
                    0.98f, 0.86f, 1.0f, 0.96f * fade);
            // Camada média violeta-elétrico
            drawTiltedRing(m, out, destination, eventHorizonR * 2.15, 0.068 * coreScale,
                    44, age * 0.68, lensTilt,
                    0.74f, 0.24f, 1.0f, 0.88f * fade);
            // Camada externa roxo-profundo (estende até a borda da área 4x4)
            drawTiltedRing(m, out, destination, eventHorizonR * 3.05, 0.045 * coreScale,
                    48, age * 0.45, lensTilt,
                    0.42f, 0.06f, 0.78f, 0.68f * fade);
            // Filete fino orbital externo
            drawTiltedRing(m, out, destination, eventHorizonR * 3.75, 0.018 * coreScale,
                    48, -age * 0.35, lensTilt,
                    0.58f, 0.14f, 0.92f, 0.48f * fade);

            // 4. JATOS RELATIVÍSTICOS POLARES (Feixes finos saindo dos polos norte e sul)
            double jetLen = 1.55 * coreScale * (0.9 + 0.1 * Math.sin(age * 0.6));
            if (jetLen > 0.08) {
                Vec3 poleAxis = new Vec3(0.0, Math.cos(lensTilt), Math.sin(lensTilt)).normalize();
                Vec3 northTip = destination.add(poleAxis.scale(jetLen));
                Vec3 southTip = destination.subtract(poleAxis.scale(jetLen));

                PrayerVfxGeometry.taperedTube(m, out, destination.add(poleAxis.scale(eventHorizonR * 0.8)), northTip,
                        0.055 * coreScale, 0.004,
                        0.82f, 0.45f, 1.0f, 0.68f * fade);
                PrayerVfxGeometry.taperedTube(m, out, destination.subtract(poleAxis.scale(eventHorizonR * 0.8)), southTip,
                        0.055 * coreScale, 0.004,
                        0.82f, 0.45f, 1.0f, 0.68f * fade);
            }

            // 5. CORRENTES DE MATÉRIA ESPIRALANDO DA ÁREA 4x4 PARA O HORIZONTE DE EVENTOS
            int tendrils = 12;
            for (int i = 0; i < tendrils; i++) {
                double flowOffset = frac(bhPhase * 2.5 + (i / (double) tendrils));
                double startRadius = SUCTION_RADIUS * (1.05 - flowOffset * 0.38) * (1.0 - compress * 0.45);
                double baseAngle = i * (Math.PI * 2.0 / tendrils) + age * 0.38;
                double yOffset = Math.sin(i * 2.1 + age * 0.22) * 0.48 * (1.0 - compress);

                Vec3 prev = null;
                for (int step = 0; step <= 10; step++) {
                    double t = step / 10.0; // 0 = borda externa (4x4), 1 = horizonte de eventos
                    double r = startRadius * Math.pow(1.0 - t, 1.45) + (eventHorizonR * 0.85 * t);
                    double theta = baseAngle + t * Math.PI * 2.15;
                    double y = yOffset * Math.pow(1.0 - t, 1.3);

                    // Inclina levemente o plano de sucção para acompanhar o disco de acreção
                    double tiltedY = y + Math.sin(theta) * r * Math.sin(lensTilt);
                    double tiltedZ = Math.sin(theta) * r * Math.cos(lensTilt);

                    Vec3 point = destination.add(
                            Math.cos(theta) * r,
                            tiltedY,
                            tiltedZ
                    );

                    if (prev != null) {
                        // Aquece de roxo-escuro na borda até branco-lilás incandescente ao cair no centro
                        float heat = (float) (t * t);
                        float red = 0.26f + 0.72f * heat;
                        float green = 0.03f + 0.78f * heat;
                        float blue = 0.56f + 0.44f * heat;
                        float alpha = (float) Math.sin(t * Math.PI) * 0.88f * fade;

                        PrayerVfxGeometry.tube(m, out, prev, point, 0.038 * (1.0 - t * 0.6),
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
        float alpha = (float) Math.pow(1.0 - boomEase, 1.35) * fade;

        // Flash central branco-violeta que expande e dissipa
        PrayerVfxGeometry.ellipsoid(m, out, destination,
                new Vec3(0.20 + boomEase * 1.15, 0.20 + boomEase * 1.15, 0.20 + boomEase * 1.15),
                12, 16, 0.96f, 0.82f, 1.0f, 0.65f * alpha, age * 0.8);
        PrayerVfxGeometry.wireSphere(m, out, destination, 0.24 + boomEase * 1.45, 18, age * 0.8,
                0.95f, 0.78f, 1.0f, alpha, 0.035 * (1.0 - boomEase * 0.5));

        // Onda de choque horizontal cobrindo o raio 4x4
        PrayerVfxGeometry.torus(m, out, destination, 0.25 + boomEase * 2.25,
                0.14 * (1.0 - boomEase * 0.75), 36, age,
                0.78f, 0.28f, 1.0f, alpha);

        // Segunda onda de choque inclinada para dar volume 3D na explosão
        drawTiltedRing(m, out, destination, 0.20 + boomEase * 1.95,
                0.08 * (1.0 - boomEase * 0.75), 32, -age, -0.55,
                0.55f, 0.10f, 0.95f, alpha * 0.85f);

        // 14 estilhaços de sombra disparando radialmente para fora
        for (int shard = 0; shard < 14; shard++) {
            double yaw = shard * (Math.PI * 2.0 / 14.0) + 0.25;
            double pitch = ((shard % 3) - 1) * 0.42;
            Vec3 rayDir = new Vec3(
                    Math.cos(yaw) * Math.cos(pitch),
                    Math.sin(pitch),
                    Math.sin(yaw) * Math.cos(pitch)
            );
            Vec3 start = destination.add(rayDir.scale(0.25 + boomEase * 0.7));
            Vec3 end = destination.add(rayDir.scale(0.45 + boomEase * 2.35));
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
