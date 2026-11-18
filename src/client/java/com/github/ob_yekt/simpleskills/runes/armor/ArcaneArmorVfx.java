package com.github.ob_yekt.simpleskills.runes.armor;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.EquipmentPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.BODY;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.HEAD;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.LEFT_ARM;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.LEG;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.RIGHT_ARM;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.drawOctahedralNode;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.drawRuneCircle;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.drawStarFlare;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.drawTiltedEllipse;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.fract;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.mix;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.unit;

public final class ArcaneArmorVfx {
    private ArcaneArmorVfx() {}

    public static void submit(SubmitNodeCollector collector, PoseStack poseStack,
                              EquipmentPart part, double time, int seed) {
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, vertices) ->
                draw(pose.pose(), vertices, part, time, seed));
    }

    private static void draw(Matrix4fc matrix, VertexConsumer out, EquipmentPart part,
                             double time, int seed) {
        double pulse = 0.78 + 0.20 * Math.sin(time * 1.35 + seed * 0.01);
        float r = 0.76f, g = 0.36f, b = 1.0f;

        switch (part) {
            case HELMET -> {
                double bob = Math.sin(time * 0.16) * 0.014;
                Vec3 aboveHead = new Vec3(0.0, HEAD.minY() - 0.11 + bob, 0.0);
                drawRuneCircle(matrix, out, aboveHead, new Vec3(0, -1, 0), new Vec3(0, 0, -1),
                        0.26, time * 0.045, r, g, b, (float) pulse, 0.0024);

                // Runas arcanas e cristais orbitando a coroa do capacete
                drawOrbitingGlyphsAndCrystals(matrix, out, aboveHead,
                        0.32, 0.32, 0.024, 5, 4, 0.045, 0.011,
                        time, seed + 11, pulse, false);
            }
            case CHEST_TORSO -> {
                Vec3 behindBack = new Vec3(0.0, 0.34, BODY.maxZ() + 0.05);
                drawRuneCircle(matrix, out, behindBack, new Vec3(0, 0, 1), new Vec3(0, -1, 0),
                        0.31, time * 0.038, r, g, b, (float) pulse, 0.0026);
                drawRuneCircle(matrix, out, behindBack.add(0.0, 0.0, 0.018), new Vec3(0, 0, 1), new Vec3(0, -1, 0),
                        0.23, -time * 0.055, 0.90f, 0.58f, 1.0f, (float) (0.76 * pulse), 0.0018);

                // 6 glifos rúnicos flutuando ao redor do selo dorsal + cristais orbitando
                drawBackGlyphHalo(matrix, out, behindBack.add(0.0, 0.0, 0.025), 0.38, 6,
                        time, seed + 23, pulse);
                drawOrbitingGlyphsAndCrystals(matrix, out, new Vec3(0.0, 0.36, 0.0),
                        0.35, 0.24, 0.024, 0, 6, 0.06, 0.0115,
                        time, seed + 29, pulse, false);
            }
            case ARM_RIGHT -> {
                Vec3 rightShoulder = new Vec3(RIGHT_ARM.minX() - 0.035, 0.18, 0.0);
                drawRuneCircle(matrix, out, rightShoulder, new Vec3(-1, 0, 0), new Vec3(0, -1, 0),
                        0.155, time * 0.055, r, g, b, (float) pulse, 0.0020);

                drawOrbitingGlyphsAndCrystals(matrix, out, new Vec3((RIGHT_ARM.minX() + RIGHT_ARM.maxX()) * 0.5, 0.34, 0.0),
                        0.185, 0.185, 0.019, 3, 3, 0.04, 0.009,
                        time, seed + 37, pulse, false);
            }
            case ARM_LEFT -> {
                Vec3 leftShoulder = new Vec3(LEFT_ARM.maxX() + 0.035, 0.18, 0.0);
                drawRuneCircle(matrix, out, leftShoulder, new Vec3(1, 0, 0), new Vec3(0, -1, 0),
                        0.155, -time * 0.055, r, g, b, (float) pulse, 0.0020);

                drawOrbitingGlyphsAndCrystals(matrix, out, new Vec3((LEFT_ARM.minX() + LEFT_ARM.maxX()) * 0.5, 0.34, 0.0),
                        0.185, 0.185, 0.019, 3, 3, 0.04, 0.009,
                        time, seed + 43, pulse, true);
            }
            case KNEE_RIGHT, KNEE_LEFT -> {
                Vec3 kneeFront = new Vec3(0.0, 0.36, LEG.minZ() - 0.032);
                double dir = part == EquipmentPart.KNEE_RIGHT ? 1.0 : -1.0;
                drawRuneCircle(matrix, out, kneeFront, new Vec3(0, 0, -1), new Vec3(0, -1, 0),
                        0.135, dir * time * 0.06, r, g, b, (float) pulse, 0.0019);

                drawOrbitingGlyphsAndCrystals(matrix, out, new Vec3(0.0, 0.36, 0.0),
                        0.175, 0.175, 0.017, 2, 2, 0.03, 0.0082,
                        time, seed + (part == EquipmentPart.KNEE_RIGHT ? 53 : 59), pulse, part == EquipmentPart.KNEE_LEFT);
            }
            case LEG_WAIST -> {
                Vec3 waist = new Vec3(0.0, 0.68, 0.0);
                drawTiltedEllipse(matrix, out, waist, 0.28, 0.17, time * 0.05, 0.0, 0.0,
                        r, g, b, (float) (0.55 * pulse), 28, 0.0016, true);

                drawOrbitingGlyphsAndCrystals(matrix, out, waist,
                        0.32, 0.21, 0.020, 4, 4, 0.035, 0.0095,
                        time, seed + 67, pulse, false);
            }
            case BOOT_RIGHT, BOOT_LEFT -> {
                Vec3 ankle = new Vec3(0.0, 0.64, 0.0);
                drawTiltedEllipse(matrix, out, ankle, 0.175, 0.175, time * 0.06, 0.0, 0.0,
                        r, g, b, (float) (0.65 * pulse), 24, 0.0016, true);

                drawOrbitingGlyphsAndCrystals(matrix, out, ankle,
                        0.205, 0.205, 0.017, 3, 3, 0.035, 0.0082,
                        time, seed + (part == EquipmentPart.BOOT_RIGHT ? 71 : 79), pulse, part == EquipmentPart.BOOT_LEFT);
            }
            default -> {
            }
        }
    }

    /**
     * Glifos arcanos flutuando ao redor do círculo rúnico nas costas do peitoral.
     */
    private static void drawBackGlyphHalo(Matrix4fc matrix, VertexConsumer out, Vec3 center,
                                          double radius, int count, double time, int seed, double pulse) {
        Vec3 up = new Vec3(0.0, -1.0, 0.0);
        Vec3 right = new Vec3(1.0, 0.0, 0.0);

        for (int i = 0; i < count; i++) {
            long gSeed = mix(seed * 61L + i * 389L);
            double life = fract(time * 0.025 + i * (1.0 / count) + unit(gSeed, 1));
            double fade = Math.sin(Math.PI * life);
            if (fade < 0.06) continue;

            double ang = time * 0.038 + i * (Math.PI * 2.0 / count);
            double rOff = radius + life * 0.035;
            Vec3 pos = center
                    .add(Math.cos(ang) * rOff, -Math.sin(ang) * rOff, 0.008 * Math.sin(Math.PI * life));

            double glyphSize = 0.024 * (0.82 + 0.18 * fade);
            float alpha = (float) (0.90 * pulse * fade);
            drawArcaneGlyph(matrix, out, pos, up, right, glyphSize, i + seed, alpha);
        }
    }

    /**
     * Renderiza glifos rúnicos que surgem e sobem suavemente (eixo -Y no modelo de armadura)
     * e cristais octaédricos com brilho estelar orbitando ao redor da peça.
     */
    private static void drawOrbitingGlyphsAndCrystals(Matrix4fc matrix, VertexConsumer out, Vec3 center,
                                                      double radiusX, double radiusZ, double glyphSize,
                                                      int glyphCount, int crystalCount,
                                                      double riseSpan, double crystalSize,
                                                      double time, int seed, double pulse, boolean reverse) {
        double dir = reverse ? -1.0 : 1.0;
        Vec3 upDir = new Vec3(0.0, -1.0, 0.0);

        for (int i = 0; i < glyphCount; i++) {
            long gSeed = mix(seed * 59L + i * 373L);
            double life = fract(time * 0.026 + i * (1.0 / glyphCount) + unit(gSeed, 1));
            double fade = Math.sin(Math.PI * life);
            if (fade < 0.06) continue;

            double ang = dir * time * 0.075 + i * (Math.PI * 2.0 / glyphCount) + unit(gSeed, 2) * 0.8;
            double cosA = Math.cos(ang);
            double sinA = Math.sin(ang);

            // Sobe no espaço do jogador (-Y no PoseStack de armadura)
            double yOff = (0.35 - life) * riseSpan + Math.sin(time * 0.16 + i * 1.9) * 0.008;
            Vec3 pos = center.add(cosA * radiusX, yOff, sinA * radiusZ);

            // Orienta o glifo tangente ao cilindro da peça para ficar legível de qualquer ângulo
            Vec3 rightDir = new Vec3(-sinA, 0.0, cosA).normalize();
            double size = glyphSize * (0.82 + 0.18 * fade);
            float alpha = (float) (0.92 * pulse * fade);

            drawArcaneGlyph(matrix, out, pos, upDir, rightDir, size, i + seed, alpha);
        }

        for (int i = 0; i < crystalCount; i++) {
            double frac = (i + 0.5) / crystalCount;
            double ang = dir * time * 0.13 + frac * Math.PI * 2.0;
            double bobY = Math.sin(ang * 2.0 + i * 1.4) * (riseSpan * 0.45);

            Vec3 orb = center.add(
                    Math.cos(ang) * (radiusX + 0.018),
                    bobY,
                    Math.sin(ang) * (radiusZ + 0.018)
            );

            drawOctahedralNode(matrix, out, orb, crystalSize,
                    0.82f, 0.50f, 1.0f, (float) (0.90 * pulse));
            drawStarFlare(matrix, out, orb, crystalSize * 1.75,
                    0.96f, 0.84f, 1.0f, (float) (0.52 * pulse));
        }
    }

    /**
     * Os mesmos 8 símbolos rúnicos arcanos do ArcaneToolVfx.
     */
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
            case 0 -> { // Othala
                line(matrix, out, top, left, stroke, r, g, b, alpha);
                line(matrix, out, top, right, stroke, r, g, b, alpha);
                line(matrix, out, left, botR, stroke, r, g, b, alpha);
                line(matrix, out, right, botL, stroke, r, g, b, alpha);
            }
            case 1 -> { // Dagaz
                line(matrix, out, topL, botL, stroke, r, g, b, alpha);
                line(matrix, out, topR, botR, stroke, r, g, b, alpha);
                line(matrix, out, topL, botR, stroke, r, g, b, alpha);
                line(matrix, out, topR, botL, stroke, r, g, b, alpha);
            }
            case 2 -> { // Algiz
                line(matrix, out, bot, top, stroke, r, g, b, alpha);
                line(matrix, out, center, topL, stroke, r, g, b, alpha);
                line(matrix, out, center, topR, stroke, r, g, b, alpha);
            }
            case 3 -> { // Ansuz
                line(matrix, out, botL, topL, stroke, r, g, b, alpha);
                line(matrix, out, topL, midUp.add(rightDir.scale(size * 0.65)), stroke, r, g, b, alpha);
                line(matrix, out, midUp.subtract(rightDir.scale(size * 0.62)), midDn.add(rightDir.scale(size * 0.65)), stroke, r, g, b, alpha);
            }
            case 4 -> { // Thurisaz
                line(matrix, out, botL, topL, stroke, r, g, b, alpha);
                line(matrix, out, midUp.subtract(rightDir.scale(size * 0.62)), right, stroke, r, g, b, alpha);
                line(matrix, out, right, midDn.subtract(rightDir.scale(size * 0.62)), stroke, r, g, b, alpha);
            }
            case 5 -> { // Ingwaz
                line(matrix, out, midUp, left, stroke, r, g, b, alpha);
                line(matrix, out, midUp, right, stroke, r, g, b, alpha);
                line(matrix, out, midDn, left, stroke, r, g, b, alpha);
                line(matrix, out, midDn, right, stroke, r, g, b, alpha);
                line(matrix, out, bot, top, stroke * 0.85, r, g, b, alpha);
            }
            case 6 -> { // Berkano
                line(matrix, out, botL, topL, stroke, r, g, b, alpha);
                line(matrix, out, topL, midUp.add(rightDir.scale(size * 0.55)), stroke, r, g, b, alpha);
                line(matrix, out, midUp.add(rightDir.scale(size * 0.55)), center.subtract(rightDir.scale(size * 0.62)), stroke, r, g, b, alpha);
                line(matrix, out, center.subtract(rightDir.scale(size * 0.62)), midDn.add(rightDir.scale(size * 0.55)), stroke, r, g, b, alpha);
                line(matrix, out, midDn.add(rightDir.scale(size * 0.55)), botL, stroke, r, g, b, alpha);
            }
            default -> { // Eihwaz
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
