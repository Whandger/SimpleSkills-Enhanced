package com.github.ob_yekt.simpleskills.prayer.divine;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerHolyBoltVfx {
    private PrayerHolyBoltVfx() {}

    // Quantidade de feixes diagonais de luz que perfuram o alvo.
    private static final int BEAM_COUNT = 7;

    // 3. HOLY_BOLT — 7 feixes finos de luz sagrada disparam instantaneamente do céu
    // em diagonais cruzadas, perfurando o inimigo na velocidade da luz.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 1, 3);
        if (fade <= 0.01f) return;

        double t = clamp01(age / Math.max(1.0, duration));

        // Selo rápido de mira sob o alvo enquanto os feixes disparam
        double lockFade = Math.sin(clamp01(t / 0.75) * Math.PI);
        if (lockFade > 0.01) {
            Vec3 ground = new Vec3(destination.x, destination.y - 0.85, destination.z);
            PrayerVfxPrimitives.ringPlane(m, out, ground, PrayerVfxPrimitives.UP,
                    0.90 - lockFade * 0.20, 0.018, age * 0.16,
                    1.0f, 0.88f, 0.42f, (float) (0.65 * lockFade) * fade);
        }

        // 7 feixes de luz disparando em rajada ultrarrápida
        for (int i = 0; i < BEAM_COUNT; i++) {
            double startT = i * 0.028;
            double strikeDuration = 0.06; // Praticamente instantâneo (velocidade da luz)
            double lingerDuration = 0.34; // Pós-imagem incandescente do rastro cortando o ar

            double localAge = t - startT;
            if (localAge < 0.0 || localAge > strikeDuration + lingerDuration) {
                continue;
            }

            // 0 -> 1 em fração de segundo (disparo imediato)
            double strike = clamp01(localAge / strikeDuration);
            double strikeEase = 1.0 - Math.pow(1.0 - strike, 3.5);

            // 0 -> 1 enquanto o rastro de luz dissipa
            double dissipate = clamp01((localAge - strikeDuration) / lingerDuration);
            float beamAlpha = (float) Math.pow(1.0 - dissipate, 1.5) * fade;
            if (beamAlpha <= 0.01f) continue;

            // Direção diagonal vinda do céu
            double azimuth = i * 2.399963 + 0.4;
            double tilt = 0.34 + (i % 3) * 0.11;
            Vec3 skyDir = new Vec3(
                    Math.cos(azimuth) * Math.sin(tilt),
                    Math.cos(tilt),
                    Math.sin(azimuth) * Math.sin(tilt)
            ).normalize();
            Vec3 pierceDir = skyDir.scale(-1.0);

            // Ponto exato no corpo do inimigo onde este feixe perfura
            double spreadR = (i == 0) ? 0.0 : (0.14 + (i % 3) * 0.10);
            Vec3 piercePoint = destination.add(
                    Math.cos(azimuth + 1.2) * spreadR,
                    -0.15 + ((i % 4) - 1.5) * 0.18,
                    Math.sin(azimuth + 1.2) * spreadR
            );

            Vec3 skyOrigin = piercePoint.add(skyDir.scale(10.0));
            Vec3 exitPoint = piercePoint.add(pierceDir.scale(1.85));

            // Ponta atravessa o alvo quase no frame 1; a cauda encolhe rapidamente para o ponto de impacto
            Vec3 tip = skyOrigin.lerp(exitPoint, strikeEase);
            double tailProgress = PrayerVfxPrimitives.smooth(0.05, 1.0, dissipate);
            Vec3 tail = skyOrigin.lerp(exitPoint, tailProgress * 0.96);

            // Pulso inicial mais intenso no instante do disparo, afinando logo em seguida
            double flashBoost = 1.0 + (1.0 - dissipate) * 0.45;
            double thinness = (0.032 + (i % 2) * 0.008) * (1.0 - dissipate * 0.72) * flashBoost;

            // 1) Aura dourada externa do feixe
            PrayerVfxGeometry.taperedTube(m, out, tail, tip,
                    thinness * 2.3, thinness * 0.30,
                    1.0f, 0.82f, 0.26f, 0.36f * beamAlpha);
            // 2) Lâmina principal dourado-claro
            PrayerVfxGeometry.taperedTube(m, out, tail, tip,
                    thinness * 1.2, thinness * 0.10,
                    1.0f, 0.94f, 0.60f, 0.88f * beamAlpha);
            // 3) Filamento central branco cortante (velocidade da luz)
            PrayerVfxGeometry.taperedTube(m, out, tail, tip,
                    thinness * 0.50, 0.003,
                    1.0f, 1.0f, 0.96f, 1.0f * beamAlpha);

            // Anel de disparo no céu
            if (dissipate < 0.4) {
                float portalAlpha = (float) (1.0 - dissipate / 0.4) * fade * 0.75f;
                PrayerVfxPrimitives.ringPlane(m, out, skyOrigin, pierceDir,
                        0.14 + dissipate * 0.22, 0.012, age * 0.25 + i,
                        1.0f, 0.94f, 0.65f, portalAlpha);
            }

            // Perfuração imediata assim que o feixe cruza o alvo
            if (strike >= 0.75) {
                double impactP = clamp01((localAge - strikeDuration * 0.75) / lingerDuration);
                float impactAlpha = (float) Math.pow(1.0 - impactP, 1.45) * fade;

                // Clarão estelar intenso no ponto perfurado
                if (impactP < 0.38) {
                    float starA = (float) (1.0 - impactP / 0.38) * fade;
                    PrayerVfxPrimitives.starburst(m, out, piercePoint, pierceDir,
                            0.32 + impactP * 1.35, starA, 1.0f, 0.96f, 0.74f);
                }

                // Anel de choque inclinado no ângulo da perfuração
                PrayerVfxPrimitives.ringPlane(m, out, piercePoint, pierceDir,
                        0.12 + impactP * 0.62, 0.014, age * 0.2 + i,
                        1.0f, 0.90f, 0.46f, 0.85f * impactAlpha);

                // 3 faíscas de luz atravessando para o lado de saída do golpe
                for (int s = 0; s < 3; s++) {
                    double sAng = azimuth + (s - 1) * 0.85;
                    Vec3 radial = new Vec3(Math.cos(sAng), 0.2, Math.sin(sAng)).normalize();
                    Vec3 sparkDir = pierceDir.scale(0.88).add(radial.scale(0.42)).normalize();
                    Vec3 sparkPos = piercePoint.add(sparkDir.scale(0.22 + impactP * 1.35));

                    PrayerVfxPrimitives.crystal(m, out, sparkPos, sparkDir,
                            0.19 * (1.0 - impactP * 0.55), 0.016 * (1.0 - impactP * 0.55),
                            1.0f, 0.97f, 0.76f, impactAlpha);
                }
            }
        }
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
