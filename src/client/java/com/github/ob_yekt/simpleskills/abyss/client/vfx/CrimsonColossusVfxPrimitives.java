package com.github.ob_yekt.simpleskills.abyss.client.vfx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class CrimsonColossusVfxPrimitives {
    private CrimsonColossusVfxPrimitives() {}

    public static void addDoubleQuad(Matrix4fc m, VertexConsumer out,
                                     Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                                     float r, float g, float b, float a) {
        out.addVertex(m, (float) p0.x, (float) p0.y, (float) p0.z).setColor(r, g, b, a);
        out.addVertex(m, (float) p1.x, (float) p1.y, (float) p1.z).setColor(r, g, b, a);
        out.addVertex(m, (float) p2.x, (float) p2.y, (float) p2.z).setColor(r, g, b, a);
        out.addVertex(m, (float) p3.x, (float) p3.y, (float) p3.z).setColor(r, g, b, a);

        out.addVertex(m, (float) p3.x, (float) p3.y, (float) p3.z).setColor(r, g, b, a);
        out.addVertex(m, (float) p2.x, (float) p2.y, (float) p2.z).setColor(r, g, b, a);
        out.addVertex(m, (float) p1.x, (float) p1.y, (float) p1.z).setColor(r, g, b, a);
        out.addVertex(m, (float) p0.x, (float) p0.y, (float) p0.z).setColor(r, g, b, a);
    }

    public static void drawBeamSegment(Matrix4fc m, VertexConsumer out,
                                       Vec3 start, Vec3 end,
                                       double radiusStart, double radiusEnd,
                                       float r, float g, float b, float a) {
        Vec3 delta = end.subtract(start);
        if (delta.lengthSqr() < 1.0E-6D) return;
        Vec3 dir = delta.normalize();

        Vec3 ref = Math.abs(dir.y) < 0.92D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 side1 = dir.cross(ref).normalize();
        Vec3 side2 = dir.cross(side1).normalize();
        Vec3 diag1 = side1.add(side2).normalize();
        Vec3 diag2 = side1.subtract(side2).normalize();

        Vec3[] axes = {side1, side2, diag1, diag2};
        for (Vec3 axis : axes) {
            Vec3 s0 = start.subtract(axis.scale(radiusStart));
            Vec3 s1 = start.add(axis.scale(radiusStart));
            Vec3 e1 = end.add(axis.scale(radiusEnd));
            Vec3 e0 = end.subtract(axis.scale(radiusEnd));
            addDoubleQuad(m, out, s0, s1, e1, e0, r, g, b, a);
        }
    }

    public static void drawLightningArc(Matrix4fc m, VertexConsumer out,
                                        Vec3 start, Vec3 end,
                                        int segments, double jitter,
                                        double coreRadius, float age, int seed, float alpha) {
        Vec3 delta = end.subtract(start);
        if (delta.lengthSqr() < 1.0E-5D || segments < 2) return;
        Vec3 dir = delta.normalize();
        Vec3 ref = Math.abs(dir.y) < 0.90D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 u = dir.cross(ref).normalize();
        Vec3 v = dir.cross(u).normalize();

        int timeStep = (int) (age * 1.6F);
        Vec3 prev = start;
        for (int i = 1; i <= segments; i++) {
            double t = i / (double) segments;
            Vec3 next;
            if (i == segments) {
                next = end;
            } else {
                double envelope = Math.sin(t * Math.PI);
                int s = seed * 97 + i * 31 + timeStep * 17;
                double offU = (rnd(s) - 0.5D) * 2.0D * jitter * envelope;
                double offV = (rnd(s + 1) - 0.5D) * 2.0D * jitter * envelope;
                next = start.lerp(end, t).add(u.scale(offU)).add(v.scale(offV));
            }

            drawBeamSegment(m, out, prev, next, coreRadius * 2.5D, coreRadius * 2.5D,
                    0.96F, 0.14F, 0.24F, alpha * 0.52F);
            drawBeamSegment(m, out, prev, next, coreRadius, coreRadius,
                    1.00F, 0.78F, 0.82F, alpha * 0.95F);
            prev = next;
        }
    }

    public static void drawShockwaveRing(Matrix4fc m, VertexConsumer out,
                                         Vec3 center, double innerRadius, double outerRadius,
                                         double heightOffset, int sides,
                                         float r, float g, float b, float alpha) {
        if (outerRadius <= innerRadius || sides < 6) return;
        for (int i = 0; i < sides; i++) {
            double a0 = (i / (double) sides) * Math.PI * 2.0D;
            double a1 = ((i + 1) / (double) sides) * Math.PI * 2.0D;

            double c0 = Math.cos(a0), s0 = Math.sin(a0);
            double c1 = Math.cos(a1), s1 = Math.sin(a1);

            Vec3 p0 = center.add(c0 * innerRadius, heightOffset, s0 * innerRadius);
            Vec3 p1 = center.add(c0 * outerRadius, heightOffset + 0.04D, s0 * outerRadius);
            Vec3 p2 = center.add(c1 * outerRadius, heightOffset + 0.04D, s1 * outerRadius);
            Vec3 p3 = center.add(c1 * innerRadius, heightOffset, s1 * innerRadius);

            addDoubleQuad(m, out, p0, p1, p2, p3, r, g, b, alpha);
        }
    }

    public static void drawShockwaveWall(Matrix4fc m, VertexConsumer out,
                                         Vec3 center, double radius, double wallHeight,
                                         int sides, float r, float g, float b, float alpha) {
        if (radius <= 0.05D || wallHeight <= 0.02D) return;
        for (int i = 0; i < sides; i++) {
            double a0 = (i / (double) sides) * Math.PI * 2.0D;
            double a1 = ((i + 1) / (double) sides) * Math.PI * 2.0D;

            Vec3 b0 = center.add(Math.cos(a0) * radius, 0.02D, Math.sin(a0) * radius);
            Vec3 b1 = center.add(Math.cos(a1) * radius, 0.02D, Math.sin(a1) * radius);
            Vec3 t1 = b1.add(0.0D, wallHeight, 0.0D);
            Vec3 t0 = b0.add(0.0D, wallHeight, 0.0D);

            addDoubleQuad(m, out, b0, b1, t1, t0, r, g, b, alpha);
        }
    }

    public static void drawEnergyOrb(Matrix4fc m, VertexConsumer out,
                                     Vec3 center, double radius,
                                     float r, float g, float b, float alpha) {
        if (radius <= 0.01D) return;
        int segs = 12;
        for (int plane = 0; plane < 3; plane++) {
            for (int i = 0; i < segs; i++) {
                double a0 = (i / (double) segs) * Math.PI * 2.0D;
                double a1 = ((i + 1) / (double) segs) * Math.PI * 2.0D;
                Vec3 p0 = planePoint(center, radius, a0, plane);
                Vec3 p1 = planePoint(center, radius, a1, plane);
                addDoubleQuad(m, out, center, p0, p1, center, r, g, b, alpha);
            }
        }
    }

    private static Vec3 planePoint(Vec3 c, double r, double ang, int plane) {
        double cos = Math.cos(ang) * r;
        double sin = Math.sin(ang) * r;
        return switch (plane) {
            case 0 -> c.add(cos, sin, 0.0D);
            case 1 -> c.add(0.0D, sin, cos);
            default -> c.add(cos, 0.0D, sin);
        };
    }

    public static double rnd(int seed) {
        double x = Math.sin(seed * 12.9898D + 78.233D) * 43758.5453D;
        return x - Math.floor(x);
    }
}
