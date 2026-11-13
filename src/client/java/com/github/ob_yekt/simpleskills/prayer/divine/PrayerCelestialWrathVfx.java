package com.github.ob_yekt.simpleskills.prayer.divine;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerCelestialWrathVfx {
    private PrayerCelestialWrathVfx() {}

    // 10. CELESTIAL_WRATH — a sky-sized seal commands four staggered descending pillars.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 8, 12);
        double open = PrayerVfxPrimitives.smooth(0, 15, age);
        Vec3 sky = new Vec3(0, 15, 0);
        PrayerVfxPrimitives.ringPlane(m, out, sky, PrayerVfxPrimitives.UP, 4.1 * open, 0.06, age * 0.018,
                1.0f, 0.78f, 0.26f, 0.8f * fade);
        PrayerVfxPrimitives.ringPlane(m, out, sky, new Vec3(1, 0.25, 0), 3.1 * open, 0.035,
                -age * 0.03, 1.0f, 0.92f, 0.58f, 0.9f * fade);
        PrayerVfxPrimitives.ringPlane(m, out, sky, new Vec3(0.15, 0, 1), 2.1 * open, 0.028,
                age * 0.04, 0.96f, 0.84f, 0.45f, 0.8f * fade);
        PrayerVfxPrimitives.runeHalo(m, out, sky, 1.35, -age * 0.018, fade, 1.0f, 0.9f, 0.5f);
        for (int wing = 0; wing < 2; wing++) {
            double side = wing == 0 ? -1 : 1;
            for (int feather = 0; feather < 5; feather++) {
                Vec3 start = sky.add(side * (0.6 + feather * 0.42), -0.4, 0);
                Vec3 tip = sky.add(side * (2.8 + feather * 0.32), -0.9 - feather * 0.22,
                        (feather - 2) * 0.35);
                PrayerVfxGeometry.taperedTube(m, out, start, tip, 0.08, 0.006,
                        1.0f, 0.88f, 0.48f, 0.58f * fade);
            }
        }
        double strikePhase = (age - 14) / 36.0;
        for (int strike = 0; strike < 4; strike++) {
            double startAt = strike * 0.17;
            double fall = PrayerVfxPrimitives.smooth(startAt, startAt + 0.16, strikePhase);
            double retract = PrayerVfxPrimitives.smooth(startAt + 0.25, startAt + 0.42, strikePhase);
            double length = 14 * fall * (1 - retract * 0.35);
            double angle = strike * Math.PI / 2 + 0.4;
            Vec3 base = new Vec3(Math.cos(angle) * (1.0 + strike % 2), 0.1,
                    Math.sin(angle) * (1.0 + strike % 2));
            Vec3 top = base.add(0, length, 0);
            PrayerVfxGeometry.taperedTube(m, out, top, base, 0.34, 0.06,
                    0.94f, 0.76f, 0.34f, 0.24f * fade);
            PrayerVfxGeometry.taperedTube(m, out, top, base, 0.12, 0.025,
                    1.0f, 0.97f, 0.72f, 0.82f * fade);
            if (fall > 0.55) {
                double ring = 0.25 + fall * (1.1 + retract * 1.5);
                PrayerVfxPrimitives.ringPlane(m, out, base, PrayerVfxPrimitives.UP, ring, 0.026, age * 0.07,
                        1.0f, 0.83f, 0.4f, (float) (0.7 * (1 - retract) * fade));
            }
        }
    }
}
