package com.github.ob_yekt.simpleskills.prayer.eletro;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerChainLightningVfx {
    private PrayerChainLightningVfx() {}

    // 23. CHAIN_LIGHTNING — the main discharge branches into sequential, visibly thinning jumps.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 2, 5);
        double progress = PrayerVfxPrimitives.smooth(2, duration * 0.42, age);
        Vec3 first = destination.scale(progress);
        PrayerVfxPrimitives.bolt(m, out, Vec3.ZERO, first, 0xC4A1L + (long) age / 2, 17, 0.25, fade);
        for (int hop = 0; hop < 3; hop++) {
            double startPhase = 0.25 + hop * 0.2;
            double grow = PrayerVfxPrimitives.smooth(startPhase, startPhase + 0.16, progress);
            if (grow <= 0) continue;
            double angle = hop * Math.PI * 2 / 3 + age * 0.018;
            Vec3 from = destination.add(0, 0.35, 0).add(Math.cos(angle) * hop * 0.22,
                    0, Math.sin(angle) * hop * 0.22);
            Vec3 to = from.add(Math.cos(angle) * (1.5 + hop * 0.48) * grow,
                    (hop % 2 == 0 ? 1 : -1) * 0.65 * grow,
                    Math.sin(angle) * (1.5 + hop * 0.48) * grow);
            PrayerVfxPrimitives.bolt(m, out, from, to, hop * 739L + (long) age / 2,
                    8 - hop, 0.16 * (1 - hop * 0.2), (float) (fade * (1 - hop * 0.17)));
            PrayerVfxGeometry.wireSphere(m, out, to, 0.12 + grow * 0.12, 7,
                    age * 0.12 + hop, 0.62f, 0.88f, 1.0f, fade * (float) grow, 0.012);
        }
        PrayerVfxGeometry.ellipsoid(m, out, destination, new Vec3(0.18, 0.23, 0.18), 7, 10,
                0.8f, 0.94f, 1.0f, 0.75f * fade, age * 0.3);
    }
}
