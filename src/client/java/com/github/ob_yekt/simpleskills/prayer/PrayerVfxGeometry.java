package com.github.ob_yekt.simpleskills.prayer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

/** Small mesh primitives shared by the Prayer prototypes. All geometry is emitted in world space. */
public final class PrayerVfxGeometry {
    private static final int TUBE_SIDES = 6;

    private PrayerVfxGeometry() { }

    public static void tube(Matrix4fc matrix, VertexConsumer out, Vec3 from, Vec3 to,
                     double radius, float r, float g, float b, float alpha) {
        taperedTube(matrix, out, from, to, radius, radius, r, g, b, alpha);
    }

    public static void taperedTube(Matrix4fc matrix, VertexConsumer out, Vec3 from, Vec3 to,
                            double startRadius, double endRadius, float r, float g, float b, float alpha) {
        Vec3 axis = to.subtract(from);
        if (axis.lengthSqr() < 1.0E-8 || startRadius + endRadius < 1.0E-5) return;
        axis = axis.normalize();
        Vec3 basisA = axis.cross(Math.abs(axis.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0));
        if (basisA.lengthSqr() < 1.0E-8) basisA = new Vec3(0, 0, 1);
        basisA = basisA.normalize();
        Vec3 basisB = axis.cross(basisA).normalize();
        for (int side = 0; side < TUBE_SIDES; side++) {
            double a0 = side * Math.PI * 2 / TUBE_SIDES;
            double a1 = (side + 1) * Math.PI * 2 / TUBE_SIDES;
            Vec3 radial0 = basisA.scale(Math.cos(a0)).add(basisB.scale(Math.sin(a0)));
            Vec3 radial1 = basisA.scale(Math.cos(a1)).add(basisB.scale(Math.sin(a1)));
            Vec3 v0 = from.add(radial0.scale(startRadius));
            Vec3 v1 = from.add(radial1.scale(startRadius));
            Vec3 v2 = to.add(radial1.scale(endRadius));
            Vec3 v3 = to.add(radial0.scale(endRadius));
            float facet = 0.78f + 0.22f * (float) Math.max(0, radial0.dot(basisA) * 0.5 + 0.5);
            quad(matrix, out, v0, v1, v2, v3, r * facet, g * facet, b * facet, alpha);
        }
    }

    public static void torus(Matrix4fc matrix, VertexConsumer out, Vec3 center, double radius, double tubeRadius,
                      int segments, double rotation, float r, float g, float b, float alpha) {
        if (segments < 3 || radius <= 0) return;
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2 / segments + rotation;
            double a1 = (i + 1) * Math.PI * 2 / segments + rotation;
            Vec3 from = center.add(Math.cos(a0) * radius, 0, Math.sin(a0) * radius);
            Vec3 to = center.add(Math.cos(a1) * radius, 0, Math.sin(a1) * radius);
            taperedTube(matrix, out, from, to, tubeRadius, tubeRadius, r, g, b, alpha);
        }
    }

    public static void wireSphere(Matrix4fc matrix, VertexConsumer out, Vec3 center, double radius,
                           int divisions, double phase, float r, float g, float b, float alpha, double thickness) {
        if (divisions < 6 || radius <= 0) return;
        for (int latitude = -2; latitude <= 2; latitude++) {
            double phi = latitude * Math.PI / 6.0;
            double ringRadius = radius * Math.cos(phi);
            double y = radius * Math.sin(phi);
            torusAtPlane(matrix, out, center.add(0, y, 0), ringRadius, thickness,
                    divisions, phase, 0, r, g, b, alpha);
        }
        for (int longitude = 0; longitude < 6; longitude++) {
            double theta = phase + longitude * Math.PI / 6.0;
            Vec3 previous = null;
            for (int step = 0; step <= divisions; step++) {
                double phi = -Math.PI / 2 + step * Math.PI / divisions;
                Vec3 point = center.add(Math.cos(phi) * Math.cos(theta) * radius,
                        Math.sin(phi) * radius,
                        Math.cos(phi) * Math.sin(theta) * radius);
                if (previous != null) tube(matrix, out, previous, point, thickness, r, g, b, alpha);
                previous = point;
            }
        }
    }

    private static void torusAtPlane(Matrix4fc matrix, VertexConsumer out, Vec3 center, double radius,
                                     double tubeRadius, int segments, double phase, int plane,
                                     float r, float g, float b, float alpha) {
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2 / segments + phase;
            double a1 = (i + 1) * Math.PI * 2 / segments + phase;
            Vec3 p0 = plane == 0 ? center.add(Math.cos(a0) * radius, 0, Math.sin(a0) * radius)
                    : center.add(Math.cos(a0) * radius, Math.sin(a0) * radius, 0);
            Vec3 p1 = plane == 0 ? center.add(Math.cos(a1) * radius, 0, Math.sin(a1) * radius)
                    : center.add(Math.cos(a1) * radius, Math.sin(a1) * radius, 0);
            tube(matrix, out, p0, p1, tubeRadius, r, g, b, alpha);
        }
    }

    public static void ellipsoid(Matrix4fc matrix, VertexConsumer out, Vec3 center, Vec3 radii,
                          int latitudes, int longitudes, float r, float g, float b, float alpha,
                          double rotation) {
        Vec3[][] rings = new Vec3[latitudes + 1][longitudes];
        for (int latitude = 0; latitude <= latitudes; latitude++) {
            double phi = -Math.PI / 2 + Math.PI * latitude / latitudes;
            for (int longitude = 0; longitude < longitudes; longitude++) {
                double theta = Math.PI * 2 * longitude / longitudes;
                double wobble = 1 + 0.025 * Math.sin(theta * 5 + phi * 3 + rotation);
                double x = Math.cos(phi) * Math.cos(theta) * radii.x * wobble;
                double y = Math.sin(phi) * radii.y * wobble;
                double z = Math.cos(phi) * Math.sin(theta) * radii.z * wobble;
                double rx = x * Math.cos(rotation) - z * Math.sin(rotation);
                double rz = x * Math.sin(rotation) + z * Math.cos(rotation);
                rings[latitude][longitude] = center.add(rx, y, rz);
            }
        }
        for (int latitude = 0; latitude < latitudes; latitude++) {
            double heightShade = 0.68 + 0.32 * (latitude / (double) latitudes);
            for (int longitude = 0; longitude < longitudes; longitude++) {
                int next = (longitude + 1) % longitudes;
                float facet = (float) (heightShade * (0.88 + 0.12 * Math.sin(longitude * 2.4 + rotation)));
                quad(matrix, out, rings[latitude][longitude], rings[latitude][next],
                        rings[latitude + 1][next], rings[latitude + 1][longitude],
                        r * facet, g * facet, b * facet, alpha);
            }
        }
    }

    public static void quad(Matrix4fc matrix, VertexConsumer out, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                     float r, float g, float blue, float alpha) {
        vertex(matrix, out, a, r, g, blue, alpha);
        vertex(matrix, out, b, r, g, blue, alpha);
        vertex(matrix, out, c, r, g, blue, alpha);
        vertex(matrix, out, d, r, g, blue, alpha);
    }

    public static void triangle(Matrix4fc matrix, VertexConsumer out, Vec3 a, Vec3 b, Vec3 c,
                         float r, float g, float blue, float alpha) {
        // Custom geometry is submitted as quads; repeating the last point closes a triangle safely.
        quad(matrix, out, a, b, c, c, r, g, blue, alpha);
    }

    private static void vertex(Matrix4fc matrix, VertexConsumer out, Vec3 point,
                               float r, float g, float b, float alpha) {
        out.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .setColor(r, g, b, alpha);
    }
}
