package com.github.ob_yekt.simpleskills.prayer.divine;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerDivineSmiteVfx {
    private PrayerDivineSmiteVfx() {}

    // 7. DIVINE_SMITE — a suspended seal compresses into a focused vertical sentence of light.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 3, 7);
        double descend = PrayerVfxPrimitives.smooth(5, Math.min(duration - 4, 18), age);
        double charge = PrayerVfxPrimitives.smooth(0, 8, age);
        double altitude = 12 * (1 - descend);
        Vec3 crown = new Vec3(0, 12, 0);
        PrayerVfxPrimitives.ringPlane(m, out, crown, PrayerVfxPrimitives.UP, 1.55 * (0.8 + charge * 0.2), 0.028,
                age * 0.035, 1.0f, 0.77f, 0.24f, 0.92f * fade);
        PrayerVfxPrimitives.ringPlane(m, out, crown, new Vec3(1, 0.15, 0), 1.08, 0.018,
                -age * 0.06, 1.0f, 0.94f, 0.62f, 0.9f * fade);
        PrayerVfxPrimitives.runeHalo(m, out, crown, 0.82, age * 0.03, fade, 1.0f, 0.9f, 0.48f);
        Vec3 beamTop = new Vec3(0, Math.max(altitude, 0.45), 0);
        double beamWidth = 0.24 + 0.16 * Math.sin(age * 0.6);
        PrayerVfxGeometry.taperedTube(m, out, beamTop, new Vec3(0, 0.12, 0), beamWidth,
                0.035, 1.0f, 0.75f, 0.25f, 0.42f * fade);
        PrayerVfxGeometry.taperedTube(m, out, beamTop, new Vec3(0, 0.12, 0), beamWidth * 0.48,
                0.018, 1.0f, 0.95f, 0.7f, 0.92f * fade);
        for (int lance = 0; lance < 8; lance++) {
            double angle = lance * Math.PI / 4 + age * 0.025;
            Vec3 start = new Vec3(Math.cos(angle) * 0.4, altitude + 0.35, Math.sin(angle) * 0.4);
            Vec3 end = new Vec3(Math.cos(angle) * 1.45, 0.08, Math.sin(angle) * 1.45);
            PrayerVfxGeometry.taperedTube(m, out, start, end, 0.075, 0.004,
                    1.0f, 0.88f, 0.48f, 0.7f * fade);
        }
        double impact = PrayerVfxPrimitives.smooth(0.7, 0.92, age / duration);
        PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, 0.07, 0), PrayerVfxPrimitives.UP, 0.25 + impact * 2.0, 0.035,
                age * 0.08, 1.0f, 0.88f, 0.46f, (float) (0.8 * (1 - impact) * fade));
        if (impact > 0) PrayerVfxPrimitives.starburst(m, out, new Vec3(0, 0.1, 0), PrayerVfxPrimitives.UP, 1.8 * impact,
                (float) (0.65 * (1 - impact) * fade), 1.0f, 0.92f, 0.59f);
    }
}
