package com.github.ob_yekt.simpleskills.runes.armor;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.Bounds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.boltSegment;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.clamp;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.fract;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.lerp;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.mix;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.unit;

public final class StormArmorVfx {
    private StormArmorVfx() {}

    public static void submit(SubmitNodeCollector collector, PoseStack poseStack,
                              Bounds bounds, double time, int seed) {
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, vertices) ->
                draw(pose.pose(), vertices, bounds, time, seed));
    }

    private static void draw(Matrix4fc matrix, VertexConsumer out, Bounds bounds,
                             double time, int seed) {
        double scale = bounds.strokeScale();
        int boltCount = bounds.height() > 0.55 ? 4 : 3;

        for (int bolt = 0; bolt < boltCount; bolt++) {
            double boltTime = time * 0.34 + bolt * 1.37 + unit(seed, bolt * 17) * 2.0;
            long burst = (long) Math.floor(boltTime);
            double phase = fract(boltTime);
            long boltSeed = mix(burst * 0x9E3779B97F4A7C15L + seed * 31L + bolt * 131L);
            if (unit(boltSeed, 0) < 0.18) continue;

            double attack = Math.min(1.0, phase * 14.0);
            double decay = Math.pow(1.0 - phase, 0.65);
            double flicker = 0.82 + 0.18 * Math.sin(time * 9.5 + bolt * 4.1);
            double pulse = attack * decay * flicker;
            if (pulse < 0.05) continue;

            SurfacePoint surface = surfacePoint(bounds, boltSeed, 0.007 * scale);
            drawArmorBolt(matrix, out, surface, bounds, time, boltSeed, pulse, scale);
        }
    }

    private static void drawArmorBolt(Matrix4fc matrix, VertexConsumer out, SurfacePoint surface,
                                      Bounds bounds, double time, long seed, double pulse, double scale) {
        Vec3 a = surface.tangentA();
        Vec3 b = surface.tangentB();
        Vec3 n = surface.normal();
        double maxSpan = Math.max(bounds.width(), Math.max(bounds.height(), bounds.depth()));
        double travel = clamp(maxSpan * (0.34 + unit(seed, 4) * 0.24), 0.08, 0.38);
        double sign = unit(seed, 5) < 0.5 ? -1.0 : 1.0;

        Vec3 start = surface.point();
        Vec3 previous = start;
        Vec3 branchA = null;
        Vec3 branchB = null;

        int steps = 7;
        for (int step = 1; step <= steps; step++) {
            double t = step / (double) steps;
            double envelope = Math.sin(Math.PI * t);
            double jagged = (unit(seed, 10 + step) - 0.5) * 0.13 * scale * (0.45 + envelope)
                    + Math.sin(time * 2.4 + step * 2.7 + seed) * 0.012 * scale;
            double cross = (unit(seed, 30 + step) - 0.5) * 0.075 * scale * envelope;
            double lift = Math.sin(Math.PI * t) * (0.012 + unit(seed, 50 + step) * 0.014) * scale;

            Vec3 next = start
                    .add(a.scale(sign * travel * t))
                    .add(b.scale(jagged + cross))
                    .add(n.scale(lift));

            if (step == 2) branchA = next;
            if (step == 4) branchB = next;

            boltSegment(matrix, out, previous, next, pulse, scale * (1.0 - t * 0.55));
            previous = next;
        }

        if (branchA != null) {
            drawSubBranch(matrix, out, branchA, a, b, n, sign, seed, 71, pulse * 0.85, scale * 0.72);
        }
        if (branchB != null && unit(seed, 89) > 0.35) {
            drawSubBranch(matrix, out, branchB, a, b, n, -sign * 0.7, seed, 97, pulse * 0.70, scale * 0.58);
        }

        double nodeRadius = 0.0085 * scale * pulse;
        PrayerVfxGeometry.ellipsoid(matrix, out, start, new Vec3(nodeRadius, nodeRadius, nodeRadius),
                4, 6, 0.65f, 0.94f, 1.0f, (float) (0.78 * pulse), time * 0.4);
        PrayerVfxGeometry.ellipsoid(matrix, out, previous,
                new Vec3(nodeRadius * 0.85, nodeRadius * 0.85, nodeRadius * 0.85),
                4, 6, 0.85f, 0.98f, 1.0f, (float) (0.85 * pulse), -time * 0.4);
    }

    private static void drawSubBranch(Matrix4fc matrix, VertexConsumer out, Vec3 origin,
                                      Vec3 a, Vec3 b, Vec3 n, double sign,
                                      long seed, int salt, double pulse, double scale) {
        double dirB = (unit(seed, salt) < 0.5 ? -1.0 : 1.0) * (0.045 + unit(seed, salt + 1) * 0.055) * scale;
        double dirA = sign * (0.04 + unit(seed, salt + 2) * 0.065) * scale;
        Vec3 mid = origin.add(b.scale(dirB * 0.55)).add(a.scale(dirA * 0.45)).add(n.scale(0.006 * scale));
        Vec3 end = origin.add(b.scale(dirB)).add(a.scale(dirA));
        boltSegment(matrix, out, origin, mid, pulse, scale);
        boltSegment(matrix, out, mid, end, pulse * 0.75, scale * 0.75);
    }

    private static SurfacePoint surfacePoint(Bounds bounds, long seed, double offset) {
        double x = lerp(bounds.minX(), bounds.maxX(), unit(seed, 1));
        double y = lerp(bounds.minY(), bounds.maxY(), unit(seed, 2));
        double z = lerp(bounds.minZ(), bounds.maxZ(), unit(seed, 3));
        int face = (int) (unit(seed, 4) * 6.0);
        Vec3 normal;
        Vec3 tangentA;
        Vec3 tangentB;
        switch (face) {
            case 0 -> {
                x = bounds.minX() - offset;
                normal = new Vec3(-1, 0, 0);
                tangentA = new Vec3(0, 1, 0);
                tangentB = new Vec3(0, 0, 1);
            }
            case 1 -> {
                x = bounds.maxX() + offset;
                normal = new Vec3(1, 0, 0);
                tangentA = new Vec3(0, 1, 0);
                tangentB = new Vec3(0, 0, 1);
            }
            case 2 -> {
                y = bounds.minY() - offset;
                normal = new Vec3(0, -1, 0);
                tangentA = new Vec3(1, 0, 0);
                tangentB = new Vec3(0, 0, 1);
            }
            case 3 -> {
                y = bounds.maxY() + offset;
                normal = new Vec3(0, 1, 0);
                tangentA = new Vec3(1, 0, 0);
                tangentB = new Vec3(0, 0, 1);
            }
            case 4 -> {
                z = bounds.minZ() - offset;
                normal = new Vec3(0, 0, -1);
                tangentA = new Vec3(0, 1, 0);
                tangentB = new Vec3(1, 0, 0);
            }
            default -> {
                z = bounds.maxZ() + offset;
                normal = new Vec3(0, 0, 1);
                tangentA = new Vec3(0, 1, 0);
                tangentB = new Vec3(1, 0, 0);
            }
        }
        return new SurfacePoint(new Vec3(x, y, z), normal, tangentA, tangentB);
    }

    private record SurfacePoint(Vec3 point, Vec3 normal, Vec3 tangentA, Vec3 tangentB) {}
}
