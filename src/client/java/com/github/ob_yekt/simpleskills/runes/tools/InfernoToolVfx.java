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

import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.fract;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.lerp;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.mix;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.safeNormalize;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.unit;

public final class InfernoToolVfx {
    private InfernoToolVfx() {}

    public static void submit(SubmitNodeCollector collector, PoseStack poseStack,
                              ItemGeometryInfo info, double time, int seed) {
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, vertices) ->
                drawCoolingFireSparks(pose.pose(), vertices, info, time, seed));
    }

    /**
     * Faíscas de fogo que surgem próximas à lâmina/cabeça do equipamento,
     * sobem perdendo velocidade, esfriam de amarelo-laranja para cinza-brasa
     * e desaparecem suavemente.
     */
    private static void drawCoolingFireSparks(Matrix4fc matrix, VertexConsumer out, ItemGeometryInfo info,
                                              double time, int seed) {
        List<Vec3> all = info.allSamples();
        List<Vec3> head = info.headSamples();
        if (all.isEmpty()) return;

        Vec3 bladeAxis = safeNormalize(info.bladeTip().subtract(info.bladeStart()), new Vec3(1, 1, 0));
        Vec3 perpXY = new Vec3(-bladeAxis.y, bladeAxis.x, 0.0).normalize();
        Vec3 upDir = safeNormalize(bladeAxis.scale(0.65).add(0.0, 0.75, 0.0), new Vec3(0, 1, 0));

        int sparks = 34;
        for (int i = 0; i < sparks; i++) {
            long sSeed = mix(seed * 131L + i * 509L);
            double life = fract(time * 0.036 + i * (1.0 / sparks) + unit(sSeed, 1));

            // Concentra ~2/3 das faíscas na lâmina/cabeça e ~1/3 no restante do item
            List<Vec3> pool = (!head.isEmpty() && i % 3 != 0) ? head : all;
            int idx = (int) (unit(sSeed, 2) * pool.size()) % pool.size();
            Vec3 origin = pool.get(idx);

            double zSign = (i % 2 == 0) ? 1.0 : -1.0;
            double sideSign = ((i / 2) % 2 == 0) ? 1.0 : -1.0;

            // Desacelera ao longo da vida: sai rápido do metal quente e flutua devagar ao esfriar
            double travel = 1.0 - Math.pow(1.0 - life, 1.65);
            double riseDist = travel * (0.042 + unit(sSeed, 3) * 0.034);
            double outward = sideSign * (0.004 + travel * (0.014 + unit(sSeed, 4) * 0.014));
            double flutter = Math.sin(time * 0.24 + i * 2.3 + life * 5.0) * (0.005 + 0.006 * life);
            double zOffset = zSign * (info.halfDepth() + 0.005 + unit(sSeed, 5) * 0.013 + travel * 0.006);

            Vec3 pos = origin
                    .add(upDir.scale(riseDist))
                    .add(perpXY.scale(outward + flutter))
                    .add(0.0, 0.0, zOffset);

            // Curva de aparição rápida e sumiço suave no final
            double fadeIn = Math.min(1.0, life / 0.10);
            double fadeOut = life < 0.72 ? 1.0 : Math.pow(1.0 - (life - 0.72) / 0.28, 1.35);
            double visibility = fadeIn * fadeOut;
            if (visibility < 0.03) continue;

            // Evolução térmica da faísca:
            // 0.00 -> 0.45: amarelo-branco incandescente -> laranja vivo -> vermelho-brasa
            // 0.45 -> 0.80: esfria virando cinza de brasa apagada
            // 0.80 -> 1.00: cinza frio encolhendo até sumir
            float coreR;
            float coreG;
            float coreB;
            float coreAlpha;

            if (life < 0.45) {
                double t = life / 0.45;
                coreR = 1.00f;
                coreG = (float) lerp(0.94, 0.30, Math.pow(t, 0.85));
                coreB = (float) lerp(0.56, 0.03, Math.min(1.0, t * 1.45));
                coreAlpha = (float) (0.95 * visibility);
            } else if (life < 0.78) {
                double t = (life - 0.45) / 0.33;
                coreR = (float) lerp(1.00, 0.36, t);
                coreG = (float) lerp(0.30, 0.35, t);
                coreB = (float) lerp(0.03, 0.37, t);
                coreAlpha = (float) lerp(0.92, 0.52, t);
            } else {
                double t = (life - 0.78) / 0.22;
                coreR = (float) lerp(0.36, 0.20, t);
                coreG = (float) lerp(0.35, 0.20, t);
                coreB = (float) lerp(0.37, 0.22, t);
                coreAlpha = (float) (0.52 * (1.0 - t) * (1.0 - t));
            }

            // Tamanho encolhe conforme a faísca esfria e vira cinza
            double baseR = 0.0021 + unit(sSeed, 6) * 0.0012;
            double r = baseR * fadeIn * lerp(1.05, 0.42, Math.pow(life, 0.85));

            // Halo quente de fogo que se apaga antes da faísca virar cinza
            if (life < 0.58) {
                double heatFade = (1.0 - life / 0.58) * fadeIn;
                float haloG = (float) lerp(0.52, 0.12, life / 0.58);
                PrayerVfxGeometry.ellipsoid(matrix, out, pos, new Vec3(r * 1.75, r * 1.95, r * 1.75),
                        4, 6, 1.00f, haloG, 0.03f, (float) (0.48 * heatFade), 0.0);

                // Pequeno rastro térmico enquanto está quente
                Vec3 tail = pos.subtract(upDir.scale(0.009 * heatFade)).subtract(perpXY.scale(outward * 0.18));
                PrayerVfxGeometry.taperedTube(matrix, out, pos, tail,
                        r * 0.42, 0.0001, 1.00f, haloG, 0.04f, (float) (0.42 * heatFade));
            }

            // Núcleo da faísca (incandescente no início -> cinza apagado no fim)
            PrayerVfxGeometry.ellipsoid(matrix, out, pos, new Vec3(r, r * 1.25, r),
                    4, 6, coreR, coreG, coreB, coreAlpha, 0.0);
        }
    }
}
