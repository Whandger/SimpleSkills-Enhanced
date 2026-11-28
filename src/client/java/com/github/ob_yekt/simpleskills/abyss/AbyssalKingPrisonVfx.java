package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

/**
 * Procedural 3D arcane containment prison and 4-phase rupture sequence for the Abyssal Fallen King.
 * Built exclusively on {@link PrayerVfxPrimitives} and {@link PrayerVfxGeometry} to match the
 * visual language and rendering pipeline of SimpleSkills Enhanced.
 */
public final class AbyssalKingPrisonVfx {

    // Palette: Deep Purple, Bright Violet, Dark Magenta, Translucent Abyssal Black, Royal Crown Gold
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

    private static final float ABYSS_BLACK_R = 0.08f;
    private static final float ABYSS_BLACK_G = 0.02f;
    private static final float ABYSS_BLACK_B = 0.15f;

    private static final float CROWN_GOLD_R = 0.96f;
    private static final float CROWN_GOLD_G = 0.78f;
    private static final float CROWN_GOLD_B = 0.28f;

    private static final int ORB_COUNT = 7;
    private static final int RUNE_COUNT = 8;
    private static final int CRYSTAL_COUNT = 10;
    private static final int CRACK_COUNT = 14;
    private static final int SHATTER_SHARD_COUNT = 180;

    private static final Vec3 RING_TILT_A = PrayerVfxPrimitives.safeNormalize(
            new Vec3(0.46, 1.0, 0.24), PrayerVfxPrimitives.UP);
    private static final Vec3 RING_TILT_B = PrayerVfxPrimitives.safeNormalize(
            new Vec3(-0.44, 1.0, 0.32), PrayerVfxPrimitives.UP);
    private static final Vec3 RING_TILT_C = PrayerVfxPrimitives.safeNormalize(
            new Vec3(0.18, 0.68, -0.71), PrayerVfxPrimitives.UP);

    private AbyssalKingPrisonVfx() {}

    /**
     * Renders the magical prison in {@code STATE_SITTING} and its 4-phase rupture across
     * {@code STATE_AWAKENING} ({@code awakeningProgress} from 0.0 to 1.0 over 48 ticks).
     */
    public static void draw(
            Matrix4fc matrix,
            VertexConsumer out,
            float age,
            int kingState,
            float awakeningProgress
    ) {
        if (kingState == AbyssalFallenKingEntity.STATE_ACTIVE) {
            return;
        }

        float t = kingState == AbyssalFallenKingEntity.STATE_AWAKENING
                ? Mth.clamp(awakeningProgress, 0.0f, 1.0f)
                : 0.0f;

        if (t >= 0.995f) {
            return;
        }

        // Phase envelopes synchronized with the 48-tick (2.4s) awakening animation:
        // Phase 1 [0.00 -> 0.30]: Instability — orbs accelerate, rings wobble, runes flicker, first cracks
        // Phase 2 [0.30 -> 0.62]: Rupture — cracks spread, violet lightning arcs, field overcharges
        // Phase 3 [0.62 -> 0.94]: Liberation — shatter into shards, shockwave, residual energy absorbed
        // Phase 4 [0.75 -> 1.00]: Awakening — prison vanishes cleanly as the King lands and enters combat
        double instability = PrayerVfxPrimitives.smooth(0.00, 0.30, t);
        double fracture = PrayerVfxPrimitives.smooth(0.30, 0.62, t);
        double liberation = PrayerVfxPrimitives.smooth(0.62, 0.94, t);
        double overcharge = Math.sin(clamp01((t - 0.25) / 0.42) * Math.PI);

        // Containment shell integrity drops sharply once liberation detonation triggers at t = 0.62
        float sealAlpha = (float) (1.0 - PrayerVfxPrimitives.smooth(0.58, 0.72, t));

        // Center aligned to the suspended King's torso/chest (King spans Y = 0.0 .. 3.12 in local pose)
        Vec3 center = new Vec3(0.0, 1.62, 0.0);

        // 1. Containment Field, Anchors & Orbital Gyroscope Rings
        if (sealAlpha > 0.005f) {
            drawContainmentField(matrix, out, center, age, instability, fracture, overcharge, sealAlpha);
            drawOrbitalRings(matrix, out, center, age, instability, fracture, overcharge, sealAlpha);
        }

        // 2. Seven Orbiting Energy Orbs, Sparks & Constellation Tethers
        drawEnergyOrbsAndTethers(matrix, out, center, age, instability, fracture, liberation, sealAlpha);

        // 3. Ancient Floating Runes & Faceted Energy Crystals
        drawFloatingRunes(matrix, out, center, age, instability, fracture, liberation);
        drawOrbitingCrystals(matrix, out, center, age, instability, liberation);

        // 4. Phase 1 & 2: Luminous Surface Cracks, Escaping Energy Jets & Violet Lightning Bolts
        if (t > 0.04f && sealAlpha > 0.01f) {
            drawFracturesAndLightning(matrix, out, center, age, instability, fracture, overcharge, sealAlpha);
        }

        // 5. Phase 3 & 4: Shatter Explosion Shards, Expanding Shockwave & Energy Absorption
        if (t >= 0.58f) {
            drawLiberationAndAbsorption(matrix, out, center, age, liberation, t);
        }
    }

    private static void drawContainmentField(
            Matrix4fc matrix,
            VertexConsumer out,
            Vec3 center,
            float age,
            double instability,
            double fracture,
            double overcharge,
            float sealAlpha
    ) {
        double pulseFreq = 0.085 + instability * 0.34 + fracture * 0.45;
        double pulseAmp = 0.022 + instability * 0.032 + fracture * 0.045;
        double pulse = 1.0 + Math.sin(age * pulseFreq) * pulseAmp + overcharge * 0.06;

        double rx = 1.48 * pulse;
        double ry = 2.05 * pulse;
        double rz = 1.48 * pulse;

        // Outer translucent deep-purple containment barrier (low alpha so the King remains clearly visible)
        float shellR = Mth.lerp((float) overcharge, DEEP_PURPLE_R, BRIGHT_VIOLET_R);
        float shellG = Mth.lerp((float) overcharge, DEEP_PURPLE_G, BRIGHT_VIOLET_G);
        float shellB = Mth.lerp((float) overcharge, DEEP_PURPLE_B, BRIGHT_VIOLET_B);
        float shellAlpha = (float) (0.052 + overcharge * 0.085) * sealAlpha;

        PrayerVfxGeometry.ellipsoid(
                matrix, out, center,
                new Vec3(rx, ry, rz),
                14, 20,
                shellR, shellG, shellB,
                shellAlpha,
                age * (0.012 + instability * 0.035)
        );

        // Inner dark abyssal-magenta harmonic veil
        PrayerVfxGeometry.ellipsoid(
                matrix, out, center,
                new Vec3(rx * 0.94, ry * 0.95, rz * 0.94),
                10, 16,
                Mth.lerp((float) fracture, ABYSS_BLACK_R, DARK_MAGENTA_R),
                Mth.lerp((float) fracture, ABYSS_BLACK_G, DARK_MAGENTA_G),
                Mth.lerp((float) fracture, ABYSS_BLACK_B, DARK_MAGENTA_B),
                (float) (0.042 + fracture * 0.055) * sealAlpha,
                -age * (0.016 + instability * 0.040)
        );

        // Geodesic containment lattice framing the King without obstructing his silhouette
        PrayerVfxGeometry.wireSphere(
                matrix, out, center,
                1.52 * pulse,
                18,
                age * (0.014 + instability * 0.045),
                Mth.lerp((float) overcharge, VIOLET_R, BRIGHT_VIOLET_R),
                Mth.lerp((float) overcharge, VIOLET_G, BRIGHT_VIOLET_G),
                Mth.lerp((float) overcharge, VIOLET_B, BRIGHT_VIOLET_B),
                (float) (0.34 + overcharge * 0.38) * sealAlpha,
                0.010 + overcharge * 0.006
        );

        // Lower sealing sigil beneath the suspended King & upper golden-violet crown anchor
        Vec3 lowerAnchor = center.add(0.0, -1.78, 0.0);
        PrayerVfxPrimitives.circleSigil(
                matrix, out, lowerAnchor,
                1.28 * pulse,
                age * (0.018 + instability * 0.05),
                (float) (0.58 + overcharge * 0.25) * sealAlpha,
                VIOLET_R, VIOLET_G, VIOLET_B
        );

        Vec3 upperAnchor = center.add(0.0, 1.84, 0.0);
        PrayerVfxPrimitives.ringPlane(
                matrix, out, upperAnchor, PrayerVfxPrimitives.UP,
                0.86 * pulse, 0.016, -age * 0.028,
                CROWN_GOLD_R, CROWN_GOLD_G, CROWN_GOLD_B,
                0.76f * sealAlpha
        );
        PrayerVfxPrimitives.ringPlane(
                matrix, out, upperAnchor.add(0.0, 0.06, 0.0), PrayerVfxPrimitives.UP,
                0.58 * pulse, 0.012, age * 0.042,
                VIOLET_R, VIOLET_G, VIOLET_B,
                0.72f * sealAlpha
        );
    }

    private static void drawOrbitalRings(
            Matrix4fc matrix,
            VertexConsumer out,
            Vec3 center,
            float age,
            double instability,
            double fracture,
            double overcharge,
            float sealAlpha
    ) {
        double speedBoost = 1.0 + instability * 2.4 + fracture * 3.8;
        double wobbleAmt = instability * 0.16 + fracture * 0.34;

        // Ring 1: Equatorial sovereign ring with inner golden filigree matching the King's crown
        Vec3 eqNormal = wobbleNormal(PrayerVfxPrimitives.UP, age * 0.32, wobbleAmt * 0.65);
        PrayerVfxPrimitives.ringPlane(
                matrix, out, center, eqNormal,
                1.66, 0.018 + overcharge * 0.008, age * 0.020 * speedBoost,
                VIOLET_R, VIOLET_G, VIOLET_B,
                0.82f * sealAlpha
        );
        PrayerVfxPrimitives.ringPlane(
                matrix, out, center.add(0.0, 0.03, 0.0), eqNormal,
                1.58, 0.010, -age * 0.030 * speedBoost,
                CROWN_GOLD_R, CROWN_GOLD_G, CROWN_GOLD_B,
                0.78f * sealAlpha
        );

        // Ring 2: Tilted dark-magenta containment band
        Vec3 tiltA = wobbleNormal(RING_TILT_A, age * 0.41 + 1.1, wobbleAmt);
        PrayerVfxPrimitives.ringPlane(
                matrix, out, center, tiltA,
                1.74, 0.015 + overcharge * 0.006, -age * 0.026 * speedBoost,
                DARK_MAGENTA_R, DARK_MAGENTA_G, DARK_MAGENTA_B,
                0.78f * sealAlpha
        );

        // Ring 3: Counter-tilted bright-violet containment band
        Vec3 tiltB = wobbleNormal(RING_TILT_B, age * 0.37 + 2.4, wobbleAmt);
        PrayerVfxPrimitives.ringPlane(
                matrix, out, center, tiltB,
                1.72, 0.015 + overcharge * 0.006, age * 0.029 * speedBoost,
                BRIGHT_VIOLET_R, BRIGHT_VIOLET_G, BRIGHT_VIOLET_B,
                0.76f * sealAlpha
        );

        // Ring 4: Steep abyssal-black & gold gyroscopic seal
        Vec3 tiltC = wobbleNormal(RING_TILT_C, age * 0.48 + 3.9, wobbleAmt * 1.15);
        PrayerVfxPrimitives.ringPlane(
                matrix, out, center, tiltC,
                1.62, 0.020, -age * 0.022 * speedBoost,
                ABYSS_BLACK_R, ABYSS_BLACK_G, ABYSS_BLACK_B,
                0.74f * sealAlpha
        );
        PrayerVfxPrimitives.ringPlane(
                matrix, out, center, tiltC,
                1.65, 0.008, age * 0.034 * speedBoost,
                CROWN_GOLD_R, CROWN_GOLD_G, CROWN_GOLD_B,
                0.68f * sealAlpha
        );
    }

    private static void drawEnergyOrbsAndTethers(
            Matrix4fc matrix,
            VertexConsumer out,
            Vec3 center,
            float age,
            double instability,
            double fracture,
            double liberation,
            float sealAlpha
    ) {
        double speedMul = 1.0 + instability * 2.6 + fracture * 4.2;
        double flingOut = liberation * liberation * 3.8;
        float orbAlpha = (float) (1.0 - PrayerVfxPrimitives.smooth(0.18, 0.85, liberation));
        if (orbAlpha <= 0.01f) return;

        Vec3[] orbPositions = new Vec3[ORB_COUNT];

        for (int i = 0; i < ORB_COUNT; i++) {
            double dir = (i % 2 == 0) ? 1.0 : -1.0;
            double baseSpeed = (0.022 + (i % 3) * 0.006) * dir * speedMul;
            double baseAngle = i * (Math.PI * 2.0 / ORB_COUNT) + age * baseSpeed;

            // Varied orbital heights and radii around the suspended King
            double tierY = -0.82 + (i / (double) (ORB_COUNT - 1)) * 1.78;
            double bob = Math.sin(age * (0.065 + i * 0.011) + i * 1.6) * (0.16 + instability * 0.14);
            double jitter = (instability * 0.12 + fracture * 0.24) * Math.sin(age * 1.15 + i * 2.7);

            double orbitRadius = 1.46 + (i % 3) * 0.09 + jitter + flingOut;
            Vec3 orbPos = center.add(
                    Math.cos(baseAngle) * orbitRadius,
                    tierY + bob + flingOut * (0.18 * (i - 3)),
                    Math.sin(baseAngle) * orbitRadius
            );
            orbPositions[i] = orbPos;

            double sizeVar = 0.115 + (i % 3) * 0.016 + ((i % 2) * 0.008);
            double orbRadius = sizeVar * (1.0 + Math.sin(age * 0.14 + i) * 0.08 + fracture * 0.28)
                    * (1.0 - liberation * 0.65);

            if (orbRadius <= 0.008) continue;

            // Layer 1: Bright white-violet inner core
            PrayerVfxGeometry.ellipsoid(
                    matrix, out, orbPos,
                    new Vec3(orbRadius * 0.58, orbRadius * 0.58, orbRadius * 0.58),
                    6, 10,
                    BRIGHT_VIOLET_R, BRIGHT_VIOLET_G, BRIGHT_VIOLET_B,
                    0.96f * orbAlpha,
                    age * 0.06 + i
            );

            // Layer 2: Intense deep-purple energy body
            PrayerVfxGeometry.ellipsoid(
                    matrix, out, orbPos,
                    new Vec3(orbRadius, orbRadius, orbRadius),
                    8, 12,
                    VIOLET_R, VIOLET_G, VIOLET_B,
                    0.88f * orbAlpha,
                    -age * 0.045 - i
            );

            // Layer 3: Translucent outer magenta-purple aura + geodesic cage
            PrayerVfxGeometry.ellipsoid(
                    matrix, out, orbPos,
                    new Vec3(orbRadius * 1.65, orbRadius * 1.65, orbRadius * 1.65),
                    8, 12,
                    DARK_MAGENTA_R, DARK_MAGENTA_G, DARK_MAGENTA_B,
                    0.24f * orbAlpha,
                    age * 0.03
            );

            PrayerVfxGeometry.wireSphere(
                    matrix, out, orbPos,
                    orbRadius * 1.88,
                    10,
                    -age * 0.05 + i,
                    (i % 3 == 0) ? CROWN_GOLD_R : BRIGHT_VIOLET_R,
                    (i % 3 == 0) ? CROWN_GOLD_G : BRIGHT_VIOLET_G,
                    (i % 3 == 0) ? CROWN_GOLD_B : BRIGHT_VIOLET_B,
                    0.56f * orbAlpha,
                    0.009
            );

            // Layer 4: Micro-sparks & trailing wisps emerging from each orb
            for (int s = 0; s < 4; s++) {
                double sparkCycle = ((age * (0.045 + instability * 0.06)) + i * 0.23 + s * 0.25) % 1.0;
                double sparkAlpha = Math.sin(sparkCycle * Math.PI) * 0.85 * orbAlpha;
                if (sparkAlpha <= 0.02) continue;

                double sparkAng = baseAngle + s * (Math.PI * 0.5) + sparkCycle * 2.4;
                double sparkDist = orbRadius * (1.2 + sparkCycle * 2.1);
                double sparkDy = (sparkCycle - 0.35) * 0.34 * ((s % 2 == 0) ? 1.0 : -1.0);

                Vec3 sparkPos = orbPos.add(
                        Math.cos(sparkAng) * sparkDist,
                        sparkDy,
                        Math.sin(sparkAng) * sparkDist
                );
                Vec3 sparkAxis = PrayerVfxPrimitives.safeNormalize(
                        sparkPos.subtract(orbPos), PrayerVfxPrimitives.UP);

                PrayerVfxPrimitives.crystal(
                        matrix, out, sparkPos, sparkAxis,
                        0.095 * (1.0 - sparkCycle * 0.4),
                        0.022 * (1.0 - sparkCycle * 0.4),
                        (s == 0) ? CROWN_GOLD_R : BRIGHT_VIOLET_R,
                        (s == 0) ? CROWN_GOLD_G : BRIGHT_VIOLET_G,
                        (s == 0) ? CROWN_GOLD_B : BRIGHT_VIOLET_B,
                        (float) sparkAlpha
                );
            }

            // Disintegration starburst as orbs fly apart in Phase 3
            if (liberation > 0.05 && liberation < 0.75) {
                float burstAlpha = (float) Math.sin(((liberation - 0.05) / 0.70) * Math.PI) * 0.90f;
                PrayerVfxPrimitives.starburst(
                        matrix, out, orbPos,
                        PrayerVfxPrimitives.safeNormalize(orbPos.subtract(center), PrayerVfxPrimitives.UP),
                        0.45 + liberation * 0.55,
                        burstAlpha,
                        BRIGHT_VIOLET_R, BRIGHT_VIOLET_G, BRIGHT_VIOLET_B
                );
            }
        }

        // Energy tether lines connecting neighboring orbs and sealing poles while containment holds
        if (sealAlpha > 0.02f) {
            float tetherFlicker = fracture > 0.05
                    ? (float) (0.55 + 0.45 * Math.sin(age * 1.9))
                    : 1.0f;
            float tetherAlpha = sealAlpha * tetherFlicker;

            Vec3 upperPole = center.add(0.0, 1.84, 0.0);
            Vec3 lowerPole = center.add(0.0, -1.78, 0.0);

            for (int i = 0; i < ORB_COUNT; i++) {
                Vec3 from = orbPositions[i];
                Vec3 next = orbPositions[(i + 1) % ORB_COUNT];

                // Outer magenta-violet energy sheath + bright inner filament
                PrayerVfxGeometry.taperedTube(
                        matrix, out, from, next,
                        0.022, 0.022,
                        DARK_MAGENTA_R, DARK_MAGENTA_G, DARK_MAGENTA_B,
                        0.32f * tetherAlpha
                );
                PrayerVfxGeometry.taperedTube(
                        matrix, out, from, next,
                        0.008, 0.008,
                        BRIGHT_VIOLET_R, BRIGHT_VIOLET_G, BRIGHT_VIOLET_B,
                        0.76f * tetherAlpha
                );

                // Cross-chords on alternate orbs to weave the arcane cage
                if (i % 2 == 0) {
                    Vec3 chordTarget = orbPositions[(i + 3) % ORB_COUNT];
                    PrayerVfxGeometry.taperedTube(
                            matrix, out, from, chordTarget,
                            0.007, 0.007,
                            DEEP_PURPLE_R, DEEP_PURPLE_G, DEEP_PURPLE_B,
                            0.44f * tetherAlpha
                    );

                    // Anchor line tying the orb to the upper or lower containment seal
                    Vec3 pole = (i % 4 == 0) ? upperPole : lowerPole;
                    PrayerVfxGeometry.taperedTube(
                            matrix, out, from, pole,
                            0.010, 0.004,
                            CROWN_GOLD_R, CROWN_GOLD_G, CROWN_GOLD_B,
                            0.36f * tetherAlpha
                    );
                }
            }
        }
    }

    private static void drawFloatingRunes(
            Matrix4fc matrix,
            VertexConsumer out,
            Vec3 center,
            float age,
            double instability,
            double fracture,
            double liberation
    ) {
        float runeFade = (float) (1.0 - PrayerVfxPrimitives.smooth(0.15, 0.95, liberation));
        if (runeFade <= 0.01f) return;

        double spin = -age * (0.014 + instability * 0.038);
        double expand = liberation * 2.2;

        for (int r = 0; r < RUNE_COUNT; r++) {
            double ang = r * (Math.PI * 2.0 / RUNE_COUNT) + spin;
            double rowY = ((r % 2 == 0) ? 0.58 : -0.52)
                    + Math.sin(age * 0.09 + r * 1.3) * (0.10 + instability * 0.12);
            double rad = 1.84 + expand + instability * 0.08 * Math.sin(age * 0.9 + r);

            Vec3 runePos = center.add(Math.cos(ang) * rad, rowY, Math.sin(ang) * rad);

            // In Phase 1 & 2 runes flicker and fail irregularly
            double flicker = 1.0;
            if (instability > 0.02) {
                double wave = Math.sin(age * (1.4 + r * 0.27) + r * 2.3);
                double failBias = instability * 0.35 + fracture * 0.55;
                flicker = wave < (failBias - 0.65) ? 0.12 : (0.65 + 0.35 * Math.abs(wave));
            }

            float alpha = (float) (0.78 * flicker) * runeFade;
            if (alpha <= 0.02f) continue;

            double scale = 0.21 * (1.0 - liberation * 0.45);
            PrayerVfxPrimitives.runeHalo(
                    matrix, out, runePos,
                    scale,
                    -ang + age * 0.02,
                    alpha,
                    VIOLET_R, VIOLET_G, VIOLET_B
            );

            // Golden ancient seal accent bar flanking each rune
            Vec3 tangent = new Vec3(-Math.sin(ang), 0.0, Math.cos(ang));
            PrayerVfxGeometry.tube(
                    matrix, out,
                    runePos.subtract(tangent.scale(scale * 0.72)).add(0.0, -scale * 0.85, 0.0),
                    runePos.add(tangent.scale(scale * 0.72)).add(0.0, -scale * 0.85, 0.0),
                    0.008,
                    CROWN_GOLD_R, CROWN_GOLD_G, CROWN_GOLD_B,
                    alpha * 0.82f
            );
        }
    }

    private static void drawOrbitingCrystals(
            Matrix4fc matrix,
            VertexConsumer out,
            Vec3 center,
            float age,
            double instability,
            double liberation
    ) {
        float crystalFade = (float) (1.0 - PrayerVfxPrimitives.smooth(0.10, 0.90, liberation));
        if (crystalFade <= 0.01f) return;

        double speed = 1.0 + instability * 2.5;
        double scatter = liberation * 2.8;

        for (int c = 0; c < CRYSTAL_COUNT; c++) {
            boolean upperBelt = c < 5;
            double dir = upperBelt ? 1.0 : -1.0;
            double ang = (c % 5) * (Math.PI * 2.0 / 5.0) + age * 0.024 * dir * speed + (upperBelt ? 0.3 : 0.9);
            double beltY = (upperBelt ? 0.96 : -0.94) + Math.sin(age * 0.11 + c * 1.9) * 0.14;
            double rad = (upperBelt ? 1.68 : 1.76) + scatter;

            Vec3 pos = center.add(
                    Math.cos(ang) * rad,
                    beltY + scatter * (upperBelt ? 0.35 : -0.25),
                    Math.sin(ang) * rad
            );

            double tilt = age * 0.07 * dir + c;
            Vec3 axis = PrayerVfxPrimitives.safeNormalize(
                    new Vec3(Math.cos(tilt) * 0.42, 1.0, Math.sin(tilt) * 0.42),
                    PrayerVfxPrimitives.UP
            );

            // Alternate between Bright Violet, Dark Magenta, and Royal Crown Gold
            float cr;
            float cg;
            float cb;
            if (c % 5 == 0) {
                cr = CROWN_GOLD_R;
                cg = CROWN_GOLD_G;
                cb = CROWN_GOLD_B;
            } else if (c % 2 == 0) {
                cr = VIOLET_R;
                cg = VIOLET_G;
                cb = VIOLET_B;
            } else {
                cr = DARK_MAGENTA_R;
                cg = DARK_MAGENTA_G;
                cb = DARK_MAGENTA_B;
            }

            double scale = 1.0 - liberation * 0.5;
            PrayerVfxPrimitives.crystal(
                    matrix, out, pos, axis,
                    0.22 * scale, 0.052 * scale,
                    cr, cg, cb,
                    0.86f * crystalFade
            );
        }
    }

    private static void drawFracturesAndLightning(
            Matrix4fc matrix,
            VertexConsumer out,
            Vec3 center,
            float age,
            double instability,
            double fracture,
            double overcharge,
            float sealAlpha
    ) {
        // Phase 1 starts with 6 small cracks; Phase 2 spreads up to 14 cracks across the entire surface
        int activeCracks = Math.min(CRACK_COUNT, 4 + (int) Math.round(instability * 4.0 + fracture * 6.0));
        double crackReach = 0.45 + instability * 0.45 + fracture * 0.75;
        float crackAlpha = (float) Math.min(1.0, instability * 0.65 + fracture * 0.85) * sealAlpha;

        for (int i = 0; i < activeCracks; i++) {
            double theta = i * 2.399963 + 0.4;
            double phi = -0.95 + ((i * 0.37) % 1.9);

            Vec3 p0 = pointOnEllipsoid(center, 1.49, 2.06, 1.49, theta, phi);
            Vec3 p1 = pointOnEllipsoid(
                    center, 1.49, 2.06, 1.49,
                    theta + 0.55 * crackReach * ((i % 2 == 0) ? 1.0 : -1.0),
                    Mth.clamp((float) (phi + 0.48 * crackReach * ((i % 3 == 0) ? -1.0 : 1.0)), -1.35f, 1.35f)
            );

            List<Vec3> crackPts = PrayerVfxPrimitives.jagged(p0, p1, 7, 4100L + i * 31L, 0.11 + fracture * 0.08);
            for (int j = 0; j < crackPts.size() - 1; j++) {
                double taper = 1.0 - (j / (double) (crackPts.size() - 1)) * 0.35;
                // Outer magenta-violet crack glow
                PrayerVfxGeometry.taperedTube(
                        matrix, out,
                        crackPts.get(j), crackPts.get(j + 1),
                        (0.022 + fracture * 0.016) * taper,
                        (0.008 + fracture * 0.006) * taper,
                        DARK_MAGENTA_R, DARK_MAGENTA_G, DARK_MAGENTA_B,
                        0.72f * crackAlpha
                );
                // Hot white-violet core fissure
                PrayerVfxGeometry.taperedTube(
                        matrix, out,
                        crackPts.get(j), crackPts.get(j + 1),
                        (0.009 + fracture * 0.007) * taper,
                        0.003 * taper,
                        BRIGHT_VIOLET_R, BRIGHT_VIOLET_G, BRIGHT_VIOLET_B,
                        0.95f * crackAlpha
                );
            }

            // Phase 2: Energy jets escaping outward through the fissures + violet lightning arcs
            if (fracture > 0.08) {
                Vec3 midCrack = crackPts.get(crackPts.size() / 2);
                Vec3 outward = PrayerVfxPrimitives.safeNormalize(midCrack.subtract(center), PrayerVfxPrimitives.UP);
                double jetLen = (0.35 + fracture * 0.95) * (0.75 + 0.25 * Math.sin(age * 0.9 + i));
                Vec3 jetTip = midCrack.add(outward.scale(jetLen));

                PrayerVfxGeometry.taperedTube(
                        matrix, out, midCrack, jetTip,
                        0.032 * fracture, 0.002,
                        VIOLET_R, VIOLET_G, VIOLET_B,
                        (float) (0.78 * fracture) * sealAlpha
                );

                // Violet lightning bolts arcing between cracks and across the failing barrier
                if (i % 2 == 0) {
                    long boltSeed = ((long) (age * 0.85)) * 97L + i * 17L;
                    Vec3 arcTarget = (i % 4 == 0)
                            ? pointOnEllipsoid(center, 1.54, 2.10, 1.54, theta + 1.35, -phi * 0.7)
                            : midCrack.add(outward.scale(0.65 + overcharge * 0.45));

                    drawVioletBolt(
                            matrix, out,
                            midCrack, arcTarget,
                            boltSeed, 6,
                            0.14 + fracture * 0.12,
                            (float) (0.85 * fracture) * sealAlpha
                    );
                }
            }
        }
    }

    private static void drawLiberationAndAbsorption(
            Matrix4fc matrix,
            VertexConsumer out,
            Vec3 center,
            float age,
            double liberation,
            float totalProgress
    ) {
        if (liberation <= 0.005) return;

        // 1. Expanding purple-violet shockwave rings & wave shell
        double waveEase = 1.0 - Math.pow(1.0 - liberation, 2.4);
        double waveRadius = 1.25 + waveEase * 5.2;
        float waveAlpha = (float) (Math.sin(Math.min(1.0, liberation * 1.15) * Math.PI) * 0.88);

        if (waveAlpha > 0.01f) {
            Vec3 groundWave = new Vec3(0.0, 0.18, 0.0);
            PrayerVfxPrimitives.ringPlane(
                    matrix, out, groundWave, PrayerVfxPrimitives.UP,
                    waveRadius, 0.048 * (1.0 - liberation * 0.6), age * 0.03,
                    VIOLET_R, VIOLET_G, VIOLET_B,
                    waveAlpha
            );
            PrayerVfxPrimitives.ringPlane(
                    matrix, out, groundWave.add(0.0, 0.04, 0.0), PrayerVfxPrimitives.UP,
                    waveRadius * 0.78, 0.024 * (1.0 - liberation * 0.5), -age * 0.04,
                    CROWN_GOLD_R, CROWN_GOLD_G, CROWN_GOLD_B,
                    waveAlpha * 0.72f
            );
            PrayerVfxGeometry.wireSphere(
                    matrix, out, center,
                    1.35 + waveEase * 3.6,
                    14, age * 0.04,
                    DARK_MAGENTA_R, DARK_MAGENTA_G, DARK_MAGENTA_B,
                    waveAlpha * 0.42f,
                    0.012 * (1.0 - liberation * 0.7)
            );
        }

        // 2. 180 procedural 3D energy shards blasting outward from the shattered prison
        float shardFade = (float) (1.0 - PrayerVfxPrimitives.smooth(0.45, 1.0, liberation));
        if (shardFade > 0.01f) {
            for (int i = 0; i < SHATTER_SHARD_COUNT; i++) {
                double h1 = fract(Math.sin(i * 12.9898 + 4.1414) * 43758.5453);
                double h2 = fract(Math.sin(i * 78.233 + 1.7321) * 24634.6345);
                double h3 = fract(Math.sin(i * 39.425 + 9.1823) * 19283.4123);

                double theta = h1 * Math.PI * 2.0;
                double cosPhi = h2 * 2.0 - 1.0;
                double sinPhi = Math.sqrt(Math.max(0.0, 1.0 - cosPhi * cosPhi));

                double dirX = sinPhi * Math.cos(theta);
                double dirY = cosPhi;
                double dirZ = sinPhi * Math.sin(theta);

                Vec3 start = center.add(dirX * 1.45, dirY * 2.00, dirZ * 1.45);
                double speed = 2.2 + h3 * 3.6;
                double travel = waveEase * speed;
                double gravityDrop = liberation * liberation * (0.45 + h2 * 0.55);

                Vec3 pos = start.add(dirX * travel, dirY * travel - gravityDrop, dirZ * travel);

                double spinA = age * (0.18 + h3 * 0.25) + i;
                Vec3 axis = PrayerVfxPrimitives.safeNormalize(
                        new Vec3(
                                dirX + Math.cos(spinA) * 0.6,
                                dirY + Math.sin(spinA) * 0.6,
                                dirZ + Math.cos(spinA * 0.7) * 0.6
                        ),
                        PrayerVfxPrimitives.UP
                );

                float sr;
                float sg;
                float sb;
                int paletteSlot = i % 10;
                if (paletteSlot == 0) {
                    sr = CROWN_GOLD_R;
                    sg = CROWN_GOLD_G;
                    sb = CROWN_GOLD_B;
                } else if (paletteSlot <= 3) {
                    sr = BRIGHT_VIOLET_R;
                    sg = BRIGHT_VIOLET_G;
                    sb = BRIGHT_VIOLET_B;
                } else if (paletteSlot <= 6) {
                    sr = DEEP_PURPLE_R;
                    sg = DEEP_PURPLE_G;
                    sb = DEEP_PURPLE_B;
                } else if (paletteSlot <= 8) {
                    sr = DARK_MAGENTA_R;
                    sg = DARK_MAGENTA_G;
                    sb = DARK_MAGENTA_B;
                } else {
                    sr = ABYSS_BLACK_R;
                    sg = ABYSS_BLACK_G;
                    sb = ABYSS_BLACK_B;
                }

                double shardLen = (0.14 + h3 * 0.18) * (1.0 - liberation * 0.45);
                double shardRad = (0.028 + h1 * 0.032) * (1.0 - liberation * 0.45);

                if (i % 3 == 0) {
                    PrayerVfxPrimitives.crystal(
                            matrix, out, pos, axis,
                            shardLen, shardRad,
                            sr, sg, sb,
                            0.88f * shardFade
                    );
                } else {
                    Vec3[] frame = PrayerVfxPrimitives.basis(axis);
                    Vec3 tip = pos.add(axis.scale(shardLen * 0.75));
                    Vec3 left = pos.subtract(axis.scale(shardLen * 0.35)).add(frame[0].scale(shardRad * 1.35));
                    Vec3 right = pos.subtract(axis.scale(shardLen * 0.35)).subtract(frame[0].scale(shardRad * 1.35));
                    PrayerVfxGeometry.triangle(
                            matrix, out, left, right, tip,
                            sr, sg, sb,
                            0.84f * shardFade
                    );
                }
            }
        }

        // 3. Residual arcane energy absorbed into the King's chest & blade as he awakens
        double absorb = PrayerVfxPrimitives.smooth(0.68, 0.98, totalProgress);
        if (absorb > 0.01 && absorb < 0.99) {
            float absorbAlpha = (float) Math.sin(absorb * Math.PI) * 0.85f;
            Vec3 kingChest = new Vec3(0.0, 1.68, 0.0);

            for (int stream = 0; stream < 8; stream++) {
                double dir = (stream % 2 == 0) ? 1.0 : -1.0;
                double ang = stream * (Math.PI * 2.0 / 8.0) + age * 0.09 * dir + absorb * 3.8 * dir;
                double startR = 2.45 * (1.0 - absorb) + 0.18;
                double startY = 0.45 + (stream % 4) * 0.72;
                double curY = Mth.lerp(absorb, startY, kingChest.y);

                Vec3 head = new Vec3(Math.cos(ang) * startR, curY, Math.sin(ang) * startR);
                double prevAbsorb = Math.max(0.0, absorb - 0.14);
                double prevR = 2.45 * (1.0 - prevAbsorb) + 0.18;
                double prevY = Mth.lerp(prevAbsorb, startY, kingChest.y);
                double prevAng = ang - 0.55 * dir;
                Vec3 tail = new Vec3(Math.cos(prevAng) * prevR, prevY, Math.sin(prevAng) * prevR);

                boolean goldStream = stream % 4 == 0;
                PrayerVfxGeometry.taperedTube(
                        matrix, out, tail, head,
                        0.030 * (1.0 - absorb * 0.5),
                        0.004,
                        goldStream ? CROWN_GOLD_R : BRIGHT_VIOLET_R,
                        goldStream ? CROWN_GOLD_G : BRIGHT_VIOLET_G,
                        goldStream ? CROWN_GOLD_B : BRIGHT_VIOLET_B,
                        absorbAlpha
                );
            }
        }
    }

    private static void drawVioletBolt(
            Matrix4fc matrix,
            VertexConsumer out,
            Vec3 start,
            Vec3 end,
            long seed,
            int segments,
            double amplitude,
            float alpha
    ) {
        if (alpha <= 0.01f) return;
        List<Vec3> points = PrayerVfxPrimitives.jagged(start, end, segments, seed, amplitude);
        for (int i = 0; i < points.size() - 1; i++) {
            double taper = 1.0 - (i / (double) (points.size() - 1)) * 0.35;
            PrayerVfxGeometry.tube(
                    matrix, out, points.get(i), points.get(i + 1),
                    0.048 * taper,
                    DARK_MAGENTA_R, DARK_MAGENTA_G, DARK_MAGENTA_B,
                    0.26f * alpha
            );
            PrayerVfxGeometry.tube(
                    matrix, out, points.get(i), points.get(i + 1),
                    0.020 * taper,
                    VIOLET_R, VIOLET_G, VIOLET_B,
                    0.82f * alpha
            );
            PrayerVfxGeometry.tube(
                    matrix, out, points.get(i), points.get(i + 1),
                    0.008 * taper,
                    BRIGHT_VIOLET_R, BRIGHT_VIOLET_G, BRIGHT_VIOLET_B,
                    alpha
            );
        }
    }

    private static Vec3 wobbleNormal(Vec3 base, double phase, double amount) {
        if (amount <= 1.0E-5) return base;
        Vec3 perturbed = base.add(
                Math.sin(phase) * amount,
                0.0,
                Math.cos(phase * 1.17) * amount
        );
        return PrayerVfxPrimitives.safeNormalize(perturbed, base);
    }

    private static Vec3 pointOnEllipsoid(Vec3 center, double rx, double ry, double rz, double theta, double phi) {
        double cosPhi = Math.cos(phi);
        return center.add(
                cosPhi * Math.cos(theta) * rx,
                Math.sin(phi) * ry,
                cosPhi * Math.sin(theta) * rz
        );
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
