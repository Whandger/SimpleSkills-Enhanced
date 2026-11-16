package com.github.ob_yekt.simpleskills.prayer.ice;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerIcePrisonVfx {
    private PrayerIcePrisonVfx() {}

    private static final int BARS = 10;
    // Fração da duração em que a gaiola termina de fechar.
    private static final double BUILD_END = 0.46;
    // Fração da duração em que a prisão começa a quebrar (ajuste aqui para quebrar mais cedo/tarde).
    private static final double SHATTER_START = 0.78;
    // Quantos cacos nascem de cada peça.
    private static final int BAR_PIECES = 4;
    private static final int DOME_PIECES = 2;
    private static final int FROST_PIECES = 2;
    private static final int RING_SHARDS = 10;
    // Estilhaços pequenos extras no momento da quebra.
    private static final int DUST_COUNT = 26;
    // Alturas dos anéis horizontais (as "travessas" da gaiola) e altura média das barras.
    private static final double[] RING_HEIGHTS = {0.55, 1.35, 2.1};
    private static final double CAGE_HEIGHT = 2.75;

    // 33. ICE_PRISON — gaiola de estacas de gelo que sobe do chão e se fecha em cúpula sobre a vítima.
    // Depois de um tempo ela racha, brilha e se estilhaça por completo antes de sumir.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 4, 2);
        double build = PrayerVfxPrimitives.smooth(2, duration * BUILD_END, age);

        // Fase de quebra: ts vai de 0 (inteira) a 1 (tudo virou pó).
        double shatterStart = duration * SHATTER_START;
        double shatterEnd = duration - 1.0;
        double ts = Math.min(1.0, Math.max(0.0, (age - shatterStart) / Math.max(1.0, shatterEnd - shatterStart)));
        boolean broken = ts > 0.0;
        // Pré-rachadura: a prisão treme e clareia pouco antes de quebrar.
        double crackle = PrayerVfxPrimitives.smooth(shatterStart - 4.0, shatterStart, age);
        // Flash branco: sobe na rachadura e some rápido no início da quebra.
        double flash = broken ? Math.max(0.0, 1.0 - ts * 5.0) : crackle;
        double tremble = broken ? 0.0 : crackle * 0.025;
        // As peças originais encolhem rápido no começo da quebra, enquanto os cacos já voam.
        double mainScale = broken ? 1.0 - PrayerVfxPrimitives.smooth(0.0, 0.2, ts) : 1.0;
        float ringScale = (float) mainScale;

        float iceR = tint(0.36f, flash), iceG = tint(0.75f, flash), iceB = tint(1.0f, flash);
        float coreR = tint(0.88f, flash), coreG = tint(0.98f, flash), coreB = tint(1.0f, flash);
        float ringR = tint(0.51f, flash), ringG = tint(0.87f, flash), ringB = tint(1.0f, flash);

        // Anel de gelo no chão.
        double groundRadius = 0.78 + build * 0.3;
        if (ringScale > 0.02f) {
            PrayerVfxPrimitives.ringPlane(m, out, destination.add(0, 0.08, 0), PrayerVfxPrimitives.UP, groundRadius,
                    0.035, age * 0.012, ringR, ringG, ringB, 0.86f * fade * ringScale);
        }
        if (broken) {
            ringShards(m, out, destination, 0.08, groundRadius, 12, 300, ts, ringR, ringG, ringB, 0.86f * fade);
        }

        // Espinhos de geada rastejando no chão, ao redor da gaiola.
        double frost = PrayerVfxPrimitives.smooth(2, duration * 0.36, age);
        if (frost > 0.0) {
            for (int j = 0; j < 12; j++) {
                double ang = j * Math.PI / 6 + 0.25;
                double dist = 1.08 + (j % 3) * 0.1;
                double size = (0.28 + (j % 4) * 0.1) * frost;
                Vec3 axis = new Vec3(Math.cos(ang) * 0.55, 1.0, Math.sin(ang) * 0.55).normalize();
                Vec3 center = destination.add(Math.cos(ang) * dist, 0.1 + size * 0.4, Math.sin(ang) * dist);
                piece(m, out, center, axis, size * 1.6, size * 0.4, 400 + j, FROST_PIECES, ts, mainScale,
                        shake(age, 400 + j, tremble), destination, iceR, iceG, iceB, 0.75f * fade);
            }
        }

        // Barras principais: nascem do chão em ordem embaralhada e se inclinam para dentro, formando a cúpula.
        double stagger = duration * 0.012;
        for (int i = 0; i < BARS; i++) {
            double angle = i * Math.PI * 2.0 / BARS + 0.1;
            double baseRadius = 0.9 + (i % 2) * 0.12;
            double topHeight = 2.6 + (i % 3) * 0.3;
            Vec3 base = destination.add(Math.cos(angle) * baseRadius, 0.1, Math.sin(angle) * baseRadius);
            Vec3 top = destination.add(Math.cos(angle) * baseRadius * 0.5, topHeight, Math.sin(angle) * baseRadius * 0.5);
            Vec3 full = top.subtract(base);
            double fullLength = full.length();
            Vec3 axis = full.scale(1.0 / fullLength);

            int order = (i * 3) % BARS;
            double grow = PrayerVfxPrimitives.smooth(2 + order * stagger, duration * 0.30 + order * stagger, age);
            if (grow <= 0.0) continue;

            double length = fullLength * grow;
            Vec3 center = base.add(axis.scale(length * 0.5));
            double width = 0.31 - (i % 3) * 0.035;
            Vec3 sh = shake(age, i, tremble);

            piece(m, out, center, axis, length, width, i, BAR_PIECES, ts, mainScale, sh, destination,
                    iceR, iceG, iceB, 0.78f * fade);
            // Núcleo claro por dentro da barra.
            piece(m, out, center, axis, length * 0.68, 0.13, i + 50, 2, ts, mainScale, sh, destination,
                    coreR, coreG, coreB, 0.85f * fade);
        }

        // Anéis horizontais: as travessas que prendem as barras.
        for (int k = 0; k < RING_HEIGHTS.length; k++) {
            double y = RING_HEIGHTS[k];
            double radius = 0.96 * (1.0 - 0.5 * (y / CAGE_HEIGHT));
            double rb = PrayerVfxPrimitives.smooth(duration * (0.20 + k * 0.05), duration * (0.34 + k * 0.05), age);
            if (rb <= 0.0) continue;

            float a = 0.8f * fade * (float) rb;
            double spin = (k % 2 == 0 ? 1.0 : -1.0) * age * 0.02;
            if (ringScale > 0.02f) {
                PrayerVfxPrimitives.ringPlane(m, out, destination.add(0, y, 0), PrayerVfxPrimitives.UP,
                        radius * (0.7 + 0.3 * rb), 0.025, spin, ringR, ringG, ringB, a * ringScale);
            }
            if (broken) {
                ringShards(m, out, destination, y, radius, RING_SHARDS, 320 + k * 20, ts, ringR, ringG, ringB, a);
            }
        }

        // Pontas de gelo pendendo no topo, fechando a cúpula.
        double dome = PrayerVfxPrimitives.smooth(duration * 0.26, duration * 0.44, age);
        if (dome > 0.0) {
            for (int j = 0; j < 6; j++) {
                double ang = j * Math.PI / 3 + 0.4;
                Vec3 axis = new Vec3(-Math.cos(ang) * 0.2, -1.0, -Math.sin(ang) * 0.2).normalize();
                Vec3 center = destination.add(Math.cos(ang) * 0.28, 2.72 + (1.0 - dome) * 0.6, Math.sin(ang) * 0.28);
                piece(m, out, center, axis, 0.55 * dome, 0.15, 200 + j, DOME_PIECES, ts, mainScale,
                        shake(age, 200 + j, tremble), destination, iceR, iceG, iceB, 0.8f * fade);
            }
        }

        // Cascas de gelo translúcido: uma fina colada no corpo e outra maior, na altura da gaiola.
        double pulse = 1.0 + 0.15 * Math.sin(age * 0.35);
        float shellScale = (float) (build * pulse * (1.0 + flash * 2.5) * mainScale);
        float outerAlpha = 0.09f * fade * shellScale;
        float innerAlpha = 0.07f * fade * shellScale;
        if (outerAlpha > 0.004f) {
            PrayerVfxGeometry.ellipsoid(m, out, destination.add(0, 1.35, 0), new Vec3(0.6, 1.3, 0.6),
                    8, 12, iceR, iceG, iceB, outerAlpha, -age * 0.02);
        }
        if (innerAlpha > 0.004f) {
            PrayerVfxGeometry.ellipsoid(m, out, destination.add(0, 1.0, 0), new Vec3(0.4, 0.85, 0.4),
                    8, 12, iceR, iceG, iceB, innerAlpha, age * 0.03);
        }

        if (broken) {
            // Onda de choque no chão e nuvem de estilhaços saindo do centro.
            double ease = 1.0 - (1.0 - ts) * (1.0 - ts);
            double waveThickness = 0.07 * (1.0 - ts);
            if (waveThickness > 0.006) {
                PrayerVfxPrimitives.ringPlane(m, out, destination.add(0, 0.1, 0), PrayerVfxPrimitives.UP,
                        0.9 + ease * 2.0, waveThickness, 0.0, 0.7f, 0.92f, 1.0f, (float) (0.7 * (1.0 - ts)) * fade);
            }
            drawDust(m, out, destination.add(0, 1.2, 0), ts, coreR, coreG, coreB, 0.9f * fade);
        }
    }

    // Desenha a peça inteira (com tremor) antes da quebra; depois, encolhe a original e solta os cacos.
    private static void piece(Matrix4fc m, VertexConsumer out, Vec3 center, Vec3 axis, double length, double width,
                              int seed, int pieces, double ts, double mainScale, Vec3 shake, Vec3 origin,
                              float r, float g, float b, float a) {
        if (ts <= 0.0) {
            PrayerVfxPrimitives.crystal(m, out, center.add(shake), axis, length, width, r, g, b, a);
            return;
        }
        if (mainScale > 0.02) {
            PrayerVfxPrimitives.crystal(m, out, center, axis, length * mainScale, width * mainScale, r, g, b, a);
        }
        shatterCrystal(m, out, center, axis, length, width, seed, pieces, ts, origin, r, g, b, a);
    }

    // Quebra um cristal em vários cacos que voam para fora, giram, caem e encolhem até sumir.
    private static void shatterCrystal(Matrix4fc m, VertexConsumer out, Vec3 center, Vec3 axis,
                                       double length, double width, int seed, int pieces, double ts,
                                       Vec3 origin, float r, float g, float b, float a) {
        double shrink = Math.pow(1.0 - ts, 0.8);
        if (shrink < 0.04) return;

        double ease = 1.0 - (1.0 - ts) * (1.0 - ts); // sai rápido e desacelera
        double fall = 1.8 * ts * ts;                 // gravidade
        float alpha = (float) (a * (1.0 - 0.5 * ts));

        // Direção horizontal "para fora" a partir do eixo da prisão.
        Vec3 flat = new Vec3(center.x - origin.x, 0, center.z - origin.z);
        flat = flat.lengthSqr() > 1.0E-4 ? flat.normalize() : Vec3.ZERO;

        for (int p = 0; p < pieces; p++) {
            int s = seed * 37 + p * 11;

            Vec3 dir = new Vec3(
                    flat.x * 0.9 + (rnd(s) - 0.5) * 1.4,
                    0.25 + rnd(s + 1) * 0.9,
                    flat.z * 0.9 + (rnd(s + 2) - 0.5) * 1.4).normalize();
            double speed = 0.9 + rnd(s + 3) * 1.7;

            // Cada caco nasce em um ponto diferente ao longo do cristal original.
            double along = pieces > 1 ? (p / (double) (pieces - 1) - 0.5) : 0.0;
            Vec3 start = center.add(axis.scale(along * length * 0.7));
            Vec3 pos = start.add(dir.scale(speed * ease)).add(0, -fall, 0);

            // Giro do caco em pleno ar.
            double spin = ts * (4.0 + rnd(s + 4) * 6.0);
            Vec3 pieceAxis = new Vec3(
                    axis.x + Math.sin(spin + s) * 0.9,
                    axis.y + Math.cos(spin * 1.3 + s) * 0.9,
                    axis.z + Math.sin(spin * 0.8 + s * 0.5) * 0.9).normalize();

            double pieceLength = length * (0.30 + rnd(s + 5) * 0.25) * shrink;
            double pieceWidth = width * (0.55 + rnd(s + 6) * 0.30) * shrink;

            PrayerVfxPrimitives.crystal(m, out, pos, pieceAxis, pieceLength, pieceWidth, r, g, b, alpha);
        }
    }

    // Cacos que nascem ao longo de um anel (o anel em si só some, os cacos é que voam).
    private static void ringShards(Matrix4fc m, VertexConsumer out, Vec3 destination, double y, double radius,
                                   int count, int seedBase, double ts, float r, float g, float b, float a) {
        double segment = Math.PI * 2.0 * radius / count * 1.8;
        for (int j = 0; j < count; j++) {
            double ang = j * Math.PI * 2.0 / count;
            Vec3 center = destination.add(Math.cos(ang) * radius, y, Math.sin(ang) * radius);
            Vec3 axis = new Vec3(-Math.sin(ang), 0.1, Math.cos(ang)).normalize();
            shatterCrystal(m, out, center, axis, segment, 0.08, seedBase + j, 1, ts, destination, r, g, b, a);
        }
    }

    // Nuvem de estilhaços minúsculos que sai do centro da prisão na hora da quebra.
    private static void drawDust(Matrix4fc m, VertexConsumer out, Vec3 origin, double ts,
                                 float r, float g, float b, float a) {
        double shrink = Math.pow(1.0 - ts, 0.8);
        if (shrink < 0.04) return;

        double ease = 1.0 - (1.0 - ts) * (1.0 - ts);
        double fall = 1.4 * ts * ts;
        float alpha = (float) (a * (1.0 - 0.5 * ts));

        for (int k = 0; k < DUST_COUNT; k++) {
            int s = 700 + k * 13;
            double angle = rnd(s) * Math.PI * 2.0;
            Vec3 dir = new Vec3(Math.cos(angle), (rnd(s + 1) - 0.3) * 1.6, Math.sin(angle)).normalize();
            double speed = 1.2 + rnd(s + 2) * 2.4;

            Vec3 pos = origin.add(dir.scale(speed * ease)).add(0, -fall, 0);

            double spin = ts * (6.0 + rnd(s + 3) * 8.0);
            Vec3 axis = new Vec3(Math.sin(spin + s), Math.cos(spin * 1.2 + s), Math.sin(spin * 0.7 + s * 0.3)).normalize();

            double size = (0.18 + rnd(s + 4) * 0.22) * shrink;
            PrayerVfxPrimitives.crystal(m, out, pos, axis, size * 2.0, size * 0.45, r, g, b, alpha);
        }
    }

    // Tremor leve do gelo antes de quebrar.
    private static Vec3 shake(float age, int i, double amount) {
        if (amount <= 0.0) return Vec3.ZERO;
        return new Vec3(Math.sin(age * 47.0 + i * 3.1) * amount, 0, Math.cos(age * 53.0 + i * 1.7) * amount);
    }

    // Clareia a cor em direção ao branco.
    private static float tint(float base, double flash) {
        return (float) (base + (1.0f - base) * flash * 0.75);
    }

    // Número pseudo-aleatório determinístico (0..1): o mesmo caco age igual em todo frame, sem tremer.
    private static double rnd(int seed) {
        double x = Math.sin(seed * 12.9898 + 78.233) * 43758.5453;
        return x - Math.floor(x);
    }
}
