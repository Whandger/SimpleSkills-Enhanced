package com.github.ob_yekt.simpleskills.runes.armor;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.Bounds;
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

public final class InfernoArmorVfx {
    private InfernoArmorVfx() {}

    public static void submit(SubmitNodeCollector collector, PoseStack poseStack,
                              Bounds bounds, double time, int seed) {
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, vertices) ->
                drawHeatAndEmbers(pose.pose(), vertices, bounds, time, seed));
    }

    private static void drawHeatAndEmbers(Matrix4fc matrix, VertexConsumer out, Bounds bounds,
                                          double time, int seed) {
        Vec3 center = boundsCenter(bounds);
        // Raio posicionado por fora da caixa de cada peça para não cortar dentro da armadura
        double rx = bounds.width() * 0.54 + 0.024;
        double rz = bounds.depth() * 0.54 + 0.024;

        // Seed única por peça para braços, pernas e botas não ficarem espelhados
        long pieceSalt = mix(seed * 97L
                + Double.doubleToLongBits(bounds.minX()) * 31L
                + Double.doubleToLongBits(bounds.minY()) * 17L
                + Double.doubleToLongBits(bounds.height()) * 13L);

        boolean isTorso = bounds.height() > 0.55;
        boolean isMedium = bounds.height() > 0.34;
        int emberCount = isTorso ? 16 : (isMedium ? 11 : 8);
        int sparkCount = isTorso ? 12 : (isMedium ? 8 : 6);

        // 1. Brasas incandescentes subindo com a convecção do calor ao redor de toda a armadura
        drawRisingEmbers(matrix, out, bounds, center, rx, rz, time, pieceSalt, emberCount);

        // 2. Faíscas de calor (estilo Divine) que surgem perto da armadura e estouram em fagulhas
        drawCracklingHeatSparks(matrix, out, bounds, center, rx, rz, time, pieceSalt, sparkCount);
    }

    /**
     * Brasas vivas que se desprendem da superfície da armadura, sobem (-Y) oscilando
     * no ar quente e esfriam de amarelo-branco para laranja vivo e vermelho-brasa.
     */
    private static void drawRisingEmbers(Matrix4fc matrix, VertexConsumer out, Bounds bounds,
                                         Vec3 center, double rx, double rz,
                                         double time, long pieceSalt, int count) {
        double scale = bounds.strokeScale();

        for (int i = 0; i < count; i++) {
            long eSeed = mix(pieceSalt + i * 419L);
            double life = fract(time * 0.026 + i * (1.0 / count) + unit(eSeed, 1));
            double fade = Math.sin(Math.PI * Math.pow(life, 0.75));
            if (fade < 0.05) continue;

            double baseAngle = unit(eSeed, 2) * Math.PI * 2.0 + time * 0.022;
            double turbulence = Math.sin(time * 0.16 + i * 2.3 + life * 4.5) * 0.22;
            double angle = baseAngle + turbulence;

            // Abre levemente para fora enquanto sobe com o ar quente
            double radial = 0.94 + unit(eSeed, 3) * 0.24 + life * 0.22;
            double startY = lerp(bounds.maxY() - 0.02, bounds.minY() + 0.04, unit(eSeed, 4));
            double riseY = -life * (0.16 + unit(eSeed, 5) * 0.12);

            Vec3 pos = new Vec3(
                    center.x + Math.cos(angle) * rx * radial,
                    startY + riseY,
                    center.z + Math.sin(angle) * rz * radial);

            // Temperatura da brasa: nasce amarelo-quente, passa por laranja intenso e esfria em vermelho
            float coreR = 1.0f;
            float coreG = (float) lerp(0.95, 0.26, Math.pow(life, 0.75));
            float coreB = (float) lerp(0.56, 0.01, Math.min(1.0, life * 1.7));

            float haloR = (float) lerp(1.00, 0.85, life);
            float haloG = (float) lerp(0.52, 0.08, Math.pow(life, 0.65));
            float haloB = 0.01f;

            double r = (0.0040 + unit(eSeed, 6) * 0.0020) * scale * (0.55 + 0.45 * fade);

            // Halo térmico externo
            PrayerVfxGeometry.ellipsoid(matrix, out, pos, new Vec3(r * 1.75, r * 2.2, r * 1.75),
                    4, 6, haloR, haloG, haloB, (float) (0.46 * fade), 0.0);
            // Núcleo incandescente da brasa
            PrayerVfxGeometry.ellipsoid(matrix, out, pos, new Vec3(r * 0.78, r * 1.15, r * 0.78),
                    4, 6, coreR, coreG, coreB, (float) (0.94 * fade), 0.0);

            // Pequeno rastro ascendente de calor atrás da brasa
            Vec3 tail = pos.add(
                    -Math.sin(angle) * 0.004 * scale,
                    (0.016 + 0.014 * fade) * scale,
                    Math.cos(angle) * 0.004 * scale);
            PrayerVfxGeometry.taperedTube(matrix, out, pos, tail,
                    r * 0.45, 0.0001, haloR, haloG, haloB, (float) (0.52 * fade));
        }
    }

    /**
     * Faíscas superaquecidas (mesma cadência da Divine):
     * ~1.6s brilhando e tremendo perto da armadura -> ~0.5s estourando em estilhaços de faísca.
     */
    private static void drawCracklingHeatSparks(Matrix4fc matrix, VertexConsumer out, Bounds bounds,
                                                Vec3 center, double rx, double rz,
                                                double time, long pieceSalt, int count) {
        double scale = bounds.strokeScale();
        double cycleRate = 0.024; // ~2.08s por ciclo completo (20 ticks/s)
        double popStart = 0.76;   // ~1.58s aquecendo antes de estourar

        for (int i = 0; i < count; i++) {
            long sSeed = mix(pieceSalt + 9973L + i * 577L);
            double life = fract(time * cycleRate + i * (1.0 / count) + unit(sSeed, 1));

            double baseAngle = unit(sSeed, 2) * Math.PI * 2.0 + time * 0.02;
            double radialScale = 0.94 + unit(sSeed, 3) * 0.24;
            double startY = lerp(bounds.minY() + 0.03, bounds.maxY() - 0.03, unit(sSeed, 4));

            double driftT = Math.min(life / popStart, 1.0);
            double riseY = -driftT * (0.055 + unit(sSeed, 5) * 0.045);
            double expandRadial = radialScale + driftT * 0.16;
            double jitterAngle = baseAngle + Math.sin(time * 0.22 + i * 2.5) * 0.16;

            Vec3 pos = new Vec3(
                    center.x + Math.cos(jitterAngle) * rx * expandRadial,
                    startY + riseY,
                    center.z + Math.sin(jitterAngle) * rz * expandRadial);

            if (life < popStart) {
                // FASE 1: Ponto superaquecido tremendo próximo à armadura
                double t = life / popStart;
                double fadeIn = Math.min(1.0, t / 0.14);
                double flicker = 0.74 + 0.26 * Math.sin(time * 0.45 + i * 3.1);
                double prePop = t > 0.82 ? 1.0 + ((t - 0.82) / 0.18) * 0.50 : 1.0;

                double r = (0.0038 + unit(sSeed, 6) * 0.0016) * scale * fadeIn * prePop;
                float alpha = (float) (fadeIn * flicker);

                // Halo laranja-fogo
                PrayerVfxGeometry.ellipsoid(matrix, out, pos, new Vec3(r * 1.65, r * 1.65, r * 1.65),
                        4, 6, 1.00f, 0.46f, 0.04f, alpha * 0.52f, 0.0);
                // Núcleo branco-amarelado incandescente
                PrayerVfxGeometry.ellipsoid(matrix, out, pos, new Vec3(r * 0.72, r * 0.72, r * 0.72),
                        4, 6, 1.00f, 0.95f, 0.62f, alpha * 0.96f, 0.0);
            } else {
                // FASE 2: Estouro em fagulhas/faíscas de calor disparando para fora
                double burstT = (life - popStart) / (1.0 - popStart);
                double expand = 1.0 - Math.pow(1.0 - burstT, 2.4);
                double fadeOut = Math.pow(1.0 - burstT, 1.15);

                // Flash central rápido no estalo da faísca
                if (burstT < 0.40) {
                    double flashFade = 1.0 - (burstT / 0.40);
                    double flashR = 0.0082 * scale * (1.0 - burstT * 0.5);
                    PrayerVfxGeometry.ellipsoid(matrix, out, pos, new Vec3(flashR, flashR, flashR),
                            4, 6, 1.00f, 0.96f, 0.68f, (float) (flashFade * 0.95), 0.0);
                }

                // 6 estilhaços de faísca voando em direções orgânicas e subindo levemente
                int streaks = 6;
                double baseSpin = unit(sSeed, 7) * Math.PI * 2.0;
                double maxDist = (0.042 + unit(sSeed, 6) * 0.020) * scale;

                for (int k = 0; k < streaks; k++) {
                    double a = baseSpin + k * (Math.PI * 2.0 / streaks) + (unit(sSeed, 10 + k) - 0.5) * 0.45;
                    double tiltZ = (unit(sSeed, 20 + k) - 0.5) * 0.85;
                    double lenScale = 0.65 + unit(sSeed, 30 + k) * 0.55;

                    Vec3 dir = new Vec3(
                            Math.cos(a),
                            Math.sin(a) - 0.28 * burstT, // leve impulso ascendente do ar quente
                            tiltZ).normalize();

                    double headDist = maxDist * lenScale * expand;
                    double tailDist = headDist * lerp(0.15, 0.62, burstT);

                    Vec3 tailPos = pos.add(dir.scale(tailDist));
                    Vec3 headPos = pos.add(dir.scale(headDist));

                    double headThick = Math.max(0.0005, 0.0018 * scale * (1.0 - burstT * 0.5));
                    // Ponta quente amarelo-dourada -> cauda laranja-brasa
                    PrayerVfxGeometry.taperedTube(matrix, out, tailPos, headPos,
                            0.00012, headThick,
                            1.00f, (float) lerp(0.88, 0.36, burstT), (float) lerp(0.42, 0.03, burstT),
                            (float) (fadeOut * 0.95));
                }
            }
        }
    }
}
