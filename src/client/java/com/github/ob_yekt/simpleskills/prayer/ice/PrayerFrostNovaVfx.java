package com.github.ob_yekt.simpleskills.prayer.ice;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerFrostNovaVfx {
    private PrayerFrostNovaVfx() {}

    // 32. FROST_NOVA — crystalline growth erupts at the caster as a low expanding ice wall.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 3, 8);
        double wave = PrayerVfxPrimitives.smooth(4, duration * 0.64, age);
        Vec3 center = new Vec3(0, 0.72, 0);
        PrayerVfxGeometry.wireSphere(m, out, center, 0.45 + wave * 2.2, 16, age * 0.025,
                0.27f, 0.77f, 1.0f, 0.32f * fade * (float) (1 - wave * 0.35), 0.018);
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(0.55, 0.65, 0.55), 8, 12,
                0.55f, 0.88f, 1.0f, 0.13f * fade, age * 0.04);
        PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, 0.08, 0), PrayerVfxPrimitives.UP, 0.25 + wave * 4.0, 0.07,
                age * 0.025, 0.46f, 0.84f, 1.0f, 0.8f * fade * (float) (1 - wave * 0.65));
        for (int shard = 0; shard < 16; shard++) {
            double angle = shard * Math.PI / 8 + age * 0.006;
            double radius = 0.55 + wave * (1.65 + (shard % 4) * 0.36);
            double height = (0.34 + (shard % 4) * 0.2) * PrayerVfxPrimitives.smooth(0.08, 0.42, wave);
            Vec3 point = new Vec3(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
            Vec3 axis = new Vec3(Math.cos(angle) * 0.45, 1.0, Math.sin(angle) * 0.45);
            PrayerVfxPrimitives.crystal(m, out, point, axis, 0.9 + (shard % 3) * 0.22, 0.17,
                    0.33f, 0.73f, 1.0f, 0.84f * fade * (float) (1 - wave * 0.25));
        }
    }
}
