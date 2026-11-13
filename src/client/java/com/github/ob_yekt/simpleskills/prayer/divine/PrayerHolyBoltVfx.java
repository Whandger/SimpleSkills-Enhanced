package com.github.ob_yekt.simpleskills.prayer.divine;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerHolyBoltVfx {
    private PrayerHolyBoltVfx() {}

    // 3. HOLY_BOLT — a narrow, faceted lance with a rotating tail cross and star-shaped contact flash.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 2, 4);
        double travel = PrayerVfxPrimitives.smooth(2, duration * 0.56, age);
        Vec3 direction = PrayerVfxPrimitives.safeNormalize(destination.subtract(Vec3.ZERO), new Vec3(0, 0, -1));
        Vec3 center = Vec3.ZERO.lerp(destination, travel);
        drawLance(m, out, center, direction, 1.35, 0.17, age * 0.24,
                1.0f, 0.88f, 0.33f, 0.92f * fade);
        PrayerVfxPrimitives.ringPlane(m, out, center.subtract(direction.scale(0.34)), direction, 0.22, 0.016,
                age * 0.5, 1.0f, 0.81f, 0.2f, 0.9f * fade);
        PrayerVfxPrimitives.ringPlane(m, out, center.subtract(direction.scale(0.56)), direction, 0.13, 0.012,
                -age * 0.62, 1.0f, 0.96f, 0.65f, 0.92f * fade);
        if (travel > 0.88) {
            double impact = PrayerVfxPrimitives.smooth(0.88, 1, travel);
            Vec3 point = destination;
            PrayerVfxPrimitives.starburst(m, out, point, PrayerVfxPrimitives.UP, 0.35 + impact * 0.9,
                    (float) (0.9 * (1 - impact) * fade), 1.0f, 0.85f, 0.32f);
            PrayerVfxPrimitives.ringPlane(m, out, point, direction, 0.16 + impact * 0.55, 0.021,
                    age * 0.13, 1.0f, 0.97f, 0.69f, (float) (0.82 * (1 - impact) * fade));
        }
    }

    private static void drawLance(Matrix4fc m, VertexConsumer out, Vec3 center, Vec3 direction,
                                  double length, double width, double rotation,
                                  float r, float g, float b, float alpha) {
        Vec3 axis = PrayerVfxPrimitives.safeNormalize(direction, new Vec3(0, 0, -1));
        Vec3 base = center.subtract(axis.scale(length * 0.44));
        Vec3 tip = center.add(axis.scale(length * 0.56));
        Vec3[] frame = PrayerVfxPrimitives.basis(axis);
        Vec3 up = frame[0].scale(Math.cos(rotation)).add(frame[1].scale(Math.sin(rotation)));
        Vec3 side = frame[1].scale(Math.cos(rotation)).subtract(frame[0].scale(Math.sin(rotation)));
        Vec3[] corners = new Vec3[4];
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 2;
            corners[i] = center.add(up.scale(Math.cos(angle) * width)).add(side.scale(Math.sin(angle) * width));
        }
        Vec3 rear = base.subtract(axis.scale(length * 0.18));
        for (int i = 0; i < 4; i++) {
            int next = (i + 1) % 4;
            PrayerVfxGeometry.quad(m, out, rear, corners[i], corners[next], rear,
                    r * 0.72f, g * 0.65f, b * 0.42f, alpha * 0.8f);
            PrayerVfxGeometry.triangle(m, out, corners[i], corners[next], tip,
                    r, g, b, alpha);
            PrayerVfxGeometry.tube(m, out, corners[i], tip, 0.012,
                    Math.min(1, r + 0.12f), Math.min(1, g + 0.1f), Math.min(1, b + 0.1f), alpha);
        }
        PrayerVfxGeometry.taperedTube(m, out, rear, tip, width * 0.28, 0.006,
                1.0f, 0.98f, 0.82f, alpha);
    }
}
