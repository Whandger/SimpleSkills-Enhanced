package com.github.ob_yekt.simpleskills.prayer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import java.util.ArrayList;
import java.util.List;

/** Dispatches the custom procedural 3D renderers for every Prayer ability. */
public final class PrayerVfxRenderer extends EntityRenderer<PrayerVfxEntity, PrayerVfxRenderer.State> {
    public PrayerVfxRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0;
    }

    @Override
    public boolean shouldRender(PrayerVfxEntity entity, net.minecraft.client.renderer.culling.Frustum frustum,
                               double x, double y, double z, float partialTick) {
        return true;
    }

    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(PrayerVfxEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.style = entity.style();
        state.end = entity.endOffset();
        state.hand = entity.handOffset();
        state.age = entity.life() + partialTick;
        state.duration = entity.maxLife();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        float age = state.age;
        collector.submitCustomGeometry(pose, RenderTypes.lightning(), (matrix, vertices) -> {
            Matrix4fc transform = matrix.pose();
            PrayerAbility ability = null;
            if (state.style == 0) {
                PrayerLightningVfx.draw(transform, vertices, state.hand, state.end, age, state.duration);
                ability = PrayerAbility.LIGHTNING_BOLT;
            } else if (state.style == 1) {
                healing(transform, vertices, age, state.duration);
                ability = PrayerAbility.DIVINE_HEALING;
            } else if (state.style == 2) {
                meteor(transform, vertices, age, state.duration);
                ability = PrayerAbility.METEOR;
            } else {
                ability = PrayerVisualEffects.abilityForStyle(state.style);
                if (ability != null) PrayerAbilityVfx.draw(transform, vertices, ability, state.end, age, state.duration);
            }
            if (ability != null) PrayerVfxFlair.draw(transform, vertices, ability, state.end, age, state.duration);
        });
    }

    // Divine Healing: a ground-carved sigil, independently animated aerial runes, and ascending energy.
    private static void healing(Matrix4fc m, VertexConsumer out, float age, int duration) {
        float fade = fadeOut(age, duration, 5, 7);
        double awaken = smooth(0, 8, age);
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
        double finale = smooth(duration - 9, duration - 2, age);
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

    // Meteor: falling irregular rock, molten fissures, corkscrew exhaust, and staged shockwave impact.
    private static void meteor(Matrix4fc m, VertexConsumer out, float age, int duration) {
        double flightTicks = duration * 0.64;
        double flight = clamp(age / flightTicks);
        double eased = flight * flight;
        Vec3 center = new Vec3(-4.6 * (1 - flight), 18.0 * (1 - eased) + 0.42 * eased,
                -2.7 * (1 - flight));
        double impact = smooth(flightTicks, duration - 3, age);
        double bodyFade = 1 - impact * 0.86;
        float fade = fadeOut(age, duration, 2, 3);
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

    private static double smooth(double start, double end, double value) {
        double t = clamp((value - start) / Math.max(1.0E-5, end - start));
        return t * t * (3 - 2 * t);
    }

    private static float fadeOut(float age, int duration, int fadeIn, int fadeOut) {
        return (float) Math.min(smooth(0, fadeIn, age), 1 - smooth(duration - fadeOut, duration, age));
    }

    private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }

    public static final class State extends EntityRenderState {
        int style, duration;
        float age;
        Vec3 end = Vec3.ZERO;
        Vec3 hand = Vec3.ZERO;
    }
}
