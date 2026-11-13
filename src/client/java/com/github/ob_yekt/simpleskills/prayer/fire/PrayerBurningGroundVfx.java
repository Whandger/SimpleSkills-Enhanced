package com.github.ob_yekt.simpleskills.prayer.fire;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerBurningGroundVfx {
    private PrayerBurningGroundVfx() {}

    // 28. BURNING_GROUND — a scorched plate, branching molten fractures and separated ember chimneys.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 7, 10);
        double expansion = PrayerVfxPrimitives.smooth(0, 10, age);
        double radius = 3.1 * expansion;
        PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, 0.04, 0), PrayerVfxPrimitives.UP, radius, 0.045, age * 0.006,
                0.65f, 0.12f, 0.025f, 0.8f * fade);
        PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, 0.055, 0), PrayerVfxPrimitives.UP, radius * 0.58, 0.025, -age * 0.012,
                1.0f, 0.42f, 0.04f, 0.7f * fade);
        for (int crack = 0; crack < 12; crack++) {
            double angle = crack * Math.PI / 6 + Math.sin(age * 0.025 + crack) * 0.04;
            double reach = radius * (0.54 + (crack % 4) * 0.12);
            Vec3 start = new Vec3(Math.cos(angle) * 0.25, 0.065, Math.sin(angle) * 0.25);
            Vec3 knee = new Vec3(Math.cos(angle + 0.12) * reach * 0.52, 0.06,
                    Math.sin(angle + 0.12) * reach * 0.52);
            Vec3 end = new Vec3(Math.cos(angle - 0.08) * reach, 0.055, Math.sin(angle - 0.08) * reach);
            PrayerVfxGeometry.tube(m, out, start, knee, 0.038, 0.72f, 0.14f, 0.018f, 0.8f * fade);
            PrayerVfxGeometry.tube(m, out, start, knee, 0.012, 1.0f, 0.79f, 0.18f, fade);
            PrayerVfxGeometry.tube(m, out, knee, end, 0.027, 0.95f, 0.24f, 0.025f, 0.76f * fade);
        }
        for (int vent = 0; vent < 8; vent++) {
            double angle = vent * Math.PI / 4 + age * 0.004;
            Vec3 base = new Vec3(Math.cos(angle) * radius * 0.68, 0.12, Math.sin(angle) * radius * 0.68);
            Vec3 tip = base.add(Math.cos(angle) * 0.1, 0.55 + (vent % 3) * 0.24, Math.sin(angle) * 0.1);
            PrayerVfxPrimitives.flamePetal(m, out, base, base.add(0, tip.y * 0.55, 0), tip, 0.14,
                    0.82f * fade);
        }
    }
}
