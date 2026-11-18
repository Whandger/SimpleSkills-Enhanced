package com.github.ob_yekt.simpleskills.runes.tools;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.ItemGeometryInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

import java.util.List;

import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.boltSegment;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.fract;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.lerpVec;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.mix;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.unit;

public final class StormToolVfx {
    private StormToolVfx() {}

    public static void submit(SubmitNodeCollector collector, PoseStack poseStack,
                              ItemGeometryInfo info, double time, int seed) {
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, vertices) ->
                draw(pose.pose(), vertices, info, time, seed));
    }

    private static void draw(Matrix4fc matrix, VertexConsumer out, ItemGeometryInfo info,
                             double time, int seed) {
        List<Vec3> samples = info.allSamples();
        int n = samples.size();
        if (n < 2) return;

        double scale = info.bounds().strokeScale() * 0.78;
        int boltCount = 3;

        for (int bolt = 0; bolt < boltCount; bolt++) {
            double boltTime = time * 0.38 + bolt * 1.43 + unit(seed, bolt * 19) * 2.0;
            long burst = (long) Math.floor(boltTime);
            double phase = fract(boltTime);
            long boltSeed = mix(burst * 0x9E3779B97F4A7C15L + seed * 43L + bolt * 157L);
            if (unit(boltSeed, 0) < 0.15) continue;

            double attack = Math.min(1.0, phase * 16.0);
            double decay = Math.pow(1.0 - phase, 0.62);
            double pulse = attack * decay * (0.85 + 0.15 * Math.sin(time * 11.0 + bolt * 3.7));
            if (pulse < 0.05) continue;

            int idxA = (int) (unit(boltSeed, 1) * n) % n;
            int idxB = (int) (unit(boltSeed, 2) * n) % n;
            if (idxA == idxB) idxB = (idxA + Math.max(1, n / 3)) % n;

            double zFace = ((bolt + burst) & 1) == 0 ? 0.0045 : -0.0045;
            Vec3 pStart = samples.get(idxA).add(0, 0, zFace);
            Vec3 pEnd = samples.get(idxB).add(0, 0, zFace);
            Vec3 span = pEnd.subtract(pStart);
            double dist = span.length();
            if (dist < 0.025) continue;

            Vec3 dir = span.scale(1.0 / dist);
            Vec3 perpXY = new Vec3(-dir.y, dir.x, 0.0);
            if (perpXY.lengthSqr() < 1.0E-6) perpXY = new Vec3(1, 0, 0);
            perpXY = perpXY.normalize();

            Vec3 prev = pStart;
            Vec3 forkOrigin = null;
            int steps = 6;
            for (int step = 1; step <= steps; step++) {
                double t = step / (double) steps;
                double envelope = Math.sin(Math.PI * t);
                double jitterXY = (unit(boltSeed, 10 + step) - 0.5) * 0.022 * envelope
                        + Math.sin(time * 3.1 + step * 2.5 + bolt) * 0.004 * envelope;
                double jitterZ = (unit(boltSeed, 30 + step) - 0.5) * 0.005 * envelope;

                Vec3 next = pStart.add(span.scale(t))
                        .add(perpXY.scale(jitterXY))
                        .add(0, 0, jitterZ);
                if (step == 3) forkOrigin = next;

                boltSegment(matrix, out, prev, next, pulse, scale * (1.0 - t * 0.35));
                prev = next;
            }

            if (forkOrigin != null && unit(boltSeed, 70) > 0.35) {
                int idxC = (int) (unit(boltSeed, 71) * n) % n;
                Vec3 forkTarget = lerpVec(forkOrigin, samples.get(idxC).add(0, 0, zFace), 0.55);
                if (forkOrigin.distanceToSqr(forkTarget) > 1.0E-5) {
                    boltSegment(matrix, out, forkOrigin, forkTarget, pulse * 0.72, scale * 0.65);
                }
            }

            double nodeR = 0.0055 * scale * pulse;
            PrayerVfxGeometry.ellipsoid(matrix, out, pStart, new Vec3(nodeR, nodeR, nodeR),
                    4, 6, 0.72f, 0.96f, 1.0f, (float) (0.85 * pulse), time * 0.4);
            PrayerVfxGeometry.ellipsoid(matrix, out, pEnd, new Vec3(nodeR * 0.85, nodeR * 0.85, nodeR * 0.85),
                    4, 6, 0.88f, 0.99f, 1.0f, (float) (0.90 * pulse), -time * 0.4);
        }
    }
}
