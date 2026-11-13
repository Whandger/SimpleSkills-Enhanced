package com.github.ob_yekt.simpleskills.prayer.divine;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerSanctuaryVfx {
    private PrayerSanctuaryVfx() {}

    // 6. SANCTUARY — a fixed sacred territory with eight standing light-pillars and a central altar.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 10, 12);
        double expansion = PrayerVfxPrimitives.smooth(0, 12, age);
        double radius = 3.15 * expansion;
        PrayerVfxPrimitives.circleSigil(m, out, new Vec3(0, 0.04, 0), radius, age * 0.008, fade,
                0.96f, 0.78f, 0.27f);
        PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, 0.07, 0), new Vec3(1, 0.08, 0), radius * 0.72,
                0.025, -age * 0.01, 1.0f, 0.9f, 0.53f, 0.82f * fade);
        for (int pillar = 0; pillar < 8; pillar++) {
            double angle = pillar * Math.PI / 4 + age * 0.003;
            Vec3 base = new Vec3(Math.cos(angle) * radius * 0.82, 0.15, Math.sin(angle) * radius * 0.82);
            double height = (0.55 + PrayerVfxPrimitives.smooth(pillar * 1.3, pillar * 1.3 + 6, age) * 1.65)
                    * (0.92 + 0.08 * Math.sin(age * 0.12 + pillar));
            Vec3 top = base.add(0, height, 0);
            PrayerVfxGeometry.taperedTube(m, out, base, top, 0.11, 0.025,
                    0.91f, 0.75f, 0.27f, 0.6f * fade);
            PrayerVfxGeometry.ellipsoid(m, out, top, new Vec3(0.12, 0.16, 0.12), 6, 8,
                    1.0f, 0.94f, 0.66f, 0.84f * fade, age * 0.1 + pillar);
            PrayerVfxPrimitives.ringPlane(m, out, top, PrayerVfxPrimitives.UP, 0.18, 0.012, age * 0.035 + pillar,
                    1.0f, 0.84f, 0.37f, 0.8f * fade);
        }
        // A stepped altar gives the centre a landmark distinct from the boundary circle.
        for (int tier = 0; tier < 3; tier++) {
            double r = 0.68 - tier * 0.16;
            double y = 0.14 + tier * 0.29;
            PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, y, 0), PrayerVfxPrimitives.UP, r, 0.035,
                    age * (tier % 2 == 0 ? 0.025 : -0.03), 0.95f, 0.78f, 0.29f, 0.85f * fade);
        }
        PrayerVfxGeometry.ellipsoid(m, out, new Vec3(0, 0.92, 0), new Vec3(0.25, 0.42, 0.25),
                8, 12, 1.0f, 0.91f, 0.56f, 0.58f * fade, age * 0.035);
    }
}
