package com.github.ob_yekt.simpleskills.abyss.client.vfx;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class CrimsonColossusLaserVfx {
    private CrimsonColossusLaserVfx() {}

    private static final Vec3[] CRYSTAL_TIPS = {
            new Vec3( 0.00D, 3.85D, -0.10D),
            new Vec3(-0.55D, 3.35D, -0.05D),
            new Vec3( 0.55D, 3.35D, -0.05D),
            new Vec3(-0.62D, 3.10D,  0.30D),
            new Vec3( 0.62D, 3.10D,  0.30D),
            new Vec3(-0.65D, 3.05D, -0.35D),
            new Vec3( 0.65D, 3.05D, -0.35D),
    };

    public static void draw(Matrix4fc m, VertexConsumer out,
                            Vec3 forwardLocal, Vec3 mouthPos, Vec3 aimVectorLocal,
                            float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 2, 6);
        double charge = PrayerVfxPrimitives.smooth(0.0D, 26.0D, age);
        double fire = PrayerVfxPrimitives.smooth(26.0D, 30.0D, age)
                * (1.0D - PrayerVfxPrimitives.smooth(78.0D, duration, age));

        Vec3 right = new Vec3(-forwardLocal.z, 0.0D, forwardLocal.x);
        Vec3 crownHub = new Vec3(0.0D, 3.85D, 0.0D);

        // Energia elétrica vermelha nos cristais das costas
        float lightningAlpha = (float) ((0.55D + 0.45D * charge) * fade);
        for (int i = 1; i < CRYSTAL_TIPS.length; i++) {
            Vec3 localTip = CRYSTAL_TIPS[i];
            Vec3 localTipPosition = forwardLocal.scale(localTip.z)
                    .add(right.scale(localTip.x)).add(0.0D, localTip.y, 0.0D);
            CrimsonColossusVfxPrimitives.drawLightningArc(
                    m, out, localTipPosition, crownHub,
                    5, 0.24D, 0.026D, age, 100 + i * 19, lightningAlpha
            );
        }

        if (charge > 0.25D) {
            Vec3 browBridge = mouthPos.subtract(forwardLocal.scale(0.60D)).add(0.0D, 0.63D, 0.0D);
            CrimsonColossusVfxPrimitives.drawLightningArc(
                    m, out, crownHub, browBridge,
                    5, 0.22D, 0.030D, age, 301, lightningAlpha
            );
            CrimsonColossusVfxPrimitives.drawLightningArc(
                    m, out, browBridge, mouthPos,
                    4, 0.18D, 0.032D, age, 302, lightningAlpha
            );
        }

        double orbRadius = (0.18D + 0.34D * charge + 0.14D * fire) * (1.0D + 0.12D * Math.sin(age * 1.9D));
        CrimsonColossusVfxPrimitives.drawEnergyOrb(
                m, out, mouthPos, orbRadius * 1.35D, 0.95F, 0.12F, 0.24F, (float) (0.55D * fade));
        CrimsonColossusVfxPrimitives.drawEnergyOrb(
                m, out, mouthPos, orbRadius * 0.72D, 1.00F, 0.85F, 0.88F, (float) (0.95D * fade));

        // Feixe Laser saindo da boca
        if (fire > 0.01D) {
            // aimVectorLocal already represents the exact mouth-to-target vector
            // in the same local coordinate space as this geometry. Do not add a
            // vertical offset: that was causing the beam to pass above players.
            Vec3 beamEnd = aimVectorLocal.lengthSqr() > 0.25D
                    ? mouthPos.add(aimVectorLocal)
                    : mouthPos.add(forwardLocal.scale(14.0D));

            double pulse = 1.0D + 0.16D * Math.sin(age * 2.4D);
            double outerR = 0.44D * fire * pulse;
            double midR = 0.25D * fire * pulse;
            double coreR = 0.11D * fire;

            CrimsonColossusVfxPrimitives.drawBeamSegment(
                    m, out, mouthPos, beamEnd, outerR * 0.85D, outerR * 1.15D,
                    0.92F, 0.08F, 0.20F, (float) (0.38D * fire * fade));
            CrimsonColossusVfxPrimitives.drawBeamSegment(
                    m, out, mouthPos, beamEnd, midR, midR,
                    1.00F, 0.22F, 0.34F, (float) (0.72D * fire * fade));
            CrimsonColossusVfxPrimitives.drawBeamSegment(
                    m, out, mouthPos, beamEnd, coreR, coreR,
                    1.00F, 0.90F, 0.92F, (float) (0.98D * fire * fade));

            Vec3 beamVec = beamEnd.subtract(mouthPos);
            double beamLen = beamVec.length();
            if (beamLen > 0.5D) {
                Vec3 beamDir = beamVec.normalize();
                int ringCount = Math.max(3, (int) (beamLen / 2.2D));
                for (int rIdx = 0; rIdx < ringCount; rIdx++) {
                    double frac = ((rIdx / (double) ringCount) + (age * 0.11D)) % 1.0D;
                    Vec3 ringCenter = mouthPos.add(beamDir.scale(frac * beamLen));
                    double ringR = (0.35D + 0.32D * Math.sin(frac * Math.PI)) * fire;
                    CrimsonColossusVfxPrimitives.drawEnergyOrb(
                            m, out, ringCenter, ringR, 0.98F, 0.20F, 0.32F, (float) (0.42D * fire * fade));
                }
            }

            CrimsonColossusVfxPrimitives.drawEnergyOrb(
                    m, out, beamEnd, 0.72D * fire * pulse, 0.98F, 0.18F, 0.30F, (float) (0.75D * fire * fade));
            for (int k = 0; k < 4; k++) {
                double ang = (age * 0.7D) + k * (Math.PI * 0.5D);
                Vec3 sparkTip = beamEnd.add(Math.cos(ang) * 1.15D, 0.35D + (k % 2) * 0.45D, Math.sin(ang) * 1.15D);
                CrimsonColossusVfxPrimitives.drawLightningArc(
                        m, out, beamEnd, sparkTip, 3, 0.22D, 0.022D, age, 500 + k, (float) (0.85D * fire * fade));
            }
        }
    }
}
