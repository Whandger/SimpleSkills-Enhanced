package com.github.ob_yekt.simpleskills.prayer.unholy;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerCorruptionVfx {
    private PrayerCorruptionVfx() {}

    // 14. CORRUPTION — a projectile implants a fractured core; angular veins crawl across the impact.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 3, 6);
        double travel = PrayerVfxPrimitives.smooth(2, duration * 0.46, age);
        Vec3 point = Vec3.ZERO.lerp(destination, travel);
        double emerge = PrayerVfxPrimitives.smooth(0.36, 0.55, travel);
        double radius = 0.12 + emerge * (0.5 + Math.sin(age * 0.22) * 0.04);
        PrayerVfxGeometry.ellipsoid(m, out, point, new Vec3(radius, radius * 1.18, radius), 8, 14,
                0.065f, 0.025f, 0.11f, 0.9f * fade, age * 0.17);
        PrayerVfxGeometry.wireSphere(m, out, point, radius * 1.35, 10, -age * 0.11,
                0.66f, 0.07f, 0.26f, 0.72f * fade, 0.018);
        for (int vein = 0; vein < 9; vein++) {
            double angle = vein * Math.PI * 2 / 9 + age * 0.018;
            Vec3 start = point.add(Math.cos(angle) * radius * 0.35, Math.sin(angle * 1.7) * radius * 0.35,
                    Math.sin(angle) * radius * 0.35);
            Vec3 end = point.add(Math.cos(angle + 0.26) * (radius * 1.85),
                    Math.sin(angle * 2.1 + age * 0.1) * radius * 1.4,
                    Math.sin(angle + 0.26) * (radius * 1.85));
            PrayerVfxPrimitives.bolt(m, out, start, end, vein * 67L + (long) age / 4, 5, 0.11, 0.85f * fade * (float) emerge);
            if (vein % 2 == 0) PrayerVfxPrimitives.crystal(m, out, end, end.subtract(start), 0.3, 0.11,
                    0.32f, 0.05f, 0.34f, 0.74f * fade * (float) emerge);
        }
        if (travel > 0.92) {
            double implosion = PrayerVfxPrimitives.smooth(0.92, 1, travel);
            PrayerVfxPrimitives.ringPlane(m, out, destination, PrayerVfxPrimitives.UP, 0.2 + implosion * 1.1, 0.025,
                    -age * 0.05, 0.75f, 0.12f, 0.38f, (float) (0.72 * (1 - implosion) * fade));
        }
    }
}
