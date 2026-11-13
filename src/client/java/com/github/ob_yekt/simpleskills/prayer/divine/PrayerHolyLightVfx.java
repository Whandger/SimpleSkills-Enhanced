package com.github.ob_yekt.simpleskills.prayer.divine;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerHolyLightVfx {
    private PrayerHolyLightVfx() {}

    // 9. HOLY_LIGHT — calm column, suspended script and slow orbiting crown rings.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 10, 12);
        double height = 5.2 * PrayerVfxPrimitives.smooth(0, 13, age);
        double pulse = 1 + Math.sin(age * 0.12) * 0.035;
        PrayerVfxPrimitives.circleSigil(m, out, new Vec3(0, 0.06, 0), 1.7 * pulse, age * 0.009, fade,
                0.94f, 0.85f, 0.49f);
        PrayerVfxGeometry.taperedTube(m, out, new Vec3(0, 0.12, 0), new Vec3(0, height, 0),
                0.72, 0.32, 1.0f, 0.82f, 0.36f, 0.12f * fade);
        PrayerVfxGeometry.taperedTube(m, out, new Vec3(0, 0.12, 0), new Vec3(0, height, 0),
                0.28, 0.12, 1.0f, 0.94f, 0.68f, 0.25f * fade);
        PrayerVfxGeometry.taperedTube(m, out, new Vec3(0, 0.12, 0), new Vec3(0, height, 0),
                0.09, 0.045, 1.0f, 1.0f, 0.93f, 0.6f * fade);
        for (int ring = 0; ring < 4; ring++) {
            double y = 0.5 + ring * 1.22 + Math.sin(age * 0.08 + ring) * 0.12;
            PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, y, 0), PrayerVfxPrimitives.UP, 0.45 + ring * 0.13,
                    0.022, age * (ring % 2 == 0 ? 0.02 : -0.025),
                    0.98f, 0.87f, 0.56f, 0.82f * fade);
            PrayerVfxPrimitives.runeHalo(m, out, new Vec3(0, y + 0.26, 0), 0.24, age * 0.018 + ring,
                    0.78f * fade, 1.0f, 0.92f, 0.68f);
        }
        for (int ray = 0; ray < 8; ray++) {
            double angle = ray * Math.PI / 4 + age * 0.012;
            Vec3 base = new Vec3(Math.cos(angle) * 0.68, 0.2, Math.sin(angle) * 0.68);
            Vec3 top = base.add(0, height * (0.45 + (ray % 3) * 0.12), 0);
            PrayerVfxGeometry.taperedTube(m, out, base, top, 0.022, 0.008,
                    0.95f, 0.84f, 0.5f, 0.62f * fade);
        }
    }
}
