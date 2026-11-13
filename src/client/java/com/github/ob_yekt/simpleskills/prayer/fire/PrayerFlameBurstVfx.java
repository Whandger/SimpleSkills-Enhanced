package com.github.ob_yekt.simpleskills.prayer.fire;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerFlameBurstVfx {
    private PrayerFlameBurstVfx() {}

    // 27. FLAME_BURST — a compressed ember opens as an asymmetric three-dimensional fire flower.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 2, 6);
        double open = PrayerVfxPrimitives.smooth(4, duration * 0.52, age);
        Vec3 center = new Vec3(0, 1.0, 0);
        double core = 0.14 + (1 - open) * 0.18;
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(core, core, core), 8, 12,
                1.0f, 0.56f, 0.08f, 0.95f * fade, age * 0.2);
        for (int petal = 0; petal < 12; petal++) {
            double angle = petal * Math.PI / 6 + age * 0.012;
            Vec3 dir = new Vec3(Math.cos(angle), (petal % 3 - 1) * 0.34 + 0.15, Math.sin(angle)).normalize();
            Vec3 start = center.add(dir.scale(0.08));
            Vec3 knee = center.add(dir.scale(0.5 + open * (0.5 + (petal % 3) * 0.22)))
                    .add(0, Math.sin(open * Math.PI) * (0.22 + petal % 2 * 0.16), 0);
            Vec3 tip = center.add(dir.scale(0.72 + open * (1.0 + (petal % 4) * 0.25)));
            PrayerVfxPrimitives.flamePetal(m, out, start, knee, tip, 0.2 - (petal % 3) * 0.025,
                    0.86f * fade * (float) (1 - open * 0.2));
        }
        PrayerVfxPrimitives.ringPlane(m, out, center, PrayerVfxPrimitives.UP, 0.24 + open * 1.5, 0.035, age * 0.045,
                1.0f, 0.37f, 0.045f, 0.74f * fade * (float) (1 - open * 0.4));
    }
}
