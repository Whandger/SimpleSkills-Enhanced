package com.github.ob_yekt.simpleskills.abyss.client.vfx;

import com.github.ob_yekt.simpleskills.abyss.entity.CrimsonColossusEntity;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

/**
 * Colossus leap, in three stages:
 * 1. Windup: a ring tightens on the ground under its feet.
 * 2. Airborne: crimson embers orbit the body, so the jump reads as a jump.
 * 3. Landing: a shock front spreads outward from the exact point it touched down. Cracks, a dark stain,
 *    crystal spikes and arcs follow the front, so the impact zone grows around the landing spot.
 *
 * The front radius comes from CrimsonColossusEntity#leapFrontRadius, the same curve the damage uses.
 */
public final class CrimsonColossusLeapShockwaveVfx {
    private CrimsonColossusLeapShockwaveVfx() {}

    private static final double TAU = Math.PI * 2.0D;
    private static final double WTL = CrimsonColossusVfxPrimitives.WORLD_TO_LAYER;
    private static final int SPIKES = 14;
    private static final int ARCS = 12;
    private static final int CRACKS = 10;

    /**
     * @param forward     unit body-forward direction in layer space (unused, kept for a uniform signature)
     * @param impactCenter landing point relative to the Colossus base, in layer units
     * @param impactTick  attack tick of the landing, or -1 while airborne
     */
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 forward, Vec3 impactCenter,
                            int impactTick, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 2, 8);
        double launch = CrimsonColossusEntity.LEAP_LAUNCH_TICK;

        if (impactTick < 0) {
            if (age < launch + 3.0D) {
                drawWindup(m, out, age, launch, fade);
            }
            if (age >= launch) {
                drawEmbers(m, out, age, fade);
            }
            return;
        }

        drawShockFront(m, out, impactCenter, age - impactTick, age, fade);
    }

    private static void drawWindup(Matrix4fc m, VertexConsumer out, float age, double launch, float fade) {
        double charge = PrayerVfxPrimitives.smooth(0.0D, launch, age);
        double fadeOut = 1.0D - PrayerVfxPrimitives.smooth(launch, launch + 3.0D, age);
        double r = 0.6D + charge * 2.2D;
        float alpha = (float) (0.6D * charge * fadeOut * fade);
        CrimsonColossusVfxPrimitives.drawShockwaveRing(m, out, Vec3.ZERO, r * 0.78D, r, 0.03D, 32,
                0.98F, 0.16F, 0.28F, alpha);
    }

    /** Four crimson embers orbiting the body while it is in the air. They move with the boss. */
    private static void drawEmbers(Matrix4fc m, VertexConsumer out, float age, float fade) {
        for (int k = 0; k < 4; k++) {
            double ang = age * 0.42D + k * (Math.PI * 0.5D);
            double radius = 2.0D + 0.25D * Math.sin(age * 0.3D + k);
            double height = 2.3D + 0.7D * Math.sin(age * 0.37D + k * 1.7D);
            Vec3 pos = new Vec3(Math.cos(ang) * radius, height, Math.sin(ang) * radius);
            CrimsonColossusVfxPrimitives.drawEnergyOrb(m, out, pos, 0.20D + 0.05D * Math.sin(age + k),
                    0.98F, 0.20F, 0.32F, (float) (0.85D * fade));
        }
    }

    private static void drawShockFront(Matrix4fc m, VertexConsumer out, Vec3 c, double since,
                                       float age, float fade) {
        if (since < -0.5D) return;

        // Flash at the landing point.
        double flash = 1.0D - PrayerVfxPrimitives.smooth(0.0D, 4.0D, since);
        if (since >= 0.0D && flash > 0.01D) {
            CrimsonColossusVfxPrimitives.drawEnergyOrb(m, out, c.add(0.0D, 0.6D, 0.0D),
                    0.5D + 0.9D * (1.0D - flash), 1.0F, 0.80F, 0.86F, (float) (0.9D * flash * fade));
        }

        double frontWorld = CrimsonColossusEntity.leapFrontRadius(since);
        double front = frontWorld * WTL;
        double fadeOut = 1.0D - PrayerVfxPrimitives.smooth(
                CrimsonColossusEntity.LEAP_SPREAD_TICKS + 2.0D,
                CrimsonColossusEntity.LEAP_SPREAD_TICKS + 10.0D, since);
        float alpha = (float) (fadeOut * fade);
        if (front < 0.02D || alpha <= 0.01F) return;

        // Dark stain: fills the area the front has already crossed, so the landing reads as a crater.
        CrimsonColossusVfxPrimitives.drawShockwaveRing(m, out, c, 0.05D, front * 0.98D, 0.012D, 40,
                0.42F, 0.04F, 0.10F, 0.38F * alpha);
        // Leading wave band.
        CrimsonColossusVfxPrimitives.drawShockwaveRing(m, out, c, Math.max(0.02D, front - 0.55D), front, 0.05D, 48,
                0.98F, 0.16F, 0.28F, 0.95F * alpha);
        // Bright lip right on the front edge.
        CrimsonColossusVfxPrimitives.drawShockwaveRing(m, out, c, Math.max(0.01D, front - 0.12D), front + 0.02D,
                0.07D, 48, 1.00F, 0.72F, 0.78F, 0.90F * alpha);
        // Wake: a dimmer ring trailing behind the front.
        double wake = front * 0.62D;
        CrimsonColossusVfxPrimitives.drawShockwaveRing(m, out, c, Math.max(0.02D, wake - 0.3D), wake, 0.045D, 36,
                0.90F, 0.12F, 0.22F, 0.45F * alpha);

        drawCracks(m, out, c, front, alpha);
        drawSpikes(m, out, c, since, alpha);
        drawFrontArcs(m, out, c, front, age, alpha);
        drawDust(m, out, c, front, since, alpha);
    }

    /** Radial cracks that grow out to the front. */
    private static void drawCracks(Matrix4fc m, VertexConsumer out, Vec3 c, double front, float alpha) {
        double maxReach = 6.0D * WTL;
        for (int i = 0; i < CRACKS; i++) {
            double ang = i * TAU / CRACKS + Math.sin(i * 7.1D) * 0.18D;
            Vec3 dir = new Vec3(Math.cos(ang), 0.0D, Math.sin(ang));
            double reach = Math.min(front, maxReach) * (0.6D + 0.4D * ((i % 3) / 2.0D));
            Vec3 start = c.add(dir.scale(0.4D)).add(0.0D, 0.02D, 0.0D);
            Vec3 end = c.add(dir.scale(reach)).add(0.0D, 0.02D, 0.0D);
            CrimsonColossusVfxPrimitives.drawBeamSegment(m, out, start, end, 0.035D, 0.012D,
                    0.92F, 0.12F, 0.22F, 0.85F * alpha);
        }
    }

    /**
     * Crystal spikes that erupt as the front passes them. Each spike sits at a fixed distance and rises
     * when the front reaches that distance, then sinks back.
     */
    private static void drawSpikes(Matrix4fc m, VertexConsumer out, Vec3 c, double since, float alpha) {
        for (int i = 0; i < SPIKES; i++) {
            double spot = 0.30D + 0.65D * frac(i * 0.618D);
            double ang = i * TAU / SPIKES + (i % 2) * 0.17D;

            // Invert the front's easing to find when the front reaches this spot.
            double reachAt = (1.0D - Math.sqrt(1.0D - spot)) * CrimsonColossusEntity.LEAP_SPREAD_TICKS;
            double rise = PrayerVfxPrimitives.smooth(reachAt, reachAt + 3.0D, since)
                    * (1.0D - PrayerVfxPrimitives.smooth(reachAt + 9.0D, reachAt + 16.0D, since));
            if (rise < 0.03D) continue;

            Vec3 dir = new Vec3(Math.cos(ang), 0.0D, Math.sin(ang));
            Vec3 base = c.add(dir.scale(spot * CrimsonColossusEntity.LEAP_SPREAD_RADIUS * WTL));
            Vec3 axis = dir.scale(0.28D).add(0.0D, 1.0D, 0.0D).normalize();
            double length = 1.45D * rise;
            PrayerVfxPrimitives.crystal(m, out, base.add(axis.scale(length * 0.5D)), axis,
                    length, 0.22D * rise, 0.98F, 0.18F, 0.30F, (float) (0.95D * rise * alpha));
        }
    }

    /** Short lightning crossing the front, so the edge crackles while it moves. */
    private static void drawFrontArcs(Matrix4fc m, VertexConsumer out, Vec3 c, double front, float age, float alpha) {
        if (front < 0.4D) return;
        for (int i = 0; i < ARCS; i++) {
            double ang = i * TAU / ARCS + age * 0.04D;
            Vec3 dir = new Vec3(Math.cos(ang), 0.0D, Math.sin(ang));
            Vec3 a = c.add(dir.scale(front * 0.95D)).add(0.0D, 0.10D, 0.0D);
            Vec3 b = c.add(dir.scale(front * 0.72D)).add(0.0D, 0.16D, 0.0D);
            CrimsonColossusVfxPrimitives.drawLightningArc(m, out, a, b, 4, 0.10D, 0.02D,
                    age, 1300 + i * 17, 0.85F * alpha);
        }
    }

    /** Embers thrown up just behind the front. */
    private static void drawDust(Matrix4fc m, VertexConsumer out, Vec3 c, double front, double since, float alpha) {
        if (front < 0.6D) return;
        for (int k = 0; k < 10; k++) {
            double ang = k * TAU / 10.0D + k * 0.31D;
            double r = Math.max(0.1D, front - 0.5D - (k % 3) * 0.45D);
            double lift = 0.15D + 0.25D * Math.sin(since * 0.45D + k);
            Vec3 pos = c.add(Math.cos(ang) * r, lift, Math.sin(ang) * r);
            CrimsonColossusVfxPrimitives.drawEnergyOrb(m, out, pos, 0.10D, 0.90F, 0.25F, 0.35F, 0.6F * alpha);
        }
    }

    private static double frac(double x) {
        return x - Math.floor(x);
    }
}
