package com.github.ob_yekt.simpleskills.prayer.unholy;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerLifeDrainVfx {
    private PrayerLifeDrainVfx() {}

    // 12. LIFE_DRAIN — a living tether returns moving blood-soul motes from the victim to the caster.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 3, 6);
        double connect = PrayerVfxPrimitives.smooth(2, 8, age);
        Vec3 direction = PrayerVfxPrimitives.safeNormalize(destination, new Vec3(0, 0, -1));
        Vec3 side = PrayerVfxPrimitives.basis(direction)[0];
        Vec3 previous = Vec3.ZERO;
        int segments = 28;
        for (int i = 1; i <= segments; i++) {
            double t = i / (double) segments;
            double sway = Math.sin(t * Math.PI * 3 + age * 0.16) * 0.16;
            Vec3 point = destination.scale(t).add(side.scale(sway * Math.sin(age * 0.08 + t * 2)));
            double width = 0.065 + Math.sin(t * Math.PI) * 0.075;
            PrayerVfxGeometry.tube(m, out, previous, point, width * 2.2,
                    0.22f, 0.025f, 0.13f, 0.19f * fade * (float) connect);
            PrayerVfxGeometry.tube(m, out, previous, point, width,
                    0.58f, 0.055f, 0.19f, 0.68f * fade * (float) connect);
            PrayerVfxGeometry.tube(m, out, previous, point, width * 0.28,
                    1.0f, 0.22f, 0.28f, 0.86f * fade * (float) connect);
            previous = point;
        }
        for (int mote = 0; mote < 9; mote++) {
            double t = (mote / 9.0 + age * 0.075) % 1.0;
            Vec3 point = destination.scale(1 - t).add(side.scale(Math.sin(t * Math.PI * 5 + age * 0.12) * 0.13));
            double size = 0.055 + 0.02 * Math.sin(age * 0.3 + mote);
            PrayerVfxGeometry.ellipsoid(m, out, point, new Vec3(size, size * 1.25, size), 6, 9,
                    1.0f, 0.34f, 0.28f, fade * (float) connect, age * 0.22 + mote);
            PrayerVfxGeometry.wireSphere(m, out, point, size * 1.7, 7, -age * 0.18 + mote,
                    0.72f, 0.09f, 0.3f, 0.62f * fade * (float) connect, 0.009);
        }
        PrayerVfxGeometry.wireSphere(m, out, Vec3.ZERO, 0.2 + connect * 0.18, 10, age * 0.2,
                0.83f, 0.15f, 0.3f, 0.72f * fade, 0.015);
        PrayerVfxGeometry.ellipsoid(m, out, destination, new Vec3(0.36, 0.55, 0.36), 8, 12,
                0.47f, 0.07f, 0.19f, 0.14f * fade * (float) connect, age * 0.12);
    }
}
