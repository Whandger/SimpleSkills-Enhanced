package com.github.ob_yekt.simpleskills.prayer.eletro;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerThunderstormVfx {
    private PrayerThunderstormVfx() {}

    // 24. THUNDERSTORM — a layered storm cell builds overhead and sends staggered bolts to its ground.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 10, 12);
        double build = PrayerVfxPrimitives.smooth(0, 14, age);
        for (int lobe = 0; lobe < 7; lobe++) {
            double angle = lobe * Math.PI * 2 / 7;
            Vec3 center = new Vec3(Math.cos(angle) * (1.1 + lobe % 3 * 0.35),
                    10.6 + Math.sin(age * 0.08 + lobe) * 0.15,
                    Math.sin(angle) * (0.8 + lobe % 2 * 0.4));
            double size = (0.74 + (lobe % 3) * 0.13) * build;
            PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(size, size * 0.42, size * 0.72),
                    7, 12, 0.12f, 0.2f, 0.34f, 0.72f * fade, angle);
        }
        PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, 10.35, 0), PrayerVfxPrimitives.UP, 2.3, 0.045, age * 0.018,
                0.21f, 0.38f, 0.63f, 0.65f * fade);
        for (int strand = 0; strand < 6; strand++) {
            double angle = strand * Math.PI / 3 + age * 0.02;
            Vec3 from = new Vec3(Math.cos(angle) * 1.55, 10.7, Math.sin(angle) * 1.2);
            Vec3 to = new Vec3(Math.cos(angle + 0.42) * 1.3, 10.55, Math.sin(angle + 0.42) * 1.3);
            PrayerVfxPrimitives.bolt(m, out, from, to, strand * 331L + (long) age / 2, 5, 0.16, 0.8f * fade);
        }
        for (int strike = 0; strike < 4; strike++) {
            double phase = (age - 8 - strike * 11) / 9.0;
            double grow = PrayerVfxPrimitives.smooth(0, 0.34, phase) * (1 - PrayerVfxPrimitives.smooth(0.64, 1, phase));
            if (grow <= 0) continue;
            double angle = strike * 2.4 + 0.35;
            Vec3 ground = new Vec3(Math.cos(angle) * (0.9 + strike % 2), 0.1,
                    Math.sin(angle) * (0.9 + strike % 2));
            Vec3 start = ground.add(0, 10.2 * grow, 0);
            PrayerVfxPrimitives.bolt(m, out, start, ground, strike * 1301L + (long) age / 2,
                    16, 0.32, fade * (float) grow);
            PrayerVfxPrimitives.ringPlane(m, out, ground, PrayerVfxPrimitives.UP, 0.2 + grow * 1.05, 0.024, age * 0.08,
                    0.42f, 0.78f, 1.0f, 0.7f * fade * (float) grow);
        }
    }
}
