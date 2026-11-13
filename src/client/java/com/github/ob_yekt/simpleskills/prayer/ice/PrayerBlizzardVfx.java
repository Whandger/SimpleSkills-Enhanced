package com.github.ob_yekt.simpleskills.prayer.ice;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerBlizzardVfx {
    private PrayerBlizzardVfx() {}

    // 34. BLIZZARD — a low blue storm ceiling drives a spiral of suspended faceted ice.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 9, 12);
        double build = PrayerVfxPrimitives.smooth(0, 14, age);
        for (int lobe = 0; lobe < 9; lobe++) {
            double angle = lobe * Math.PI * 2 / 9;
            Vec3 center = new Vec3(Math.cos(angle) * (1.4 + lobe % 3 * 0.32),
                    4.8 + Math.sin(age * 0.06 + lobe) * 0.28,
                    Math.sin(angle) * (1.1 + lobe % 2 * 0.38));
            double size = (0.7 + (lobe % 3) * 0.14) * build;
            PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(size, size * 0.55, size * 0.74),
                    7, 12, 0.31f, 0.57f, 0.82f, 0.56f * fade, angle);
        }
        PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, 4.0, 0), PrayerVfxPrimitives.UP, 3.1, 0.04, age * 0.035,
                0.43f, 0.78f, 1.0f, 0.6f * fade);
        for (int crystalIndex = 0; crystalIndex < 18; crystalIndex++) {
            double angle = crystalIndex * 2.399963 + age * (crystalIndex % 2 == 0 ? 0.012 : -0.016);
            double radius = 0.45 + (crystalIndex % 5) * 0.47;
            double y = 1.2 + (crystalIndex % 6) * 0.72 + Math.sin(age * 0.1 + crystalIndex) * 0.2;
            Vec3 point = new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            Vec3 axis = new Vec3(Math.cos(angle) * 0.3, -1.0, Math.sin(angle) * 0.3);
            PrayerVfxPrimitives.crystal(m, out, point, axis, 0.46 + (crystalIndex % 3) * 0.16, 0.11,
                    0.45f, 0.79f, 1.0f, 0.82f * fade);
        }
        for (int wind = 0; wind < 3; wind++) {
            double y = 1.0 + wind * 1.2;
            PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, y, 0), new Vec3(0.2, 1, 0.1),
                    1.1 + wind * 0.68, 0.022, age * (wind % 2 == 0 ? 0.045 : -0.052),
                    0.37f, 0.71f, 0.98f, 0.62f * fade);
        }
    }
}
