package com.github.ob_yekt.simpleskills.runes.armor;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.Bounds;
import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.EquipmentPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.BODY;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.HEAD;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.boundsCenter;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.drawTiltedEllipse;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.fract;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.lerp;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.lerpVec;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.mix;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.safeNormalize;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.unit;

public final class DivineArmorVfx {
    private DivineArmorVfx() {}

    public static void submit(SubmitNodeCollector collector, PoseStack poseStack,
                              EquipmentPart part, Bounds bounds, double time, int seed) {
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, vertices) ->
                draw(pose.pose(), vertices, part, bounds, time, seed));
    }

    private static void draw(Matrix4fc matrix, VertexConsumer out, EquipmentPart part,
                             Bounds bounds, double time, int seed) {
        double pulse = 0.85 + 0.15 * Math.sin(time * 0.85 + seed * 0.01);

        switch (part) {
            case HELMET -> {
                double bob = Math.sin(time * 0.14) * 0.012;
                Vec3 haloCenter = new Vec3(0.0, HEAD.minY() - 0.19 + bob, 0.0);
                drawTiltedEllipse(matrix, out, haloCenter, 0.24, 0.24,
                        time * 0.04, 0.0, 0.0,
                        1.0f, 0.80f, 0.24f, (float) (0.88 * pulse), 36, 0.0045, false);
                drawTiltedEllipse(matrix, out, haloCenter, 0.24, 0.24,
                        -time * 0.04, 0.0, 0.0,
                        1.0f, 0.97f, 0.78f, (float) (0.96 * pulse), 36, 0.0019, false);

                drawDivineBurstSparkles(matrix, out, part, bounds, time, seed, 6);
            }
            case CHEST_TORSO -> {
                drawFoldedAngelWings(matrix, out, time, seed, pulse);
                drawDivineBurstSparkles(matrix, out, part, bounds, time, seed, 9);
            }
            case ARM_RIGHT, ARM_LEFT, LEG_WAIST, KNEE_RIGHT, KNEE_LEFT, BOOT_RIGHT, BOOT_LEFT -> {
                drawDivineBurstSparkles(matrix, out, part, bounds, time, seed, 6);
            }
            default -> {
            }
        }
    }

    /**
     * Asas angelicais dobradas com 3 camadas bem definidas de penas sobrepostas:
     * 1. Plumas coberteiras curtas e cheias no arco superior acima dos ombros;
     * 2. Penas secundárias médias em escama no meio;
     * 3. Rêmiges primárias longas abrindo em leque escalonado (sem fechar em "C").
     */
    private static void drawFoldedAngelWings(Matrix4fc matrix, VertexConsumer out, double time, int seed, double pulse) {
        double breathePhase = time * 0.12;
        double breathe = Math.sin(breathePhase);
        double tipSway = Math.sin(breathePhase - 0.8);

        for (int side = -1; side <= 1; side += 2) {
            // Esqueleto anatômico da asa dobrada:
            // Escápula -> Articulação superior (pulso da asa acima do ombro) -> Cotovelo dobrado nas costas
            Vec3 scapula = new Vec3(side * 0.08, 0.10, BODY.maxZ() + 0.03);
            Vec3 wingWrist = new Vec3(side * (0.37 + breathe * 0.015), -0.22, BODY.maxZ() + 0.14 + breathe * 0.010);
            Vec3 wingElbow = new Vec3(side * (0.25 + breathe * 0.010), 0.24, BODY.maxZ() + 0.13 + breathe * 0.008);

            // Normal levemente inclinada para fora e para trás para exibir bem a largura de cada pena
            Vec3 wingNormal = safeNormalize(new Vec3(side * 0.28, -0.06, 0.95), new Vec3(0, 0, 1));

            // 1. CAMADA PRIMÁRIA: 9 penas longas de voo abrindo em leque escalonado até as pernas
            // u = 0 (pena externa mais alta) -> u = 1 (pena interna mais baixa)
            int primaries = 9;
            for (int f = 0; f < primaries; f++) {
                double u = f / (double) (primaries - 1);
                Vec3 attach = lerpVec(wingWrist, wingElbow, Math.pow(u, 0.88));
                double localRustle = Math.sin(breathePhase - u * 1.1) * (0.008 + 0.018 * u);

                // As penas externas abrem levemente para fora e as internas descem retas (sem curvar em C)
                double spreadX = side * lerp(0.32, -0.06, Math.pow(u, 0.80)) + side * localRustle;
                double spreadZ = lerp(0.10, -0.02, u) + localRustle * 0.5;
                Vec3 dir = safeNormalize(new Vec3(spreadX, 1.0, spreadZ), new Vec3(0, 1, 0));

                // Leve caimento natural na ponta sem fechar para dentro
                Vec3 tipCurve = new Vec3(side * lerp(0.025, -0.015, u), 0.020, tipSway * 0.016 * (1.0 - 0.4 * u));

                // Comprimento escalonado: penas externas médias, centrais bem longas, internas menores
                double length = lerp(0.42, 0.68, Math.sin(Math.PI * (0.18 + 0.68 * u)));
                double maxWidth = lerp(0.068, 0.084, Math.sin(Math.PI * (0.2 + 0.6 * u)));

                drawFeatherBlade(matrix, out, attach, dir, tipCurve, wingNormal,
                        length, maxWidth, 7,
                        1.0f, 0.97f, 0.78f,   // Eixo central claro
                        0.96f, 0.74f, 0.20f,   // Borda da pena dourado-âmbar visível
                        (float) (0.44 * pulse), true);
            }

            // 2. CAMADA SECUNDÁRIA: 8 penas médias sobrepondo a parte superior/central das primárias
            int secondaries = 8;
            for (int f = 0; f < secondaries; f++) {
                double u = f / (double) (secondaries - 1);
                Vec3 attach = lerpVec(
                        lerpVec(scapula, wingWrist, 0.55),
                        wingElbow.add(-side * 0.02, -0.03, 0.015),
                        u)
                        .add(0.0, 0.02, 0.016);
                double localRustle = Math.sin(breathePhase - u * 0.9) * 0.008;

                double spreadX = side * lerp(0.24, -0.05, u) + side * localRustle;
                Vec3 dir = safeNormalize(new Vec3(spreadX, 1.0, 0.08), new Vec3(0, 1, 0));
                Vec3 tipCurve = new Vec3(side * 0.010, 0.015, 0.008);

                double length = lerp(0.26, 0.40, Math.sin(Math.PI * (0.20 + 0.70 * u)));
                double maxWidth = 0.082;

                drawFeatherBlade(matrix, out, attach, dir, tipCurve, wingNormal,
                        length, maxWidth, 6,
                        1.0f, 0.98f, 0.84f,
                        0.98f, 0.80f, 0.28f,
                        (float) (0.42 * pulse), true);
            }

            // 3. CAMADA COBERTEIRA: 7 plumas arredondadas coroando o arco superior dos ombros
            int coverts = 7;
            for (int f = 0; f < coverts; f++) {
                double u = f / (double) (coverts - 1);
                // Acompanha do centro das costas até a ponta alta do ombro
                Vec3 archPt = quadraticBezier(
                        scapula,
                        new Vec3(side * 0.24, -0.24, BODY.maxZ() + 0.12),
                        wingWrist,
                        u).add(0.0, 0.0, 0.028);

                double spreadX = side * lerp(0.02, 0.26, u);
                Vec3 dir = safeNormalize(new Vec3(spreadX, 1.0, 0.10), new Vec3(0, 1, 0));
                Vec3 tipCurve = new Vec3(side * 0.008, 0.010, 0.006);

                double length = lerp(0.15, 0.26, Math.sin(Math.PI * (0.22 + 0.68 * u)));
                double maxWidth = 0.088;

                drawFeatherBlade(matrix, out, archPt, dir, tipCurve, wingNormal,
                        length, maxWidth, 5,
                        1.0f, 0.99f, 0.90f,
                        1.0f, 0.86f, 0.38f,
                        (float) (0.46 * pulse), false);
            }

            // 4. Pequenas plumas soltas flutuando suavemente abaixo das asas
            int driftingPlumes = 4;
            for (int p = 0; p < driftingPlumes; p++) {
                long pSeed = mix(seed * 911L + side * 137L + p * 479L);
                double life = fract(time * 0.018 + p * (1.0 / driftingPlumes) + unit(pSeed, 1));
                double fade = Math.sin(Math.PI * life);
                if (fade < 0.08) continue;

                double u = 0.15 + unit(pSeed, 2) * 0.75;
                Vec3 origin = lerpVec(wingWrist, wingElbow, u).add(side * 0.08, 0.42, 0.03);

                double swayX = Math.sin(time * 0.14 + p * 2.3) * 0.045 + side * life * 0.05;
                double fallY = life * 0.24;
                double swayZ = Math.cos(time * 0.12 + p * 1.9) * 0.028 + life * 0.03;
                Vec3 plumePos = origin.add(swayX, fallY, swayZ);

                double tilt = Math.sin(time * 0.16 + p * 2.0) * 0.55;
                Vec3 pDir = safeNormalize(new Vec3(side * 0.25 + tilt, 0.92, 0.16), new Vec3(0, 1, 0));
                drawFeatherBlade(matrix, out, plumePos, pDir, new Vec3(0, 0.01, 0), wingNormal,
                        0.065, 0.032, 4,
                        1.0f, 0.98f, 0.82f,
                        1.0f, 0.80f, 0.28f,
                        (float) (0.62 * fade * pulse), false);
            }
        }
    }

    /**
     * Desenha uma pena com silhueta bem marcada (base estreita, corpo largo e ponta afilada),
     * borda dourada visível para separar cada pena da vizinha e raque central luminosa.
     */
    private static void drawFeatherBlade(Matrix4fc matrix, VertexConsumer out,
                                         Vec3 root, Vec3 dir, Vec3 tipCurve, Vec3 wingNormal,
                                         double length, double maxWidth, int segments,
                                         float coreR, float coreG, float coreB,
                                         float edgeR, float edgeG, float edgeB,
                                         float peakAlpha, boolean drawShaft) {
        Vec3 prevCenter = null;
        Vec3 prevLeft = null;
        Vec3 prevRight = null;
        float prevCenterAlpha = 0.0f;

        for (int i = 0; i <= segments; i++) {
            double s = i / (double) segments;
            Vec3 center = root
                    .add(dir.scale(length * s))
                    .add(tipCurve.scale(s * s));

            // Perfil clássico de pena: cresce rápido na base, mantém largura no corpo e afila na ponta
            double grow = Math.min(1.0, s / 0.24);
            double taper = s < 0.66 ? 1.0 : Math.pow(1.0 - (s - 0.66) / 0.34, 0.72);
            double widthProfile = (0.22 + 0.78 * Math.sin(grow * Math.PI * 0.5)) * taper;
            double halfW = maxWidth * 0.5 * widthProfile;

            double longitudinalFade = Math.pow(Math.min(1.0, s * 6.0), 0.45) * Math.pow(1.0 - s, 0.25);
            float centerAlpha = (float) (peakAlpha * longitudinalFade);

            Vec3 tangent = (i < segments)
                    ? safeNormalize(
                            root.add(dir.scale(length * ((i + 1) / (double) segments)))
                                    .add(tipCurve.scale(Math.pow((i + 1) / (double) segments, 2)))
                                    .subtract(center),
                            dir)
                    : (prevCenter != null ? safeNormalize(center.subtract(prevCenter), dir) : dir);

            Vec3 widthAxis = safeNormalize(tangent.cross(wingNormal), new Vec3(1, 0, 0));
            Vec3 cupOffset = wingNormal.scale(halfW * 0.16);

            Vec3 leftEdge = center.subtract(widthAxis.scale(halfW * 0.85)).add(cupOffset);
            Vec3 rightEdge = center.add(widthAxis.scale(halfW * 1.15)).add(cupOffset);

            if (prevCenter != null) {
                // Borda com alpha suficiente (34% do centro) para o contorno da pena aparecer claramente
                float edgeAlphaPrev = prevCenterAlpha * 0.34f;
                float edgeAlphaCurr = centerAlpha * 0.34f;

                emitQuad(matrix, out,
                        prevLeft, prevCenter, center, leftEdge,
                        edgeR, edgeG, edgeB, edgeAlphaPrev,
                        coreR, coreG, coreB, prevCenterAlpha,
                        coreR, coreG, coreB, centerAlpha,
                        edgeR, edgeG, edgeB, edgeAlphaCurr);

                emitQuad(matrix, out,
                        prevCenter, prevRight, rightEdge, center,
                        coreR, coreG, coreB, prevCenterAlpha,
                        edgeR, edgeG, edgeB, edgeAlphaPrev,
                        edgeR, edgeG, edgeB, edgeAlphaCurr,
                        coreR, coreG, coreB, centerAlpha);

                if (drawShaft && i < segments) {
                    double r0 = lerp(0.0022, 0.0003, s);
                    double r1 = lerp(0.0022, 0.0003, (i + 1) / (double) segments);
                    PrayerVfxGeometry.taperedTube(matrix, out, prevCenter, center,
                            r0, r1, 1.0f, 0.98f, 0.86f, Math.min(1.0f, centerAlpha * 1.45f));
                }
            }

            prevCenter = center;
            prevLeft = leftEdge;
            prevRight = rightEdge;
            prevCenterAlpha = centerAlpha;
        }
    }

    private static void emitQuad(Matrix4fc matrix, VertexConsumer out,
                                 Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                                 float r0, float g0, float b0, float a0,
                                 float r1, float g1, float b1, float a1,
                                 float r2, float g2, float b2, float a2,
                                 float r3, float g3, float b3, float a3) {
        out.addVertex(matrix, (float) p0.x, (float) p0.y, (float) p0.z).setColor(r0, g0, b0, a0);
        out.addVertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).setColor(r1, g1, b1, a1);
        out.addVertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z).setColor(r2, g2, b2, a2);
        out.addVertex(matrix, (float) p3.x, (float) p3.y, (float) p3.z).setColor(r3, g3, b3, a3);
    }

    private static Vec3 quadraticBezier(Vec3 p0, Vec3 p1, Vec3 p2, double t) {
        double u = 1.0 - t;
        return p0.scale(u * u)
                .add(p1.scale(2.0 * u * t))
                .add(p2.scale(t * t));
    }

    /**
     * Partículas douradas que surgem por fora de cada peça da armadura,
     * flutuam suavemente por ~2s e explodem em estrelas afiladas de 8 pontas.
     */
    private static void drawDivineBurstSparkles(Matrix4fc matrix, VertexConsumer out, EquipmentPart part,
                                                Bounds bounds, double time, int seed, int count) {
        Vec3 center = boundsCenter(bounds);
        double rx = bounds.width() * 0.56 + 0.028;
        double rz = bounds.depth() * 0.56 + 0.028;

        double cycleRate = 0.021;   // ~2.38s por ciclo completo (20 ticks/s)
        double explodeStart = 0.82; // ~1.95s brilhando antes de explodir em estrela

        for (int i = 0; i < count; i++) {
            long pSeed = mix(seed * 79L + part.ordinal() * 1301L + i * 353L);
            double life = fract(time * cycleRate + i * (1.0 / count) + unit(pSeed, 1));

            double baseAngle = unit(pSeed, 2) * Math.PI * 2.0 + time * 0.025;
            double radialScale = 0.92 + unit(pSeed, 3) * 0.28;
            double startY = lerp(bounds.minY() + 0.03, bounds.maxY() - 0.03, unit(pSeed, 4));

            double driftProgress = Math.min(life / explodeStart, 1.0);
            double riseY = -driftProgress * (0.045 + unit(pSeed, 5) * 0.035);
            double expandRadial = radialScale + driftProgress * 0.14;
            double wobbleAngle = baseAngle + Math.sin(time * 0.12 + i * 2.1) * 0.14;

            Vec3 pos = new Vec3(
                    center.x + Math.cos(wobbleAngle) * rx * expandRadial,
                    startY + riseY,
                    center.z + Math.sin(wobbleAngle) * rz * expandRadial);

            double starTilt = unit(pSeed, 6) * (Math.PI * 0.25);

            if (life < explodeStart) {
                double t = life / explodeStart;
                double fadeIn = Math.min(1.0, t / 0.12);
                double shimmer = 0.78 + 0.22 * Math.sin(time * 0.35 + i * 2.7);
                double prePop = t > 0.85 ? 1.0 + ((t - 0.85) / 0.15) * 0.45 : 1.0;

                double r = (0.0042 + unit(pSeed, 7) * 0.0018) * fadeIn * prePop;
                float alpha = (float) (fadeIn * shimmer);

                PrayerVfxGeometry.ellipsoid(matrix, out, pos, new Vec3(r * 1.55, r * 1.55, r * 1.55),
                        4, 6, 1.00f, 0.80f, 0.22f, alpha * 0.48f, 0.0);
                PrayerVfxGeometry.ellipsoid(matrix, out, pos, new Vec3(r * 0.72, r * 0.72, r * 0.72),
                        4, 6, 1.00f, 0.96f, 0.68f, alpha * 0.95f, 0.0);

                drawDelicateStar(matrix, out, pos, r * 2.6, starTilt, alpha * 0.55f);
            } else {
                double burstT = (life - explodeStart) / (1.0 - explodeStart);
                double expand = 1.0 - Math.pow(1.0 - burstT, 2.6);
                double fadeOut = Math.pow(1.0 - burstT, 1.25);

                double maxRadius = 0.036 + unit(pSeed, 7) * 0.016;
                double currentRadius = lerp(0.005, maxRadius, expand);
                double spin = starTilt + burstT * 0.35;

                if (burstT < 0.45) {
                    double flashFade = 1.0 - (burstT / 0.45);
                    double flashR = 0.0085 * (1.0 - burstT * 0.5);
                    PrayerVfxGeometry.ellipsoid(matrix, out, pos, new Vec3(flashR, flashR, flashR),
                            4, 6, 1.00f, 0.99f, 0.85f, (float) (flashFade * 0.95), 0.0);
                }

                drawExplosionStar(matrix, out, pos, currentRadius, spin, (float) fadeOut);
            }
        }
    }

    private static void drawDelicateStar(Matrix4fc matrix, VertexConsumer out, Vec3 center,
                                         double radius, double angle, float alpha) {
        double thick = Math.max(0.00045, radius * 0.07);
        for (int k = 0; k < 4; k++) {
            double a = angle + k * (Math.PI * 0.5);
            Vec3 tip = center.add(Math.cos(a) * radius, Math.sin(a) * radius, 0.0);
            PrayerVfxGeometry.taperedTube(matrix, out, center, tip,
                    thick, 0.00012, 1.00f, 0.92f, 0.52f, alpha);
        }
    }

    private static void drawExplosionStar(Matrix4fc matrix, VertexConsumer out, Vec3 center,
                                          double radius, double angle, float alpha) {
        double mainThick = Math.max(0.00085, radius * 0.030);
        double diagThick = mainThick * 0.65;

        for (int k = 0; k < 4; k++) {
            double a = angle + k * (Math.PI * 0.5);
            Vec3 tip = center.add(Math.cos(a) * radius, Math.sin(a) * radius, 0.0);
            PrayerVfxGeometry.taperedTube(matrix, out, center, tip,
                    mainThick, 0.00015, 1.00f, 0.96f, 0.70f, alpha * 0.95f);
        }

        double diagRadius = radius * 0.52;
        for (int k = 0; k < 4; k++) {
            double a = angle + Math.PI * 0.25 + k * (Math.PI * 0.5);
            Vec3 tip = center.add(Math.cos(a) * diagRadius, Math.sin(a) * diagRadius, 0.0);
            PrayerVfxGeometry.taperedTube(matrix, out, center, tip,
                    diagThick, 0.00012, 1.00f, 0.82f, 0.28f, alpha * 0.80f);
        }
    }
}