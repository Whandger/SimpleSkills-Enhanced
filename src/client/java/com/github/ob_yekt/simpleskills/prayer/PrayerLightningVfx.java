package com.github.ob_yekt.simpleskills.prayer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

/** Procedural 3D geometry and animation for the Lightning Bolt spell. */
final class PrayerLightningVfx {
    private PrayerLightningVfx() {}

    static void draw(Matrix4fc matrix, VertexConsumer vertices, Vec3 hand, Vec3 destination,
                     float age, int duration) {
        float fade = fadeOut(age, duration, 5, 5);
        long seed = (long) Math.floor(age * 3.0f);
        double charge = smooth(0, 6, age);
        Vec3 wrist = hand.add(0, -0.12, 0);

        // The charge is concentrated at a hand-sized volume instead of the player's centre.
        PrayerVfxGeometry.wireSphere(matrix, vertices, wrist, 0.10 + charge * 0.16, 12, age * 0.28,
                0.20f, 0.58f, 1.0f, 0.20f * fade, 0.012);
        for (int arc = 0; arc < 5; arc++) {
            double angle = age * 0.39 + arc * Math.PI * 2 / 5;
            Vec3 start = wrist.add(Math.cos(angle) * 0.13, Math.sin(angle * 1.7) * 0.13,
                    Math.sin(angle) * 0.13);
            Vec3 end = wrist.add(Math.cos(angle + 1.3) * (0.22 + charge * 0.12),
                    Math.sin(angle * 2.1 + 0.7) * 0.19,
                    Math.sin(angle + 1.3) * (0.22 + charge * 0.12));
            boltSegment(matrix, vertices, start, end, 0.045,
                    0.72f, 0.94f, 1.0f, 0.72f * fade);
        }

        Vec3 target = destination;
        if (target.subtract(hand).lengthSqr() < 0.04) target = hand.add(0, 0, -8);
        float travel = (float) smooth(5, Math.max(10, duration * 0.52), age);
        Vec3 tip = hand.lerp(target, travel);
        List<Vec3> main = jagged(hand, tip, 17, seed, 0.18 + travel * 0.12);
        for (int i = 0; i < main.size() - 1; i++) {
            double taper = 1.0 - i / (double) (main.size() - 1) * 0.40;
            double pulse = 0.88 + 0.12 * Math.sin(age * 1.8 + i * 0.9);
            boltSegment(matrix, vertices, main.get(i), main.get(i + 1), 0.13 * taper * pulse,
                    0.10f, 0.42f, 1.0f, 0.15f * fade);
            boltSegment(matrix, vertices, main.get(i), main.get(i + 1), 0.047 * taper * pulse,
                    0.34f, 0.78f, 1.0f, 0.72f * fade);
            boltSegment(matrix, vertices, main.get(i), main.get(i + 1), 0.016 * taper,
                    0.94f, 0.99f, 1.0f, fade);
        }

        // Branches are rooted in the main channel and change shape several times per tick.
        int branches = travel > 0.38f ? 7 : travel > 0.18f ? 3 : 0;
        for (int branch = 0; branch < branches; branch++) {
            int split = Math.min(main.size() - 2, 2 + branch * 2);
            Vec3 root = main.get(split);
            double sign = (branch & 1) == 0 ? 1 : -1;
            double length = (0.48 + (branch % 3) * 0.22) * travel;
            Vec3 branchEnd = root.add(sign * length,
                    (branch % 3 - 1) * 0.22 * travel,
                    sign * (0.34 + (branch % 2) * 0.26) * travel);
            List<Vec3> fork = jagged(root, branchEnd, 5, seed + branch * 193L, 0.09 * travel);
            for (int i = 0; i < fork.size() - 1; i++) {
                boltSegment(matrix, vertices, fork.get(i), fork.get(i + 1), 0.072 * (1 - i * 0.11),
                        0.14f, 0.53f, 1.0f, 0.27f * fade);
                boltSegment(matrix, vertices, fork.get(i), fork.get(i + 1), 0.022 * (1 - i * 0.1),
                        0.68f, 0.91f, 1.0f, 0.78f * fade);
            }
        }

        float impact = (float) smooth(duration * 0.42, duration * 0.64, age);
        if (impact > 0) {
            float bloom = (float) (Math.exp(-impact * 2.2) * fade);
            double radius = 0.35 + impact * 0.45;
            PrayerVfxGeometry.ellipsoid(matrix, vertices, target, new Vec3(radius, radius * 1.3, radius),
                    8, 14, 0.10f, 0.48f, 1.0f, 0.13f * bloom, age * 0.08);
            PrayerVfxGeometry.wireSphere(matrix, vertices, target, radius * 1.3, 18, age * 0.16,
                    0.16f, 0.60f, 1.0f, 0.58f * bloom, 0.018);
            PrayerVfxGeometry.torus(matrix, vertices, target.add(0, 0.08, 0), 0.18 + impact * 0.78,
                    0.022, 48, age * 0.07, 0.33f, 0.79f, 1.0f, 0.88f * bloom);
            for (int ray = 0; ray < 12; ray++) {
                double angle = age * 0.08 + ray * Math.PI / 6;
                double length = 0.55 + (ray % 3) * 0.19;
                Vec3 end = target.add(Math.cos(angle) * length * impact,
                        (ray % 4 - 1) * 0.20 * impact,
                        Math.sin(angle) * length * impact);
                List<Vec3> impactFork = jagged(target, end, 4, seed + ray * 71L, 0.08 * impact);
                for (int i = 0; i < impactFork.size() - 1; i++) {
                    boltSegment(matrix, vertices, impactFork.get(i), impactFork.get(i + 1), 0.055,
                            0.24f, 0.68f, 1.0f, 0.6f * bloom);
                    boltSegment(matrix, vertices, impactFork.get(i), impactFork.get(i + 1), 0.018,
                            0.90f, 0.98f, 1.0f, bloom);
                }
            }
        }
    }

    private static void boltSegment(Matrix4fc matrix, VertexConsumer vertices, Vec3 start, Vec3 end,
                                    double radius, float red, float green, float blue, float alpha) {
        PrayerVfxGeometry.tube(matrix, vertices, start, end, radius, red, green, blue, alpha);
    }

    private static List<Vec3> jagged(Vec3 from, Vec3 to, int segments, long seed, double amplitude) {
        java.util.Random random = new java.util.Random(seed);
        List<Vec3> points = new ArrayList<>(segments + 1);
        points.add(from);
        Vec3 direction = to.subtract(from);
        Vec3 side = direction.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 0.01) side = new Vec3(1, 0, 0);
        side = side.normalize();
        Vec3 second = direction.normalize().cross(side).normalize();
        for (int i = 1; i < segments; i++) {
            double progress = i / (double) segments;
            double jitter = amplitude * (0.35 + 0.65 * Math.sin(Math.PI * progress));
            points.add(from.add(direction.scale(progress))
                    .add(side.scale((random.nextDouble() * 2 - 1) * jitter))
                    .add(second.scale((random.nextDouble() * 2 - 1) * jitter)));
        }
        points.add(to);
        return points;
    }

    private static double smooth(double start, double end, double value) {
        double t = clamp((value - start) / Math.max(1.0E-5, end - start));
        return t * t * (3 - 2 * t);
    }

    private static float fadeOut(float age, int duration, int fadeIn, int fadeOut) {
        return (float) Math.min(smooth(0, fadeIn, age), 1 - smooth(duration - fadeOut, duration, age));
    }

    private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }
}
