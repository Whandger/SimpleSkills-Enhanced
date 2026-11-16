package com.github.ob_yekt.simpleskills.prayer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

/** Shared procedural primitives and math utilities for Prayer ability VFX classes. */
public final class PrayerVfxPrimitives {
    public static final Vec3 UP = new Vec3(0, 1, 0);

    private PrayerVfxPrimitives() {}

    public static void soulFigure(Matrix4fc m, VertexConsumer out, Vec3 center, double scale,
                           double rotation, float alpha) {
        Vec3 head = center.add(0, scale * 0.72, 0);
        PrayerVfxGeometry.ellipsoid(m, out, head, new Vec3(scale * 0.28, scale * 0.34, scale * 0.25),
                7, 10, 0.64f, 0.30f, 0.94f, 0.72f * alpha, rotation);
        Vec3 chest = center.add(0, scale * 0.15, 0);
        PrayerVfxGeometry.ellipsoid(m, out, chest, new Vec3(scale * 0.43, scale * 0.52, scale * 0.31),
                8, 12, 0.40f, 0.12f, 0.77f, 0.56f * alpha, -rotation * 0.7);
        for (int ribbon = 0; ribbon < 3; ribbon++) {
            double angle = rotation + ribbon * Math.PI * 2 / 3;
            Vec3 previous = center.add(Math.cos(angle) * scale * 0.36, scale * 0.38,
                    Math.sin(angle) * scale * 0.36);
            for (int step = 1; step <= 7; step++) {
                double t = step / 7.0;
                double a = angle + t * Math.PI * 1.5;
                Vec3 next = center.add(Math.cos(a) * scale * (0.34 * (1 - t) + 0.06),
                        scale * (0.38 - t * 1.25), Math.sin(a) * scale * (0.34 * (1 - t) + 0.06));
                PrayerVfxGeometry.taperedTube(m, out, previous, next, scale * 0.07 * (1 - t * 0.65),
                        0.003, 0.7f, 0.35f, 1.0f, alpha * (float) (0.72 * (1 - t * 0.5)));
                previous = next;
            }
        }
        ringPlane(m, out, center, new Vec3(0.3, 1, 0.2), scale * 0.63, scale * 0.035,
                rotation, 0.80f, 0.40f, 1.0f, 0.66f * alpha);
    }

    public static void flamePetal(Matrix4fc m, VertexConsumer out, Vec3 base, Vec3 knee, Vec3 tip,
                           double width, float alpha) {
        if (alpha <= 0.005f) return;
        PrayerVfxGeometry.taperedTube(m, out, base, knee, width, width * 0.62,
                1.0f, 0.20f, 0.018f, 0.48f * alpha);
        PrayerVfxGeometry.taperedTube(m, out, knee, tip, width * 0.62, 0.003,
                1.0f, 0.48f, 0.055f, 0.82f * alpha);
        PrayerVfxGeometry.tube(m, out, base, tip, width * 0.20,
                1.0f, 0.88f, 0.28f, 0.86f * alpha);
    }

    public static void circleSigil(Matrix4fc m, VertexConsumer out, Vec3 center, double radius,
                            double rotation, float alpha, float r, float g, float b) {
        ringPlane(m, out, center, UP, radius, 0.024, rotation, r, g, b, 0.92f * alpha);
        ringPlane(m, out, center.add(0, 0.025, 0), UP, radius * 0.76, 0.014,
                -rotation * 1.5, 1.0f, 0.93f, 0.69f, 0.76f * alpha);
        ringPlane(m, out, center.add(0, 0.035, 0), UP, radius * 0.35, 0.018,
                rotation * 0.8, r, g, b, 0.82f * alpha);
        for (int mark = 0; mark < 24; mark++) {
            double angle = mark * Math.PI * 2 / 24 + rotation;
            double inner = radius * (mark % 3 == 0 ? 0.81 : 0.88);
            double outer = radius * (mark % 3 == 0 ? 1.04 : 0.96);
            Vec3 from = center.add(Math.cos(angle) * inner, 0.06, Math.sin(angle) * inner);
            Vec3 to = center.add(Math.cos(angle + 0.012) * outer, 0.06, Math.sin(angle + 0.012) * outer);
            PrayerVfxGeometry.tube(m, out, from, to, mark % 3 == 0 ? 0.018 : 0.01,
                    r, g, b, (mark % 3 == 0 ? 0.84f : 0.5f) * alpha);
        }
        for (int spoke = 0; spoke < 8; spoke++) {
            double angle = spoke * Math.PI / 4 + rotation;
            Vec3 from = center.add(Math.cos(angle) * radius * 0.34, 0.045, Math.sin(angle) * radius * 0.34);
            Vec3 to = center.add(Math.cos(angle + Math.PI / 4) * radius * 0.71, 0.045,
                    Math.sin(angle + Math.PI / 4) * radius * 0.71);
            PrayerVfxGeometry.tube(m, out, from, to, 0.01, r, g, b, 0.64f * alpha);
        }
    }

    public static void runeHalo(Matrix4fc m, VertexConsumer out, Vec3 center, double scale,
                         double rotation, float alpha, float r, float g, float b) {
        Vec3 u = new Vec3(Math.cos(rotation), 0, Math.sin(rotation));
        Vec3 v = UP;
        Vec3[] p = {
                runePoint(center, u, v, 0, scale), runePoint(center, u, v, -0.55 * scale, 0.3 * scale),
                runePoint(center, u, v, 0.55 * scale, 0.3 * scale), runePoint(center, u, v, -0.42 * scale, -0.45 * scale),
                runePoint(center, u, v, 0.42 * scale, -0.45 * scale), runePoint(center, u, v, 0, -scale),
                runePoint(center, u, v, 0, 0)
        };
        int[][] edges = {{0, 1}, {0, 2}, {1, 2}, {1, 3}, {2, 4}, {3, 4}, {3, 5}, {4, 5}, {0, 6}, {6, 5}};
        for (int[] edge : edges) {
            PrayerVfxGeometry.tube(m, out, p[edge[0]], p[edge[1]], 0.019, r, g, b, alpha);
            PrayerVfxGeometry.tube(m, out, p[edge[0]], p[edge[1]], 0.006,
                    1.0f, 0.98f, 0.83f, alpha * 0.82f);
        }
    }

    public static void starburst(Matrix4fc m, VertexConsumer out, Vec3 center, Vec3 normal, double radius,
                          float alpha, float r, float g, float b) {
        Vec3[] basis = basis(normal);
        for (int ray = 0; ray < 12; ray++) {
            double angle = ray * Math.PI / 6;
            Vec3 dir = basis[0].scale(Math.cos(angle)).add(basis[1].scale(Math.sin(angle)));
            Vec3 tip = center.add(dir.scale(radius * (ray % 3 == 0 ? 1.0 : 0.72)));
            PrayerVfxGeometry.taperedTube(m, out, center, tip, 0.025, 0.002, r, g, b, alpha);
        }
        ringPlane(m, out, center, normal, radius * 0.48, 0.025, 0.0, r, g, b, alpha * 0.9f);
    }

    public static void crystal(Matrix4fc m, VertexConsumer out, Vec3 center, Vec3 axis,
                        double length, double radius, float r, float g, float b, float alpha) {
        if (length <= 0 || radius <= 0) return;
        Vec3 n = safeNormalize(axis, UP);
        Vec3[] frame = basis(n);
        Vec3 upper = center.add(n.scale(length * 0.5));
        Vec3 lower = center.subtract(n.scale(length * 0.5));
        Vec3[] ring = new Vec3[6];
        for (int i = 0; i < ring.length; i++) {
            double a = i * Math.PI * 2 / ring.length;
            double ripple = 0.88 + 0.12 * Math.sin(a * 3 + length * 7);
            ring[i] = center.add(frame[0].scale(Math.cos(a) * radius * ripple))
                    .add(frame[1].scale(Math.sin(a) * radius * ripple));
        }
        for (int i = 0; i < ring.length; i++) {
            int next = (i + 1) % ring.length;
            float shade = 0.72f + 0.28f * (float) ((i % 3) / 2.0);
            PrayerVfxGeometry.triangle(m, out, ring[i], ring[next], upper,
                    r * shade, g * shade, b * shade, alpha);
            PrayerVfxGeometry.triangle(m, out, ring[next], ring[i], lower,
                    r * shade * 0.68f, g * shade * 0.75f, b * shade, alpha * 0.84f);
        }
        PrayerVfxGeometry.tube(m, out, lower, upper, 0.008,
                Math.min(1, r + 0.16f), Math.min(1, g + 0.16f), Math.min(1, b + 0.12f), alpha * 0.82f);
    }

    public static void ringPlane(Matrix4fc m, VertexConsumer out, Vec3 center, Vec3 normal,
                          double radius, double thickness, double rotation,
                          float r, float g, float b, float alpha) {
        if (radius <= 0 || thickness <= 0) return;
        Vec3[] frame = basis(normal);
        Vec3 previous = center.add(frame[0].scale(radius));
        int segments = 48;
        for (int i = 1; i <= segments; i++) {
            double angle = Math.PI * 2 * i / segments + rotation;
            Vec3 next = center.add(frame[0].scale(Math.cos(angle) * radius))
                    .add(frame[1].scale(Math.sin(angle) * radius));
            PrayerVfxGeometry.tube(m, out, previous, next, thickness, r, g, b, alpha);
            previous = next;
        }
    }

    public static void bolt(Matrix4fc m, VertexConsumer out, Vec3 start, Vec3 end, long seed,
                     int segments, double amplitude, float alpha) {
        List<Vec3> points = jagged(start, end, segments, seed, amplitude);
        for (int i = 0; i < points.size() - 1; i++) {
            double taper = 1 - i / (double) (points.size() - 1) * 0.38;
            PrayerVfxGeometry.tube(m, out, points.get(i), points.get(i + 1), 0.09 * taper,
                    0.12f, 0.48f, 1.0f, 0.18f * alpha);
            PrayerVfxGeometry.tube(m, out, points.get(i), points.get(i + 1), 0.034 * taper,
                    0.44f, 0.82f, 1.0f, 0.82f * alpha);
            PrayerVfxGeometry.tube(m, out, points.get(i), points.get(i + 1), 0.012 * taper,
                    0.96f, 1.0f, 1.0f, alpha);
        }
    }

    public static List<Vec3> jagged(Vec3 from, Vec3 to, int segments, long seed, double amplitude) {
        Random random = new Random(seed);
        List<Vec3> points = new ArrayList<>(segments + 1);
        Vec3 direction = to.subtract(from);
        Vec3[] frame = basis(direction);
        points.add(from);
        for (int i = 1; i < segments; i++) {
            double t = i / (double) segments;
            double wobble = amplitude * (0.32 + 0.68 * Math.sin(Math.PI * t));
            points.add(from.add(direction.scale(t))
                    .add(frame[0].scale((random.nextDouble() * 2 - 1) * wobble))
                    .add(frame[1].scale((random.nextDouble() * 2 - 1) * wobble)));
        }
        points.add(to);
        return points;
    }

    public static Vec3[] basis(Vec3 direction) {
        Vec3 axis = safeNormalize(direction, UP);
        Vec3 ref = Math.abs(axis.y) < 0.92 ? UP : new Vec3(1, 0, 0);
        Vec3 u = safeNormalize(axis.cross(ref), new Vec3(1, 0, 0));
        Vec3 v = safeNormalize(axis.cross(u), new Vec3(0, 0, 1));
        return new Vec3[] {u, v};
    }

    private static Vec3 runePoint(Vec3 center, Vec3 u, Vec3 v, double x, double y) {
        return center.add(u.scale(x)).add(v.scale(y));
    }

    public static Vec3 safeNormalize(Vec3 value, Vec3 fallback) {
        return value.lengthSqr() < 1.0E-8 ? fallback : value.normalize();
    }

    public static double smooth(double start, double end, double value) {
        double t = clamp((value - start) / Math.max(1.0E-5, end - start));
        return t * t * (3 - 2 * t);
    }

    public static float fade(float age, int duration, int fadeIn, int fadeOut) {
        return (float) Math.min(smooth(0, fadeIn, age), 1 - smooth(duration - fadeOut, duration, age));
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
