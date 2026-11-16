package com.github.ob_yekt.simpleskills.prayer.fire;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerFireballVfx {
    private PrayerFireballVfx() {}

    // Gradiente de calor: 0 = brasa escura, 1 = branco-amarelado incandescente
    private static final float[][] HEAT = {
            {0.55f, 0.08f, 0.02f},
            {0.95f, 0.24f, 0.03f},
            {1.00f, 0.58f, 0.10f},
            {1.00f, 0.95f, 0.68f}
    };

    // Quanto do calor sobe (+Y) no impacto. 0 = nada sobe, 1 = sobe como a versão antiga. O estouro segue o eixo do disparo.
    private static final double HEAT_RISE = 0.30;

    // 26. FIREBALL — Bola de fogo reta: coração incandescente em camadas, línguas em espiral e esteira trançada.
    // No impacto o fogo espalha no plano do acerto e volta na direção de quem lançou, como se batesse no alvo.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 1, 4);
        double impactTick = Math.max(4.0, duration * 0.58); // sincronizado com frac(.58) do PrayerSoundtrack

        // Acelera ao sair da mão e chega ao alvo com velocidade (sem "frear" antes do impacto)
        double lin = clamp01(age / impactTick);
        double travel = lin * lin * (2.2 - 1.2 * lin);

        double dist = destination.length();
        Vec3 launchDir = PrayerVfxPrimitives.safeNormalize(destination, new Vec3(0, 0, 1));

        if (age < 6.0f) {
            castFlare(m, out, launchDir, age, fade);
        }

        float projFade = fade * (float) (1.0 - PrayerVfxPrimitives.smooth(impactTick - 0.6, impactTick + 0.8, age));
        if (projFade > 0.01f) {
            projectile(m, out, destination, dist, launchDir, travel, age, projFade);
        }

        if (age >= impactTick - 0.4) {
            impact(m, out, destination, launchDir, age, impactTick, duration, fade);
        }
    }

    // ───────────────────────── Disparo na mão ─────────────────────────

    private static void castFlare(Matrix4fc m, VertexConsumer out, Vec3 launchDir, float age, float fade) {
        double p = PrayerVfxPrimitives.smooth(0, 6.0, age);
        float a = (float) Math.pow(1.0 - p, 1.3) * fade;
        if (a <= 0.01f) return;

        Vec3[] f = PrayerVfxPrimitives.basis(launchDir);

        // Clarão curto no punho
        double flash = 0.10 + 0.16 * (1.0 - p);
        PrayerVfxGeometry.ellipsoid(m, out, launchDir.scale(0.14), new Vec3(flash, flash, flash),
                6, 8, 1.0f, 0.93f, 0.62f, 0.80f * a, age * 0.5);

        // Línguas que saem em espiral na direção do disparo
        for (int i = 0; i < 9; i++) {
            double h1 = hash01(i * 31 + 7);
            double h2 = hash01(i * 47 + 13);
            double ang = h1 * Math.PI * 2.0 + p * 1.6;
            Vec3 side = radial(f, ang);
            Vec3 side2 = radial(f, ang + 0.9);
            double reach = 0.28 + p * (0.70 + h2 * 0.35);

            Vec3 base = launchDir.scale(0.10);
            Vec3 knee = launchDir.scale(reach * 0.50).add(side.scale(reach * 0.42)).add(0, 0.08 * p, 0);
            Vec3 tip = launchDir.scale(reach).add(side2.scale(reach * 0.20)).add(0, 0.26 * p, 0);
            PrayerVfxPrimitives.flamePetal(m, out, base, knee, tip, 0.17 * (1.0 - p * 0.5), a);
        }
    }

    // ───────────────────────── Projétil em voo ─────────────────────────

    private static void projectile(Matrix4fc m, VertexConsumer out, Vec3 destination, double dist,
                                   Vec3 launchDir, double travel, float age, float pf) {
        Vec3 center = destination.scale(travel);
        Vec3 velDir = launchDir; // trajetória reta
        Vec3[] frame = PrayerVfxPrimitives.basis(velDir);

        // 1. Coração incandescente em 4 camadas (branco → amarelo → laranja → halo avermelhado), pulsando
        double pulse = 1.0 + 0.09 * Math.sin(age * 1.3) + 0.05 * Math.sin(age * 2.9 + 1.0);
        double c1 = 0.11 * pulse;
        double c2 = 0.19 * pulse;
        double c3 = 0.29 * pulse;
        double c4 = 0.42 * pulse;
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(c1, c1, c1),
                6, 8, 1.0f, 0.98f, 0.82f, 0.95f * pf, age * 0.5);
        PrayerVfxGeometry.ellipsoid(m, out, center.subtract(velDir.scale(0.04)), new Vec3(c2, c2 * 0.92, c2 * 1.06),
                7, 9, 1.0f, 0.80f, 0.28f, 0.80f * pf, -age * 0.35);
        PrayerVfxGeometry.ellipsoid(m, out, center.subtract(velDir.scale(0.10)), new Vec3(c3 * 1.05, c3 * 0.90, c3),
                8, 10, 1.0f, 0.46f, 0.06f, 0.36f * pf, age * 0.22);
        PrayerVfxGeometry.ellipsoid(m, out, center.subtract(velDir.scale(0.17)), new Vec3(c4, c4 * 0.88, c4 * 1.05),
                8, 10, 0.95f, 0.22f, 0.03f, 0.13f * pf, -age * 0.15);

        // 2. Línguas de fogo em duas camadas: externas longas em espiral, internas curtas e densas
        tongues(m, out, center, velDir, frame, age, pf, 18, 0, 0.70, 0.85, 0.24, 1.00, 1.00);
        tongues(m, out, center, velDir, frame, age, pf, 12, 100, 0.32, 0.45, 0.14, 0.62, 0.95);

        // 3. Filamentos quentes (amarelo-branco no miolo, laranja na ponta) dentro das chamas
        for (int s = 0; s < 7; s++) {
            double h1 = hash01(s * 61 + 23);
            double h2 = hash01(s * 37 + 5);
            Vec3 r = radial(frame, h1 * Math.PI * 2.0 + age * 0.35);
            double len = 0.40 + h2 * 0.45;
            double wob = Math.sin(age * 0.9 + s * 1.7) * 0.05;

            Vec3 p0 = center.add(velDir.scale(0.06)).add(r.scale(0.07));
            Vec3 p1 = center.subtract(velDir.scale(len * 0.5)).add(r.scale(0.15 + wob)).add(0, 0.05, 0);
            Vec3 p2 = center.subtract(velDir.scale(len)).add(r.scale(0.10 - wob)).add(0, 0.14, 0);
            PrayerVfxGeometry.taperedTube(m, out, p0, p1, 0.034, 0.022, 1.0f, 0.90f, 0.50f, 0.80f * pf);
            PrayerVfxGeometry.taperedTube(m, out, p1, p2, 0.022, 0.004, 1.0f, 0.62f, 0.16f, 0.65f * pf);
        }

        // 4. Esteira trançada: duas fitas de fogo em hélice que esfriam e afinam para trás
        double ribLen = Math.min(0.45, 3.2 / Math.max(1.0, dist));
        double tStart = Math.max(0.0, travel - ribLen);
        double span = travel - tStart;
        if (span > 0.01) {
            int seg = 14;
            for (int strand = 0; strand < 2; strand++) {
                Vec3 prevP = null;
                double prevU = 0.0;
                for (int k = 0; k <= seg; k++) {
                    double u = (double) k / seg; // 0 = cabeça, 1 = cauda
                    double tk = travel - span * u;
                    double rho = (0.06 + 0.16 * u) * (0.8 + 0.2 * Math.sin(age * 0.4 + strand));
                    double a = strand * Math.PI + age * 0.45 + u * 5.2;
                    Vec3 p = destination.scale(tk)
                            .add(frame[0].scale(Math.cos(a) * rho))
                            .add(frame[1].scale(Math.sin(a) * rho))
                            .add(0, u * 0.12, 0);
                    if (prevP != null) {
                        float[] c = heat(1.0 - u * 0.85);
                        float al = (float) Math.pow(1.0 - u, 1.2) * 0.85f * pf;
                        PrayerVfxGeometry.taperedTube(m, out, prevP, p,
                                0.045 * (1.0 - prevU) + 0.005, 0.045 * (1.0 - u) + 0.005,
                                c[0], c[1], c[2], al);
                    }
                    prevP = p;
                    prevU = u;
                }
            }
        }

        // 5. Labaredas soltas deixadas pelo caminho: sobem, giram e apagam
        int stations = Math.max(6, (int) (dist * 3.0));
        double wispWindow = Math.min(0.45, 2.8 / Math.max(1.0, dist));
        for (int s = 1; s <= stations; s++) {
            double tSpawn = (double) s / stations;
            if (tSpawn > travel) break;

            double ageAlong = travel - tSpawn;
            if (ageAlong > wispWindow) continue;

            double u = ageAlong / wispWindow; // 0 = acabou de sair da bola, 1 = apagando atrás
            Vec3 anchor = destination.scale(tSpawn);

            for (int k = 0; k < 2; k++) {
                int seed = s * 97 + k * 37;
                double r1 = hash01(seed);
                double r2 = hash01(seed + 5);
                double r3 = hash01(seed + 13);

                double spreadAng = r1 * Math.PI * 2.0 + age * 0.12 + u * 2.4;
                double spreadDist = (0.06 + r2 * 0.22) * (0.4 + u * 0.9);
                Vec3 drift = frame[0].scale(Math.cos(spreadAng) * spreadDist)
                        .add(frame[1].scale(Math.sin(spreadAng) * spreadDist));

                double rise = u * (0.35 + r3 * 0.45);
                double flameH = (0.28 + r2 * 0.32) * Math.sin(Math.min(1.0, u * 1.2) * Math.PI * 0.85 + 0.25);
                double sway = Math.sin(age * 0.65 + seed) * 0.12 * u;

                Vec3 wBase = anchor.add(drift).add(0, rise * 0.4, 0);
                Vec3 wKnee = wBase.subtract(velDir.scale(0.14 * u))
                        .add(drift.scale(0.5))
                        .add(sway, flameH * 0.55, -sway);
                Vec3 wTip = wBase.subtract(velDir.scale(0.26 * u))
                        .add(-sway * 0.7, flameH, sway * 0.7);

                double wWidth = (0.15 + r1 * 0.09) * (1.0 - u * 0.75);
                float wAlpha = (float) Math.pow(1.0 - u, 1.35) * 0.82f * pf;
                if (wAlpha > 0.02f && wWidth > 0.01) {
                    PrayerVfxPrimitives.flamePetal(m, out, wBase, wKnee, wTip, wWidth, wAlpha);
                }
            }

            // Fagulhas finas que esfriam enquanto escapam
            if (s % 2 == 0) {
                double eAng = hash01(s * 53) * Math.PI * 2.0 + age * 0.2;
                Vec3 ep0 = anchor.add(Math.cos(eAng) * (0.15 + u * 0.5), u * 0.55, Math.sin(eAng) * (0.15 + u * 0.5));
                Vec3 ep1 = ep0.subtract(velDir.scale(0.22 * (1.0 - u))).add(0, 0.10, 0);
                float[] c = heat(0.95 - u * 0.55);
                PrayerVfxGeometry.taperedTube(m, out, ep0, ep1, 0.022 * (1.0 - u), 0.003,
                        c[0], c[1], c[2], (float) (1.0 - u) * 0.85f * pf);
            }
        }
    }

    // Uma camada de línguas de fogo ao redor do núcleo, enroladas em espiral ao longo da esteira
    private static void tongues(Matrix4fc m, VertexConsumer out, Vec3 center, Vec3 velDir, Vec3[] frame,
                                float age, float pf, int count, int seed,
                                double lenBase, double lenVar, double bulgeBase,
                                double widthScale, double alphaScale) {
        for (int t = 0; t < count; t++) {
            double h1 = hash01((t + seed) * 17 + 3);
            double h2 = hash01((t + seed) * 29 + 11);
            double h3 = hash01((t + seed) * 43 + 19);

            double speed = 0.55 + h1 * 0.55;
            double phase = h2 * Math.PI * 2.0;
            double life = 0.5 + 0.5 * Math.sin(age * speed + phase); // cada língua cresce e recolhe no seu ritmo

            double ang = h3 * Math.PI * 2.0 + age * 0.20 + Math.sin(age * 0.25 + phase) * 0.30;
            double twist = 0.9 + h1 * 0.9; // quanto a língua se enrola até a ponta
            Vec3 r0 = radial(frame, ang);
            Vec3 r1 = radial(frame, ang + twist * 0.5);
            Vec3 r2 = radial(frame, ang + twist);

            double frontBias = 0.22 - h1 * 0.32;
            double baseRadius = 0.07 + h2 * 0.12;
            double bulge = bulgeBase + h3 * 0.18 + 0.07 * life;
            double tailLen = lenBase + h1 * lenVar + 0.35 * life;
            double lift = (0.10 + h2 * 0.26) * (0.6 + 0.4 * life); // calor sobe (+Y)
            double whip = Math.sin(age * (0.75 + h3 * 0.4) + phase) * 0.14;

            Vec3 base = center.add(velDir.scale(frontBias)).add(r0.scale(baseRadius));
            Vec3 knee = center.subtract(velDir.scale(tailLen * 0.30))
                    .add(r1.scale(bulge))
                    .add(0, lift * 0.45, 0);
            Vec3 tip = center.subtract(velDir.scale(tailLen))
                    .add(r2.scale(0.10 + 0.10 * h2 + whip))
                    .add(0, lift, 0);

            double width = (0.15 + h2 * 0.12) * (0.65 + 0.35 * life) * widthScale;
            float alpha = (float) ((0.55 + 0.40 * life) * alphaScale) * pf;
            PrayerVfxPrimitives.flamePetal(m, out, base, knee, tip, width, alpha);
        }
    }

    // ───────────────────────── Impacto ─────────────────────────
    // hitDir = direção em que a bola chegou. O fogo abre no plano perpendicular a ela e volta para "back",
    // a direção de quem lançou, como um splash contra a superfície do alvo.

    private static void impact(Matrix4fc m, VertexConsumer out, Vec3 center, Vec3 hitDir, float age,
                               double impactTick, int duration, float fade) {
        double burst = PrayerVfxPrimitives.smooth(impactTick, duration, age);
        double fast = PrayerVfxPrimitives.smooth(impactTick, impactTick + (duration - impactTick) * 0.35, age);
        float alpha = (float) Math.pow(1.0 - burst, 0.85) * fade;
        if (alpha <= 0.015f) return;

        Vec3 back = hitDir.scale(-1.0);
        Vec3[] frame = PrayerVfxPrimitives.basis(hitDir); // frame[0], frame[1]: plano do impacto

        // 1. Clarão no ponto exato do acerto + brilho achatado no plano do impacto, só nos primeiros ticks
        if (fast < 1.0) {
            float hot = (float) Math.pow(1.0 - fast, 1.4) * fade;
            double r1 = 0.20 + fast * 0.55;
            PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(r1, r1, r1),
                    8, 10, 1.0f, 0.96f, 0.70f, 0.90f * hot, age * 0.35);
            double r2 = 0.35 + fast * 1.05;
            Vec3 flat = new Vec3(r2 * (1.0 - 0.62 * Math.abs(hitDir.x)),
                    r2 * (1.0 - 0.62 * Math.abs(hitDir.y)),
                    r2 * (1.0 - 0.62 * Math.abs(hitDir.z)));
            PrayerVfxGeometry.ellipsoid(m, out, center.add(back.scale(0.05)), flat,
                    8, 10, 1.0f, 0.50f, 0.08f, 0.45f * hot, -age * 0.25);
        }

        // 2. Cabeça da erupção: gota de calor que recua em direção a quem lançou e esfria (branco → laranja → vermelho)
        double cr = (0.18 + 0.20 * fast) * (1.0 - burst * 0.55);
        float[] hc = heat(1.0 - burst * 0.9);
        float coreA = (float) Math.pow(1.0 - burst, 1.25) * 0.50f * fade;
        PrayerVfxGeometry.ellipsoid(m, out, center.add(back.scale(0.12 + burst * 1.15)),
                new Vec3(cr * (1.0 + 0.55 * Math.abs(back.x)), cr * (1.0 + 0.55 * Math.abs(back.y)), cr * (1.0 + 0.55 * Math.abs(back.z))),
                7, 9, hc[0], hc[1], hc[2], coreA, age * 0.3);

        // 3. 44 plumas: 14 abrindo no plano do impacto (algumas envolvem o alvo), 22 em cone para trás, 8 no jato central.
        //    Todas giram num vórtice em torno do eixo do disparo.
        int plumes = 44;
        for (int i = 0; i < plumes; i++) {
            double h1 = hash01(i * 41 + 3);
            double h2 = hash01(i * 67 + 17);
            double h3 = hash01(i * 89 + 29);
            double h4 = hash01(i * 103 + 7);

            double delay = h4 * 0.14;
            if (burst < delay) continue;
            double localT = (burst - delay) / (1.0 - delay);
            double expand = 1.0 - Math.pow(1.0 - localT, 2.2); // rápida no começo, desacelera no ar

            boolean splash = i < 14;
            boolean jet = i >= 36;
            // elev: 0 = no plano do impacto, >0 = inclinada para trás (back), <0 = envolvendo o alvo
            double elev = splash ? (-0.20 + h2 * 0.45)
                    : jet ? (1.15 + h2 * 0.35)
                    : (0.30 + h2 * 0.90);
            double maxReach = splash ? (1.20 + h3 * 0.95)
                    : jet ? (0.80 + h3 * 0.90)
                    : (1.00 + h3 * 1.10);

            double azim = h1 * Math.PI * 2.0 + localT * 0.85 + Math.sin(age * 0.14 + i) * 0.08;
            double reach = 0.16 + expand * maxReach;

            Vec3 ring = radial(frame, azim);
            Vec3 tangent = radial(frame, azim + Math.PI * 0.5);
            Vec3 dir3D = ring.scale(Math.cos(elev)).add(back.scale(Math.sin(elev)));

            // Convecção leve: o calor ainda sobe, mas sem dominar a direção do estouro
            double lift = localT * (0.45 + h2 * 0.95) * HEAT_RISE;
            double flicker = Math.sin(age * (0.55 + h3 * 0.45) + i * 1.9) * 0.12 * localT;
            double curl = reach * 0.22 * localT; // pontas se enrolam no sentido do vórtice

            Vec3 base = center.add(dir3D.scale(0.10 + expand * 0.28)).add(0, 0.03 + lift * 0.25, 0);
            Vec3 knee = center.add(dir3D.scale(reach * 0.62))
                    .add(tangent.scale(curl * 0.5))
                    .add(flicker, 0.06 + lift * 0.65, -flicker);
            Vec3 tip = center.add(dir3D.scale(reach * 0.92))
                    .add(tangent.scale(curl))
                    .add(-flicker * 0.6, 0.10 + lift * 1.25 + h3 * 0.15, flicker * 0.6);

            double width = (0.24 + h3 * 0.20) * (1.0 - localT * 0.55) * (jet ? 1.15 : 1.0);
            float pAlpha = (float) Math.pow(1.0 - localT, 0.95) * (0.72f + 0.25f * (float) h1) * fade;
            if (pAlpha > 0.02f && width > 0.015) {
                PrayerVfxPrimitives.flamePetal(m, out, base, knee, tip, width, pAlpha);
            }
        }

        // 4. Fogo residual: labaredas coladas no plano do impacto (como na frente do alvo), apontando para trás
        for (int i = 0; i < 10; i++) {
            double h1 = hash01(i * 59 + 2);
            double h2 = hash01(i * 83 + 14);
            double h3 = hash01(i * 37 + 21);

            double l = (burst - (0.12 + h3 * 0.30)) / 0.55;
            if (l <= 0.0 || l >= 1.0) continue;
            double env = Math.sin(l * Math.PI);

            double rad = 0.30 + h2 * 1.25;
            double sway = Math.sin(age * 0.5 + i * 2.1) * 0.10;

            Vec3 base = center.add(radial(frame, h1 * Math.PI * 2.0).scale(rad)).add(back.scale(0.04));
            Vec3 knee = base.add(back.scale(0.22 + 0.30 * env)).add(sway * 0.5, 0.05 * env, -sway * 0.5);
            Vec3 tip = base.add(back.scale(0.45 + 0.65 * env * (0.7 + h2 * 0.5))).add(sway * 1.4, 0.18 * env, -sway * 1.4);
            PrayerVfxPrimitives.flamePetal(m, out, base, knee, tip, 0.12 + 0.16 * env, (float) (0.80 * env) * fade);
        }

        // 5. 24 brasas em cone para trás, com gravidade: esfriam (amarelo → laranja → vermelho) e cintilam
        for (int s = 0; s < 24; s++) {
            double ang = hash01(s * 73 + 5) * Math.PI * 2.0;
            double side = 0.9 + hash01(s * 31 + 9) * 1.8;
            double backSpeed = 0.8 + hash01(s * 19 + 3) * 2.2;
            double t0 = Math.max(0.0, burst - 0.12);

            Vec3 p0 = emberArc(center, back, frame, ang, side, backSpeed, t0);
            Vec3 p1 = emberArc(center, back, frame, ang, side, backSpeed, burst);
            float[] c = heat(0.95 - burst * 0.70 - hash01(s * 11 + 1) * 0.10);
            float twinkle = 0.65f + 0.35f * (float) Math.sin(age * 1.6 + s * 2.3);
            PrayerVfxGeometry.taperedTube(m, out, p0, p1, 0.010, 0.040 * (1.0 - burst * 0.6),
                    c[0], c[1], c[2], alpha * twinkle);
        }
    }

    // ───────────────────────── Utilidades ─────────────────────────

    private static Vec3 emberArc(Vec3 origin, Vec3 back, Vec3[] frame, double angle,
                                  double sideSpeed, double backSpeed, double t) {
        double k = t * 1.45;
        double y = 0.10 + 0.60 * k - 1.6 * t * t; // sobe um pouco com o calor e depois cai
        return origin.add(radial(frame, angle).scale(sideSpeed * k))
                .add(back.scale(backSpeed * k))
                .add(0, Math.max(-0.8, y), 0);
    }

    private static Vec3 radial(Vec3[] frame, double angle) {
        return frame[0].scale(Math.cos(angle)).add(frame[1].scale(Math.sin(angle)));
    }

    private static float[] heat(double h) {
        double x = clamp01(h) * 3.0;
        int i = Math.min(2, (int) x);
        float k = (float) (x - i);
        float[] a = HEAT[i];
        float[] b = HEAT[i + 1];
        return new float[]{
                a[0] + (b[0] - a[0]) * k,
                a[1] + (b[1] - a[1]) * k,
                a[2] + (b[2] - a[2]) * k
        };
    }

    private static double clamp01(double v) {
        return v < 0.0 ? 0.0 : Math.min(v, 1.0);
    }

    private static double hash01(int seed) {
        int x = seed * 0x27d4eb2d;
        x ^= (x >>> 15);
        x *= 0x85ebca6b;
        x ^= (x >>> 13);
        return (x & 0x7fffffff) / (double) 0x7fffffff;
    }
}
