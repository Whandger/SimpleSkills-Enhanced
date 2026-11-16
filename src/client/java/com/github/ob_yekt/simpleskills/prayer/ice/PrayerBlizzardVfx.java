package com.github.ob_yekt.simpleskills.prayer.ice;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerBlizzardVfx {
    private PrayerBlizzardVfx() {}

    // 34. BLIZZARD — uma cobertura de nuvens ocupa uma área de 30x30 blocos,
    // despeja cristais de gelo que ficam cravados no chão e depois se estilhaçam,
    // enquanto as nuvens se dissipam naturalmente no céu.
    private static final int CLOUD_GRID = 5;
    private static final int CRYSTAL_COUNT = 100;
    private static final double AREA_RADIUS = 15.0;
    private static final double CLOUD_HEIGHT = 10.0;
    private static final double GROUND_Y = 0.0;
    private static final int SHATTER_PIECES = 4;

    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 8, 6);
        if (fade <= 0.01f) return;

        drawClouds(m, out, age, duration, fade);
        drawIceRain(m, out, age, duration, fade);
    }

    // Nuvens sobrepostas em uma grade 5x5 que se formam no início e se dissipam
    // naturalmente de fora para dentro após a chuva de gelo terminar.
    private static void drawClouds(Matrix4fc m, VertexConsumer out, float age, int duration, float fade) {
        double build = PrayerVfxPrimitives.smooth(0.0, duration * 0.14, age);
        double dissipateStart = duration * 0.52;
        double dissipateEnd = duration * 0.94;

        for (int gx = 0; gx < CLOUD_GRID; gx++) {
            for (int gz = 0; gz < CLOUD_GRID; gz++) {
                int id = gx * CLOUD_GRID + gz;
                double x = -12.0 + gx * 6.0;
                double z = -12.0 + gz * 6.0;

                // Nuvens das bordas dissipam primeiro e o centro por último
                double distNorm = Math.hypot(x, z) / 17.0;
                double localStart = dissipateStart + (1.0 - distNorm) * (duration * 0.14) - rnd(id + 5) * 2.0;
                double localEnd = Math.min(duration - 1.0, dissipateEnd - distNorm * (duration * 0.06));
                double dissipate = PrayerVfxPrimitives.smooth(localStart, Math.max(localStart + 5.0, localEnd), age);

                float cloudAlpha = fade * (float) (build * (1.0 - dissipate));
                if (cloudAlpha <= 0.005f) continue;

                double sway = 1.0 - dissipate * 0.85;
                double driftX = Math.sin(age * 0.012 + id * 1.7) * 0.16 * sway;
                double driftZ = Math.cos(age * 0.011 + id * 1.3) * 0.16 * sway;
                double puff = 0.88 + rnd(id + 10) * 0.24;

                double riseOffset = dissipate * (1.1 + rnd(id + 15) * 0.6);
                double spreadScale = 1.0 + dissipate * 0.22;
                double flattenScale = 1.0 - dissipate * 0.35;

                Vec3 center = new Vec3(
                        x + driftX,
                        CLOUD_HEIGHT + riseOffset + Math.sin(age * 0.016 + id) * 0.10 * sway,
                        z + driftZ
                );

                Vec3 radii = new Vec3(
                        (4.0 + rnd(id + 20) * 0.8) * puff * build * spreadScale,
                        (0.72 + rnd(id + 30) * 0.28) * build * flattenScale,
                        (4.0 + rnd(id + 40) * 0.8) * puff * build * spreadScale
                );

                PrayerVfxGeometry.ellipsoid(
                        m, out, center, radii, 7, 12,
                        0.38f + (float) dissipate * 0.14f,
                        0.59f + (float) dissipate * 0.12f,
                        0.79f + (float) dissipate * 0.10f,
                        0.48f * cloudAlpha,
                        rnd(id + 50) * Math.PI
                );
            }
        }
    }

    // Linha do tempo escalonada proporcionalmente à duração para garantir que TODOS os
    // cristais terminem de quebrar antes do fim da animação (mesmo em durações curtas).
    private static void drawIceRain(Matrix4fc m, VertexConsumer out, float age, int duration, float fade) {
        double firstDrop = duration * 0.10;
        double rainSpan = duration * 0.30;
        double fallDuration = Math.max(5.0, duration * 0.15);
        double shatterDuration = Math.max(6.0, duration * 0.18);

        for (int i = 0; i < CRYSTAL_COUNT; i++) {
            int id = 1000 + i * 19;

            double x = (rnd(id) * 2.0 - 1.0) * AREA_RADIUS;
            double z = (rnd(id + 1) * 2.0 - 1.0) * AREA_RADIUS;
            double length = 0.82 + rnd(id + 2) * 0.62;
            double width = 0.13 + rnd(id + 3) * 0.055;

            double start = firstDrop
                    + (i / (double) CRYSTAL_COUNT) * rainSpan
                    + rnd(id + 4) * (duration * 0.02);

            double elapsed = age - start;
            if (elapsed < 0.0) {
                continue;
            }

            double windX = (rnd(id + 5) - 0.5) * 0.7;
            double windZ = (rnd(id + 6) - 0.5) * 0.7;

            if (elapsed < fallDuration) {
                double p = clamp01(elapsed / fallDuration);
                double y = CLOUD_HEIGHT - 0.7 - p * (CLOUD_HEIGHT - 0.25);
                Vec3 position = new Vec3(
                        x + windX * p,
                        y,
                        z + windZ * p
                );
                Vec3 axis = new Vec3(windX * 0.25, -1.0, windZ * 0.25).normalize();

                PrayerVfxPrimitives.crystal(
                        m, out, position, axis, length, width,
                        0.43f, 0.77f, 1.0f, 0.9f * fade
                );
                continue;
            }

            // Garante que o cristal fique cravado por um tempo, mas sempre quebre antes de (duration - 1.0)
            double maxShatterStart = duration - shatterDuration - 1.0;
            double desiredShatterStart = start + fallDuration + duration * (0.14 + rnd(id + 7) * 0.10);
            double shatterStart = Math.max(fallDuration + 2.0, Math.min(desiredShatterStart - start, maxShatterStart - start));

            double ts = clamp01((elapsed - shatterStart) / shatterDuration);
            if (ts >= 1.0) {
                continue;
            }

            double crackle = PrayerVfxPrimitives.smooth(shatterStart - 3.0, shatterStart, elapsed);
            double flash = ts > 0.0 ? Math.max(0.0, 1.0 - ts * 5.0) : crackle;
            double tremble = ts > 0.0 ? 0.0 : crackle * 0.03;
            double mainScale = 1.0 - PrayerVfxPrimitives.smooth(0.0, 0.2, ts);

            float cr = tint(0.48f, flash);
            float cg = tint(0.81f, flash);
            float cb = tint(1.0f, flash);
            float alpha = 0.92f * fade;

            Vec3 axis = new Vec3(windX * 0.30, 1.0, windZ * 0.30).normalize();
            Vec3 baseOrigin = new Vec3(x + windX, GROUND_Y, z + windZ);
            Vec3 center = baseOrigin.add(0, length * 0.34, 0);

            if (ts <= 0.0) {
                PrayerVfxPrimitives.crystal(
                        m, out, center.add(shake(age, i, tremble)), axis, length, width,
                        cr, cg, cb, alpha
                );
                continue;
            }

            if (mainScale > 0.02) {
                PrayerVfxPrimitives.crystal(
                        m, out, center, axis, length * mainScale, width * mainScale,
                        cr, cg, cb, alpha
                );
            }

            shatterCrystal(m, out, center, axis, length, width, i, SHATTER_PIECES, ts, baseOrigin,
                    cr, cg, cb, alpha);
        }
    }

    private static void shatterCrystal(Matrix4fc m, VertexConsumer out, Vec3 center, Vec3 axis,
                                       double length, double width, int seed, int pieces, double ts,
                                       Vec3 origin, float r, float g, float b, float a) {
        double shrink = Math.pow(1.0 - ts, 0.8);
        if (shrink < 0.04) return;

        double ease = 1.0 - (1.0 - ts) * (1.0 - ts);
        double fall = 1.8 * ts * ts;
        float alpha = (float) (a * (1.0 - 0.5 * ts));

        Vec3 flat = new Vec3(center.x - origin.x, 0, center.z - origin.z);
        flat = flat.lengthSqr() > 1.0E-4 ? flat.normalize() : Vec3.ZERO;

        for (int p = 0; p < pieces; p++) {
            int s = seed * 37 + p * 11;

            Vec3 dir = new Vec3(
                    flat.x * 0.9 + (rnd(s) - 0.5) * 1.4,
                    0.25 + rnd(s + 1) * 0.9,
                    flat.z * 0.9 + (rnd(s + 2) - 0.5) * 1.4).normalize();
            double speed = 0.9 + rnd(s + 3) * 1.7;

            double along = pieces > 1 ? (p / (double) (pieces - 1) - 0.5) : 0.0;
            Vec3 start = center.add(axis.scale(along * length * 0.7));
            Vec3 pos = start.add(dir.scale(speed * ease)).add(0, -fall, 0);

            double spin = ts * (4.0 + rnd(s + 4) * 6.0);
            Vec3 pieceAxis = new Vec3(
                    axis.x + Math.sin(spin + s) * 0.9,
                    axis.y + Math.cos(spin * 1.3 + s) * 0.9,
                    axis.z + Math.sin(spin * 0.8 + s * 0.5) * 0.9).normalize();

            double pieceLength = length * (0.30 + rnd(s + 5) * 0.25) * shrink;
            double pieceWidth = width * (0.55 + rnd(s + 6) * 0.30) * shrink;

            PrayerVfxPrimitives.crystal(m, out, pos, pieceAxis, pieceLength, pieceWidth, r, g, b, alpha);
        }
    }

    private static Vec3 shake(float age, int i, double amount) {
        if (amount <= 0.0) return Vec3.ZERO;
        return new Vec3(Math.sin(age * 47.0 + i * 3.1) * amount, 0, Math.cos(age * 53.0 + i * 1.7) * amount);
    }

    private static float tint(float base, double flash) {
        return (float) (base + (1.0f - base) * flash * 0.75);
    }

    private static double rnd(int seed) {
        double x = Math.sin(seed * 12.9898 + 78.233) * 43758.5453;
        return x - Math.floor(x);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
