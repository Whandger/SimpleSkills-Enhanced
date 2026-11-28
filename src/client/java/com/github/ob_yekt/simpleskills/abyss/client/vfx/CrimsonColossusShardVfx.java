package com.github.ob_yekt.simpleskills.abyss.client.vfx;

import com.github.ob_yekt.simpleskills.abyss.entity.CrimsonColossusEntity;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class CrimsonColossusShardVfx {
    private CrimsonColossusShardVfx() {}

    private static final double CAST_START = CrimsonColossusEntity.BARRAGE_WAVE_START;
    private static final double TRAVEL_END = 0.62D;
    private static final double SHATTER_START = 0.74D;
    private static final int PATH_PIECES = 3;
    private static final int IMPACT_PIECES = 5;
    private static final int DUST_COUNT = 20;

    public static void draw(Matrix4fc m, VertexConsumer out,
                            Vec3 forward, Vec3 targetOffset,
                            float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 2, 4);
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);

        if (age < 12.0F) {
            double handCharge = PrayerVfxPrimitives.smooth(0.0D, 8.0D, age)
                    * (1.0D - PrayerVfxPrimitives.smooth(9.0D, 12.0D, age));
            Vec3 raisedHand = forward.scale(1.45D).subtract(right.scale(1.25D)).add(0.0D, 3.15D, 0.0D);
            CrimsonColossusVfxPrimitives.drawEnergyOrb(
                    m, out, raisedHand, 0.58D * handCharge, 0.98F, 0.16F, 0.30F, (float) (0.90D * fade));
            PrayerVfxPrimitives.crystal(
                    m, out, raisedHand.add(forward.scale(0.35D)),
                    forward.add(0.0D, 0.45D, 0.0D).normalize(),
                    1.25D * handCharge, 0.28D * handCharge,
                    0.98F, 0.22F, 0.34F, (float) (0.95D * fade));
        }

        if (age < CAST_START) {
            return;
        }

        float localAge = (float) (age - CAST_START);
        int activeDuration = (int) (duration - CAST_START);
        double travel = PrayerVfxPrimitives.smooth(0.0D, activeDuration * TRAVEL_END, localAge);

        double shatterStart = activeDuration * SHATTER_START;
        double shatterEnd = activeDuration - 1.0D;
        double ts = Math.min(1.0D, Math.max(0.0D, (localAge - shatterStart) / Math.max(1.0D, shatterEnd - shatterStart)));

        double crackle = PrayerVfxPrimitives.smooth(shatterStart - 3.0D, shatterStart, localAge);
        double flash = ts > 0.0D ? Math.max(0.0D, 1.0D - ts * 5.0D) : crackle;
        double tremble = ts > 0.0D ? 0.0D : crackle * 0.035D;
        double mainScale = 1.0D - PrayerVfxPrimitives.smooth(0.0D, 0.22D, ts);

        float cr = tint(0.95F, flash);
        float cg = tint(0.14F, flash);
        float cb = tint(0.28F, flash);

        Vec3 pathStart = forward.scale(1.2D).add(0.0D, 0.05D, 0.0D);
        Vec3 destination = targetOffset.lengthSqr() > 1.0D
                ? new Vec3(targetOffset.x, Math.max(-1.0D, targetOffset.y), targetOffset.z)
                : forward.scale(12.0D * CrimsonColossusVfxPrimitives.WORLD_TO_LAYER);

        int totalSpikes = 24;
        int numSpikes = (int) (travel * totalSpikes);
        for (int i = 0; i < numSpikes; i++) {
            double t = i / (double) totalSpikes;
            Vec3 groundPath = pathStart.lerp(destination, t);

            double zig = Math.sin(i * 14.3D) * 0.42D;
            double zag = Math.cos(i * 21.7D) * 0.42D;
            Vec3 basePoint = groundPath.add(zig, 0.0D, zag);

            double scale = 0.55D + (i % 3) * 0.30D;
            Vec3 axis = new Vec3(zig * 0.30D, 1.0D, zag * 0.30D).normalize();

            Vec3 center = basePoint.add(0.0D, scale * 0.55D, 0.0D);
            double length = scale * 1.75D;
            double width = scale * 0.40D;
            float alpha = 0.92F * fade;

            if (ts <= 0.0D) {
                PrayerVfxPrimitives.crystal(m, out, center.add(shake(localAge, i, tremble)), axis,
                        length, width, cr, cg, cb, alpha);
                continue;
            }

            if (mainScale > 0.02D) {
                PrayerVfxPrimitives.crystal(m, out, center, axis,
                        length * mainScale, width * mainScale, cr, cg, cb, alpha);
            }
            shatterCrystal(m, out, center, axis, length, width, i, PATH_PIECES, ts, groundPath,
                    cr, cg, cb, alpha);
        }

        if (travel > 0.85D) {
            double impact = PrayerVfxPrimitives.smooth(0.85D, 1.0D, travel);
            for (int i = 0; i < 14; i++) {
                double angle = i * (Math.PI * 2.0D / 14.0D);
                Vec3 dir = new Vec3(Math.cos(angle), 0.75D + (i % 3) * 0.45D, Math.sin(angle)).normalize();
                double scale = (1.75D + (i % 4) * 0.65D) * impact;
                Vec3 spikeBase = destination.add(Math.cos(angle) * 0.55D, 0.0D, Math.sin(angle) * 0.55D);

                Vec3 center = spikeBase.add(dir.scale(scale * 0.55D));
                double length = scale * 2.35D;
                double width = scale * 0.48D;
                float alpha = (float) (0.94D * (1.0D - impact * 0.18D) * fade);

                if (ts <= 0.0D) {
                    PrayerVfxPrimitives.crystal(m, out, center.add(shake(localAge, i + 40, tremble)), dir,
                            length, width, cr, cg, cb, alpha);
                    continue;
                }

                if (mainScale > 0.02D) {
                    PrayerVfxPrimitives.crystal(m, out, center, dir,
                            length * mainScale, width * mainScale, cr, cg, cb, alpha);
                }
                shatterCrystal(m, out, center, dir, length, width, i + 100, IMPACT_PIECES, ts, destination,
                        cr, cg, cb, alpha);
            }

            if (ts > 0.0D) {
                drawDust(m, out, destination, ts, cr, cg, cb, 0.92F * fade);
            }
        }
    }

    private static void shatterCrystal(Matrix4fc m, VertexConsumer out, Vec3 center, Vec3 axis,
                                       double length, double width, int seed, int pieces, double ts,
                                       Vec3 origin, float r, float g, float b, float a) {
        double shrink = Math.pow(1.0D - ts, 0.8D);
        if (shrink < 0.04D) return;

        double ease = 1.0D - (1.0D - ts) * (1.0D - ts);
        double fall = 1.85D * ts * ts;
        float alpha = (float) (a * (1.0D - 0.5D * ts));

        Vec3 flat = new Vec3(center.x - origin.x, 0.0D, center.z - origin.z);
        flat = flat.lengthSqr() > 1.0E-4D ? flat.normalize() : Vec3.ZERO;

        for (int p = 0; p < pieces; p++) {
            int s = seed * 37 + p * 11;
            Vec3 dir = new Vec3(
                    flat.x * 0.9D + (rnd(s) - 0.5D) * 1.4D,
                    0.28D + rnd(s + 1) * 0.95D,
                    flat.z * 0.9D + (rnd(s + 2) - 0.5D) * 1.4D).normalize();
            double speed = 0.95D + rnd(s + 3) * 1.75D;

            double along = pieces > 1 ? (p / (double) (pieces - 1) - 0.5D) : 0.0D;
            Vec3 start = center.add(axis.scale(along * length * 0.7D));
            Vec3 pos = start.add(dir.scale(speed * ease)).add(0.0D, -fall, 0.0D);

            double spin = ts * (4.0D + rnd(s + 4) * 6.0D);
            Vec3 pieceAxis = new Vec3(
                    axis.x + Math.sin(spin + s) * 0.9D,
                    axis.y + Math.cos(spin * 1.3D + s) * 0.9D,
                    axis.z + Math.sin(spin * 0.8D + s * 0.5D) * 0.9D).normalize();

            double pieceLength = length * (0.30D + rnd(s + 5) * 0.25D) * shrink;
            double pieceWidth = width * (0.55D + rnd(s + 6) * 0.30D) * shrink;

            PrayerVfxPrimitives.crystal(m, out, pos, pieceAxis, pieceLength, pieceWidth, r, g, b, alpha);
        }
    }

    private static void drawDust(Matrix4fc m, VertexConsumer out, Vec3 impactCenter, double ts,
                                 float r, float g, float b, float a) {
        double shrink = Math.pow(1.0D - ts, 0.8D);
        if (shrink < 0.04D) return;

        double ease = 1.0D - (1.0D - ts) * (1.0D - ts);
        double fall = 1.4D * ts * ts;
        float alpha = (float) (a * (1.0D - 0.5D * ts));
        Vec3 origin = impactCenter.add(0.0D, 0.45D, 0.0D);

        for (int k = 0; k < DUST_COUNT; k++) {
            int s = 500 + k * 13;
            double angle = rnd(s) * Math.PI * 2.0D;
            Vec3 dir = new Vec3(Math.cos(angle), 0.3D + rnd(s + 1) * 1.0D, Math.sin(angle)).normalize();
            double speed = 1.1D + rnd(s + 2) * 2.3D;

            Vec3 pos = origin.add(dir.scale(speed * ease)).add(0.0D, -fall, 0.0D);

            double spin = ts * (6.0D + rnd(s + 3) * 8.0D);
            Vec3 axis = new Vec3(
                    Math.sin(spin + s),
                    Math.cos(spin * 1.2D + s),
                    Math.sin(spin * 0.7D + s * 0.3D)).normalize();

            double size = (0.20D + rnd(s + 4) * 0.24D) * shrink;
            PrayerVfxPrimitives.crystal(m, out, pos, axis, size * 2.1D, size * 0.45D, r, g, b, alpha);
        }
    }

    private static Vec3 shake(float age, int i, double amount) {
        if (amount <= 0.0D) return Vec3.ZERO;
        return new Vec3(Math.sin(age * 47.0D + i * 3.1D) * amount, 0.0D, Math.cos(age * 53.0D + i * 1.7D) * amount);
    }

    private static float tint(float base, double flash) {
        return (float) (base + (1.0F - base) * flash * 0.75D);
    }

    private static double rnd(int seed) {
        double x = Math.sin(seed * 12.9898D + 78.233D) * 43758.5453D;
        return x - Math.floor(x);
    }
}
