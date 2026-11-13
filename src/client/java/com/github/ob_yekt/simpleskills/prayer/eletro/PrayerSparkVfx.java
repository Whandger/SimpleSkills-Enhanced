package com.github.ob_yekt.simpleskills.prayer.eletro;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerSparkVfx {
    private static final float VISUAL_TICKS = 5.0f;

    private PrayerSparkVfx() {}

    // 21. SPARK — Relâmpago instantâneo da mão ao alvo com 3 ramificações e impacto imediato.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 hand, Vec3 destination,
                            float age, int duration) {
        float maxLife = Math.min(duration, VISUAL_TICKS);
        if (age < 0.0f || age >= maxLife) return;

        // Impacto imediato no frame 0 e dissipação rápida em 5 ticks (0.25s)
        float progress = age / maxLife;
        float fade = (float) Math.pow(1.0f - progress, 1.45);
        if (fade <= 0.01f) return;

        Vec3 target = destination.subtract(hand).lengthSqr() < 0.04
                ? hand.add(0, 0, -5.0)
                : destination;
        Vec3 path = target.subtract(hand);
        double dist = path.length();
        Vec3 dir = path.scale(1.0 / dist);
        Vec3[] frame = PrayerVfxPrimitives.basis(dir);

        // Seed fixa por disparo para manter um único relâmpago nítido e estável
        long seed = Double.doubleToLongBits(target.x) * 31L
                ^ Double.doubleToLongBits(target.y) * 17L
                ^ Double.doubleToLongBits(target.z);

        // 1. Canal principal do relâmpago (já conectado da mão até o mob no frame 0)
        int segments = Math.max(8, Math.min(16, (int) (dist * 2.0)));
        double amp = Math.min(0.28, 0.09 + dist * 0.025);
        List<Vec3> mainBolt = buildBolt(hand, target, segments, seed, amp);
        drawBolt(m, out, mainBolt, 0.090, 0.034, 0.012, fade);

        // 2. Três ramificações distribuídas ao longo do canal principal (25%, 52% e 78%)
        if (mainBolt.size() >= 6) {
            int[] splitIndices = {
                Math.max(1, mainBolt.size() / 4),
                mainBolt.size() / 2,
                Math.min(mainBolt.size() - 2, (mainBolt.size() * 3) / 4)
            };
            double[] baseAngles = { 0.75, -2.05, 2.35 };
            for (int b = 0; b < splitIndices.length; b++) {
                Vec3 root = mainBolt.get(splitIndices[b]);
                Random rng = new Random(seed + (b + 1) * 409L);
                double forkLen = 0.30 + rng.nextDouble() * 0.24;
                double theta = baseAngles[b] + (rng.nextDouble() - 0.5) * 0.45;
                Vec3 forkEnd = root.add(dir.scale(forkLen * 0.72))
                        .add(frame[0].scale(Math.cos(theta) * forkLen))
                        .add(frame[1].scale(Math.sin(theta) * forkLen));
                List<Vec3> fork = buildBolt(root, forkEnd, 3, seed + b * 97L, 0.048);
                drawBolt(m, out, fork, 0.038, 0.014, 0.004, fade * 0.78f);
            }
        }

        // 3. Estalo rápido na mão no momento do disparo (primeiros 2 ticks)
        if (age < 2.0f) {
            float handAlpha = (1.0f - age / 2.0f) * fade;
            for (int i = 0; i < 3; i++) {
                double a = i * (Math.PI * 2.0 / 3.0) + 0.4;
                double r = 0.14 + age * 0.04;
                Vec3 tip = hand.add(dir.scale(0.12))
                        .add(frame[0].scale(Math.cos(a) * r))
                        .add(frame[1].scale(Math.sin(a) * r));
                PrayerVfxGeometry.taperedTube(m, out, hand, tip, 0.016, 0.001,
                        0.45f, 0.86f, 1.0f, 0.85f * handAlpha);
                PrayerVfxGeometry.taperedTube(m, out, hand, tip, 0.006, 0.001,
                        0.96f, 1.0f, 1.0f, handAlpha);
            }
        }

        // 4. Hit imediato no mob desde o primeiro frame (anel de choque + 4 descargas curtas)
        double hitBurst = Math.min(1.0, age / 1.5);
        double ringRadius = 0.18 + hitBurst * 0.38;
        PrayerVfxGeometry.torus(m, out, target, ringRadius, 0.018 * (1.0 - progress * 0.65),
                28, 0.0, 0.45f, 0.88f, 1.0f, 0.90f * fade);

        for (int s = 0; s < 4; s++) {
            double ang = s * (Math.PI / 2.0) + 0.45;
            double reach = 0.28 + hitBurst * 0.26;
            Vec3 end = target.add(Math.cos(ang) * reach,
                    ((s % 2 == 0) ? 0.22 : -0.22),
                    Math.sin(ang) * reach);
            List<Vec3> spark = buildBolt(target, end, 3, seed + s * 53L, 0.042);
            drawBolt(m, out, spark, 0.036, 0.014, 0.004, fade * 0.90f);
        }
    }

    private static void drawBolt(Matrix4fc m, VertexConsumer out, List<Vec3> pts,
                                 double outerR, double midR, double coreR, float alpha) {
        if (alpha <= 0.005f || pts.size() < 2) return;
        int last = pts.size() - 1;
        for (int i = 0; i < last; i++) {
            double t0 = 1.0 - (i / (double) last) * 0.45;
            double t1 = 1.0 - ((i + 1) / (double) last) * 0.45;
            Vec3 a = pts.get(i);
            Vec3 b = pts.get(i + 1);
            PrayerVfxGeometry.taperedTube(m, out, a, b, outerR * t0, outerR * t1,
                    0.12f, 0.48f, 1.0f, 0.18f * alpha);
            PrayerVfxGeometry.taperedTube(m, out, a, b, midR * t0, midR * t1,
                    0.38f, 0.84f, 1.0f, 0.85f * alpha);
            PrayerVfxGeometry.taperedTube(m, out, a, b, coreR * t0, coreR * t1,
                    0.96f, 1.0f, 1.0f, alpha);
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
            double jitter = amplitude * (0.25 + 0.75 * env);
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
