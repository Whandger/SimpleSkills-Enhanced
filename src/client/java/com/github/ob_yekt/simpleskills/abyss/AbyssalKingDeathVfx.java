package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

/**
 * Procedural 3D visual effects for the 5-phase cinematic death of the Abyssal Fallen King:
 * - Phase 1 (0.00..0.20): Dissipating purple aura wisps as the King falters
 * - Phase 3 (0.50..1.00): Violet shockwave, luminous ground fissures & stone shards on sword plant
 * - Phase 4 (0.68..0.84): Subtle golden-violet impact ring where the crown lands beside the sword
 * - Phase 5 (0.70..1.00): Downward-drifting ash, dark armor fragments & fading abyssal embers
 */
public final class AbyssalKingDeathVfx {

    private static final float DEEP_PURPLE_R = 0.34f;
    private static final float DEEP_PURPLE_G = 0.05f;
    private static final float DEEP_PURPLE_B = 0.72f;

    private static final float VIOLET_R = 0.72f;
    private static final float VIOLET_G = 0.22f;
    private static final float VIOLET_B = 1.00f;

    private static final float BRIGHT_VIOLET_R = 0.90f;
    private static final float BRIGHT_VIOLET_G = 0.52f;
    private static final float BRIGHT_VIOLET_B = 1.00f;

    private static final float DARK_MAGENTA_R = 0.74f;
    private static final float DARK_MAGENTA_G = 0.08f;
    private static final float DARK_MAGENTA_B = 0.64f;

    private static final float CROWN_GOLD_R = 0.96f;
    private static final float CROWN_GOLD_G = 0.78f;
    private static final float CROWN_GOLD_B = 0.28f;

    private static final float ASH_R = 0.20f;
    private static final float ASH_G = 0.17f;
    private static final float ASH_B = 0.24f;

    private static final float SWORD_IMPACT_T = 2.25f / 4.50f; // 0.500
    private static final float CROWN_IMPACT_T = 3.05f / 4.50f; // 0.678
    private static final float CRUMBLE_START_T = 3.15f / 4.50f; // 0.700

    private AbyssalKingDeathVfx() {
    }

    public static void draw(
            Matrix4fc matrix,
            VertexConsumer out,
            float age,
            float deathProgress,
            float bodyYawDeg
    ) {
        float t = Mth.clamp(deathProgress, 0.0f, 1.0f);
        if (t <= 0.001f) {
            return;
        }

        // Convert entity body yaw into local forward/right axes matching MobRenderer orientation
        float yawRad = bodyYawDeg * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yawRad), 0.0, Mth.cos(yawRad));
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);

        Vec3 swordAnchor = forward.scale(AbyssalFallenKingEntity.PLANTED_SWORD_FORWARD_OFFSET)
                .add(0.0, 0.025, 0.0);
        Vec3 crownAnchor = forward.scale(AbyssalFallenKingEntity.FALLEN_CROWN_FORWARD_OFFSET)
                .add(right.scale(AbyssalFallenKingEntity.FALLEN_CROWN_RIGHT_OFFSET))
                .add(0.0, 0.025, 0.0);

        // Phase 1..2 (0.00..0.48): Dissipating violet aura wisps as the King loses strength
        if (t < 0.52f) {
            drawDissipatingAura(matrix, out, age, t);
        }

        // Phase 3..5 (0.50..1.00): Sword plant shockwave, ground cracks, stone shards & blade wisps
        if (t >= SWORD_IMPACT_T) {
            drawSwordPlantImpact(matrix, out, swordAnchor, age, t);
        }

        // Phase 4 (0.678..0.85): Crown landing ring & golden micro-sparks beside the sword
        if (t >= CROWN_IMPACT_T && t <= 0.86f) {
            drawCrownLandingImpact(matrix, out, crownAnchor, age, t);
        }

        // Phase 5 (0.70..1.00): Downward-falling ash & collapsing armor/magic fragments
        if (t >= CRUMBLE_START_T) {
            drawCrumblingAshAndFragments(matrix, out, age, t);
        }
    }

    private static void drawDissipatingAura(
            Matrix4fc matrix,
            VertexConsumer out,
            float age,
            float t
    ) {
        float fade = (float) (1.0 - PrayerVfxPrimitives.smooth(0.05, 0.50, t));
        if (fade <= 0.01f) {
            return;
        }

        double centerY = Mth.lerp((float) PrayerVfxPrimitives.smooth(0.18, 0.45, t), 1.55f, 1.05f);
        Vec3 center = new Vec3(0.0, centerY, 0.0);

        PrayerVfxPrimitives.ringPlane(
                matrix, out, center, PrayerVfxPrimitives.UP,
                1.05 + t * 0.45, 0.012, age * 0.02,
                VIOLET_R, VIOLET_G, VIOLET_B,
                0.42f * fade
        );

        for (int i = 0; i < 10; i++) {
            double ang = i * (Math.PI * 2.0 / 10.0) + age * 0.03 + t * 1.4;
            double rad = 0.65 + (i % 3) * 0.18 + t * 0.65;
            double driftY = ((age * 0.025 + i * 0.17) % 1.0) * 0.75;
            Vec3 pos = center.add(Math.cos(ang) * rad, -0.35 + driftY, Math.sin(ang) * rad);

            PrayerVfxPrimitives.crystal(
                    matrix, out, pos, PrayerVfxPrimitives.UP,
                    0.12 * fade, 0.024 * fade,
                    (i % 2 == 0) ? BRIGHT_VIOLET_R : DEEP_PURPLE_R,
                    (i % 2 == 0) ? BRIGHT_VIOLET_G : DEEP_PURPLE_G,
                    (i % 2 == 0) ? BRIGHT_VIOLET_B : DEEP_PURPLE_B,
                    0.68f * fade
            );
        }
    }

    private static void drawSwordPlantImpact(
            Matrix4fc matrix,
            VertexConsumer out,
            Vec3 anchor,
            float age,
            float t
    ) {
        float sinceImpact = Mth.clamp((t - SWORD_IMPACT_T) / (1.0f - SWORD_IMPACT_T), 0.0f, 1.0f);

        // 1. Expanding violet & royal-gold shockwave rings across the floor
        double waveT = PrayerVfxPrimitives.smooth(0.00, 0.42, sinceImpact);
        if (waveT < 0.995) {
            double waveEase = 1.0 - Math.pow(1.0 - waveT, 2.3);
            float waveAlpha = (float) Math.sin(Math.min(1.0, waveT * 1.1) * Math.PI) * 0.92f;

            PrayerVfxPrimitives.ringPlane(
                    matrix, out, anchor, PrayerVfxPrimitives.UP,
                    0.22 + waveEase * 2.35,
                    0.036 * (1.0 - waveT * 0.6),
                    age * 0.03,
                    VIOLET_R, VIOLET_G, VIOLET_B,
                    waveAlpha
            );
            PrayerVfxPrimitives.ringPlane(
                    matrix, out, anchor.add(0.0, 0.02, 0.0), PrayerVfxPrimitives.UP,
                    0.16 + waveEase * 1.65,
                    0.018 * (1.0 - waveT * 0.5),
                    -age * 0.04,
                    CROWN_GOLD_R, CROWN_GOLD_G, CROWN_GOLD_B,
                    waveAlpha * 0.72f
            );
        }

        // 2. Small luminous ground cracks radiating from the embedded sword blade
        float crackAlpha = (float) (0.95 * (1.0 - PrayerVfxPrimitives.smooth(0.28, 0.96, sinceImpact)));
        if (crackAlpha > 0.02f) {
            double crackExpand = PrayerVfxPrimitives.smooth(0.00, 0.16, sinceImpact);
            int crackCount = 10;
            for (int i = 0; i < crackCount; i++) {
                double ang = i * (Math.PI * 2.0 / crackCount) + 0.25;
                double len = (0.55 + (i % 3) * 0.28) * (0.25 + 0.75 * crackExpand);
                Vec3 end = anchor.add(Math.cos(ang) * len, 0.0, Math.sin(ang) * len);

                List<Vec3> pts = PrayerVfxPrimitives.jagged(anchor, end, 5, 8100L + i * 29L, 0.06);
                for (int j = 0; j < pts.size() - 1; j++) {
                    double taper = 1.0 - (j / (double) (pts.size() - 1)) * 0.45;
                    PrayerVfxGeometry.taperedTube(
                            matrix, out, pts.get(j), pts.get(j + 1),
                            0.022 * taper, 0.006 * taper,
                            DARK_MAGENTA_R, DARK_MAGENTA_G, DARK_MAGENTA_B,
                            0.65f * crackAlpha
                    );
                    PrayerVfxGeometry.taperedTube(
                            matrix, out, pts.get(j), pts.get(j + 1),
                            0.009 * taper, 0.002 * taper,
                            BRIGHT_VIOLET_R, BRIGHT_VIOLET_G, BRIGHT_VIOLET_B,
                            crackAlpha
                    );
                }
            }
        }

        // 3. Stone fragments & arcane splinters kicked up by the blade impact
        double shardT = PrayerVfxPrimitives.smooth(0.00, 0.48, sinceImpact);
        if (shardT < 0.995) {
            float shardFade = (float) (1.0 - PrayerVfxPrimitives.smooth(0.55, 1.00, shardT));
            for (int i = 0; i < 36; i++) {
                double h1 = fract(Math.sin(i * 19.31 + 2.71) * 43758.5453);
                double h2 = fract(Math.sin(i * 53.17 + 7.13) * 24634.6345);
                double ang = h1 * Math.PI * 2.0;

                double outDist = (0.18 + h2 * 1.15) * shardT;
                double arcY = Math.sin(shardT * Math.PI) * (0.38 + h1 * 0.55);
                Vec3 pos = anchor.add(Math.cos(ang) * outDist, arcY, Math.sin(ang) * outDist);

                Vec3 axis = PrayerVfxPrimitives.safeNormalize(
                        new Vec3(Math.cos(ang + shardT * 4.0), 1.0 - shardT * 1.4, Math.sin(ang + shardT * 4.0)),
                        PrayerVfxPrimitives.UP
                );

                boolean violetShard = i % 3 == 0;
                PrayerVfxPrimitives.crystal(
                        matrix, out, pos, axis,
                        0.085 + h2 * 0.06,
                        0.024 + h1 * 0.018,
                        violetShard ? VIOLET_R : ASH_R,
                        violetShard ? VIOLET_G : ASH_G,
                        violetShard ? VIOLET_B : ASH_B,
                        0.90f * shardFade
                );
            }
        }

        // 4. Residual violet wisps spiraling gently around the planted sword blade
        for (int w = 0; w < 6; w++) {
            double cycle = (age * 0.032 + w * 0.166) % 1.0;
            float wispAlpha = (float) Math.sin(cycle * Math.PI) * 0.58f;
            if (wispAlpha <= 0.02f) {
                continue;
            }
            double ang = w * (Math.PI * 2.0 / 6.0) + cycle * 3.2 + age * 0.04;
            double rad = 0.14 + (1.0 - cycle) * 0.12;
            Vec3 pos = anchor.add(Math.cos(ang) * rad, cycle * 1.05, Math.sin(ang) * rad);

            PrayerVfxPrimitives.crystal(
                    matrix, out, pos, PrayerVfxPrimitives.UP,
                    0.09 * (1.0 - cycle * 0.4),
                    0.018 * (1.0 - cycle * 0.4),
                    (w % 2 == 0) ? BRIGHT_VIOLET_R : VIOLET_R,
                    (w % 2 == 0) ? BRIGHT_VIOLET_G : VIOLET_G,
                    (w % 2 == 0) ? BRIGHT_VIOLET_B : VIOLET_B,
                    wispAlpha
            );
        }
    }

    private static void drawCrownLandingImpact(
            Matrix4fc matrix,
            VertexConsumer out,
            Vec3 crownAnchor,
            float age,
            float t
    ) {
        float u = Mth.clamp((t - CROWN_IMPACT_T) / 0.18f, 0.0f, 1.0f);
        float alpha = (float) Math.sin(u * Math.PI) * 0.80f;
        if (alpha <= 0.01f) {
            return;
        }

        double rad = 0.14 + u * 0.55;
        PrayerVfxPrimitives.ringPlane(
                matrix, out, crownAnchor, PrayerVfxPrimitives.UP,
                rad, 0.014 * (1.0 - u * 0.5), age * 0.05,
                CROWN_GOLD_R, CROWN_GOLD_G, CROWN_GOLD_B,
                alpha
        );

        for (int s = 0; s < 8; s++) {
            double ang = s * (Math.PI * 2.0 / 8.0) + u * 1.2;
            Vec3 pos = crownAnchor.add(
                    Math.cos(ang) * (0.12 + u * 0.34),
                    Math.sin(u * Math.PI) * 0.18,
                    Math.sin(ang) * (0.12 + u * 0.34)
            );
            PrayerVfxPrimitives.crystal(
                    matrix, out, pos, PrayerVfxPrimitives.UP,
                    0.055 * (1.0 - u * 0.4),
                    0.014 * (1.0 - u * 0.4),
                    CROWN_GOLD_R, CROWN_GOLD_G, CROWN_GOLD_B,
                    alpha
            );
        }
    }

    private static void drawCrumblingAshAndFragments(
            Matrix4fc matrix,
            VertexConsumer out,
            float age,
            float t
    ) {
        float c = Mth.clamp((t - CRUMBLE_START_T) / (1.0f - CRUMBLE_START_T), 0.0f, 1.0f);
        float fade = (float) (1.0 - PrayerVfxPrimitives.smooth(0.82, 1.00, c));
        if (fade <= 0.01f) {
            return;
        }

        // Ground-level arcane dissipation sigil as the King's remaining essence is absorbed into the floor
        Vec3 groundCenter = new Vec3(0.0, 0.02, 0.08);
        PrayerVfxPrimitives.ringPlane(
                matrix, out, groundCenter, PrayerVfxPrimitives.UP,
                1.05 * (1.0 - c * 0.55),
                0.018 * fade,
                -age * 0.035,
                DEEP_PURPLE_R, DEEP_PURPLE_G, DEEP_PURPLE_B,
                0.55f * fade
        );

        // 96 downward-drifting ash flakes, armor chips, and fading violet/soul embers
        double bodyCollapseTop = Mth.lerp(c, 1.85, 0.18);
        for (int i = 0; i < 96; i++) {
            double h1 = fract(Math.sin(i * 12.9898 + 4.1414) * 43758.5453);
            double h2 = fract(Math.sin(i * 78.233 + 1.7321) * 24634.6345);
            double h3 = fract(Math.sin(i * 39.425 + 9.1823) * 19283.4123);

            double fallCycle = fract(c * (1.35 + h3 * 0.85) + h1);
            double startY = 0.16 + h2 * bodyCollapseTop;
            double curY = Math.max(0.02, startY * (1.0 - fallCycle * fallCycle));

            double ang = h1 * Math.PI * 2.0 + fallCycle * 1.4 * ((i % 2 == 0) ? 1.0 : -1.0);
            double rad = (0.12 + h3 * 0.56) * (1.0 + fallCycle * 0.35);

            Vec3 pos = new Vec3(
                    Math.cos(ang) * rad,
                    curY,
                    0.08 + Math.sin(ang) * rad
            );

            Vec3 axis = PrayerVfxPrimitives.safeNormalize(
                    new Vec3(Math.cos(age * 0.12 + i), -1.2, Math.sin(age * 0.12 + i)),
                    PrayerVfxPrimitives.UP
            );

            float r;
            float g;
            float b;
            int slot = i % 6;
            if (slot == 0) {
                r = BRIGHT_VIOLET_R;
                g = BRIGHT_VIOLET_G;
                b = BRIGHT_VIOLET_B;
            } else if (slot == 1) {
                r = DARK_MAGENTA_R;
                g = DARK_MAGENTA_G;
                b = DARK_MAGENTA_B;
            } else {
                r = ASH_R + (float) (h2 * 0.08);
                g = ASH_G + (float) (h2 * 0.06);
                b = ASH_B + (float) (h2 * 0.10);
            }

            float alpha = (float) Math.sin(Math.min(1.0, fallCycle * 1.15) * Math.PI) * 0.86f * fade;
            if (alpha <= 0.02f) {
                continue;
            }

            double len = (0.055 + h2 * 0.065) * (1.0 - c * 0.35);
            double width = (0.018 + h1 * 0.020) * (1.0 - c * 0.35);

            PrayerVfxPrimitives.crystal(
                    matrix, out, pos, axis,
                    len, width,
                    r, g, b,
                    alpha
            );
        }
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }
}
