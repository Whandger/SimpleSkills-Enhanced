package com.github.ob_yekt.simpleskills.prayer.unholy;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerUnholyGroundVfx {
    private PrayerUnholyGroundVfx() {}

    // 19. UNHOLY_GROUND — the floor fractures into a dark sigil with uneven erupting thorns.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 7, 11);
        double growth = PrayerVfxPrimitives.smooth(0, 11, age);
        double radius = 3.25 * growth;
        PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, 0.055, 0), PrayerVfxPrimitives.UP, radius, 0.04, age * -0.013,
                0.38f, 0.035f, 0.18f, 0.88f * fade);
        PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, 0.07, 0), PrayerVfxPrimitives.UP, radius * 0.64, 0.018, age * 0.025,
                0.61f, 0.08f, 0.5f, 0.75f * fade);
        for (int crack = 0; crack < 14; crack++) {
            double angle = crack * Math.PI * 2 / 14 + age * 0.006;
            double reach = radius * (0.55 + (crack % 4) * 0.12);
            Vec3 from = new Vec3(Math.cos(angle) * 0.3, 0.075, Math.sin(angle) * 0.3);
            Vec3 knee = new Vec3(Math.cos(angle + 0.12) * reach * 0.55, 0.07,
                    Math.sin(angle + 0.12) * reach * 0.55);
            Vec3 end = new Vec3(Math.cos(angle - 0.08) * reach, 0.06,
                    Math.sin(angle - 0.08) * reach);
            PrayerVfxGeometry.tube(m, out, from, knee, 0.026, 0.12f, 0.025f, 0.14f, 0.84f * fade);
            PrayerVfxGeometry.tube(m, out, knee, end, 0.018, 0.59f, 0.06f, 0.47f, 0.82f * fade);
            if (crack % 2 == 0) {
                Vec3 thorn = knee.add(0, 0.3 + (crack % 3) * 0.2, 0);
                PrayerVfxPrimitives.crystal(m, out, thorn, thorn.subtract(knee), 0.58, 0.19,
                        0.19f, 0.025f, 0.2f, 0.82f * fade);
            }
        }
        for (int mote = 0; mote < 9; mote++) {
            double angle = mote * Math.PI * 2 / 9 + age * 0.014;
            double r = radius * (0.35 + (mote % 4) * 0.13);
            Vec3 point = new Vec3(Math.cos(angle) * r, 0.22 + Math.sin(age * 0.1 + mote) * 0.12,
                    Math.sin(angle) * r);
            PrayerVfxGeometry.ellipsoid(m, out, point, new Vec3(0.08, 0.15, 0.08), 6, 8,
                    0.58f, 0.08f, 0.48f, 0.78f * fade, angle);
        }
    }
}
