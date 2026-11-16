package com.github.ob_yekt.simpleskills.prayer.unholy;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerApocalypseVfx {
    private PrayerApocalypseVfx() {}

    // 20. APOCALYPSE — a huge torn sky-rift sheds black monoliths and sends red fractures to the ground.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 10, 12);
        double open = PrayerVfxPrimitives.smooth(0, 18, age);
        double close = PrayerVfxPrimitives.smooth(duration * 0.76, duration, age);
        double half = 4.8 * open * (1 - close * 0.35);
        double y = 12.0;
        List<Vec3> left = PrayerVfxPrimitives.jagged(new Vec3(-half, y, -0.2), new Vec3(0, y + 0.7, 0.18), 11,
                0xA90C1L + (long) age / 5, 0.42);
        List<Vec3> right = PrayerVfxPrimitives.jagged(new Vec3(half, y, 0.2), new Vec3(0, y + 0.7, -0.18), 11,
                0xA90C2L + (long) age / 5, 0.42);
        for (int i = 0; i < left.size() - 1; i++) {
            PrayerVfxGeometry.tube(m, out, left.get(i), left.get(i + 1), 0.14,
                    0.2f, 0.015f, 0.045f, 0.74f * fade);
            PrayerVfxGeometry.tube(m, out, left.get(i), left.get(i + 1), 0.043,
                    0.94f, 0.12f, 0.18f, 0.94f * fade);
            PrayerVfxGeometry.tube(m, out, right.get(i), right.get(i + 1), 0.14,
                    0.2f, 0.015f, 0.045f, 0.74f * fade);
            PrayerVfxGeometry.tube(m, out, right.get(i), right.get(i + 1), 0.043,
                    0.94f, 0.12f, 0.18f, 0.94f * fade);
        }
        PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, y, 0), new Vec3(1, 0.05, 0), half * 0.9,
                0.045, age * 0.02, 0.62f, 0.04f, 0.22f, 0.76f * fade);
        for (int shard = 0; shard < 12; shard++) {
            double angle = shard * Math.PI * 2 / 12 - age * 0.012;
            double r = half * (0.65 + (shard % 3) * 0.2);
            Vec3 center = new Vec3(Math.cos(angle) * r, y + Math.sin(angle * 2.5 + age * 0.06),
                    Math.sin(angle) * 1.4);
            PrayerVfxPrimitives.crystal(m, out, center, new Vec3(Math.cos(angle) * 0.3, 1, Math.sin(angle)),
                    0.75, 0.24, 0.11f, 0.025f, 0.14f, 0.92f * fade);
        }
        for (int fissure = 0; fissure < 5; fissure++) {
            double angle = fissure * Math.PI * 2 / 5 + age * 0.004;
            Vec3 sky = new Vec3(Math.cos(angle) * 0.25, y - 0.2, Math.sin(angle) * 0.25);
            Vec3 ground = new Vec3(Math.cos(angle) * (1.2 + fissure % 2), 0.07,
                    Math.sin(angle) * (1.2 + fissure % 2));
            PrayerVfxGeometry.taperedTube(m, out, sky, ground, 0.18, 0.04,
                    0.55f, 0.03f, 0.18f, 0.32f * fade);
            PrayerVfxPrimitives.bolt(m, out, sky, ground, fissure * 977L + (long) age / 3, 8, 0.22, 0.92f * fade);
        }
    }
}
