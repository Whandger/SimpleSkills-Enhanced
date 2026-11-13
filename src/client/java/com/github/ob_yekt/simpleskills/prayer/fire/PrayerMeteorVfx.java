package com.github.ob_yekt.simpleskills.prayer.fire;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerMeteorVfx {
    private PrayerMeteorVfx() {}

    // 30. METEOR — falling irregular rock, molten fissures, corkscrew exhaust, and staged shockwave impact.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        double flightTicks = duration * 0.64;
        double flight = clamp(age / flightTicks);
        double eased = flight * flight;
        Vec3 center = new Vec3(-4.6 * (1 - flight), 18.0 * (1 - eased) + 0.42 * eased,
                -2.7 * (1 - flight));
        double impact = PrayerVfxPrimitives.smooth(flightTicks, duration - 3, age);
        double bodyFade = 1 - impact * 0.86;
        float fade = PrayerVfxPrimitives.fade(age, duration, 2, 3);
        double tumble = age * 0.11;

        meteorTrail(m, out, center, age, fade * (float) bodyFade);
        meteorRock(m, out, center, 0.76, tumble, fade * (float) bodyFade);
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(0.48, 0.57, 0.46),
                8, 14, 1.0f, 0.24f, 0.025f, 0.20f * fade * (float) bodyFade, -tumble * 0.6);

        if (impact > 0) {
            double ringRadius = 0.35 + impact * 4.4;
            float impactAlpha = (float) ((1 - impact) * fade);
            PrayerVfxGeometry.torus(m, out, new Vec3(0, 0.075, 0), ringRadius, 0.075 * (1 - impact * 0.45),
                    112, age * 0.045, 1.0f, 0.32f, 0.035f, 0.95f * impactAlpha);
            PrayerVfxGeometry.torus(m, out, new Vec3(0, 0.11, 0), ringRadius * 0.72, 0.026,
                    96, -age * 0.065, 1.0f, 0.72f, 0.12f, 0.88f * impactAlpha);
            PrayerVfxGeometry.wireSphere(m, out, new Vec3(0, 0.85, 0), 0.35 + impact * 2.35,
                    18, age * 0.07, 1.0f, 0.35f, 0.045f, 0.24f * impactAlpha, 0.035);
            PrayerVfxGeometry.ellipsoid(m, out, new Vec3(0, 0.65 + impact * 0.72, 0),
                    new Vec3(0.45 + impact * 1.2, 0.62 + impact * 1.6, 0.45 + impact * 1.2),
                    9, 16, 1.0f, 0.25f, 0.035f, 0.075f * impactAlpha, age * 0.06);

            // Twelve outward tongues make the impact plume tall and directional; fragments tumble independently.
            for (int ray = 0; ray < 12; ray++) {
                double angle = ray * Math.PI * 2 / 12 + age * 0.018;
                double reach = impact * (1.1 + (ray % 4) * 0.42);
                Vec3 base = new Vec3(Math.cos(angle) * 0.18, 0.18, Math.sin(angle) * 0.18);
                Vec3 knee = new Vec3(Math.cos(angle) * reach * 0.7,
                        0.45 + Math.sin(impact * Math.PI) * (0.8 + (ray % 3) * 0.34),
                        Math.sin(angle) * reach * 0.7);
                Vec3 tip = new Vec3(Math.cos(angle) * reach, 0.12 + impact * 0.32,
                        Math.sin(angle) * reach);
                PrayerVfxGeometry.taperedTube(m, out, base, knee, 0.16 * (1 - impact * 0.5), 0.075,
                        1.0f, 0.25f, 0.025f, 0.52f * impactAlpha);
                PrayerVfxGeometry.taperedTube(m, out, knee, tip, 0.075, 0.003,
                        1.0f, 0.75f, 0.17f, 0.82f * impactAlpha);
                if (ray % 2 == 0) {
                    double fragmentDistance = impact * (1.2 + (ray % 5) * 0.36);
                    Vec3 fragment = new Vec3(Math.cos(angle) * fragmentDistance,
                            0.2 + Math.sin(impact * Math.PI) * (0.5 + (ray % 3) * 0.23),
                            Math.sin(angle) * fragmentDistance);
                    meteorRock(m, out, fragment, 0.16 + (ray % 3) * 0.035,
                            -age * 0.17 + ray, impactAlpha);
                }
            }
        }
    }

    private static void meteorTrail(Matrix4fc m, VertexConsumer out, Vec3 center, float age, float alpha) {
        for (int strand = 0; strand < 4; strand++) {
            Vec3 previousOuter = null;
            Vec3 previousCore = null;
            for (int step = 0; step <= 20; step++) {
                double t = step / 20.0;
                double angle = age * (0.16 + strand * 0.015) + t * Math.PI * 3.7 + strand * Math.PI / 2;
                double radius = 0.10 + t * 0.38;
                Vec3 point = center.add(Math.cos(angle) * radius,
                        t * (2.2 + strand * 0.18), Math.sin(angle) * radius);
                if (previousOuter != null) {
                    double width = (1 - t) * (0.12 + strand * 0.018);
                    PrayerVfxGeometry.tube(m, out, previousOuter, point, width * 1.8,
                            0.72f, 0.10f, 0.012f, 0.16f * alpha);
                    PrayerVfxGeometry.tube(m, out, previousOuter, point, width,
                            1.0f, 0.32f + strand * 0.08f, 0.025f, 0.78f * alpha);
                    PrayerVfxGeometry.tube(m, out, previousCore, point, width * 0.28,
                            1.0f, 0.82f, 0.20f, 0.84f * alpha);
                }
                previousOuter = point;
                previousCore = point;
            }
        }
    }

    private static void meteorRock(Matrix4fc m, VertexConsumer out, Vec3 center,
                                   double scale, double tumble, float alpha) {
        int latitudes = 8;
        int longitudes = 16;
        Vec3[][] rings = new Vec3[latitudes + 1][longitudes];
        for (int latitude = 0; latitude <= latitudes; latitude++) {
            double phi = -Math.PI / 2 + Math.PI * latitude / latitudes;
            for (int longitude = 0; longitude < longitudes; longitude++) {
                double theta = Math.PI * 2 * longitude / longitudes;
                double roughness = 0.83 + 0.10 * Math.sin(theta * 3 + latitude * 1.7)
                        + 0.08 * Math.cos(theta * 5 - latitude * 2.1);
                Vec3 point = new Vec3(Math.cos(phi) * Math.cos(theta) * scale * roughness,
                        Math.sin(phi) * scale * 0.82 * roughness,
                        Math.cos(phi) * Math.sin(theta) * scale * 0.93 * roughness);
                rings[latitude][longitude] = center.add(rotate(point, tumble * 0.73, tumble, tumble * 0.37));
            }
        }
        for (int latitude = 0; latitude < latitudes; latitude++) {
            for (int longitude = 0; longitude < longitudes; longitude++) {
                int next = (longitude + 1) % longitudes;
                float facet = (float) (0.72 + 0.24 * (0.5 + 0.5 * Math.sin(latitude * 2.2 + longitude * 3.4 + tumble)));
                float red = 0.19f * facet;
                float green = 0.11f * facet;
                float blue = 0.075f * facet;
                PrayerVfxGeometry.quad(m, out, rings[latitude][longitude], rings[latitude][next],
                        rings[latitude + 1][next], rings[latitude + 1][longitude], red, green, blue, 0.97f * alpha);
            }
        }

        // Molten veins crawl over the irregular 3D shell and keep the meteor legible while tumbling.
        for (int crack = 0; crack < 9; crack++) {
            double baseAngle = crack * Math.PI * 2 / 9 + tumble * 0.42;
            Vec3 previous = null;
            for (int step = 0; step <= 9; step++) {
                double phi = -1.12 + step * 0.245;
                double theta = baseAngle + 0.12 * Math.sin(step * 1.8 + crack);
                double roughness = 0.83 + 0.10 * Math.sin(theta * 3 + Math.abs(phi) * 5)
                        + 0.08 * Math.cos(theta * 5 - phi * 4);
                Vec3 point = new Vec3(Math.cos(phi) * Math.cos(theta) * scale * roughness * 1.012,
                        Math.sin(phi) * scale * 0.82 * roughness * 1.012,
                        Math.cos(phi) * Math.sin(theta) * scale * 0.93 * roughness * 1.012);
                point = center.add(rotate(point, tumble * 0.73, tumble, tumble * 0.37));
                if (previous != null) {
                    PrayerVfxGeometry.tube(m, out, previous, point, 0.030 * scale,
                            1.0f, 0.18f, 0.012f, 0.76f * alpha);
                    PrayerVfxGeometry.tube(m, out, previous, point, 0.010 * scale,
                            1.0f, 0.82f, 0.22f, 0.98f * alpha);
                }
                previous = point;
            }
        }

        // Uneven rock spurs give the shell an angular silhouette rather than a smooth orb.
        for (int spur = 0; spur < 6; spur++) {
            double angle = spur * Math.PI / 3 + tumble;
            Vec3 base = center.add(Math.cos(angle) * scale * 0.50, Math.sin(spur * 1.7) * scale * 0.28,
                    Math.sin(angle) * scale * 0.50);
            Vec3 tip = base.add(Math.cos(angle) * scale * 0.31, (spur % 2 == 0 ? 1 : -1) * scale * 0.26,
                    Math.sin(angle) * scale * 0.31);
            PrayerVfxGeometry.taperedTube(m, out, base, tip, scale * 0.14, 0.002,
                    0.23f, 0.12f, 0.075f, 0.95f * alpha);
        }
    }

    private static Vec3 rotate(Vec3 point, double pitch, double yaw, double roll) {
        double cos = Math.cos(pitch), sin = Math.sin(pitch);
        double y = point.y * cos - point.z * sin;
        double z = point.y * sin + point.z * cos;
        double x = point.x;
        cos = Math.cos(yaw); sin = Math.sin(yaw);
        double nextX = x * cos + z * sin;
        z = -x * sin + z * cos;
        x = nextX;
        cos = Math.cos(roll); sin = Math.sin(roll);
        nextX = x * cos - y * sin;
        y = x * sin + y * cos;
        return new Vec3(nextX, y, z);
    }

    private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }
}
