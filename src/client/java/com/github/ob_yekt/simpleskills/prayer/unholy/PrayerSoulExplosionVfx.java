package com.github.ob_yekt.simpleskills.prayer.unholy;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerSoulExplosionVfx {
    private PrayerSoulExplosionVfx() {}

    // 18. SOUL_EXPLOSION — a sealed black core releases orbiting souls as independent radial fragments.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 3, 9);
        double charge = 1 - PrayerVfxPrimitives.smooth(0.28, 0.48, age / duration);
        double burst = PrayerVfxPrimitives.smooth(0.34, 0.72, age / duration);
        Vec3 center = new Vec3(0, 1.0, 0);
        double radius = 0.18 + charge * 0.55;
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(radius, radius, radius), 9, 14,
                0.04f, 0.018f, 0.07f, 0.95f * fade, age * 0.1);
        PrayerVfxGeometry.wireSphere(m, out, center, radius * 1.34, 14, age * 0.07,
                0.64f, 0.12f, 0.88f, 0.78f * fade, 0.018);
        for (int soul = 0; soul < 12; soul++) {
            double angle = soul * Math.PI * 2 / 12 + age * 0.055;
            Vec3 dir = new Vec3(Math.cos(angle), Math.sin(angle * 1.7) * 0.65, Math.sin(angle)).normalize();
            double orbit = 0.72 + (soul % 3) * 0.14;
            double expelled = burst * (1.25 + (soul % 4) * 0.42);
            Vec3 point = center.add(dir.scale(orbit + expelled));
            PrayerVfxPrimitives.soulFigure(m, out, point, 0.13 + (soul % 3) * 0.018, angle + age * 0.08,
                    fade * (float) (0.9 - burst * 0.25));
            PrayerVfxGeometry.taperedTube(m, out, center, point, 0.035, 0.002,
                    0.57f, 0.11f, 0.78f, 0.42f * fade * (float) burst);
        }
        for (int ring = 0; ring < 3; ring++) {
            double r = 0.2 + burst * (1.3 + ring * 0.9);
            PrayerVfxPrimitives.ringPlane(m, out, center, ring == 0 ? PrayerVfxPrimitives.UP : ring == 1 ? new Vec3(1, 0.3, 0) : new Vec3(0.2, 0, 1),
                    r, 0.025, age * (ring % 2 == 0 ? 0.04 : -0.05), 0.72f, 0.17f, 0.91f,
                    (float) (0.62 * (1 - burst * 0.45) * fade));
        }
    }
}
