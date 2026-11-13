package com.github.ob_yekt.simpleskills.prayer.eletro;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerStormJudgmentVfx {
    private PrayerStormJudgmentVfx() {}

    // 25. STORM_JUDGMENT — a celestial electric seal focuses into a single broad, branching verdict.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 8, 11);
        double charge = PrayerVfxPrimitives.smooth(0, 18, age);
        double strike = PrayerVfxPrimitives.smooth(0.38, 0.58, age / duration);
        Vec3 crown = new Vec3(0, 14, 0);
        PrayerVfxPrimitives.ringPlane(m, out, crown, PrayerVfxPrimitives.UP, 3.6 * (0.5 + charge * 0.5), 0.055,
                age * 0.026, 0.35f, 0.74f, 1.0f, 0.87f * fade);
        PrayerVfxPrimitives.ringPlane(m, out, crown, new Vec3(1, 0.2, 0), 2.7, 0.035,
                -age * 0.04, 0.86f, 0.94f, 1.0f, 0.92f * fade);
        PrayerVfxPrimitives.ringPlane(m, out, crown, new Vec3(0.15, 0, 1), 1.75, 0.025,
                age * 0.06, 0.5f, 0.82f, 1.0f, 0.8f * fade);
        PrayerVfxPrimitives.runeHalo(m, out, crown, 1.0, age * -0.03, fade, 0.76f, 0.91f, 1.0f);
        PrayerVfxGeometry.ellipsoid(m, out, crown, new Vec3(0.36, 0.5, 0.36), 8, 12,
                0.55f, 0.86f, 1.0f, 0.7f * fade, age * 0.12);
        Vec3 ground = new Vec3(0, 0.12, 0);
        PrayerVfxGeometry.taperedTube(m, out, new Vec3(0, 14 * strike, 0), ground,
                0.56, 0.16, 0.16f, 0.58f, 1.0f, 0.26f * fade);
        PrayerVfxGeometry.taperedTube(m, out, new Vec3(0, 14 * strike, 0), ground,
                0.22, 0.055, 0.65f, 0.91f, 1.0f, 0.9f * fade);
        for (int branch = 0; branch < 8; branch++) {
            double angle = branch * Math.PI / 4 + age * 0.016;
            Vec3 end = ground.add(Math.cos(angle) * (1.4 + branch % 3 * 0.52) * strike,
                    (branch % 3) * 0.24, Math.sin(angle) * (1.4 + branch % 3 * 0.52) * strike);
            PrayerVfxPrimitives.bolt(m, out, ground, end, branch * 449L + (long) age / 2, 7, 0.17,
                    0.9f * fade * (float) strike);
        }
        PrayerVfxPrimitives.ringPlane(m, out, ground, PrayerVfxPrimitives.UP, 0.2 + strike * 2.5, 0.04, age * 0.07,
                0.35f, 0.78f, 1.0f, 0.78f * fade * (float) (1 - strike * 0.6));
    }
}
