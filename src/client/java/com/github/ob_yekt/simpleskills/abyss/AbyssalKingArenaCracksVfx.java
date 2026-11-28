package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

/**
 * Client-side ground fissures for the Corrupted King and Abyssal Sovereign phases.
 * Uses the same procedural Prayer geometry as the existing magical abilities.
 */
public final class AbyssalKingArenaCracksVfx {
    private static final int MAIN_CRACKS = 10;

    private AbyssalKingArenaCracksVfx() {}

    public static void draw(Matrix4fc matrix, VertexConsumer out, float age, int phase) {
        if (phase < 2) return;

        float phaseStrength = phase >= 3 ? 0.92F : 0.62F;
        float pulse = 0.72F + 0.28F * (float) Math.sin(age * (phase >= 3 ? 0.13F : 0.075F));
        float alpha = phaseStrength * pulse;
        Vec3 origin = new Vec3(0.0D, 0.035D, 0.0D);

        // A dim central seal makes the cracks read as one corrupted arena effect.
        PrayerVfxPrimitives.circleSigil(
                matrix, out, origin,
                phase >= 3 ? 1.42D : 1.16D,
                age * (phase >= 3 ? 0.012D : 0.006D),
                alpha * 0.28F,
                0.48F, 0.08F, 0.86F
        );

        for (int i = 0; i < MAIN_CRACKS; i++) {
            double angle = i * (Math.PI * 2.0 / MAIN_CRACKS) + 0.17 * Math.sin(age * 0.012F + i);
            double length = 4.7D + (i % 3) * 0.62D + (phase >= 3 ? 0.8D : 0.0D);

            Vec3 start = origin.add(Math.cos(angle) * 0.30D, 0.0D, Math.sin(angle) * 0.30D);
            Vec3 end = origin.add(Math.cos(angle) * length, 0.015D, Math.sin(angle) * length);
            List<Vec3> points = PrayerVfxPrimitives.jagged(
                    start, end, 9, 0xA81B5D3L + i * 97L, phase >= 3 ? 0.28D : 0.20D
            );

            drawCrack(matrix, out, points, alpha);

            // Two branching fractures per main crack create an irregular shattered floor pattern.
            for (int branch = 0; branch < 2; branch++) {
                int anchorIndex = branch == 0 ? 3 : 6;
                Vec3 branchStart = points.get(anchorIndex);
                double side = branch == 0 ? 1.0D : -1.0D;
                double branchAngle = angle + side * (0.48D + (i % 2) * 0.12D);
                double branchLength = 1.0D + (i % 3) * 0.28D + (phase >= 3 ? 0.35D : 0.0D);
                Vec3 branchEnd = branchStart.add(
                        Math.cos(branchAngle) * branchLength,
                        0.006D,
                        Math.sin(branchAngle) * branchLength
                );
                List<Vec3> branchPoints = PrayerVfxPrimitives.jagged(
                        branchStart, branchEnd, 4, 0x5C13A7L + i * 31L + branch, 0.12D
                );
                drawCrack(matrix, out, branchPoints, alpha * 0.78F);
            }
        }

        // Additional energetic pulses appear only in the final phase.
        if (phase >= 3) {
            for (int i = 0; i < 6; i++) {
                double angle = age * 0.025D + i * (Math.PI / 3.0D);
                Vec3 from = origin.add(Math.cos(angle) * 1.25D, 0.055D, Math.sin(angle) * 1.25D);
                Vec3 to = origin.add(Math.cos(angle + 0.42D) * 2.9D, 0.065D,
                        Math.sin(angle + 0.42D) * 2.9D);
                PrayerVfxGeometry.taperedTube(
                        matrix, out, from, to,
                        0.018D, 0.002D,
                        0.86F, 0.42F, 1.0F,
                        alpha * 0.62F
                );
            }
        }
    }

    private static void drawCrack(Matrix4fc matrix, VertexConsumer out, List<Vec3> points, float alpha) {
        for (int segment = 0; segment < points.size() - 1; segment++) {
            double taper = 1.0D - (segment / (double) Math.max(1, points.size() - 1)) * 0.52D;
            Vec3 from = points.get(segment);
            Vec3 to = points.get(segment + 1);

            // Wide dark-magenta outer fissure with a thin, bright violet core.
            PrayerVfxGeometry.taperedTube(
                    matrix, out, from, to,
                    0.035D * taper, 0.012D * taper,
                    0.34F, 0.025F, 0.68F,
                    0.38F * alpha
            );
            PrayerVfxGeometry.taperedTube(
                    matrix, out, from.add(0.0D, 0.006D, 0.0D), to.add(0.0D, 0.006D, 0.0D),
                    0.010D * taper, 0.003D * taper,
                    0.86F, 0.40F, 1.0F,
                    0.78F * alpha
            );
        }
    }
}
