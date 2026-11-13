package com.github.ob_yekt.simpleskills.prayer.ice;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerIcePrisonVfx {
    private PrayerIcePrisonVfx() {}

    // 33. ICE_PRISON — six large facets close around the chosen victim from different angles.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 4, 7);
        double build = PrayerVfxPrimitives.smooth(2, duration * 0.46, age);
        PrayerVfxPrimitives.ringPlane(m, out, destination.add(0, 0.08, 0), PrayerVfxPrimitives.UP, 0.68 + build * 0.36,
                0.035, age * 0.012, 0.42f, 0.81f, 1.0f, 0.86f * fade);
        for (int stake = 0; stake < 8; stake++) {
            double angle = stake * Math.PI / 4 + 0.1;
            double radius = 0.68 + (stake % 2) * 0.18;
            Vec3 base = destination.add(Math.cos(angle) * radius, 0.12, Math.sin(angle) * radius);
            double height = (2.4 + (stake % 3) * 0.48) * build;
            Vec3 axis = new Vec3(Math.cos(angle) * 0.25, 1.0, Math.sin(angle) * 0.25);
            Vec3 center = base.add(0, height * 0.48, 0);
            PrayerVfxPrimitives.crystal(m, out, center, axis, height, 0.31 - (stake % 3) * 0.035,
                    0.36f, 0.75f, 1.0f, 0.78f * fade);
            PrayerVfxPrimitives.crystal(m, out, center, axis, height * 0.68, 0.13,
                    0.88f, 0.98f, 1.0f, 0.85f * fade);
        }
        PrayerVfxPrimitives.ringPlane(m, out, destination.add(0, 1.15, 0), new Vec3(1, 0.1, 0),
                0.86, 0.023, -age * 0.028, 0.51f, 0.87f, 1.0f, 0.75f * fade);
        PrayerVfxGeometry.ellipsoid(m, out, destination.add(0, 1.0, 0), new Vec3(0.4, 0.85, 0.4),
                8, 12, 0.38f, 0.75f, 1.0f, 0.08f * fade * (float) build, age * 0.03);
    }
}
