package com.github.ob_yekt.simpleskills.abyss.client.vfx;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class CrimsonColossusLeapShockwaveVfx {
    private CrimsonColossusLeapShockwaveVfx() {}

    private static final double MAX_SHOCKWAVE_RADIUS = 8.0D;

    public static void draw(Matrix4fc m, VertexConsumer out,
                            Vec3 forward, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 2, 6);

        if (age >= 11.0F && age <= 21.0F) {
            double launch = PrayerVfxPrimitives.smooth(11.0D, 21.0D, age);
            float launchAlpha = (float) ((1.0D - launch) * 0.85D * fade);
            CrimsonColossusVfxPrimitives.drawShockwaveRing(
                    m, out, new Vec3(0.0D, 0.05D, 0.0D),
                    0.8D + launch * 2.0D, 1.5D + launch * 2.8D,
                    0.04D, 28, 0.98F, 0.16F, 0.28F, launchAlpha);
        }

        if (age >= 12.0F && age < 35.0F) {
            Vec3 crownCenter = new Vec3(0.0D, 3.2D, 0.0D);
            CrimsonColossusVfxPrimitives.drawEnergyOrb(
                    m, out, crownCenter, 1.45D + 0.2D * Math.sin(age * 1.5D),
                    0.95F, 0.12F, 0.24F, 0.35F * fade);
        }

        if (age >= 35.0F) {
            double progress = PrayerVfxPrimitives.smooth(35.0D, duration - 1.0D, age);
            double expand = 1.0D - Math.pow(1.0D - progress, 2.2D);
            double radius = 1.2D + expand * (MAX_SHOCKWAVE_RADIUS - 1.2D);
            float alpha = (float) ((1.0D - progress * 0.78D) * fade);

            Vec3 center = new Vec3(0.0D, 0.04D, 0.0D);

            // Onda de choque externa de 8 blocos de distância
            CrimsonColossusVfxPrimitives.drawShockwaveRing(
                    m, out, center, Math.max(0.2D, radius - 1.05D), radius,
                    0.04D, 40, 0.96F, 0.12F, 0.24F, alpha * 0.92F);

            double innerR = radius * 0.76D;
            CrimsonColossusVfxPrimitives.drawShockwaveRing(
                    m, out, center, Math.max(0.1D, innerR - 0.65D), innerR,
                    0.07D, 36, 1.00F, 0.72F, 0.78F, alpha * 0.85F);

            double wallHeight = (1.65D * (1.0D - progress * 0.65D));
            CrimsonColossusVfxPrimitives.drawShockwaveWall(
                    m, out, center, radius, wallHeight,
                    36, 0.95F, 0.14F, 0.26F, alpha * 0.42F);

            int rays = 16;
            for (int i = 0; i < rays; i++) {
                double ang = (i / (double) rays) * Math.PI * 2.0D;
                Vec3 dir = new Vec3(Math.cos(ang), 0.0D, Math.sin(ang));
                Vec3 arcStart = center.add(dir.scale(1.1D)).add(0.0D, 0.08D, 0.0D);
                Vec3 arcEnd = center.add(dir.scale(radius * 0.96D)).add(0.0D, 0.12D, 0.0D);

                CrimsonColossusVfxPrimitives.drawLightningArc(
                        m, out, arcStart, arcEnd,
                        5, 0.28D, 0.028D, age, 900 + i * 23, alpha * 0.88F);

                if (i % 2 == 0) {
                    double spikeScale = (1.15D + (i % 4) * 0.25D) * (1.0D - progress * 0.65D);
                    if (spikeScale > 0.12D) {
                        Vec3 spikeBase = center.add(dir.scale(radius * 0.88D));
                        Vec3 spikeAxis = dir.scale(0.45D).add(0.0D, 1.0D, 0.0D).normalize();
                        PrayerVfxPrimitives.crystal(
                                m, out, spikeBase.add(spikeAxis.scale(spikeScale * 0.55D)),
                                spikeAxis, spikeScale * 1.8D, spikeScale * 0.36D,
                                0.98F, 0.18F, 0.30F, alpha * 0.95F);
                    }
                }
            }
        }
    }
}
