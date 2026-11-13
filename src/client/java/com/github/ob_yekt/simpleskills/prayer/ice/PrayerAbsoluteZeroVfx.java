package com.github.ob_yekt.simpleskills.prayer.ice;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerAbsoluteZeroVfx {
    private PrayerAbsoluteZeroVfx() {}

    // 35. ABSOLUTE_ZERO — a near-motionless monumental crystal contracts the surrounding space.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 10, 14);
        double charge = PrayerVfxPrimitives.smooth(0, 18, age);
        double freeze = PrayerVfxPrimitives.smooth(0.42, 0.65, age / duration);
        Vec3 core = new Vec3(0, 3.4, 0);
        PrayerVfxPrimitives.crystal(m, out, core, PrayerVfxPrimitives.UP, 3.5, 1.05, 0.43f, 0.78f, 1.0f, 0.32f * fade);
        PrayerVfxPrimitives.crystal(m, out, core, PrayerVfxPrimitives.UP, 2.65, 0.58, 0.8f, 0.96f, 1.0f, 0.42f * fade);
        PrayerVfxPrimitives.crystal(m, out, core, PrayerVfxPrimitives.UP, 1.62, 0.24, 0.96f, 1.0f, 1.0f, 0.88f * fade);
        PrayerVfxGeometry.ellipsoid(m, out, core, new Vec3(0.32, 0.8, 0.32), 9, 14,
                0.8f, 0.96f, 1.0f, 0.46f * fade, age * 0.012);
        for (int orbit = 0; orbit < 3; orbit++) {
            Vec3 normal = orbit == 0 ? PrayerVfxPrimitives.UP : orbit == 1 ? new Vec3(1, 0.15, 0) : new Vec3(0.2, 0, 1);
            PrayerVfxPrimitives.ringPlane(m, out, core, normal, 1.5 + orbit * 0.62, 0.027,
                    age * (orbit % 2 == 0 ? 0.008 : -0.01), 0.52f, 0.84f, 1.0f, 0.74f * fade);
        }
        for (int shard = 0; shard < 14; shard++) {
            double angle = shard * 2.399963 + age * (shard % 2 == 0 ? 0.004 : -0.006);
            double radius = 1.8 + (shard % 4) * 0.52;
            Vec3 point = core.add(Math.cos(angle) * radius, Math.sin(angle * 1.6 + age * 0.008) * 1.35,
                    Math.sin(angle) * radius);
            Vec3 axis = PrayerVfxPrimitives.safeNormalize(point.subtract(core), PrayerVfxPrimitives.UP);
            PrayerVfxPrimitives.crystal(m, out, point, axis, 0.68 + (shard % 3) * 0.18, 0.14,
                    0.55f, 0.87f, 1.0f, 0.78f * fade);
        }
        double shell = 0.4 + freeze * 5.0;
        PrayerVfxGeometry.wireSphere(m, out, new Vec3(0, 1.8, 0), shell, 24, age * 0.004,
                0.5f, 0.85f, 1.0f, (float) (0.25 * (1 - freeze * 0.6) * fade), 0.018);
        PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, 0.07, 0), PrayerVfxPrimitives.UP, 0.2 + freeze * 4.5,
                0.035, age * 0.004, 0.67f, 0.91f, 1.0f, (float) (0.58 * (1 - freeze * 0.55) * fade));
    }
}
