package com.github.ob_yekt.simpleskills.runes.shields;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.Bounds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.boundsCenter;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.drawOctahedralNode;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.drawRuneCircle;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.drawStarFlare;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.fract;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.lerp;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.mix;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.unit;

/**
 * Procedural 3D Rune VFX tailored for ShieldModel local coordinates
 * (-Y = UP, +Y = DOWN, -Z = FRONT outer face, +Z = BACK handle face).
 */
public final class ShieldRuneVfx {
    private ShieldRuneVfx() {}

    public static void submitArcane(SubmitNodeCollector collector, PoseStack poseStack,
                                    Bounds bounds, double frontZ, double time, int seed) {
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, vertices) ->
                drawArcaneShield(pose.pose(), vertices, bounds, frontZ, time, seed));
    }

    public static void submitDivine(SubmitNodeCollector collector, PoseStack poseStack,
                                    Bounds bounds, double frontZ, double time, int seed) {
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, vertices) ->
                drawDivineShield(pose.pose(), vertices, bounds, frontZ, time, seed));
    }

    private static void drawArcaneShield(Matrix4fc matrix, VertexConsumer out,
                                         Bounds bounds, double frontZ, double time, int seed) {
        double pulse = 0.80 + 0.20 * Math.sin(time * 1.25 + seed * 0.01);
        Vec3 sealCenter = new Vec3(0.0, 0.0, frontZ);
        Vec3 frontNormal = new Vec3(0.0, 0.0, -1.0);
        Vec3 refUp = new Vec3(0.0, -1.0, 0.0);

        // Double concentric rotating arcane rune seal projected just in front of the shield face
        drawRuneCircle(matrix, out, sealCenter, frontNormal, refUp,
                0.295, time * 0.045, 0.76f, 0.36f, 1.0f, (float) (0.92 * pulse), 0.0024);
        drawRuneCircle(matrix, out, sealCenter.add(0.0, 0.0, -0.012), frontNormal, refUp,
                0.215, -time * 0.068, 0.92f, 0.62f, 1.0f, (float) (0.78 * pulse), 0.0017);

        // 8 floating arcane glyphs around the outer perimeter of the front seal
        Vec3 rightDir = new Vec3(1.0, 0.0, 0.0);
        int glyphs = 8;
        for (int i = 0; i < glyphs; i++) {
            long gSeed = mix(seed * 61L + i * 389L);
            double life = fract(time * 0.025 + i * (1.0 / glyphs) + unit(gSeed, 1));
            double fade = Math.sin(Math.PI * life);
            if (fade < 0.06) continue;

            double ang = time * 0.040 + i * (Math.PI * 2.0 / glyphs);
            double rOff = 0.345 + life * 0.030;
            Vec3 pos = new Vec3(
                    Math.cos(ang) * rOff,
                    -Math.sin(ang) * rOff - life * 0.025,
                    frontZ - 0.008 * Math.sin(Math.PI * life));

            double glyphSize = 0.024 * (0.82 + 0.18 * fade);
            float alpha = (float) (0.90 * pulse * fade);
            drawArcaneGlyph(matrix, out, pos, refUp, rightDir, glyphSize, i + seed, alpha);
        }

        // 6 octahedral arcane crystals orbiting around the shield body
        Vec3 center = boundsCenter(bounds);
        double rx = bounds.width() * 0.56 + 0.026;
        double rz = bounds.depth() * 0.54 + 0.026;
        int crystals = 6;
        for (int i = 0; i < crystals; i++) {
            double frac = (i + 0.5) / crystals;
            double ang = time * 0.12 + frac * Math.PI * 2.0;
            double bobY = Math.sin(ang * 2.0 + i * 1.4) * 0.34;
            Vec3 orb = center.add(Math.cos(ang) * rx, bobY, Math.sin(ang) * rz);

            drawOctahedralNode(matrix, out, orb, 0.0115, 0.82f, 0.50f, 1.0f, (float) (0.90 * pulse));
            drawStarFlare(matrix, out, orb, 0.020, 0.96f, 0.84f, 1.0f, (float) (0.52 * pulse));
        }
    }

    private static void drawDivineShield(Matrix4fc matrix, VertexConsumer out,
                                         Bounds bounds, double frontZ, double time, int seed) {
        double pulse = 0.85 + 0.15 * Math.sin(time * 0.85 + seed * 0.01);
        Vec3 emblemCenter = new Vec3(0.0, -0.04, frontZ);
        Vec3 frontNormal = new Vec3(0.0, 0.0, -1.0);
        Vec3 refUp = new Vec3(0.0, -1.0, 0.0);

        // Radiant golden-white solar halo emblem on the shield's front face
        drawRuneCircle(matrix, out, emblemCenter, frontNormal, refUp,
                0.275, time * 0.032, 1.0f, 0.80f, 0.24f, (float) (0.86 * pulse), 0.0026);
        drawRuneCircle(matrix, out, emblemCenter.add(0.0, 0.0, -0.010), frontNormal, refUp,
                0.205, -time * 0.048, 1.0f, 0.97f, 0.78f, (float) (0.92 * pulse), 0.0017);

        // 8 golden solar rays pulsing around the front crest
        int rays = 8;
        for (int i = 0; i < rays; i++) {
            double a = time * 0.032 + i * (Math.PI * 2.0 / rays);
            double rayPulse = 0.72 + 0.28 * Math.sin(time * 0.26 + i * 1.5);
            Vec3 inner = emblemCenter.add(Math.cos(a) * 0.285, Math.sin(a) * 0.285, -0.004);
            Vec3 outer = emblemCenter.add(Math.cos(a) * (0.335 + 0.020 * rayPulse),
                    Math.sin(a) * (0.335 + 0.020 * rayPulse), -0.004);
            PrayerVfxGeometry.taperedTube(matrix, out, inner, outer,
                    0.0024, 0.0003, 1.0f, 0.88f, 0.42f, (float) (0.85 * pulse * rayPulse));
        }

        // Golden divine sparkles shimmering around the shield and bursting into 8-pointed stars
        Vec3 center = boundsCenter(bounds);
        double rx = bounds.width() * 0.56 + 0.028;
        double rz = bounds.depth() * 0.56 + 0.028;
        int count = 14;
        double cycleRate = 0.021;
        double explodeStart = 0.82;

        for (int i = 0; i < count; i++) {
            long pSeed = mix(seed * 79L + i * 353L);
            double life = fract(time * cycleRate + i * (1.0 / count) + unit(pSeed, 1));

            double baseAngle = unit(pSeed, 2) * Math.PI * 2.0 + time * 0.025;
            double radialScale = 0.92 + unit(pSeed, 3) * 0.26;
            double startY = lerp(bounds.minY() + 0.04, bounds.maxY() - 0.04, unit(pSeed, 4));

            double driftProgress = Math.min(life / explodeStart, 1.0);
            double riseY = -driftProgress * (0.045 + unit(pSeed, 5) * 0.035);
            double expandRadial = radialScale + driftProgress * 0.14;
            double wobbleAngle = baseAngle + Math.sin(time * 0.12 + i * 2.1) * 0.14;

            Vec3 pos = new Vec3(
                    center.x + Math.cos(wobbleAngle) * rx * expandRadial,
                    startY + riseY,
                    center.z + Math.sin(wobbleAngle) * rz * expandRadial);

            double starTilt = unit(pSeed, 6) * (Math.PI * 0.25);

            if (life < explodeStart) {
                double t = life / explodeStart;
                double fadeIn = Math.min(1.0, t / 0.12);
                double shimmer = 0.78 + 0.22 * Math.sin(time * 0.35 + i * 2.7);
                double prePop = t > 0.85 ? 1.0 + ((t - 0.85) / 0.15) * 0.45 : 1.0;

                double r = (0.0042 + unit(pSeed, 7) * 0.0018) * fadeIn * prePop;
                float alpha = (float) (fadeIn * shimmer);

                PrayerVfxGeometry.ellipsoid(matrix, out, pos, new Vec3(r * 1.55, r * 1.55, r * 1.55),
                        4, 6, 1.00f, 0.80f, 0.22f, alpha * 0.48f, 0.0);
                PrayerVfxGeometry.ellipsoid(matrix, out, pos, new Vec3(r * 0.72, r * 0.72, r * 0.72),
                        4, 6, 1.00f, 0.96f, 0.68f, alpha * 0.95f, 0.0);
                drawDelicateStar(matrix, out, pos, r * 2.6, starTilt, alpha * 0.55f);
            } else {
                double burstT = (life - explodeStart) / (1.0 - explodeStart);
                double expand = 1.0 - Math.pow(1.0 - burstT, 2.6);
                double fadeOut = Math.pow(1.0 - burstT, 1.25);

                double maxRadius = 0.036 + unit(pSeed, 7) * 0.016;
                double currentRadius = lerp(0.005, maxRadius, expand);
                double spin = starTilt + burstT * 0.35;

                if (burstT < 0.45) {
                    double flashFade = 1.0 - (burstT / 0.45);
                    double flashR = 0.0085 * (1.0 - burstT * 0.5);
                    PrayerVfxGeometry.ellipsoid(matrix, out, pos, new Vec3(flashR, flashR, flashR),
                            4, 6, 1.00f, 0.99f, 0.85f, (float) (flashFade * 0.95), 0.0);
                }

                drawExplosionStar(matrix, out, pos, currentRadius, spin, (float) fadeOut);
            }
        }
    }

    private static void drawDelicateStar(Matrix4fc matrix, VertexConsumer out, Vec3 center,
                                         double radius, double angle, float alpha) {
        double thick = Math.max(0.00045, radius * 0.07);
        for (int k = 0; k < 4; k++) {
            double a = angle + k * (Math.PI * 0.5);
            Vec3 tip = center.add(Math.cos(a) * radius, Math.sin(a) * radius, 0.0);
            PrayerVfxGeometry.taperedTube(matrix, out, center, tip,
                    thick, 0.00012, 1.00f, 0.92f, 0.52f, alpha);
        }
    }

    private static void drawExplosionStar(Matrix4fc matrix, VertexConsumer out, Vec3 center,
                                          double radius, double angle, float alpha) {
        double mainThick = Math.max(0.00085, radius * 0.030);
        double diagThick = mainThick * 0.65;

        for (int k = 0; k < 4; k++) {
            double a = angle + k * (Math.PI * 0.5);
            Vec3 tip = center.add(Math.cos(a) * radius, Math.sin(a) * radius, 0.0);
            PrayerVfxGeometry.taperedTube(matrix, out, center, tip,
                    mainThick, 0.00015, 1.00f, 0.96f, 0.70f, alpha * 0.95f);
        }

        double diagRadius = radius * 0.52;
        for (int k = 0; k < 4; k++) {
            double a = angle + Math.PI * 0.25 + k * (Math.PI * 0.5);
            Vec3 tip = center.add(Math.cos(a) * diagRadius, Math.sin(a) * diagRadius, 0.0);
            PrayerVfxGeometry.taperedTube(matrix, out, center, tip,
                    diagThick, 0.00012, 1.00f, 0.82f, 0.28f, alpha * 0.80f);
        }
    }

    private static void drawArcaneGlyph(Matrix4fc matrix, VertexConsumer out, Vec3 center,
                                        Vec3 upDir, Vec3 rightDir, double size, int variant,
                                        float alpha) {
        double stroke = 0.0014;
        float r = 0.92f, g = 0.68f, b = 1.0f;

        Vec3 top = center.add(upDir.scale(size));
        Vec3 bot = center.subtract(upDir.scale(size));
        Vec3 left = center.subtract(rightDir.scale(size * 0.68));
        Vec3 right = center.add(rightDir.scale(size * 0.68));

        Vec3 topL = top.subtract(rightDir.scale(size * 0.62));
        Vec3 topR = top.add(rightDir.scale(size * 0.62));
        Vec3 botL = bot.subtract(rightDir.scale(size * 0.62));
        Vec3 botR = bot.add(rightDir.scale(size * 0.62));
        Vec3 midUp = center.add(upDir.scale(size * 0.35));
        Vec3 midDn = center.subtract(upDir.scale(size * 0.35));

        switch (Math.abs(variant) % 8) {
            case 0 -> {
                line(matrix, out, top, left, stroke, r, g, b, alpha);
                line(matrix, out, top, right, stroke, r, g, b, alpha);
                line(matrix, out, left, botR, stroke, r, g, b, alpha);
                line(matrix, out, right, botL, stroke, r, g, b, alpha);
            }
            case 1 -> {
                line(matrix, out, topL, botL, stroke, r, g, b, alpha);
                line(matrix, out, topR, botR, stroke, r, g, b, alpha);
                line(matrix, out, topL, botR, stroke, r, g, b, alpha);
                line(matrix, out, topR, botL, stroke, r, g, b, alpha);
            }
            case 2 -> {
                line(matrix, out, bot, top, stroke, r, g, b, alpha);
                line(matrix, out, center, topL, stroke, r, g, b, alpha);
                line(matrix, out, center, topR, stroke, r, g, b, alpha);
            }
            case 3 -> {
                line(matrix, out, botL, topL, stroke, r, g, b, alpha);
                line(matrix, out, topL, midUp.add(rightDir.scale(size * 0.65)), stroke, r, g, b, alpha);
                line(matrix, out, midUp.subtract(rightDir.scale(size * 0.62)), midDn.add(rightDir.scale(size * 0.65)), stroke, r, g, b, alpha);
            }
            case 4 -> {
                line(matrix, out, botL, topL, stroke, r, g, b, alpha);
                line(matrix, out, midUp.subtract(rightDir.scale(size * 0.62)), right, stroke, r, g, b, alpha);
                line(matrix, out, right, midDn.subtract(rightDir.scale(size * 0.62)), stroke, r, g, b, alpha);
            }
            case 5 -> {
                line(matrix, out, midUp, left, stroke, r, g, b, alpha);
                line(matrix, out, midUp, right, stroke, r, g, b, alpha);
                line(matrix, out, midDn, left, stroke, r, g, b, alpha);
                line(matrix, out, midDn, right, stroke, r, g, b, alpha);
                line(matrix, out, bot, top, stroke * 0.85, r, g, b, alpha);
            }
            case 6 -> {
                line(matrix, out, botL, topL, stroke, r, g, b, alpha);
                line(matrix, out, topL, midUp.add(rightDir.scale(size * 0.55)), stroke, r, g, b, alpha);
                line(matrix, out, midUp.add(rightDir.scale(size * 0.55)), center.subtract(rightDir.scale(size * 0.62)), stroke, r, g, b, alpha);
                line(matrix, out, center.subtract(rightDir.scale(size * 0.62)), midDn.add(rightDir.scale(size * 0.55)), stroke, r, g, b, alpha);
                line(matrix, out, midDn.add(rightDir.scale(size * 0.55)), botL, stroke, r, g, b, alpha);
            }
            default -> {
                line(matrix, out, bot, top, stroke, r, g, b, alpha);
                line(matrix, out, top, midUp.add(rightDir.scale(size * 0.62)), stroke, r, g, b, alpha);
                line(matrix, out, bot, midDn.subtract(rightDir.scale(size * 0.62)), stroke, r, g, b, alpha);
            }
        }
    }

    private static void line(Matrix4fc matrix, VertexConsumer out, Vec3 a, Vec3 b,
                             double stroke, float r, float g, float bl, float alpha) {
        PrayerVfxGeometry.taperedTube(matrix, out, a, b, stroke, stroke, r, g, bl, alpha);
    }
}
