package com.github.ob_yekt.simpleskills.prayer.fire;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerInfernoVfx {
    private PrayerInfernoVfx() {}

    // 29. INFERNO — a rising fire vortex with six independent corkscrew tongues and a hollow eye.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 6, 10);
        double rise = PrayerVfxPrimitives.smooth(0, 12, age);
        double height = 6.2 * rise;
        PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, 0.08, 0), PrayerVfxPrimitives.UP, 1.6 * rise, 0.04,
                age * 0.04, 1.0f, 0.31f, 0.045f, 0.85f * fade);
        for (int strand = 0; strand < 7; strand++) {
            Vec3 previousOuter = null;
            Vec3 previousCore = null;
            for (int step = 0; step <= 28; step++) {
                double t = step / 28.0;
                double y = 0.1 + t * height;
                double angle = age * (0.09 + strand * 0.006) + t * Math.PI * 4.3 + strand * Math.PI * 2 / 7;
                double radius = (0.55 + t * 1.15) * (0.75 + 0.25 * Math.sin(age * 0.13 + strand));
                Vec3 point = new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
                if (previousOuter != null) {
                    double width = (0.15 + 0.07 * (strand % 3)) * (1 - t * 0.72);
                    PrayerVfxGeometry.tube(m, out, previousOuter, point, width * 1.8,
                            0.76f, 0.08f, 0.012f, 0.22f * fade);
                    PrayerVfxGeometry.tube(m, out, previousOuter, point, width,
                            1.0f, 0.22f + (strand % 2) * 0.12f, 0.018f, 0.76f * fade);
                    PrayerVfxGeometry.tube(m, out, previousCore, point, width * 0.28,
                            1.0f, 0.78f, 0.2f, 0.92f * fade);
                }
                previousOuter = point;
                previousCore = point;
            }
        }
        PrayerVfxGeometry.ellipsoid(m, out, new Vec3(0, 1.1, 0), new Vec3(0.47, 1.2, 0.47),
                8, 14, 0.12f, 0.025f, 0.015f, 0.7f * fade, age * 0.08);
        for (int ring = 0; ring < 3; ring++) {
            double y = 0.24 + ring * (height * 0.3);
            PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, y, 0), PrayerVfxPrimitives.UP, 0.7 + ring * 0.43,
                    0.022, age * (ring % 2 == 0 ? 0.05 : -0.04), 1.0f, 0.55f, 0.12f, 0.75f * fade);
        }
    }
}
