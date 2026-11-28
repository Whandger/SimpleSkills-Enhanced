package com.github.ob_yekt.simpleskills.abyss.client.vfx;

import com.github.ob_yekt.simpleskills.abyss.entity.CrimsonColossusEntity;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

/**
 * Seismic slam. The impact sits exactly where the server damages (SLAM_REACH blocks ahead) and the rings
 * reach the same radius as the damage (about 6.5 blocks).
 */
public final class CrimsonColossusSlamVfx {
    private CrimsonColossusSlamVfx() {}

    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 forward, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 2, 8);
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        double impactAt = CrimsonColossusEntity.SLAM_IMPACT_TICK;

        if (age < impactAt + 2.0D) {
            double windup = PrayerVfxPrimitives.smooth(0.0D, impactAt - 2.0D, age)
                    * (1.0D - PrayerVfxPrimitives.smooth(impactAt - 1.0D, impactAt + 2.0D, age));
            Vec3 leftFist = forward.scale(1.1D).add(right.scale(1.25D)).add(0.0D, 3.65D, 0.0D);
            Vec3 rightFist = forward.scale(1.1D).subtract(right.scale(1.25D)).add(0.0D, 3.65D, 0.0D);

            CrimsonColossusVfxPrimitives.drawEnergyOrb(
                    m, out, leftFist, 0.55D * windup, 0.98F, 0.16F, 0.28F, (float) (0.85D * fade));
            CrimsonColossusVfxPrimitives.drawEnergyOrb(
                    m, out, rightFist, 0.55D * windup, 0.98F, 0.16F, 0.28F, (float) (0.85D * fade));
            CrimsonColossusVfxPrimitives.drawLightningArc(
                    m, out, leftFist, rightFist, 6, 0.32D, 0.032D, age, 77, (float) (0.90D * windup * fade));
        }

        if (age >= impactAt) {
            double impactProgress = PrayerVfxPrimitives.smooth(impactAt, duration - 2.0D, age);
            double burst = PrayerVfxPrimitives.smooth(impactAt, impactAt + 5.0D, age);
            float waveAlpha = (float) ((1.0D - impactProgress * 0.72D) * fade);

            Vec3 impactCenter = forward.scale(CrimsonColossusEntity.SLAM_REACH * CrimsonColossusVfxPrimitives.WORLD_TO_LAYER)
                    .add(0.0D, 0.04D, 0.0D);

            double r1 = 0.4D + impactProgress * 3.4D;
            CrimsonColossusVfxPrimitives.drawShockwaveRing(
                    m, out, impactCenter, Math.max(0.1D, r1 - 0.6D), r1,
                    0.03D, 32, 0.98F, 0.16F, 0.28F, waveAlpha * 0.90F);

            double r2 = 0.2D + impactProgress * 2.2D;
            CrimsonColossusVfxPrimitives.drawShockwaveRing(
                    m, out, impactCenter, Math.max(0.1D, r2 - 0.4D), r2,
                    0.05D, 28, 1.00F, 0.65F, 0.72F, waveAlpha * 0.85F);

            int crackCount = 12;
            for (int c = 0; c < crackCount; c++) {
                double baseAng = (c / (double) crackCount) * Math.PI * 2.0D + (c % 2) * 0.14D;
                double maxLen = (2.6D + (c % 3) * 0.6D) * burst;
                Vec3 dir = new Vec3(Math.cos(baseAng), 0.0D, Math.sin(baseAng));
                Vec3 perp = new Vec3(-dir.z, 0.0D, dir.x);

                Vec3 prev = impactCenter;
                int segs = 5;
                for (int s = 1; s <= segs; s++) {
                    double frac = s / (double) segs;
                    double zig = Math.sin(c * 9.3D + s * 4.7D) * 0.20D * frac;
                    Vec3 next = impactCenter.add(dir.scale(maxLen * frac)).add(perp.scale(zig));

                    double wOuter = (0.22D * (1.0D - frac * 0.65D));
                    double wCore = wOuter * 0.45D;

                    CrimsonColossusVfxPrimitives.addDoubleQuad(
                            m, out,
                            prev.subtract(perp.scale(wOuter)), prev.add(perp.scale(wOuter)),
                            next.add(perp.scale(wOuter)), next.subtract(perp.scale(wOuter)),
                            0.88F, 0.08F, 0.18F, waveAlpha * 0.85F
                    );
                    CrimsonColossusVfxPrimitives.addDoubleQuad(
                            m, out,
                            prev.subtract(perp.scale(wCore)).add(0.0D, 0.015D, 0.0D),
                            prev.add(perp.scale(wCore)).add(0.0D, 0.015D, 0.0D),
                            next.add(perp.scale(wCore)).add(0.0D, 0.015D, 0.0D),
                            next.subtract(perp.scale(wCore)).add(0.0D, 0.015D, 0.0D),
                            1.00F, 0.72F, 0.78F, waveAlpha
                    );
                    prev = next;
                }

                double spikeRise = PrayerVfxPrimitives.smooth(impactAt + 1.0D, impactAt + 9.0D, age)
                        * (1.0D - PrayerVfxPrimitives.smooth(impactAt + 10.0D, duration, age));
                if (spikeRise > 0.02D) {
                    Vec3 spikePos = impactCenter.add(dir.scale(maxLen * 0.68D)).add(0.0D, 0.45D * spikeRise, 0.0D);
                    Vec3 spikeAxis = dir.scale(0.35D).add(0.0D, 1.0D, 0.0D).normalize();
                    PrayerVfxPrimitives.crystal(
                            m, out, spikePos, spikeAxis,
                            1.35D * spikeRise, 0.30D * spikeRise,
                            0.96F, 0.16F, 0.28F, (float) (0.92D * fade)
                    );
                }
            }
        }
    }
}
