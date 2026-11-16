package com.github.ob_yekt.simpleskills.prayer.fire;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerInfernoVfx {
    private PrayerInfernoVfx() {}

    // 29. INFERNO — a rising fire vortex with seven independent corkscrew tongues and a hollow eye.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 6, 12);
        double rise = PrayerVfxPrimitives.smooth(0, 12, age);
        double height = 10.5 * rise;

        // Anel maior no solo acompanhando a base alargada do vórtice
        PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, 0.08, 0), PrayerVfxPrimitives.UP, 2.6 * rise, 0.06,
                -age * 0.04, 1.0f, 0.31f, 0.045f, 0.85f * fade);

        for (int strand = 0; strand < 7; strand++) {
            Vec3 previousOuter = null;
            Vec3 previousCore = null;
            for (int step = 0; step <= 34; step++) {
                double t = step / 34.0;
                double y = 0.1 + t * height;
                // Sinal negativo no age para a espiral subir em vez de parecer descer
                double angle = -age * (0.14 + strand * 0.008) + t * Math.PI * 4.6 + strand * Math.PI * 2 / 7;
                double radius = (0.95 + t * 2.35) * (0.78 + 0.22 * Math.sin(age * 0.13 + strand));
                Vec3 point = new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);

                if (previousOuter != null) {
                    double width = (0.24 + 0.11 * (strand % 3)) * (1 - t * 0.68);
                    PrayerVfxGeometry.tube(m, out, previousOuter, point, width * 1.9,
                            0.76f, 0.08f, 0.012f, 0.22f * fade);
                    PrayerVfxGeometry.tube(m, out, previousOuter, point, width,
                            1.0f, 0.22f + (strand % 2) * 0.12f, 0.018f, 0.76f * fade);
                    PrayerVfxGeometry.tube(m, out, previousCore, point, width * 0.30,
                            1.0f, 0.78f, 0.2f, 0.92f * fade);
                }
                previousOuter = point;
                previousCore = point;
            }
        }

        // Olho oco interno escalado para o centro do tornado
        PrayerVfxGeometry.ellipsoid(m, out, new Vec3(0, 1.8 * rise, 0), new Vec3(0.82, 1.9 * rise, 0.82),
                8, 14, 0.12f, 0.025f, 0.015f, 0.7f * fade, -age * 0.08);

        // Anéis de corrente ascendente acompahando a nova largura
        for (int ring = 0; ring < 3; ring++) {
            double y = 0.35 + ring * (height * 0.3);
            PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, y, 0), PrayerVfxPrimitives.UP, 1.15 + ring * 0.85,
                    0.032, age * (ring % 2 == 0 ? -0.05 : 0.04), 1.0f, 0.55f, 0.12f, 0.75f * fade);
        }
    }
}
