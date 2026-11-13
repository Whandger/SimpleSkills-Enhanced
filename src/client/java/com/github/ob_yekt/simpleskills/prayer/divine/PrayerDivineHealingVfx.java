package com.github.ob_yekt.simpleskills.prayer.divine;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerDivineHealingVfx {
    private PrayerDivineHealingVfx() {}

    // 2. DIVINE_HEALING — a ground-carved sigil, independently animated aerial runes, and ascending energy.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 5, 7);
        double awaken = PrayerVfxPrimitives.smooth(0, 8, age);
        double sealRotation = age * 0.022;
        double innerRotation = -age * 0.036;
        double pulse = 1 + 0.035 * Math.sin(age * 0.28);

        PrayerVfxGeometry.torus(m, out, new Vec3(0, 0.045, 0), 2.18 * pulse, 0.026,
                144, sealRotation, 0.92f, 0.48f, 0.10f, 0.82f * fade);
        PrayerVfxGeometry.torus(m, out, new Vec3(0, 0.065, 0), 1.73 * pulse, 0.020,
                120, -sealRotation * 1.6, 1.0f, 0.77f, 0.23f, 0.96f * fade);
        PrayerVfxGeometry.torus(m, out, new Vec3(0, 0.085, 0), 1.17 * pulse, 0.024,
                96, innerRotation, 1.0f, 0.86f, 0.43f, 0.91f * fade);

        // Double interlaced star paths and glyph marks make the circle read as a designed seal.
        Vec3[] outerStar = polygon(12, 2.02, 0.07, sealRotation);
        Vec3[] innerStar = polygon(12, 1.48, 0.09, -sealRotation * 1.6 + Math.PI / 12);
        for (int i = 0; i < 12; i++) {
            int next = (i + 5) % 12;
            PrayerVfxGeometry.tube(m, out, outerStar[i], outerStar[next], 0.012,
                    0.99f, 0.73f, 0.19f, 0.62f * fade);
            PrayerVfxGeometry.tube(m, out, innerStar[i], innerStar[(i + 5) % 12], 0.010,
                    1.0f, 0.92f, 0.56f, 0.64f * fade);
        }
        for (int mark = 0; mark < 36; mark++) {
            double a = mark * Math.PI * 2 / 36 + sealRotation;
            double inner = mark % 3 == 0 ? 1.84 : 1.94;
            double outer = mark % 3 == 0 ? 2.10 : 2.04;
            Vec3 from = new Vec3(Math.cos(a) * inner, 0.07, Math.sin(a) * inner);
            Vec3 to = new Vec3(Math.cos(a + 0.012) * outer, 0.07, Math.sin(a + 0.012) * outer);
            PrayerVfxGeometry.tube(m, out, from, to, mark % 3 == 0 ? 0.021 : 0.012,
                    1.0f, 0.83f, 0.34f, (mark % 3 == 0 ? 0.88f : 0.58f) * fade);
        }

        // Ten free-floating runic glyphs: each has its own orbit, bob, spin, and radial orientation.
        for (int rune = 0; rune < 10; rune++) {
            double base = rune * Math.PI * 2 / 10;
            double orbit = base + age * (rune % 2 == 0 ? 0.014 : -0.019) + Math.sin(age * 0.05 + rune) * 0.05;
            double orbitRadius = 1.52 + (rune % 3) * 0.24;
            double y = 0.42 + (rune % 5) * 0.43 + Math.sin(age * 0.09 + rune * 1.6) * 0.13;
            Vec3 center = new Vec3(Math.cos(orbit) * orbitRadius, y, Math.sin(orbit) * orbitRadius);
            Vec3 tangent = new Vec3(-Math.sin(orbit), 0, Math.cos(orbit));
            Vec3 vertical = new Vec3(0, 1, 0);
            double spin = age * (rune % 2 == 0 ? 0.055 : -0.071) + rune * 0.8;
            runeGlyph(m, out, center, tangent, vertical, 0.24 + (rune % 3) * 0.025, spin,
                    rune % 2 == 0 ? 1.0f : 0.94f, rune % 2 == 0 ? 0.76f : 0.91f,
                    0.34f, 0.83f * fade);
            PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(0.035, 0.035, 0.035),
                    5, 7, 1.0f, 0.91f, 0.58f, 0.9f * fade, spin);
            if (rune % 2 == 0) {
                Vec3 tether = new Vec3(Math.cos(orbit) * 0.28, 0.12, Math.sin(orbit) * 0.28);
                PrayerVfxGeometry.tube(m, out, tether, center, 0.012,
                        0.97f, 0.70f, 0.19f, 0.32f * fade);
            }
        }

        // Four independent helical streams climb from the engraved seal and wind around the caster.
        for (int strand = 0; strand < 4; strand++) {
            Vec3 previous = null;
            int steps = 32;
            for (int step = 0; step <= steps; step++) {
                double t = step / (double) steps;
                double angle = age * (0.12 + strand * 0.012) + t * Math.PI * 3.6 + strand * Math.PI / 2;
                double radius = (0.18 + t * 0.48) * awaken;
                Vec3 point = new Vec3(Math.cos(angle) * radius, 0.12 + t * 2.35, Math.sin(angle) * radius);
                if (previous != null) {
                    double thickness = 0.045 * (1 - t * 0.42);
                    PrayerVfxGeometry.tube(m, out, previous, point, thickness * 2.4,
                            0.96f, 0.48f, 0.10f, 0.15f * fade);
                    PrayerVfxGeometry.tube(m, out, previous, point, thickness,
                            1.0f, 0.79f, 0.31f, 0.83f * fade);
                    PrayerVfxGeometry.tube(m, out, previous, point, thickness * 0.35,
                            1.0f, 0.97f, 0.73f, 0.95f * fade);
                }
                previous = point;
            }
        }

        PrayerVfxGeometry.ellipsoid(m, out, new Vec3(0, 1.0, 0), new Vec3(0.66, 1.08, 0.66),
                10, 16, 0.95f, 0.61f, 0.16f, 0.075f * fade, age * 0.04);
        PrayerVfxGeometry.wireSphere(m, out, new Vec3(0, 1.1, 0), 0.82 + 0.05 * Math.sin(age * 0.2),
                24, -age * 0.025, 1.0f, 0.81f, 0.32f, 0.18f * fade, 0.013);

        // The ending is a clean radial release from the body and seal, not a particle burst.
        double finale = PrayerVfxPrimitives.smooth(duration - 9, duration - 2, age);
        if (finale > 0) {
            double expanding = 0.4 + finale * 2.2;
            PrayerVfxGeometry.torus(m, out, new Vec3(0, 0.10, 0), expanding, 0.036,
                    96, age * 0.08, 1.0f, 0.84f, 0.39f, (float) (0.9 * fade));
            PrayerVfxGeometry.wireSphere(m, out, new Vec3(0, 1, 0), 0.35 + finale * 1.9,
                    18, age * 0.04, 1.0f, 0.81f, 0.28f, (float) (0.34 * (1 - finale) * fade), 0.018);
            for (int ray = 0; ray < 18; ray++) {
                double angle = ray * Math.PI * 2 / 18 + sealRotation;
                Vec3 from = new Vec3(Math.cos(angle) * 0.55, 0.16, Math.sin(angle) * 0.55);
                Vec3 to = new Vec3(Math.cos(angle) * expanding, 0.14 + (ray % 3) * 0.12,
                        Math.sin(angle) * expanding);
                PrayerVfxGeometry.taperedTube(m, out, from, to, 0.045, 0.004,
                        1.0f, 0.88f, 0.50f, (float) (0.76 * fade));
            }
        }
    }

    private static void runeGlyph(Matrix4fc m, VertexConsumer out, Vec3 center, Vec3 axisU, Vec3 axisV,
                                  double scale, double rotation, float r, float g, float b, float alpha) {
        Vec3 u = axisU.scale(Math.cos(rotation)).add(axisV.scale(Math.sin(rotation)));
        Vec3 v = axisV.scale(Math.cos(rotation)).subtract(axisU.scale(Math.sin(rotation)));
        Vec3[] p = new Vec3[] {
                runePoint(center, u, v, 0, scale),
                runePoint(center, u, v, -0.58 * scale, scale * 0.36),
                runePoint(center, u, v, 0.58 * scale, scale * 0.36),
                runePoint(center, u, v, -0.46 * scale, -scale * 0.46),
                runePoint(center, u, v, 0.46 * scale, -scale * 0.46),
                runePoint(center, u, v, 0, -scale),
                runePoint(center, u, v, 0, 0)
        };
        int[][] edges = {{0,1},{0,2},{1,2},{1,3},{2,4},{3,4},{3,5},{4,5},{0,6},{6,5}};
        for (int[] edge : edges) {
            PrayerVfxGeometry.tube(m, out, p[edge[0]], p[edge[1]], 0.019,
                    r, g, b, alpha);
            PrayerVfxGeometry.tube(m, out, p[edge[0]], p[edge[1]], 0.007,
                    1.0f, 0.96f, 0.76f, alpha * 0.8f);
        }
    }

    private static Vec3 runePoint(Vec3 center, Vec3 u, Vec3 v, double across, double up) {
        return center.add(u.scale(across)).add(v.scale(up));
    }

    private static Vec3[] polygon(int count, double radius, double y, double rotation) {
        Vec3[] points = new Vec3[count];
        for (int i = 0; i < count; i++) {
            double angle = i * Math.PI * 2 / count + rotation;
            points[i] = new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
        }
        return points;
    }
}
