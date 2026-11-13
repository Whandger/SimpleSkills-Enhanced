package com.github.ob_yekt.simpleskills.prayer.divine;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerRadiantNovaVfx {
    private PrayerRadiantNovaVfx() {}

    // 8. RADIANT_NOVA — a compressed star detonates into a volumetric shell and three orthogonal waves.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 4, 8);
        double charge = 1 - PrayerVfxPrimitives.smooth(0, 12, age);
        double release = PrayerVfxPrimitives.smooth(10, duration * 0.72, age);
        Vec3 center = new Vec3(0, 1.0, 0);
        double core = 0.16 + charge * 0.36;
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(core, core, core), 10, 16,
                1.0f, 0.93f, 0.68f, 0.94f * fade, age * 0.12);
        PrayerVfxGeometry.wireSphere(m, out, center, core * 1.65, 12, age * 0.14,
                1.0f, 0.75f, 0.22f, 0.8f * fade, 0.016);
        for (int wave = 0; wave < 3; wave++) {
            double radius = 0.15 + release * (1.35 + wave * 0.72);
            Vec3 normal = wave == 0 ? PrayerVfxPrimitives.UP : wave == 1 ? new Vec3(1, 0.22, 0) : new Vec3(0.2, 0, 1);
            PrayerVfxPrimitives.ringPlane(m, out, center, normal, radius, 0.035 - wave * 0.005,
                    age * (wave % 2 == 0 ? 0.035 : -0.04), 1.0f, 0.87f, 0.43f,
                    (float) ((1 - release * 0.72) * fade));
        }
        PrayerVfxGeometry.wireSphere(m, out, center, 0.25 + release * 3.6, 20, age * 0.018,
                0.98f, 0.82f, 0.49f, (float) (0.35 * (1 - release) * fade), 0.018);
        for (int shard = 0; shard < 14; shard++) {
            double angle = shard * Math.PI * 2 / 14 + age * 0.025;
            Vec3 dir = new Vec3(Math.cos(angle), Math.sin(angle * 2.1) * 0.72, Math.sin(angle)).normalize();
            double radius = 0.38 + release * (1.3 + (shard % 4) * 0.38);
            Vec3 point = center.add(dir.scale(radius));
            PrayerVfxPrimitives.crystal(m, out, point, dir, 0.42 * (1 - release * 0.55), 0.09,
                    1.0f, 0.81f, 0.36f, (float) (0.86 * fade * (1 - release * 0.35)));
        }
    }
}
