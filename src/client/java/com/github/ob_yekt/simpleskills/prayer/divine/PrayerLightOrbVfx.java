package com.github.ob_yekt.simpleskills.prayer.divine;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerLightOrbVfx {
    private PrayerLightOrbVfx() {}

    // 1. LIGHT_ORB — three orbital planes surround a liquid-looking core as it flies from the hand.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 4, 5);
        double travel = PrayerVfxPrimitives.smooth(5, duration * 0.66, age);
        Vec3 center = new Vec3(0, 1.18, 0).lerp(destination.add(0, 1.18, 0), travel);
        double pulse = 1 + Math.sin(age * 0.48) * 0.09;
        double size = (0.12 + 0.21 * PrayerVfxPrimitives.smooth(0, 8, age)) * pulse;
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(size, size * 1.12, size), 10, 16,
                1.0f, 0.91f, 0.63f, 0.98f * fade, age * 0.18);
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(size * 0.66, size * 0.76, size * 0.66),
                8, 14, 1.0f, 1.0f, 0.94f, 0.98f * fade, -age * 0.31);
        PrayerVfxPrimitives.ringPlane(m, out, center, PrayerVfxPrimitives.UP, size * 1.55, 0.018, age * 0.22,
                1.0f, 0.78f, 0.25f, 0.92f * fade);
        PrayerVfxPrimitives.ringPlane(m, out, center, new Vec3(1, 0.4, 0), size * 1.83, 0.014, -age * 0.31,
                1.0f, 0.93f, 0.55f, 0.84f * fade);
        PrayerVfxPrimitives.ringPlane(m, out, center, new Vec3(0.25, 0.35, 1), size * 2.12, 0.012, age * 0.38,
                0.93f, 0.76f, 0.38f, 0.76f * fade);
        for (int fragment = 0; fragment < 8; fragment++) {
            double angle = fragment * Math.PI / 4 + age * (fragment % 2 == 0 ? 0.11 : -0.14);
            Vec3 axis = new Vec3(Math.cos(angle), Math.sin(angle * 1.7) * 0.6, Math.sin(angle)).normalize();
            Vec3 point = center.add(axis.scale(size * (2.3 + (fragment % 3) * 0.22)));
            PrayerVfxPrimitives.crystal(m, out, point, axis, 0.11 * fade, 0.035 * fade,
                    1.0f, 0.86f, 0.39f, 0.9f * fade);
        }
        if (travel > 0.96) {
            double burst = PrayerVfxPrimitives.smooth(0.96, 1, travel);
            PrayerVfxPrimitives.ringPlane(m, out, destination, PrayerVfxPrimitives.UP, 0.2 + burst * 1.2, 0.022,
                    age * 0.14, 1.0f, 0.91f, 0.53f, (float) (0.8 * (1 - burst) * fade));
            PrayerVfxGeometry.wireSphere(m, out, destination, 0.16 + burst * 0.6, 10,
                    age * 0.2, 1.0f, 0.93f, 0.66f, (float) (0.55 * (1 - burst) * fade), 0.012);
        }
    }
}
