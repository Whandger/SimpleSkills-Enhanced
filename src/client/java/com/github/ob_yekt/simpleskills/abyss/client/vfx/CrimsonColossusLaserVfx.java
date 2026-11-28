package com.github.ob_yekt.simpleskills.abyss.client.vfx;

import com.github.ob_yekt.simpleskills.abyss.entity.CrimsonColossusEntity;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

/**
 * Crystal laser. Frame: layer units, origin at the Colossus base (see CrimsonColossusAttackLayer).
 * The mouth and the aim vector arrive already converted, so the beam ends exactly where the server
 * damages, at the target's chest.
 */
public final class CrimsonColossusLaserVfx {
    private CrimsonColossusLaserVfx() {}

    /** Longest beam in layer units: 26 world blocks. */
    private static final double REACH_LAYER = 26.0D * CrimsonColossusVfxPrimitives.WORLD_TO_LAYER;

    private static final Vec3[] CRYSTAL_TIPS = {
            new Vec3( 0.00D, 3.85D, -0.10D),
            new Vec3(-0.55D, 3.35D, -0.05D),
            new Vec3( 0.55D, 3.35D, -0.05D),
            new Vec3(-0.62D, 3.10D,  0.30D),
            new Vec3( 0.62D, 3.10D,  0.30D),
            new Vec3(-0.65D, 3.05D, -0.35D),
            new Vec3( 0.65D, 3.05D, -0.35D),
    };

    /**
     * @param forward unit body-forward direction in layer space
     * @param mouth   mouth position in layer units
     * @param aim     mouth-to-aim vector in layer units (the aim point is the target's chest)
     */
    public static void draw(Matrix4fc m, VertexConsumer out,
                            Vec3 forward, Vec3 mouth, Vec3 aim,
                            float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 2, 8);
        double chargeEnd = CrimsonColossusEntity.LASER_CHARGE_END;
        double fireEnd = CrimsonColossusEntity.LASER_FIRE_END;
        double charge = PrayerVfxPrimitives.smooth(0.0D, chargeEnd, age);
        double fire = PrayerVfxPrimitives.smooth(chargeEnd, chargeEnd + 5.0D, age)
                * (1.0D - PrayerVfxPrimitives.smooth(fireEnd - 6.0D, fireEnd + 6.0D, age));

        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 crownHub = new Vec3(0.0D, 3.85D, 0.0D);

        // Lightning between the dorsal crystals and the crown.
        float lightningAlpha = (float) ((0.55D + 0.45D * charge) * fade);
        for (int i = 1; i < CRYSTAL_TIPS.length; i++) {
            Vec3 localTip = CRYSTAL_TIPS[i];
            Vec3 tipPosition = forward.scale(localTip.z)
                    .add(right.scale(localTip.x))
                    .add(0.0D, localTip.y, 0.0D);
            CrimsonColossusVfxPrimitives.drawLightningArc(
                    m, out, tipPosition, crownHub, 5, 0.24D, 0.026D, age, 100 + i * 19, lightningAlpha);
        }

        if (charge > 0.25D) {
            Vec3 browBridge = mouth.subtract(forward.scale(0.60D)).add(0.0D, 0.63D, 0.0D);
            CrimsonColossusVfxPrimitives.drawLightningArc(
                    m, out, crownHub, browBridge, 5, 0.22D, 0.030D, age, 301, lightningAlpha);
            CrimsonColossusVfxPrimitives.drawLightningArc(
                    m, out, browBridge, mouth, 4, 0.18D, 0.032D, age, 302, lightningAlpha);
        }

        // Where the beam is pointed (clamped to the reach of the server laser).
        Vec3 beam;
        if (aim.lengthSqr() > 0.04D) {
            beam = aim.length() > REACH_LAYER ? aim.normalize().scale(REACH_LAYER) : aim;
        } else {
            beam = forward.scale(REACH_LAYER * 0.5D);
        }
        Vec3 beamEnd = mouth.add(beam);

        // Telegraph: a thin warning line during the charge, so the player can read where the beam will go.
        double telegraph = PrayerVfxPrimitives.smooth(0.0D, 6.0D, age) * (1.0D - fire);
        if (telegraph > 0.01D) {
            double flicker = 0.75D + 0.25D * Math.sin(age * 3.1D);
            CrimsonColossusVfxPrimitives.drawBeamSegment(m, out, mouth, beamEnd, 0.02D, 0.02D,
                    1.00F, 0.35F, 0.45F, (float) (0.60D * telegraph * flicker * fade));
        }

        // Energy gathering in the mouth.
        double orbRadius = (0.18D + 0.34D * charge + 0.14D * fire) * (1.0D + 0.12D * Math.sin(age * 1.9D));
        CrimsonColossusVfxPrimitives.drawEnergyOrb(
                m, out, mouth, orbRadius * 1.35D, 0.95F, 0.12F, 0.24F, (float) (0.55D * fade));
        CrimsonColossusVfxPrimitives.drawEnergyOrb(
                m, out, mouth, orbRadius * 0.72D, 1.00F, 0.85F, 0.88F, (float) (0.95D * fade));

        if (fire > 0.01D) {
            double pulse = 1.0D + 0.16D * Math.sin(age * 2.4D);
            double outerR = 0.44D * fire * pulse;
            double midR = 0.25D * fire * pulse;
            double coreR = 0.11D * fire;

            CrimsonColossusVfxPrimitives.drawBeamSegment(m, out, mouth, beamEnd, outerR * 0.85D, outerR * 1.15D,
                    0.92F, 0.08F, 0.20F, (float) (0.38D * fire * fade));
            CrimsonColossusVfxPrimitives.drawBeamSegment(m, out, mouth, beamEnd, midR, midR,
                    1.00F, 0.22F, 0.34F, (float) (0.72D * fire * fade));
            CrimsonColossusVfxPrimitives.drawBeamSegment(m, out, mouth, beamEnd, coreR, coreR,
                    1.00F, 0.90F, 0.92F, (float) (0.98D * fire * fade));

            // Energy rings sliding along the beam toward the chest.
            double beamLen = beam.length();
            if (beamLen > 0.5D) {
                Vec3 beamDir = beam.normalize();
                int ringCount = Math.max(3, (int) (beamLen / 2.2D));
                for (int r = 0; r < ringCount; r++) {
                    double frac = ((r / (double) ringCount) + (age * 0.11D)) % 1.0D;
                    Vec3 ringCenter = mouth.add(beamDir.scale(frac * beamLen));
                    double ringR = (0.35D + 0.32D * Math.sin(frac * Math.PI)) * fire;
                    CrimsonColossusVfxPrimitives.drawEnergyOrb(
                            m, out, ringCenter, ringR, 0.98F, 0.20F, 0.32F, (float) (0.42D * fire * fade));
                }
            }

            // Impact flare on the target.
            CrimsonColossusVfxPrimitives.drawEnergyOrb(
                    m, out, beamEnd, 0.72D * fire * pulse, 0.98F, 0.18F, 0.30F, (float) (0.75D * fire * fade));
            for (int k = 0; k < 4; k++) {
                double ang = (age * 0.7D) + k * (Math.PI * 0.5D);
                Vec3 sparkTip = beamEnd.add(Math.cos(ang) * 0.9D, 0.30D + (k % 2) * 0.35D, Math.sin(ang) * 0.9D);
                CrimsonColossusVfxPrimitives.drawLightningArc(
                        m, out, beamEnd, sparkTip, 3, 0.22D, 0.022D, age, 500 + k, (float) (0.85D * fire * fade));
            }
        }
    }
}
