package com.github.ob_yekt.simpleskills.prayer.unholy;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerSoulHarvestVfx {
    private PrayerSoulHarvestVfx() {}

    // 16. SOUL_HARVEST — separate spectral figures spiral inward and dissolve into the caster.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 4, 8);
        PrayerVfxPrimitives.circleSigil(m, out, new Vec3(0, 0.06, 0), 2.3, age * -0.012, 0.52f * fade,
                0.55f, 0.13f, 0.76f);
        for (int soul = 0; soul < 7; soul++) {
            double phase = soul * Math.PI * 2 / 7 + age * (soul % 2 == 0 ? 0.045 : -0.052);
            double progress = (age / 44.0 + soul * 0.137) % 1.0;
            double radius = 2.25 * (1 - progress) + 0.18;
            double y = 0.35 + (soul % 4) * 0.52 + progress * 1.3;
            Vec3 center = new Vec3(Math.cos(phase) * radius, y, Math.sin(phase) * radius);
            double scale = 0.16 + (1 - progress) * 0.09;
            PrayerVfxPrimitives.soulFigure(m, out, center, scale, phase, fade * (float) (1 - progress * 0.35));
            Vec3 mote = center.add(0, -0.18 * scale, 0);
            PrayerVfxGeometry.taperedTube(m, out, center, mote, 0.08 * scale, 0.002,
                    0.47f, 0.14f, 0.8f, 0.65f * fade);
            PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(0.08, 0.12, 0.08), 6, 8,
                    0.75f, 0.28f, 0.96f, 0.36f * fade, phase);
        }
        PrayerVfxGeometry.wireSphere(m, out, new Vec3(0, 1, 0), 0.45 + Math.sin(age * 0.12) * 0.06,
                12, age * 0.06, 0.68f, 0.2f, 0.86f, 0.34f * fade, 0.014);
    }
}
