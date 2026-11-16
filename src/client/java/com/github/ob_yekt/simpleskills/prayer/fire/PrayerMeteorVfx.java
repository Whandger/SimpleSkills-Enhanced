package com.github.ob_yekt.simpleskills.prayer.fire;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerMeteorVfx {
    private PrayerMeteorVfx() {}

    // 30. METEOR — Nuvens estáticas e realistas no céu que se abrem com a passagem do meteoro, rastro longo e detonação.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        double flightTicks = Math.max(10.0, duration * 0.56);
        double flight = clamp(age / flightTicks);
        double eased = Math.pow(flight, 1.75);

        Vec3 startPos = new Vec3(-8.5, 38.0, -5.0);
        Vec3 impactPos = new Vec3(0.0, 0.35, 0.0);
        Vec3 center = startPos.lerp(impactPos, eased);

        Vec3 fallDir = impactPos.subtract(startPos).normalize();

        double impact = PrayerVfxPrimitives.smooth(flightTicks - 0.5, duration, age);
        double bodyFade = 1.0 - PrayerVfxPrimitives.smooth(flightTicks, flightTicks + 2.5, age);
        float fade = PrayerVfxPrimitives.fade(age, duration, 2, 7);
        if (fade <= 0.01f) return;

        // ------------------------------------------------------------------
        // 0. CAMADA DE NUVENS ESTÁTICAS NO CÉU (Sem tremedeira/movimento inútil)
        // ------------------------------------------------------------------
        drawCloudLayer(m, out, startPos, impactPos, eased, age, duration, fade);

        // ------------------------------------------------------------------
        // 1. PRESSÁGIO NO CHÃO (Mira térmica que fecha até o impacto)
        // ------------------------------------------------------------------
        if (flight < 1.0) {
            float omenAlpha = (float) Math.sin(flight * Math.PI * 0.85) * fade * 0.85f;
            double lockRadius = 0.55 + (1.0 - eased) * 2.8;
            Vec3 ground = new Vec3(0, 0.03, 0);

            PrayerVfxPrimitives.ringPlane(m, out, ground, PrayerVfxPrimitives.UP,
                    lockRadius, 0.045, age * 0.08,
                    1.0f, 0.38f, 0.04f, omenAlpha * 0.85f);
            PrayerVfxPrimitives.ringPlane(m, out, ground.add(0, 0.01, 0), PrayerVfxPrimitives.UP,
                    0.35 + eased * 0.95, 0.030, -age * 0.12,
                    1.0f, 0.78f, 0.22f, omenAlpha * 0.75f);
        }

        // ------------------------------------------------------------------
        // 2. METEORO + RASTRO LONGO ENVOLVENTE QUE APAGA ANTES DE BATER
        // ------------------------------------------------------------------
        if (bodyFade > 0.01) {
            float mAlpha = fade * (float) bodyFade;
            double tumble = age * 0.14;
            double meteorScale = 1.25;

            double atmoBurn = 1.0 - PrayerVfxPrimitives.smooth(0.62, 0.94, flight);
            float trailAlpha = mAlpha * (float) Math.pow(atmoBurn, 1.35);

            if (trailAlpha > 0.01f) {
                meteorTrail(m, out, center, fallDir, meteorScale, age, trailAlpha);
            }

            float glowAlpha = mAlpha * (0.10f + 0.26f * (float) atmoBurn);
            PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(1.75, 1.75, 1.75),
                    8, 12, 1.0f, 0.30f, 0.02f, glowAlpha, -tumble * 0.4);

            meteorRock(m, out, center, meteorScale, tumble, mAlpha);
        }

        // ------------------------------------------------------------------
        // 3. EXPLOSÃO ORGÂNICA E DENSA NO SOLO
        // ------------------------------------------------------------------
        if (impact > 0.0) {
            double impAge = Math.max(0.0, age - (flightTicks - 0.5));
            double expand = 1.0 - Math.pow(1.0 - impact, 2.35);
            float impactAlpha = (float) Math.pow(1.0 - impact, 1.20) * fade;
            Vec3 ground = new Vec3(0, 0.04, 0);

            if (impAge < 4.5) {
                float flashP = (float) (impAge / 4.5);
                float flashA = (1.0f - flashP) * (1.0f - flashP) * fade;

                PrayerVfxPrimitives.starburst(m, out, new Vec3(0, 0.35, 0), PrayerVfxPrimitives.UP,
                        1.0 + flashP * 5.2, flashA, 1.0f, 0.95f, 0.70f);

                double coreR = 0.7 + flashP * 2.6;
                PrayerVfxGeometry.ellipsoid(m, out, new Vec3(0, 0.35, 0), new Vec3(coreR, coreR * 0.55, coreR),
                        8, 12, 1.0f, 0.94f, 0.60f, flashA * 0.90f, age * 0.3);
            }

            double craterR = 1.4 + expand * 2.2;
            PrayerVfxPrimitives.ringPlane(m, out, ground, PrayerVfxPrimitives.UP,
                    craterR * 0.55, craterR * 0.45, age * 0.03,
                    1.0f, 0.48f, 0.06f, 0.55f * impactAlpha);

            drawExplosionFireWall(m, out, expand, impact, age, impactAlpha);

            double waveRadius = 0.50 + expand * 6.8;
            PrayerVfxGeometry.torus(m, out, ground, waveRadius, 0.12 * (1.0 - impact * 0.5),
                    64, age * 0.05, 1.0f, 0.34f, 0.03f, 0.90f * impactAlpha);
            PrayerVfxGeometry.torus(m, out, ground.add(0, 0.06, 0), waveRadius * 0.72, 0.055,
                    48, -age * 0.08, 1.0f, 0.85f, 0.28f, 0.88f * impactAlpha);

            Random rng = new Random(3030L);
            for (int ray = 0; ray < 24; ray++) {
                double angle = ray * (Math.PI * 2.0 / 24.0) + (rng.nextDouble() - 0.5) * 0.25;
                double speedMult = 0.75 + rng.nextDouble() * 0.65;
                double reach = expand * 5.6 * speedMult;
                double arcPeak = (0.85 + rng.nextDouble() * 1.45) * Math.sin(impact * Math.PI);

                Vec3 dirXZ = new Vec3(Math.cos(angle), 0, Math.sin(angle));
                Vec3 base = dirXZ.scale(0.30).add(0, 0.10, 0);
                Vec3 knee = dirXZ.scale(reach * 0.55).add(0, 0.25 + arcPeak, 0);
                Vec3 tip = dirXZ.scale(reach).add(0, 0.06 + arcPeak * 0.30, 0);

                double wBase = (0.26 + (ray % 3) * 0.05) * (1.0 - impact * 0.45);
                PrayerVfxGeometry.taperedTube(m, out, base, tip, wBase * 1.9, 0.02,
                        1.0f, 0.30f, 0.02f, 0.22f * impactAlpha);
                PrayerVfxGeometry.taperedTube(m, out, base, knee, wBase, wBase * 0.50,
                        1.0f, 0.44f, 0.04f, 0.72f * impactAlpha);
                PrayerVfxGeometry.taperedTube(m, out, knee, tip, wBase * 0.50, 0.004,
                        1.0f, 0.86f, 0.26f, 0.90f * impactAlpha);

                if (ray % 2 == 0) {
                    double fragReach = expand * (2.8 + (ray % 5) * 0.95);
                    double fragY = Math.max(0.12, 0.20 + Math.sin(impact * Math.PI) * (1.6 + (ray % 3) * 0.65));
                    Vec3 fragPos = dirXZ.scale(fragReach).add(0, fragY, 0);

                    Vec3 fragTail = dirXZ.scale(Math.max(0.1, fragReach - 1.15))
                            .add(0, Math.max(0.06, fragY - 0.35), 0);
                    PrayerVfxGeometry.taperedTube(m, out, fragTail, fragPos, 0.015, 0.095,
                            1.0f, 0.58f, 0.08f, 0.78f * impactAlpha);

                    meteorRock(m, out, fragPos, 0.24 + (ray % 3) * 0.05,
                            -age * 0.24 + ray, impactAlpha);
                }
            }
        }
    }

    /** Camada de nuvens estáticas no céu que se abrem com a onda de choque do meteoro. */
    private static void drawCloudLayer(Matrix4fc m, VertexConsumer out,
                                       Vec3 startPos, Vec3 impactPos, double eased,
                                       float age, int duration, float fade) {
        double pierceT = 0.30;
        Vec3 piercePoint = startPos.lerp(impactPos, pierceT);

        double part = PrayerVfxPrimitives.smooth(pierceT - 0.08, pierceT + 0.38, eased);
        double backlight = Math.sin(Math.min(1.0, PrayerVfxPrimitives.smooth(0.02, pierceT + 0.18, eased)) * Math.PI);

        float cloudFade = fade * (float) (1.0 - PrayerVfxPrimitives.smooth(duration * 0.55, duration * 0.92, age));
        if (cloudFade <= 0.01f) return;

        Random rng = new Random(3011L);
        int puffCount = 26;

        for (int i = 0; i < puffCount; i++) {
            double baseAngle = i * (Math.PI * 2.0 / puffCount) + (rng.nextDouble() - 0.5) * 0.35;
            double initialDist = (i < 8)
                    ? (0.2 + rng.nextDouble() * 1.6)
                    : (1.8 + rng.nextDouble() * 5.2);

            // Abre apenas quando o meteoro passa (movimento limpo guiado pelo progresso do impacto, sem loop trêmulo)
            double pushOut = part * (3.6 + (1.0 / (0.6 + initialDist * 0.35)) * 2.8);
            double dist = initialDist + pushOut;

            double yOffset = (rng.nextDouble() - 0.5) * 2.4 + part * (rng.nextDouble() - 0.3) * 1.1;
            double angle = baseAngle; // Sem somar age aqui para garantir que a nuvem é perfeitamente estática!

            Vec3 puffPos = piercePoint.add(
                    Math.cos(angle) * dist,
                    yOffset,
                    Math.sin(angle) * dist);

            double sizeScale = (1.85 + rng.nextDouble() * 1.65) * (1.0 - part * 0.25);
            Vec3 radii = new Vec3(sizeScale * 1.35, sizeScale * 0.62, sizeScale * 1.35);

            float proximityGlow = (float) (backlight * Math.max(0.0, 1.0 - initialDist / 4.2));

            float r = Math.min(1.0f, 0.58f + proximityGlow * 0.42f);
            float g = Math.min(1.0f, 0.60f + proximityGlow * 0.12f);
            float b = Math.max(0.15f, 0.66f - proximityGlow * 0.42f);
            float puffAlpha = cloudFade * (0.42f - (float) part * 0.16f);

            PrayerVfxGeometry.ellipsoid(m, out, puffPos, radii,
                    7, 10, r, g, b, puffAlpha, 0.0);

            PrayerVfxGeometry.ellipsoid(m, out, puffPos.add(0, -radii.y * 0.35, 0),
                    radii.scale(0.76),
                    6, 8, 0.32f + proximityGlow * 0.35f, 0.34f + proximityGlow * 0.08f, 0.40f - proximityGlow * 0.25f,
                    puffAlpha * 0.85f, 0.0);
        }

        if (part > 0.02 && part < 0.98) {
            float tearAlpha = (float) Math.sin(part * Math.PI) * cloudFade * 0.72f;
            double holeR = 1.2 + part * 4.4;
            PrayerVfxGeometry.torus(m, out, piercePoint, holeR, 0.48 * (1.0 - part * 0.4),
                    36, 0.0,
                    0.85f + (float) backlight * 0.15f,
                    0.72f + (float) backlight * 0.10f,
                    0.68f - (float) backlight * 0.35f,
                    tearAlpha);
        }
    }

    /** Cortina contínua e irregular de fogo com bloom varrendo o solo na explosão. */
    private static void drawExplosionFireWall(Matrix4fc m, VertexConsumer out,
                                              double expand, double impact, float age, float alpha) {
        if (alpha <= 0.01f) return;
        int segments = 40;
        double frontR = 0.45 + expand * 5.2;
        double backR = Math.max(0.15, frontR - (1.1 + Math.sin(impact * Math.PI) * 0.8) * (1.0 - impact * 0.4));
        double maxH = (0.65 + Math.sin(impact * Math.PI) * 1.15) * (1.0 - impact * 0.45);

        Vec3[] backFloor = new Vec3[segments];
        Vec3[] frontMid = new Vec3[segments];
        Vec3[] crestTop = new Vec3[segments];
        Vec3[] bloomTop = new Vec3[segments];

        for (int i = 0; i < segments; i++) {
            double angle = i * (Math.PI * 2.0 / segments);
            double rWave = 1.0
                    + 0.16 * Math.sin(angle * 5.0 + age * 0.30)
                    + 0.09 * Math.cos(angle * 9.0 - age * 0.45);
            double hWave = Math.max(0.22, 0.50
                    + 0.35 * Math.sin(angle * 7.0 - age * 0.55)
                    + 0.20 * Math.cos(angle * 11.0 + age * 0.70));

            Vec3 radial = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            double r0 = backR * rWave;
            double r1 = frontR * rWave;
            double rMid = r0 + (r1 - r0) * 0.65;

            backFloor[i] = new Vec3(radial.x * r0, 0.03, radial.z * r0);
            frontMid[i] = new Vec3(radial.x * r1, 0.05 + maxH * hWave * 0.55, radial.z * r1);
            crestTop[i] = new Vec3(radial.x * rMid, 0.06 + maxH * hWave, radial.z * rMid);
            bloomTop[i] = new Vec3(radial.x * rMid, 0.15 + maxH * hWave * 1.65, radial.z * rMid);
        }

        for (int i = 0; i < segments; i++) {
            int next = (i + 1) % segments;

            drawGradientQuad(m, out,
                    backFloor[i], backFloor[next], bloomTop[next], bloomTop[i],
                    1.0f, 0.42f, 0.04f, alpha * 0.25f,
                    1.0f, 0.25f, 0.02f, 0.0f);

            drawGradientQuad(m, out,
                    backFloor[i], backFloor[next], frontMid[next], frontMid[i],
                    1.0f, 0.36f, 0.03f, alpha * 0.75f,
                    1.0f, 0.62f, 0.08f, alpha * 0.82f);

            drawGradientQuad(m, out,
                    frontMid[i], frontMid[next], crestTop[next], crestTop[i],
                    1.0f, 0.62f, 0.08f, alpha * 0.82f,
                    1.0f, 0.82f, 0.22f, alpha * 0.32f);
        }
    }

    /** Rastro longo de reentrada envolvendo desde a frente do meteoro até bem lá em cima no céu. */
    private static void meteorTrail(Matrix4fc m, VertexConsumer out, Vec3 center, Vec3 fallDir,
                                    double meteorScale, float age, float alpha) {
        if (alpha <= 0.01f) return;
        Vec3 trailAxis = fallDir.scale(-1.0);
        Vec3[] frame = PrayerVfxPrimitives.basis(trailAxis);

        Vec3 noseStart = center.add(fallDir.scale(meteorScale * 0.75));
        double totalLength = 14.5;

        PrayerVfxGeometry.taperedTube(m, out, noseStart, center.add(trailAxis.scale(totalLength)),
                meteorScale * 1.38, 0.08,
                1.0f, 0.32f, 0.03f, 0.26f * alpha);

        PrayerVfxGeometry.taperedTube(m, out, noseStart, center.add(trailAxis.scale(totalLength * 0.68)),
                meteorScale * 1.05, 0.03,
                1.0f, 0.82f, 0.28f, 0.72f * alpha);

        PrayerVfxPrimitives.ringPlane(m, out, center.add(fallDir.scale(meteorScale * 0.85)), fallDir,
                meteorScale * 0.98, 0.09, age * 0.22,
                1.0f, 0.92f, 0.52f, 0.90f * alpha);

        for (int strand = 0; strand < 6; strand++) {
            Vec3 previous = null;
            double strandLen = totalLength * (0.85 + (strand % 3) * 0.12);

            for (int step = 0; step <= 26; step++) {
                double t = step / 26.0;
                double angle = -age * (0.24 + strand * 0.015) + t * Math.PI * 5.2 + strand * (Math.PI * 2.0 / 6.0);

                double envelope = meteorScale * 0.95 + Math.sin(t * Math.PI * 0.75) * 0.75;
                double radius = envelope * (1.0 - t * 0.55);

                Vec3 point = center
                        .add(fallDir.scale(meteorScale * 0.35 * (1.0 - t)))
                        .add(trailAxis.scale(t * strandLen))
                        .add(frame[0].scale(Math.cos(angle) * radius))
                        .add(frame[1].scale(Math.sin(angle) * radius));

                if (previous != null) {
                    double width = (1.0 - t * 0.90) * (0.24 + (strand % 2) * 0.06);
                    PrayerVfxGeometry.tube(m, out, previous, point, width * 1.9,
                            0.78f, 0.12f, 0.015f, 0.18f * alpha);
                    PrayerVfxGeometry.tube(m, out, previous, point, width,
                            1.0f, 0.38f + (strand % 2) * 0.10f, 0.03f, 0.78f * alpha);
                    PrayerVfxGeometry.tube(m, out, previous, point, width * 0.32,
                            1.0f, 0.88f, 0.32f, 0.86f * alpha);
                }
                previous = point;
            }
        }

        Random rng = new Random(3099L);
        for (int s = 0; s < 24; s++) {
            double distBack = 0.5 + rng.nextDouble() * (totalLength * 0.95);
            double ang = rng.nextDouble() * Math.PI * 2.0 + age * 0.04;
            double rad = (meteorScale * 0.85) + rng.nextDouble() * 0.95 * (1.0 - distBack / (totalLength * 1.3));

            Vec3 head = center
                    .add(trailAxis.scale(distBack))
                    .add(frame[0].scale(Math.cos(ang) * rad))
                    .add(frame[1].scale(Math.sin(ang) * rad));
            Vec3 tail = head.add(trailAxis.scale(0.65));

            PrayerVfxGeometry.taperedTube(m, out, tail, head, 0.002, 0.024,
                    1.0f, 0.78f, 0.22f, 0.88f * alpha);
        }
    }

    private static void meteorRock(Matrix4fc m, VertexConsumer out, Vec3 center,
                                   double scale, double tumble, float alpha) {
        int latitudes = 8;
        int longitudes = 16;
        Vec3[][] rings = new Vec3[latitudes + 1][longitudes];
        for (int latitude = 0; latitude <= latitudes; latitude++) {
            double phi = -Math.PI / 2 + Math.PI * latitude / latitudes;
            for (int longitude = 0; longitude < longitudes; longitude++) {
                double theta = Math.PI * 2 * longitude / longitudes;
                double roughness = 0.83 + 0.10 * Math.sin(theta * 3 + latitude * 1.7)
                        + 0.08 * Math.cos(theta * 5 - latitude * 2.1);
                Vec3 point = new Vec3(Math.cos(phi) * Math.cos(theta) * scale * roughness,
                        Math.sin(phi) * scale * 0.82 * roughness,
                        Math.cos(phi) * Math.sin(theta) * scale * 0.93 * roughness);
                rings[latitude][longitude] = center.add(rotate(point, tumble * 0.73, tumble, tumble * 0.37));
            }
        }
        for (int latitude = 0; latitude < latitudes; latitude++) {
            for (int longitude = 0; longitude < longitudes; longitude++) {
                int next = (longitude + 1) % longitudes;
                float facet = (float) (0.72 + 0.24 * (0.5 + 0.5 * Math.sin(latitude * 2.2 + longitude * 3.4 + tumble)));
                float red = 0.21f * facet;
                float green = 0.11f * facet;
                float blue = 0.07f * facet;
                PrayerVfxGeometry.quad(m, out, rings[latitude][longitude], rings[latitude][next],
                        rings[latitude + 1][next], rings[latitude + 1][longitude], red, green, blue, 0.97f * alpha);
            }
        }

        for (int crack = 0; crack < 9; crack++) {
            double baseAngle = crack * Math.PI * 2 / 9 + tumble * 0.42;
            Vec3 previous = null;
            for (int step = 0; step <= 9; step++) {
                double phi = -1.12 + step * 0.245;
                double theta = baseAngle + 0.12 * Math.sin(step * 1.8 + crack);
                double roughness = 0.83 + 0.10 * Math.sin(theta * 3 + Math.abs(phi) * 5)
                        + 0.08 * Math.cos(theta * 5 - phi * 4);
                Vec3 point = new Vec3(Math.cos(phi) * Math.cos(theta) * scale * roughness * 1.012,
                        Math.sin(phi) * scale * 0.82 * roughness * 1.012,
                        Math.cos(phi) * Math.sin(theta) * scale * 0.93 * roughness * 1.012);
                point = center.add(rotate(point, tumble * 0.73, tumble, tumble * 0.37));
                if (previous != null) {
                    PrayerVfxGeometry.tube(m, out, previous, point, 0.032 * scale,
                            1.0f, 0.22f, 0.015f, 0.80f * alpha);
                    PrayerVfxGeometry.tube(m, out, previous, point, 0.012 * scale,
                            1.0f, 0.86f, 0.26f, 0.98f * alpha);
                }
                previous = point;
            }
        }

        for (int spur = 0; spur < 6; spur++) {
            double angle = spur * Math.PI / 3 + tumble;
            Vec3 base = center.add(Math.cos(angle) * scale * 0.50, Math.sin(spur * 1.7) * scale * 0.28,
                    Math.sin(angle) * scale * 0.50);
            Vec3 tip = base.add(Math.cos(angle) * scale * 0.31, (spur % 2 == 0 ? 1 : -1) * scale * 0.26,
                    Math.sin(angle) * scale * 0.31);
            PrayerVfxGeometry.taperedTube(m, out, base, tip, scale * 0.14, 0.002,
                    0.24f, 0.12f, 0.07f, 0.95f * alpha);
        }
    }

    private static void drawGradientQuad(Matrix4fc m, VertexConsumer out,
                                         Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                                         float r0, float g0, float b0, float a0,
                                         float r1, float g1, float b1, float a1) {
        if (a0 <= 0.004f && a1 <= 0.004f) return;
        emitVertex(m, out, p0, r0, g0, b0, a0);
        emitVertex(m, out, p1, r0, g0, b0, a0);
        emitVertex(m, out, p2, r1, g1, b1, a1);
        emitVertex(m, out, p3, r1, g1, b1, a1);

        emitVertex(m, out, p3, r1, g1, b1, a1);
        emitVertex(m, out, p2, r1, g1, b1, a1);
        emitVertex(m, out, p1, r0, g0, b0, a0);
        emitVertex(m, out, p0, r0, g0, b0, a0);
    }

    private static void emitVertex(Matrix4fc m, VertexConsumer out, Vec3 p,
                                   float r, float g, float b, float a) {
        out.addVertex(m, (float) p.x, (float) p.y, (float) p.z)
           .setColor(r, g, b, a);
    }

    private static Vec3 rotate(Vec3 point, double pitch, double yaw, double roll) {
        double cos = Math.cos(pitch), sin = Math.sin(pitch);
        double y = point.y * cos - point.z * sin;
        double z = point.y * sin + point.z * cos;
        double x = point.x;
        cos = Math.cos(yaw); sin = Math.sin(yaw);
        double nextX = x * cos + z * sin;
        z = -x * sin + z * cos;
        x = nextX;
        cos = Math.cos(roll); sin = Math.sin(roll);
        nextX = x * cos - y * sin;
        y = x * sin + y * cos;
        return new Vec3(nextX, y, z);
    }

    private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }
}
