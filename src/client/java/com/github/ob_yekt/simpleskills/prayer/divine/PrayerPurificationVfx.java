package com.github.ob_yekt.simpleskills.prayer.divine;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerPurificationVfx {
    private PrayerPurificationVfx() {}

    // 4. PURIFICATION — a scanning halo strips dark fragments from the body into a white-gold seal.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 5, 8);
        double rise = PrayerVfxPrimitives.smooth(3, duration * 0.72, age) * 2.25;
        double scan = 0.10 + PrayerVfxPrimitives.smooth(2, duration * 0.68, age) * 2.05;
        PrayerVfxPrimitives.circleSigil(m, out, Vec3.ZERO, 1.65, age * 0.018, fade, 1.0f, 0.82f, 0.32f);
        PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, scan, 0), PrayerVfxPrimitives.UP, 1.05 + Math.sin(age * 0.3) * 0.08,
                0.025, age * 0.08, 0.96f, 0.97f, 0.79f, fade);
        PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, scan, 0), new Vec3(1, 0, 0), 0.82,
                0.012, -age * 0.12, 1.0f, 0.74f, 0.25f, 0.78f * fade);
        PrayerVfxPrimitives.runeHalo(m, out, new Vec3(0, 2.55, 0), 0.62, age * 0.035, fade,
                0.98f, 0.87f, 0.45f);
        for (int mote = 0; mote < 10; mote++) {
            double angle = mote * Math.PI * 2 / 10 + age * 0.06;
            double collapse = PrayerVfxPrimitives.smooth(duration * 0.15, duration * 0.6, age);
            double radius = 1.35 + (1 - collapse) * 0.8;
            double y = 0.3 + (mote % 4) * 0.48 + Math.sin(age * 0.12 + mote) * 0.12;
            Vec3 point = new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            double outward = PrayerVfxPrimitives.smooth(0, duration * 0.2, age) * 0.8;
            Vec3 dark = point.add(Math.cos(angle) * outward, 0.7 + rise * 0.4, Math.sin(angle) * outward);
            PrayerVfxPrimitives.crystal(m, out, dark, point.subtract(dark), 0.16, 0.065,
                    0.21f, 0.13f, 0.29f, (float) (0.7 * fade * (1 - collapse * 0.72)));
            Vec3 light = point.add(Math.cos(angle) * 0.28, rise * 0.25, Math.sin(angle) * 0.28);
            PrayerVfxGeometry.ellipsoid(m, out, light, new Vec3(0.035, 0.045, 0.035), 6, 8,
                    1.0f, 0.93f, 0.62f, fade, angle);
        }
        PrayerVfxGeometry.ellipsoid(m, out, new Vec3(0, 1.0 + rise * 0.25, 0),
                new Vec3(0.48, 1.05, 0.48), 8, 12, 1.0f, 0.88f, 0.48f, 0.08f * fade, age * 0.08);
    }
}
