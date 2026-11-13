package com.github.ob_yekt.simpleskills.prayer.unholy;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerDarkPactVfx {
    private PrayerDarkPactVfx() {}

    // 17. DARK_PACT — contract circle, floating glyph stones and binding chains converging on the caster.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 6, 8);
        PrayerVfxPrimitives.circleSigil(m, out, new Vec3(0, 0.045, 0), 1.9, age * -0.018, fade,
                0.41f, 0.045f, 0.2f);
        PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, 0.07, 0), PrayerVfxPrimitives.UP, 1.35, 0.026, age * 0.035,
                0.78f, 0.1f, 0.28f, 0.85f * fade);
        PrayerVfxGeometry.ellipsoid(m, out, new Vec3(0, 1.02, 0), new Vec3(0.52, 0.86, 0.52),
                9, 14, 0.035f, 0.018f, 0.055f, 0.42f * fade, age * 0.04);
        PrayerVfxGeometry.wireSphere(m, out, new Vec3(0, 1.05, 0), 0.78, 14, -age * 0.026,
                0.49f, 0.055f, 0.2f, 0.75f * fade, 0.017);
        for (int seal = 0; seal < 6; seal++) {
            double angle = seal * Math.PI / 3 - age * 0.023;
            Vec3 p = new Vec3(Math.cos(angle) * 1.42, 0.62 + (seal % 3) * 0.52, Math.sin(angle) * 1.42);
            PrayerVfxPrimitives.crystal(m, out, p, new Vec3(Math.cos(angle), 0.25, Math.sin(angle)), 0.48, 0.16,
                    0.32f, 0.035f, 0.15f, 0.9f * fade);
            PrayerVfxPrimitives.runeHalo(m, out, p, 0.27, angle, 0.92f * fade, 0.74f, 0.12f, 0.28f);
            List<Vec3> chain = PrayerVfxPrimitives.jagged(p, new Vec3(0, 1.05, 0), 5, seal * 709L + (long) age / 4, 0.1);
            for (int i = 0; i < chain.size() - 1; i++) {
                PrayerVfxGeometry.tube(m, out, chain.get(i), chain.get(i + 1), 0.032,
                        0.19f, 0.025f, 0.12f, 0.72f * fade);
            }
        }
        PrayerVfxGeometry.ellipsoid(m, out, new Vec3(0, 2.15, 0), new Vec3(0.22, 0.3, 0.22),
                7, 10, 0.71f, 0.09f, 0.24f, 0.72f * fade, age * 0.09);
    }
}
