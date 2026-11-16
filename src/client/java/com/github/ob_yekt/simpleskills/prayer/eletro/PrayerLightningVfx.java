package com.github.ob_yekt.simpleskills.prayer.eletro;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

/** Procedural 3D geometry and animation for the Lightning Bolt spell (Tier 2). */
public final class PrayerLightningVfx {
    private PrayerLightningVfx() {}

    // 22. LIGHTNING BOLT — Carga compacta na mão, disparo único de alta tensão com arco secundário e impacto seco.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 hand, Vec3 destination,
                            float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 1, 5);
        if (fade <= 0.01f) return;

        Vec3 target = destination.subtract(hand).lengthSqr() < 0.04
                ? hand.add(0, 0, -9.0)
                : destination;
        Vec3 path = target.subtract(hand);
        double dist = path.length();
        Vec3 dir = path.scale(1.0 / dist);
        Vec3[] frame = PrayerVfxPrimitives.basis(dir);

        // Sincronizado com PrayerSoundtrack
        double impactTick = Math.max(10.0, duration * 0.52);

        long castSeed = Double.doubleToLongBits(target.x) * 31L
                ^ Double.doubleToLongBits(target.y) * 17L
                ^ Double.doubleToLongBits(target.z);
        long flickerSeed = castSeed ^ ((long) Math.floor(age * 0.85f) * 104729L);

        // ------------------------------------------------------------------
        // 1. CARGA COMPACTA NA MÃO (0 até impactTick)
        // ------------------------------------------------------------------
        double chargeBuild = PrayerVfxPrimitives.smooth(0, impactTick * 0.88, age);
        double postCastDecay = 1.0 - PrayerVfxPrimitives.smooth(impactTick, impactTick + 4.0, age);
        float handPower = (float) (chargeBuild * postCastDecay) * fade;

        if (handPower > 0.01f) {
            Vec3 palm = hand.add(dir.scale(0.16));

            // Núcleo elétrico pequeno na palma
            double coreR = 0.04 + chargeBuild * 0.07;
            PrayerVfxGeometry.ellipsoid(m, out, palm, new Vec3(coreR * 1.9, coreR * 1.9, coreR * 1.9),
                    6, 8, 0.20f, 0.65f, 1.0f, 0.35f * handPower, age * 0.4);
            PrayerVfxGeometry.ellipsoid(m, out, palm, new Vec3(coreR, coreR, coreR),
                    6, 8, 0.95f, 0.99f, 1.0f, 0.95f * handPower, -age * 0.55);

            // 3 faíscas curtas sendo puxadas para a palma
            for (int a = 0; a < 3; a++) {
                double ang = age * 0.45 + a * (Math.PI * 2.0 / 3.0);
                double outerDist = 0.36 - chargeBuild * 0.16;
                Vec3 arcStart = palm.subtract(dir.scale(0.05))
                        .add(frame[0].scale(Math.cos(ang) * outerDist))
                        .add(frame[1].scale(Math.sin(ang) * outerDist));
                List<Vec3> pullBolt = buildBolt(arcStart, palm, 3, flickerSeed + a * 61L, 0.035);
                drawBolt(m, out, pullBolt, 0.024, 0.010, 0.004, 0.80f * handPower);
            }
        }

        // ------------------------------------------------------------------
        // 2. DISPARO ÚNICO DE RELÂMPAGO + 1 ARCO SECUNDÁRIO FINO
        // ------------------------------------------------------------------
        if (age >= impactTick - 0.3) {
            double strikeAge = Math.max(0.0, age - impactTick);
            double strikeDur = Math.max(6.0, duration - impactTick);
            double strikeP = Math.min(1.0, strikeAge / strikeDur);

            float surge = (float) Math.exp(-strikeAge * 0.35);
            float sustain = (float) Math.pow(1.0 - strikeP, 1.5);
            float boltAlpha = Math.min(1.0f, 0.25f * sustain + 0.90f * surge) * fade;

            if (boltAlpha > 0.01f) {
                int segments = Math.max(10, Math.min(18, (int) (dist * 1.8)));
                double thickness = 0.75 + 0.50 * surge;

                // Canal principal: zigue-zague afiado e bem definido
                double mainAmp = Math.min(0.26, 0.10 + dist * 0.016);
                List<Vec3> mainBolt = buildBolt(hand, target, segments, flickerSeed, mainAmp);
                drawBolt(m, out, mainBolt,
                        0.16 * thickness,
                        0.068 * thickness,
                        0.025 * thickness,
                        boltAlpha);

                // Apenas 1 arco secundário fino serpenteando pelo trajeto (sem poluir)
                List<Vec3> secondaryBolt = buildBolt(hand, target, segments, flickerSeed + 733L, mainAmp * 1.35);
                drawBolt(m, out, secondaryBolt,
                        0.055 * thickness,
                        0.022 * thickness,
                        0.008 * thickness,
                        boltAlpha * 0.70f);

                // ------------------------------------------------------------------
                // 3. IMPACTO SECO NO ALVO
                // ------------------------------------------------------------------
                drawCleanImpact(m, out, target, dir, age, strikeAge, strikeP, flickerSeed, fade);
            }
        }
    }

    private static void drawCleanImpact(Matrix4fc m, VertexConsumer out, Vec3 target, Vec3 dir,
                                        float age, double strikeAge, double strikeP, long flickerSeed,
                                        float fade) {
        float impactFade = (float) Math.pow(1.0 - strikeP, 1.1) * fade;
        if (impactFade <= 0.01f) return;

        // Clarão rápido e pontual no ponto de impacto
        if (strikeAge < 4.0) {
            double flashP = strikeAge / 4.0;
            float flashA = (float) Math.pow(1.0 - flashP, 1.4) * fade;

            PrayerVfxPrimitives.starburst(m, out, target, dir.scale(-1.0),
                    0.35 + flashP * 1.10, flashA, 0.75f, 0.95f, 1.0f);

            double coreR = 0.22 * (1.0 - flashP * 0.5);
            PrayerVfxGeometry.ellipsoid(m, out, target, new Vec3(coreR, coreR, coreR),
                    6, 8, 0.96f, 0.99f, 1.0f, 0.95f * flashA, age * 0.4);
        }

        // 1 único anel de choque perpendicular ao tiro
        if (strikeAge < 6.0) {
            double waveP = strikeAge / 6.0;
            float waveA = (float) Math.pow(1.0 - waveP, 1.3) * fade;

            PrayerVfxPrimitives.ringPlane(m, out, target.subtract(dir.scale(0.1)), dir.scale(-1.0),
                    0.18 + waveP * 1.05, 0.042 * (1.0 - waveP * 0.6), age * 0.15,
                    0.45f, 0.88f, 1.0f, 0.85f * waveA);
        }

        // 3 pequenas descargas aterrando no chão abaixo do alvo
        for (int g = 0; g < 3; g++) {
            double ang = g * (Math.PI * 2.0 / 3.0) + 0.5;
            double reach = 0.45 + strikeP * 0.25;
            Vec3 end = target.add(Math.cos(ang) * reach, -0.75, Math.sin(ang) * reach);
            List<Vec3> groundBolt = buildBolt(target, end, 4, flickerSeed + g * 199L, 0.06);
            drawBolt(m, out, groundBolt, 0.040, 0.015, 0.005, impactFade * 0.68f);
        }
    }

    private static void drawBolt(Matrix4fc m, VertexConsumer out, List<Vec3> pts,
                                 double outerR, double midR, double coreR, float alpha) {
        if (alpha <= 0.005f || pts.size() < 2) return;
        int last = pts.size() - 1;
        for (int i = 0; i < last; i++) {
            double t0 = 1.0 - (i / (double) last) * 0.30;
            double t1 = 1.0 - ((i + 1) / (double) last) * 0.30;
            Vec3 a = pts.get(i);
            Vec3 b = pts.get(i + 1);
            PrayerVfxGeometry.taperedTube(m, out, a, b, outerR * t0, outerR * t1,
                    0.12f, 0.48f, 1.0f, 0.18f * alpha);
            PrayerVfxGeometry.taperedTube(m, out, a, b, midR * t0, midR * t1,
                    0.38f, 0.85f, 1.0f, 0.85f * alpha);
            PrayerVfxGeometry.taperedTube(m, out, a, b, coreR * t0, coreR * t1,
                    0.96f, 0.99f, 1.0f, alpha);
        }
    }

    private static List<Vec3> buildBolt(Vec3 from, Vec3 to, int segments, long seed, double amplitude) {
        Random rng = new Random(seed);
        List<Vec3> points = new ArrayList<>(segments + 1);
        Vec3 diff = to.subtract(from);
        Vec3[] frame = PrayerVfxPrimitives.basis(diff);
        points.add(from);
        for (int i = 1; i < segments; i++) {
            double t = i / (double) segments;
            double env = Math.sin(Math.PI * t);
            double jitter = amplitude * (0.20 + 0.80 * env);
            double ox = (rng.nextDouble() * 2.0 - 1.0) * jitter;
            double oy = (rng.nextDouble() * 2.0 - 1.0) * jitter;
            points.add(from.add(diff.scale(t))
                    .add(frame[0].scale(ox))
                    .add(frame[1].scale(oy)));
        }
        points.add(to);
        return points;
    }
}
