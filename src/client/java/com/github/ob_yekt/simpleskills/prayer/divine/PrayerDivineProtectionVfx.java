package com.github.ob_yekt.simpleskills.prayer.divine;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerDivineProtectionVfx {
    private PrayerDivineProtectionVfx() {}

    // 5. DIVINE_PROTECTION — a segmented celestial shell, not a simple outline.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 7, 10);
        double build = PrayerVfxPrimitives.smooth(0, 12, age);
        Vec3 center = new Vec3(0, 1.0, 0);
        double pulse = 1 + Math.sin(age * 0.15) * 0.025;
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(1.02 * pulse, 1.38 * pulse, 0.78 * pulse),
                10, 18, 0.78f, 0.88f, 1.0f, 0.075f * fade, age * 0.02);
        PrayerVfxGeometry.wireSphere(m, out, center, 1.38 * pulse, 20, age * 0.018,
                1.0f, 0.79f, 0.26f, 0.52f * fade, 0.018);
        PrayerVfxPrimitives.ringPlane(m, out, center, PrayerVfxPrimitives.UP, 1.12 * pulse, 0.024, age * 0.025,
                1.0f, 0.87f, 0.43f, 0.88f * fade);
        PrayerVfxPrimitives.ringPlane(m, out, center, new Vec3(1, 0.1, 0), 1.08 * pulse, 0.016,
                -age * 0.03, 0.88f, 0.96f, 1.0f, 0.72f * fade);
        PrayerVfxPrimitives.ringPlane(m, out, center, new Vec3(0.2, 0, 1), 1.08 * pulse, 0.016,
                age * 0.028, 0.88f, 0.96f, 1.0f, 0.72f * fade);
        for (int panel = 0; panel < 12; panel++) {
            double longitude = panel * Math.PI * 2 / 12 + age * 0.008;
            double latitude = (panel % 3 - 1) * 0.56;
            Vec3 radial = new Vec3(Math.cos(longitude) * Math.cos(latitude), Math.sin(latitude),
                    Math.sin(longitude) * Math.cos(latitude));
            Vec3 point = center.add(radial.scale(1.25 * build));
            Vec3 tangent = new Vec3(-Math.sin(longitude), 0.3, Math.cos(longitude)).normalize();
            PrayerVfxPrimitives.crystal(m, out, point, radial, 0.55, 0.22,
                    0.95f, 0.86f, 0.58f, 0.36f * fade);
            PrayerVfxGeometry.tube(m, out, point.subtract(tangent.scale(0.22)),
                    point.add(tangent.scale(0.22)), 0.016, 1.0f, 0.94f, 0.7f, 0.9f * fade);
        }
        for (int rune = 0; rune < 6; rune++) {
            double angle = rune * Math.PI / 3 - age * 0.025;
            Vec3 p = center.add(Math.cos(angle) * 1.41, Math.sin(age * 0.08 + rune) * 0.12,
                    Math.sin(angle) * 1.05);
            PrayerVfxPrimitives.runeHalo(m, out, p, 0.18, angle, fade, 1.0f, 0.92f, 0.63f);
        }
    }
}
