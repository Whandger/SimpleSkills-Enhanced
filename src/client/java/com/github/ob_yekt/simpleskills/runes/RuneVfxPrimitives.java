package com.github.ob_yekt.simpleskills.runes;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

import java.util.List;

/**
 * Shared math utilities, equipment bounds, and 3D procedural primitives
 * used across armor and tool rune VFX renderers.
 */
public final class RuneVfxPrimitives {
    public static final int FULL_BRIGHT = 0x00F000F0;

    // HumanoidModel local coordinates: -Y = UP, +Y = DOWN, -Z = FRONT, +Z = BACK, -X = RIGHT, +X = LEFT
    public static final Bounds HEAD = new Bounds(-0.255, 0.255, -0.51, 0.015, -0.255, 0.255);
    public static final Bounds BODY = new Bounds(-0.265, 0.265, -0.015, 0.765, -0.14, 0.14);
    public static final Bounds RIGHT_ARM = new Bounds(-0.19, 0.07, -0.13, 0.63, -0.13, 0.13);
    public static final Bounds LEFT_ARM = new Bounds(-0.07, 0.19, -0.13, 0.63, -0.13, 0.13);
    public static final Bounds LEG = new Bounds(-0.145, 0.145, -0.015, 0.765, -0.14, 0.14);
    public static final Bounds FOOT = new Bounds(-0.145, 0.145, 0.43, 0.765, -0.14, 0.14);

    private RuneVfxPrimitives() {}

    // --------------------------------------------------------- Shared VFX Shapes

    public static void boltSegment(Matrix4fc matrix, VertexConsumer out, Vec3 from, Vec3 to,
                                   double pulse, double scale) {
        if (from.distanceToSqr(to) < 1.0E-8) return;
        PrayerVfxGeometry.taperedTube(matrix, out, from, to,
                0.0062 * scale, 0.0024 * scale,
                0.10f, 0.68f, 1.0f, (float) (0.36 * pulse));
        PrayerVfxGeometry.taperedTube(matrix, out, from, to,
                0.0022 * scale, 0.0007 * scale,
                0.88f, 0.99f, 1.0f, (float) (0.98 * pulse));
    }

    /** Renders a 3D arcane rune circle with concentric rings, inscribed hexagram, and rune glyphs. */
    public static void drawRuneCircle(Matrix4fc matrix, VertexConsumer out, Vec3 center,
                                      Vec3 normal, Vec3 refUp, double radius, double rotation,
                                      float r, float g, float b, float alpha, double stroke) {
        Vec3 n = safeNormalize(normal, new Vec3(0, 0, 1));
        Vec3 u = refUp.cross(n);
        if (u.lengthSqr() < 1.0E-6) u = new Vec3(1, 0, 0).cross(n);
        u = u.normalize();
        Vec3 v = n.cross(u).normalize();

        double innerRadius = radius * 0.72;
        int ringSegments = 32;
        Vec3 prevOuter = null;
        Vec3 prevInner = null;

        for (int i = 0; i <= ringSegments; i++) {
            double ang = rotation + i * (Math.PI * 2.0 / ringSegments);
            double cos = Math.cos(ang);
            double sin = Math.sin(ang);
            Vec3 radial = u.scale(cos).add(v.scale(sin));
            Vec3 outerPt = center.add(radial.scale(radius));
            Vec3 innerPt = center.add(radial.scale(innerRadius));

            if (prevOuter != null) {
                PrayerVfxGeometry.taperedTube(matrix, out, prevOuter, outerPt,
                        stroke, stroke, r, g, b, alpha);
                PrayerVfxGeometry.taperedTube(matrix, out, prevInner, innerPt,
                        stroke * 0.85, stroke * 0.85,
                        Math.min(1.0f, r + 0.12f), Math.min(1.0f, g + 0.22f), b, alpha * 0.90f);
            }
            prevOuter = outerPt;
            prevInner = innerPt;
        }

        for (int tri = 0; tri < 2; tri++) {
            double baseAng = rotation * (tri == 0 ? 1.0 : -1.0) + tri * (Math.PI / 3.0);
            Vec3[] pts = new Vec3[3];
            for (int k = 0; k < 3; k++) {
                double a = baseAng + k * (Math.PI * 2.0 / 3.0);
                pts[k] = center.add(u.scale(Math.cos(a) * innerRadius)).add(v.scale(Math.sin(a) * innerRadius));
            }
            for (int k = 0; k < 3; k++) {
                PrayerVfxGeometry.taperedTube(matrix, out, pts[k], pts[(k + 1) % 3],
                        stroke * 0.78, stroke * 0.78,
                        Math.min(1.0f, r + 0.15f), Math.min(1.0f, g + 0.28f), b, alpha * 0.85f);
            }
        }

        int glyphs = 8;
        for (int gIdx = 0; gIdx < glyphs; gIdx++) {
            double a = rotation + gIdx * (Math.PI * 2.0 / glyphs);
            Vec3 rad = u.scale(Math.cos(a)).add(v.scale(Math.sin(a)));
            Vec3 tan = u.scale(-Math.sin(a)).add(v.scale(Math.cos(a)));

            Vec3 stemBase = center.add(rad.scale(innerRadius * 1.04));
            Vec3 stemTop = center.add(rad.scale(radius * 0.96));
            PrayerVfxGeometry.taperedTube(matrix, out, stemBase, stemTop,
                    stroke * 0.75, stroke * 0.75, 0.92f, 0.72f, 1.0f, alpha * 0.92f);

            double sideSign = (gIdx % 2 == 0) ? 1.0 : -1.0;
            Vec3 mid = lerpVec(stemBase, stemTop, 0.55);
            Vec3 branchEnd = stemTop.add(tan.scale(sideSign * radius * 0.11)).subtract(rad.scale(radius * 0.05));
            PrayerVfxGeometry.taperedTube(matrix, out, mid, branchEnd,
                    stroke * 0.65, stroke * 0.45, 0.92f, 0.72f, 1.0f, alpha * 0.88f);
        }
    }

    public static void drawSnowflakeStar(Matrix4fc matrix, VertexConsumer out, Vec3 center,
                                         double radius, double spin, double pulse) {
        double stroke = radius * 0.16;
        for (int k = 0; k < 3; k++) {
            double ang = spin + k * (Math.PI / 3.0);
            Vec3 offset = new Vec3(Math.cos(ang) * radius, Math.sin(ang) * radius, 0.0);
            PrayerVfxGeometry.taperedTube(matrix, out, center.subtract(offset), center.add(offset),
                    stroke, stroke, 0.78f, 0.96f, 1.0f, (float) (0.88 * pulse));
        }
        PrayerVfxGeometry.taperedTube(matrix, out,
                center.add(0, 0, -radius * 0.75), center.add(0, 0, radius * 0.75),
                stroke, stroke, 0.88f, 0.98f, 1.0f, (float) (0.82 * pulse));
    }

    public static void drawStarFlare(Matrix4fc matrix, VertexConsumer out, Vec3 center,
                                     double radius, float r, float g, float b, float alpha) {
        double core = radius * 0.24;
        PrayerVfxGeometry.taperedTube(matrix, out, center.add(0, -radius, 0), center.add(0, radius, 0),
                core, 0.0002, r, g, b, alpha);
        PrayerVfxGeometry.taperedTube(matrix, out, center.add(-radius * 0.78, 0, 0), center.add(radius * 0.78, 0, 0),
                core * 0.85, 0.0002, r, g, b, alpha * 0.9f);
        PrayerVfxGeometry.taperedTube(matrix, out, center.add(0, 0, -radius * 0.78), center.add(0, 0, radius * 0.78),
                core * 0.85, 0.0002, r, g, b, alpha * 0.9f);
    }

    public static void drawOctahedralNode(Matrix4fc matrix, VertexConsumer out, Vec3 center,
                                          double radius, float r, float g, float b, float alpha) {
        Vec3 top = center.add(0, radius * 1.35, 0);
        Vec3 bottom = center.add(0, -radius * 1.35, 0);
        PrayerVfxGeometry.taperedTube(matrix, out, bottom, top,
                radius * 0.65, 0.0003, r, g, b, alpha);
        PrayerVfxGeometry.ellipsoid(matrix, out, center,
                new Vec3(radius * 0.72, radius * 0.72, radius * 0.72),
                5, 6, 1.0f, Math.min(1.0f, g + 0.25f), 1.0f, alpha, 0.0);
    }

    public static void drawTiltedEllipse(Matrix4fc matrix, VertexConsumer out, Vec3 center,
                                         double radiusX, double radiusZ, double rotation,
                                         double tiltX, double tiltZ,
                                         float red, float green, float blue, float alpha,
                                         int segments, double tubeRadius, boolean dashed) {
        Vec3 previous = null;
        for (int i = 0; i <= segments; i++) {
            double angle = rotation + i * (Math.PI * 2.0 / segments);
            double lx = Math.cos(angle) * radiusX;
            double lz = Math.sin(angle) * radiusZ;
            double ly = lx * tiltX + lz * tiltZ;
            Vec3 point = center.add(lx, ly, lz);

            if (previous != null && (!dashed || (i % 3 != 0))) {
                PrayerVfxGeometry.taperedTube(matrix, out, previous, point,
                        tubeRadius, tubeRadius, red, green, blue, alpha);
            }
            previous = point;
        }
    }

    // --------------------------------------------------------- Ribbon Primitives

    public static void texturedRibbonSegment(Matrix4fc matrix, VertexConsumer out,
                                             Vec3 from, Vec3 to, Vec3 previousSide, Vec3 side,
                                             double previousWidth, double width,
                                             double previousV, double v,
                                             float r0, float g0, float b0, float a0,
                                             float r1, float g1, float b1, float a1) {
        Vec3 a = from.subtract(previousSide.scale(previousWidth));
        Vec3 b = from.add(previousSide.scale(previousWidth));
        Vec3 c = to.add(side.scale(width));
        Vec3 d = to.subtract(side.scale(width));
        texturedVertex(matrix, out, a, 0.0f, (float) previousV, r0, g0, b0, a0);
        texturedVertex(matrix, out, b, 1.0f, (float) previousV, r0, g0, b0, a0);
        texturedVertex(matrix, out, c, 1.0f, (float) v, r1, g1, b1, a1);
        texturedVertex(matrix, out, d, 0.0f, (float) v, r1, g1, b1, a1);
    }

    public static void texturedVertex(Matrix4fc matrix, VertexConsumer out, Vec3 point,
                                      float u, float v, float r, float g, float b, float alpha) {
        int red = (int) (255.0f * clamp01(r));
        int green = (int) (255.0f * clamp01(g));
        int blue = (int) (255.0f * clamp01(b));
        int a = (int) (255.0f * clamp01(alpha));
        out.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .setColor(red, green, blue, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(0.0f, 0.0f, 1.0f);
    }

    // -------------------------------------------------------------- Vector & Math

    public static Vec3 boundsCenter(Bounds bounds) {
        return new Vec3(
                (bounds.minX() + bounds.maxX()) * 0.5,
                (bounds.minY() + bounds.maxY()) * 0.5,
                (bounds.minZ() + bounds.maxZ()) * 0.5);
    }

    public static Vec3 orthogonalVector(Vec3 tangent) {
        Vec3 ref = Math.abs(tangent.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        return tangent.cross(ref).normalize();
    }

    public static Vec3 safeNormalize(Vec3 v, Vec3 fallback) {
        double lenSqr = v.lengthSqr();
        return lenSqr < 1.0E-8 ? fallback : v.scale(1.0 / Math.sqrt(lenSqr));
    }

    public static Vec3 lerpVec(Vec3 a, Vec3 b, double t) {
        return new Vec3(lerp(a.x, b.x, t), lerp(a.y, b.y, t), lerp(a.z, b.z, t));
    }

    public static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    public static double unit(long seed, int salt) {
        long value = mix(seed + salt * 0x9E3779B97F4A7C15L);
        return (value >>> 11) * 0x1.0p-53;
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double fract(double value) {
        return value - Math.floor(value);
    }

    public static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    // -------------------------------------------------------------- Data Types

    public enum ItemCategory {
        SWORD,
        PICKAXE,
        TOOL
    }

    public enum EquipmentPart {
        HELMET,
        CHEST_TORSO,
        ARM_RIGHT,
        ARM_LEFT,
        LEG_WAIST,
        KNEE_RIGHT,
        KNEE_LEFT,
        BOOT_RIGHT,
        BOOT_LEFT,
        HELD_ITEM
    }

    public record Bounds(double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        public double width() { return maxX - minX; }
        public double height() { return maxY - minY; }
        public double depth() { return maxZ - minZ; }

        public double strokeScale() {
            double maxDim = Math.max(width(), Math.max(height(), depth()));
            return clamp(maxDim * 1.25, 0.55, 1.15);
        }
    }

    public record ItemGeometryInfo(
            ItemCategory category,
            Bounds bounds,
            double halfDepth,
            List<Vec3> allSamples,
            List<Vec3> headSamples,
            Vec3 guardPoint,
            Vec3 bladeStart,
            Vec3 bladeTip
    ) {}
}