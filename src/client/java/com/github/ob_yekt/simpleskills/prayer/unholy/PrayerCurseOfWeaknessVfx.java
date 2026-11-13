package com.github.ob_yekt.simpleskills.prayer.unholy;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerCurseOfWeaknessVfx {
    private PrayerCurseOfWeaknessVfx() {}

    // 13. CURSE_OF_WEAKNESS — a closing hex seals above the victim as two spectral chains cinch inward.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 3, 7);
        double travel = PrayerVfxPrimitives.smooth(2, duration * 0.32, age);
        Vec3 center = Vec3.ZERO.lerp(destination, travel);
        double settle = PrayerVfxPrimitives.smooth(duration * 0.25, duration * 0.43, age);
        PrayerVfxPrimitives.ringPlane(m, out, center.add(0, 0.08, 0), PrayerVfxPrimitives.UP, 0.38 + settle * 0.72,
                0.023, age * 0.06, 0.42f, 0.08f, 0.48f, fade);
        PrayerVfxPrimitives.runeHalo(m, out, center.add(0, 2.55, 0), 0.72 * (0.65 + settle * 0.35),
                -age * 0.035, fade, 0.68f, 0.18f, 0.74f);
        for (int chain = 0; chain < 2; chain++) {
            double phase = age * 0.14 + chain * Math.PI;
            Vec3 previous = center.add(Math.cos(phase) * 0.63, 0.18, Math.sin(phase) * 0.63);
            for (int step = 1; step <= 30; step++) {
                double t = step / 30.0;
                double y = 0.18 + t * 2.05;
                double angle = phase + t * Math.PI * (3.2 + chain * 0.4);
                Vec3 point = center.add(Math.cos(angle) * (0.62 - 0.12 * settle), y,
                        Math.sin(angle) * (0.62 - 0.12 * settle));
                PrayerVfxGeometry.tube(m, out, previous, point, 0.045,
                        0.17f, 0.035f, 0.25f, 0.72f * fade);
                if (step % 5 == 0) PrayerVfxPrimitives.crystal(m, out, point, point.subtract(previous), 0.15, 0.045,
                        0.65f, 0.17f, 0.76f, 0.82f * fade);
                previous = point;
            }
        }
        for (int rune = 0; rune < 6; rune++) {
            double angle = rune * Math.PI / 3 - age * 0.04;
            Vec3 point = center.add(Math.cos(angle) * 0.78, 0.6 + (rune % 3) * 0.58,
                    Math.sin(angle) * 0.78);
            PrayerVfxPrimitives.runeHalo(m, out, point, 0.18, angle, 0.75f * fade, 0.72f, 0.24f, 0.84f);
        }
    }
}
