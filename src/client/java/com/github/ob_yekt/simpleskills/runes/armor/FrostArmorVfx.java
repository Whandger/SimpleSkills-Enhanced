package com.github.ob_yekt.simpleskills.runes.armor;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.Bounds;
import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.EquipmentPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.boundsCenter;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.fract;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.lerp;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.mix;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.unit;

public final class FrostArmorVfx {
    private FrostArmorVfx() {}

    public static void submit(SubmitNodeCollector collector, PoseStack poseStack,
                              EquipmentPart part, Bounds bounds, double time, int seed) {
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, vertices) ->
                draw(pose.pose(), vertices, part, bounds, time, seed));
    }

    private static void draw(Matrix4fc matrix, VertexConsumer out, EquipmentPart part,
                             Bounds bounds, double time, int seed) {
        double pulse = 0.80 + 0.20 * Math.sin(time * 0.9 + seed * 0.02);

        switch (part) {
            case CHEST_TORSO -> drawFrostAura(matrix, out, part, bounds, time, seed, 14, 12, pulse);
            case HELMET -> drawFrostAura(matrix, out, part, bounds, time, seed, 9, 8, pulse);
            case ARM_RIGHT, ARM_LEFT, LEG_WAIST, KNEE_RIGHT, KNEE_LEFT, BOOT_RIGHT, BOOT_LEFT ->
                    drawFrostAura(matrix, out, part, bounds, time, seed, 7, 6, pulse);
            default -> {
            }
        }
    }

    private static void drawFrostAura(Matrix4fc matrix, VertexConsumer out, EquipmentPart part,
                                      Bounds bounds, double time, int seed,
                                      int flakeCount, int shardCount, double pulse) {
        Vec3 center = boundsCenter(bounds);
        // Fica ligeiramente por fora da caixa de cada peça para não cortar dentro da armadura
        double rx = bounds.width() * 0.54 + 0.026;
        double rz = bounds.depth() * 0.54 + 0.026;

        // 1. Pequenos flocos de neve finos (6 pontas afiladas) caindo suavemente ao redor da armadura
        for (int i = 0; i < flakeCount; i++) {
            long pSeed = mix(seed * 67L + part.ordinal() * 1277L + i * 331L);
            double life = fract(time * 0.026 + i * (1.0 / flakeCount) + unit(pSeed, 1));

            double baseAngle = unit(pSeed, 2) * Math.PI * 2.0 + time * 0.022;
            double windSweep = Math.sin(time * 0.07 + i * 1.9) * 0.22 + (life - 0.5) * 0.28;
            double angle = baseAngle + windSweep;

            double radialScale = 0.95 + unit(pSeed, 3) * 0.34;
            double startY = lerp(bounds.minY() - 0.02, bounds.maxY() - 0.02, unit(pSeed, 4));
            // +Y aponta para baixo no espaço do modelo de entidade do Minecraft
            double fallY = life * (0.14 + unit(pSeed, 5) * 0.08);

            double cosA = Math.cos(angle);
            double sinA = Math.sin(angle);
            Vec3 pos = new Vec3(
                    center.x + cosA * rx * radialScale,
                    startY + fallY,
                    center.z + sinA * rz * radialScale);

            // Orienta o plano do floco tangente à superfície da armadura para ficar visível de qualquer ângulo
            Vec3 tangentH = new Vec3(-sinA, 0.0, cosA);
            Vec3 tangentV = new Vec3(0.0, 1.0, 0.0);

            double fade = Math.sin(Math.PI * life);
            double radius = (0.010 + unit(pSeed, 6) * 0.0055) * (0.78 + 0.22 * fade);
            drawFineSnowflake(matrix, out, pos, tangentH, tangentV,
                    radius, time * 0.10 + i, 0.94 * pulse * fade);
        }

        // 2. Micro-cristais de gelo em losango cintilando bem próximos à superfície da armadura
        for (int i = 0; i < shardCount; i++) {
            long sSeed = mix(seed * 97L + part.ordinal() * 1499L + i * 733L);
            double life = fract(time * 0.038 + i * (1.0 / shardCount) + unit(sSeed, 1));

            double angle = unit(sSeed, 2) * Math.PI * 2.0 + time * 0.018 + (life - 0.5) * 0.18;
            double radialScale = 0.92 + unit(sSeed, 3) * 0.24;
            double startY = lerp(bounds.minY(), bounds.maxY(), unit(sSeed, 4));
            double fallY = life * (0.09 + unit(sSeed, 5) * 0.05);

            Vec3 pos = new Vec3(
                    center.x + Math.cos(angle) * rx * radialScale,
                    startY + fallY,
                    center.z + Math.sin(angle) * rz * radialScale);

            double fade = Math.sin(Math.PI * life);
            double r = (0.0034 + unit(sSeed, 6) * 0.0016) * fade;
            drawTinyIceShard(matrix, out, pos, r, 0.92 * pulse * fade);
        }
    }

    /** Desenha um floco de neve de 6 pontas fino e afilado orientado na superfície da peça. */
    private static void drawFineSnowflake(Matrix4fc matrix, VertexConsumer out, Vec3 center,
                                          Vec3 axisU, Vec3 axisV,
                                          double radius, double spin, double alpha) {
        double centerThickness = Math.max(0.00065, radius * 0.075);
        double tipThickness = 0.00018;
        float a = (float) alpha;

        for (int k = 0; k < 6; k++) {
            double ang = spin + k * (Math.PI / 3.0);
            Vec3 offset = axisU.scale(Math.cos(ang) * radius).add(axisV.scale(Math.sin(ang) * radius));
            Vec3 tip = center.add(offset);
            PrayerVfxGeometry.taperedTube(matrix, out, center, tip,
                    centerThickness, tipThickness,
                    0.82f, 0.97f, 1.0f, a);
        }
    }

    /** Desenha um micro-cristal de gelo em formato de losango afilado. */
    private static void drawTinyIceShard(Matrix4fc matrix, VertexConsumer out, Vec3 center,
                                         double radius, double alpha) {
        double h = radius * 1.45;
        float a = (float) alpha;
        PrayerVfxGeometry.taperedTube(matrix, out, center, center.add(0, h, 0),
                radius, 0.00015, 0.76f, 0.96f, 1.0f, a);
        PrayerVfxGeometry.taperedTube(matrix, out, center, center.add(0, -h, 0),
                radius, 0.00015, 0.76f, 0.96f, 1.0f, a);
    }
}
