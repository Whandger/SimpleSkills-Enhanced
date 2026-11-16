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

    // 22. LIGHTNING BOLT — Sobrecarga na mão, líder escalonado, descarga dupla ramificada e eletrocussão no alvo.
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

        // Sincronizado exatamente com when(d -> Math.max(10, d * .52)) do PrayerSoundtrack
        double impactTick = Math.max(10.0, duration * 0.52);

        // Seed base fixa por disparo + troca rápida a cada ~1.5 tick para dar vibração de alta voltagem
        long castSeed = Double.doubleToLongBits(target.x) * 31L
                ^ Double.doubleToLongBits(target.y) * 17L
                ^ Double.doubleToLongBits(target.z);
        long flickerSeed = castSeed ^ ((long) Math.floor(age * 0.75f) * 104729L);

        // ------------------------------------------------------------------
        // 1. ACÚMULO DE ALTA TENSÃO NA MÃO (0 até impactTick, com decaimento pós-disparo)
        // ------------------------------------------------------------------
        double chargeBuild = PrayerVfxPrimitives.smooth(0, impactTick * 0.85, age);
        double postCastDecay = 1.0 - PrayerVfxPrimitives.smooth(impactTick, impactTick + 6.0, age);
        float handPower = (float) (chargeBuild * postCastDecay) * fade;

        if (handPower > 0.01f) {
            Vec3 palm = hand.add(dir.scale(0.14));

            // Núcleo de plasma comprimido na mão (sem wireSphere geométrica)
            double coreR = 0.06 + chargeBuild * 0.09;
            PrayerVfxGeometry.ellipsoid(m, out, palm, new Vec3(coreR * 1.9, coreR * 1.9, coreR * 1.9),
                    8, 10, 0.16f, 0.56f, 1.0f, 0.30f * handPower, age * 0.35);
            PrayerVfxGeometry.ellipsoid(m, out, palm, new Vec3(coreR, coreR, coreR),
                    6, 8, 0.92f, 0.98f, 1.0f, 0.95f * handPower, -age * 0.5);

            // Anel magnético girando perpendicular à direção do disparo
            PrayerVfxPrimitives.ringPlane(m, out, palm, dir,
                    0.16 + chargeBuild * 0.22, 0.024 * (1.1 - chargeBuild * 0.4),
                    age * 0.35, 0.38f, 0.84f, 1.0f, 0.75f * handPower);

            // 6 arcos elétricos puxando energia ao redor para dentro da palma
            int chargeArcs = 6;
            for (int a = 0; a < chargeArcs; a++) {
                double ang = age * 0.48 + a * (Math.PI * 2.0 / chargeArcs);
                double outerDist = (0.58 - chargeBuild * 0.30) + Math.sin(age * 0.9 + a) * 0.06;
                Vec3 arcStart = palm.subtract(dir.scale(0.10))
                        .add(frame[0].scale(Math.cos(ang) * outerDist))
                        .add(frame[1].scale(Math.sin(ang) * outerDist));
                List<Vec3> pullBolt = buildBolt(arcStart, palm, 4, flickerSeed + a * 61L, 0.065);
                drawBolt(m, out, pullBolt, 0.032, 0.012, 0.004, 0.85f * handPower);
            }
        }

        // ------------------------------------------------------------------
        // 2. LÍDER ESCALONADO / PRECURSOR (ioniza o ar antes do raio principal)
        // ------------------------------------------------------------------
        double leaderStart = impactTick * 0.30;
        if (age >= leaderStart && age < impactTick + 1.0) {
            double leaderP = PrayerVfxPrimitives.smooth(leaderStart, impactTick, age);
            float leaderFade = (float) (1.0 - PrayerVfxPrimitives.smooth(impactTick - 0.5, impactTick + 1.0, age)) * fade;
            if (leaderFade > 0.01f) {
                Vec3 leaderTip = hand.lerp(target, leaderP);
                int lSegs = Math.max(8, (int) (dist * leaderP * 2.2));
                List<Vec3> leader = buildBolt(hand, leaderTip, lSegs, flickerSeed + 19L, 0.22 * leaderP);
                drawBolt(m, out, leader, 0.055, 0.020, 0.007, 0.82f * leaderFade);

                // Pequenas ramificações exploratórias na ponta do líder
                if (leader.size() >= 4) {
                    for (int f = 0; f < 3; f++) {
                        int idx = Math.max(1, leader.size() - 2 - f);
                        Vec3 root = leader.get(idx);
                        double theta = (f - 1) * 1.15 + Math.sin(age * 1.4 + f) * 0.4;
                        double fLen = 0.45 + f * 0.15;
                        Vec3 fEnd = root.add(dir.scale(fLen * 0.65))
                                .add(frame[0].scale(Math.cos(theta) * fLen))
                                .add(frame[1].scale(Math.sin(theta) * fLen));
                        List<Vec3> fork = buildBolt(root, fEnd, 3, flickerSeed + f * 83L, 0.06);
                        drawBolt(m, out, fork, 0.028, 0.010, 0.003, 0.65f * leaderFade);
                    }
                }
            }
        }

        // ------------------------------------------------------------------
        // 3. DESCARGA PRINCIPAL DE ALTA POTÊNCIA + ARCO ENTRELAÇADO + RAMIFICAÇÕES
        // ------------------------------------------------------------------
        if (age >= impactTick - 0.3) {
            double strikeAge = Math.max(0.0, age - impactTick);
            double strikeDur = Math.max(6.0, duration - impactTick);
            double strikeP = Math.min(1.0, strikeAge / strikeDur);

            // Clarão violentíssimo nos primeiros 3 ticks, mantendo arco elétrico sustentado enquanto decai
            float surge = (float) Math.exp(-strikeAge * 0.28);
            float sustain = (float) Math.pow(1.0 - strikeP, 1.35);
            float boltAlpha = Math.min(1.0f, 0.35f * sustain + 0.85f * surge) * fade;

            if (boltAlpha > 0.01f) {
                int segments = Math.max(12, Math.min(24, (int) (dist * 2.4)));
                double mainAmp = Math.min(0.45, 0.16 + dist * 0.032) * (0.75 + 0.35 * surge);

                // Canal principal pesado (muito mais largo e energético que o Spark)
                List<Vec3> mainBolt = buildBolt(hand, target, segments, flickerSeed, mainAmp);
                double thicknessScale = 0.75 + 0.55 * surge;
                drawBolt(m, out, mainBolt,
                        0.21 * thicknessScale,
                        0.082 * thicknessScale,
                        0.030 * thicknessScale,
                        boltAlpha);

                // Segundo canal entrelaçado (hélice elétrica que se prende ao raio principal)
                List<Vec3> braidBolt = buildBraidedBolt(mainBolt, frame, age, flickerSeed + 777L, 0.24 * thicknessScale);
                drawBolt(m, out, braidBolt,
                        0.11 * thicknessScale,
                        0.042 * thicknessScale,
                        0.014 * thicknessScale,
                        boltAlpha * 0.88f);

                // 7 ramificações direcionais com sub-ramificações (bifurcação em 2 níveis)
                if (mainBolt.size() >= 6) {
                    int branchCount = 7;
                    for (int b = 0; b < branchCount; b++) {
                        double fracPos = 0.14 + (b / (double) (branchCount - 1)) * 0.72;
                        int splitIdx = Math.min(mainBolt.size() - 2, Math.max(1, (int) (fracPos * (mainBolt.size() - 1))));
                        Vec3 root = mainBolt.get(splitIdx);

                        Random rng = new Random(flickerSeed + (b + 1) * 509L);
                        double theta = b * 2.399963 + (rng.nextDouble() - 0.5) * 0.6;
                        double forkLen = (0.75 + rng.nextDouble() * 0.65) * (0.7 + 0.3 * surge);

                        // Cone voltado para frente (direção do disparo), nunca para trás
                        Vec3 forkDir = dir.scale(0.68)
                                .add(frame[0].scale(Math.cos(theta) * 0.72))
                                .add(frame[1].scale(Math.sin(theta) * 0.72))
                                .normalize();
                        Vec3 forkEnd = root.add(forkDir.scale(forkLen));

                        List<Vec3> fork = buildBolt(root, forkEnd, 4, flickerSeed + b * 131L, 0.09);
                        drawBolt(m, out, fork, 0.068 * thicknessScale, 0.025 * thicknessScale, 0.008, boltAlpha * 0.85f);

                        // Sub-ramificação saindo do meio do galho principal
                        if (fork.size() >= 3) {
                            Vec3 subRoot = fork.get(2);
                            double subTheta = theta + (rng.nextBoolean() ? 0.85 : -0.85);
                            double subLen = forkLen * 0.52;
                            Vec3 subEnd = subRoot.add(dir.scale(subLen * 0.6))
                                    .add(frame[0].scale(Math.cos(subTheta) * subLen))
                                    .add(frame[1].scale(Math.sin(subTheta) * subLen));
                            List<Vec3> subFork = buildBolt(subRoot, subEnd, 3, flickerSeed + b * 997L, 0.045);
                            drawBolt(m, out, subFork, 0.036, 0.013, 0.004, boltAlpha * 0.68f);
                        }
                    }
                }
            }

            // ------------------------------------------------------------------
            // 4. DETONAÇÃO NO ALVO, ELETROCUSSÃO DE SUPERFÍCIE E PÓS-DESCARGAS
            // ------------------------------------------------------------------
            drawTargetOvercharge(m, out, target, dir, frame, age, strikeAge, strikeP, flickerSeed, fade);
        }
    }

    private static void drawTargetOvercharge(Matrix4fc m, VertexConsumer out, Vec3 target, Vec3 dir, Vec3[] frame,
                                             float age, double strikeAge, double strikeP, long flickerSeed, float fade) {
        float impactFade = (float) Math.pow(1.0 - strikeP, 0.90) * fade;
        if (impactFade <= 0.01f) return;

        // Pulso sincronizado com os estalos repetidos do PrayerSoundtrack (a cada ~4 ticks após o impacto)
        double crackleWave = Math.max(0.0, Math.sin((strikeAge - 2.0) * (Math.PI / 2.0)));
        float crackleBoost = (strikeAge > 2.0 && strikeP < 0.85) ? (float) Math.pow(crackleWave, 3.0) : 0.0f;

        // Clarão inicial de impacto (starburst voltado para o conjurador + núcleo incandescente)
        if (strikeAge < 4.5) {
            double flashP = strikeAge / 4.5;
            float flashA = (float) Math.pow(1.0 - flashP, 1.4) * fade;
            PrayerVfxPrimitives.starburst(m, out, target, dir.scale(-1.0),
                    0.45 + flashP * 1.45, flashA, 0.72f, 0.94f, 1.0f);
            double coreR = 0.28 * (1.0 - flashP * 0.5);
            PrayerVfxGeometry.ellipsoid(m, out, target, new Vec3(coreR, coreR, coreR),
                    8, 10, 0.95f, 0.99f, 1.0f, 0.95f * flashA, age * 0.4);
        }

        // Dupla onda de choque: uma perpendicular ao raio e outra expandindo no plano horizontal
        if (strikeAge < 7.0) {
            double waveP = strikeAge / 7.0;
            float waveA = (float) Math.pow(1.0 - waveP, 1.2) * fade;
            PrayerVfxPrimitives.ringPlane(m, out, target, dir.scale(-1.0),
                    0.22 + waveP * 1.25, 0.065 * (1.0 - waveP * 0.6), age * 0.15,
                    0.45f, 0.88f, 1.0f, 0.92f * waveA);
            PrayerVfxGeometry.torus(m, out, target.add(0, -0.15, 0),
                    0.25 + waveP * 1.45, 0.026 * (1.0 - waveP * 0.55),
                    36, age * 0.1, 0.25f, 0.72f, 1.0f, 0.85f * waveA);
        }

        // Arcos de alta voltagem percorrendo o corpo do alvo (substitui a wireSphere artificial)
        int bodyArcs = 8;
        for (int i = 0; i < bodyArcs; i++) {
            Random rng = new Random(flickerSeed + i * 313L);
            double a1 = rng.nextDouble() * Math.PI * 2.0;
            double y1 = (rng.nextDouble() * 2.0 - 1.0) * 0.65;
            double r1 = 0.42 + rng.nextDouble() * 0.25;

            double a2 = a1 + (rng.nextBoolean() ? 1.4 : -1.4) + (rng.nextDouble() - 0.5) * 0.8;
            double y2 = (rng.nextDouble() * 2.0 - 1.0) * 0.65;
            double r2 = 0.42 + rng.nextDouble() * 0.25;

            Vec3 p0 = target.add(Math.cos(a1) * r1, y1, Math.sin(a1) * r1);
            Vec3 p1 = target.add(Math.cos(a2) * r2, y2, Math.sin(a2) * r2);

            List<Vec3> surfaceBolt = buildBolt(p0, p1, 4, flickerSeed + i * 71L, 0.11);
            float arcAlpha = Math.min(1.0f, impactFade * (0.75f + 0.55f * crackleBoost));
            drawBolt(m, out, surfaceBolt, 0.052, 0.020, 0.006, arcAlpha);
        }

        // 6 descargas aterrando no chão e espirrando faíscas ao redor do alvo
        int groundBolts = 6;
        for (int g = 0; g < groundBolts; g++) {
            double ang = g * (Math.PI * 2.0 / groundBolts) + 0.35 + Math.sin(flickerSeed + g) * 0.25;
            double reach = 0.65 + (g % 3) * 0.28 + strikeP * 0.45;
            double dropY = (g % 2 == 0) ? -0.75 : (0.15 + (g % 3) * 0.22);

            Vec3 end = target.add(Math.cos(ang) * reach, dropY, Math.sin(ang) * reach);
            List<Vec3> groundBolt = buildBolt(target, end, 4, flickerSeed + g * 199L, 0.09);
            float gAlpha = Math.min(1.0f, impactFade * (0.80f + 0.45f * crackleBoost));
            drawBolt(m, out, groundBolt, 0.058, 0.022, 0.007, gAlpha);
        }
    }

    /** Cria um arco secundário que orbita e se prende nos nós do canal principal. */
    private static List<Vec3> buildBraidedBolt(List<Vec3> mainBolt, Vec3[] frame, float age, long seed, double radius) {
        Random rng = new Random(seed);
        List<Vec3> braid = new ArrayList<>(mainBolt.size());
        braid.add(mainBolt.get(0));
        int last = mainBolt.size() - 1;
        for (int i = 1; i < last; i++) {
            double u = i / (double) last;
            double env = Math.sin(Math.PI * u);
            double spin = u * 11.0 - age * 1.3 + (rng.nextDouble() - 0.5) * 0.9;
            double amp = radius * env * (0.65 + rng.nextDouble() * 0.55);
            Vec3 base = mainBolt.get(i);
            braid.add(base
                    .add(frame[0].scale(Math.cos(spin) * amp))
                    .add(frame[1].scale(Math.sin(spin) * amp)));
        }
        braid.add(mainBolt.get(last));
        return braid;
    }

    private static void drawBolt(Matrix4fc m, VertexConsumer out, List<Vec3> pts,
                                 double outerR, double midR, double coreR, float alpha) {
        if (alpha <= 0.005f || pts.size() < 2) return;
        int last = pts.size() - 1;
        for (int i = 0; i < last; i++) {
            double t0 = 1.0 - (i / (double) last) * 0.40;
            double t1 = 1.0 - ((i + 1) / (double) last) * 0.40;
            Vec3 a = pts.get(i);
            Vec3 b = pts.get(i + 1);
            // Corona externa azul profundo
            PrayerVfxGeometry.taperedTube(m, out, a, b, outerR * t0, outerR * t1,
                    0.10f, 0.44f, 1.0f, 0.18f * alpha);
            // Camada intermediária ciano-elétrico
            PrayerVfxGeometry.taperedTube(m, out, a, b, midR * t0, midR * t1,
                    0.36f, 0.84f, 1.0f, 0.85f * alpha);
            // Núcleo branco incandescente
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
