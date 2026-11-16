package com.github.ob_yekt.simpleskills.prayer.ice;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerIceShardVfx {
    private PrayerIceShardVfx() {}

    // Fração da duração em que o gelo termina de correr até o mob.
    private static final double TRAVEL_END = 0.58;
    // Fração da duração em que o gelo começa a quebrar (ajuste aqui para quebrar mais cedo/tarde).
    private static final double SHATTER_START = 0.72;
    // Quantos cacos nascem de cada cristal.
    private static final int PATH_PIECES = 3;
    private static final int IMPACT_PIECES = 5;
    // Quantidade de estilhaços pequenos extras no momento da quebra.
    private static final int DUST_COUNT = 18;

    // 31. ICE_SHARD — Corre pelo chão rasgando a terra, explode em cristais no mob e se estilhaça por completo antes de sumir.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 2, 4);
        double travel = PrayerVfxPrimitives.smooth(2, duration * TRAVEL_END, age);

        // Fase de quebra: ts vai de 0 (inteiro) a 1 (tudo virou pó).
        double shatterStart = duration * SHATTER_START;
        double shatterEnd = duration - 1.0;
        double ts = Math.min(1.0, Math.max(0.0, (age - shatterStart) / Math.max(1.0, shatterEnd - shatterStart)));
        // Pré-rachadura: o gelo treme e clareia pouco antes de quebrar.
        double crackle = PrayerVfxPrimitives.smooth(shatterStart - 3.0, shatterStart, age);
        // Flash branco: sobe na rachadura e some rápido no início da quebra.
        double flash = ts > 0.0 ? Math.max(0.0, 1.0 - ts * 5.0) : crackle;
        double tremble = ts > 0.0 ? 0.0 : crackle * 0.03;
        // O cristal original encolhe rápido no começo da quebra, enquanto os cacos já voam.
        double mainScale = 1.0 - PrayerVfxPrimitives.smooth(0.0, 0.2, ts);

        float cr = tint(0.35f, flash);
        float cg = tint(0.78f, flash);
        float cb = tint(1.0f, flash);

        // Estima o chão baseando-se na posição do jogador (Vec3.ZERO) e do alvo.
        double floorY = -1.2;

        int numSpikes = (int) (travel * 22);
        for (int i = 0; i < numSpikes; i++) {
            double t = i / 22.0;
            Vec3 groundPath = new Vec3(0, floorY, 0).lerp(destination.add(0, -0.5, 0), t);

            double zig = Math.sin(i * 14.3) * 0.35;
            double zag = Math.cos(i * 21.7) * 0.35;
            Vec3 basePoint = groundPath.add(zig, 0, zag);

            double scale = 0.4 + (i % 3) * 0.25;
            Vec3 axis = new Vec3(zig * 0.3, 1.0, zag * 0.3).normalize();

            Vec3 center = basePoint.add(0, scale * 0.5, 0);
            double length = scale * 1.5;
            double width = scale * 0.35;
            float alpha = 0.9f * fade;

            if (ts <= 0.0) {
                PrayerVfxPrimitives.crystal(m, out, center.add(shake(age, i, tremble)), axis, length, width,
                        cr, cg, cb, alpha);
                continue;
            }

            if (mainScale > 0.02) {
                PrayerVfxPrimitives.crystal(m, out, center, axis, length * mainScale, width * mainScale,
                        cr, cg, cb, alpha);
            }
            shatterCrystal(m, out, center, axis, length, width, i, PATH_PIECES, ts, groundPath,
                    cr, cg, cb, alpha);
        }

        // Erupção massiva ao atingir o mob
        if (travel > 0.85) {
            double impact = PrayerVfxPrimitives.smooth(0.85, 1, travel);
            Vec3 impactCenter = destination.add(0, -0.5, 0);
            for (int i = 0; i < 12; i++) {
                double angle = i * Math.PI / 6;
                Vec3 dir = new Vec3(Math.cos(angle), 0.8 + (i % 3) * 0.5, Math.sin(angle)).normalize();
                double scale = (1.5 + (i % 4) * 0.6) * impact;
                Vec3 spikeBase = impactCenter.add(Math.cos(angle) * 0.4, 0, Math.sin(angle) * 0.4);

                Vec3 center = spikeBase.add(dir.scale(scale * 0.5));
                double length = scale * 2.2;
                double width = scale * 0.45;
                float alpha = (float) (0.9 * (1 - impact * 0.2) * fade);

                if (ts <= 0.0) {
                    PrayerVfxPrimitives.crystal(m, out, center.add(shake(age, i + 40, tremble)), dir, length, width,
                            cr, cg, cb, alpha);
                    continue;
                }

                if (mainScale > 0.02) {
                    PrayerVfxPrimitives.crystal(m, out, center, dir, length * mainScale, width * mainScale,
                            cr, cg, cb, alpha);
                }
                shatterCrystal(m, out, center, dir, length, width, i + 100, IMPACT_PIECES, ts, impactCenter,
                        cr, cg, cb, alpha);
            }

            // Estilhaços pequenos extras para a quebra parecer total.
            if (ts > 0.0) {
                drawDust(m, out, impactCenter, ts, cr, cg, cb, 0.9f * fade);
            }
        }
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

        // Direção horizontal "para fora" a partir da origem da quebra.
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

    // Nuvem de estilhaços minúsculos que sai do ponto de impacto na hora da quebra.
    private static void drawDust(Matrix4fc m, VertexConsumer out, Vec3 impactCenter, double ts,
                                 float r, float g, float b, float a) {
        double shrink = Math.pow(1.0 - ts, 0.8);
        if (shrink < 0.04) return;

        double ease = 1.0 - (1.0 - ts) * (1.0 - ts);
        double fall = 1.4 * ts * ts;
        float alpha = (float) (a * (1.0 - 0.5 * ts));
        Vec3 origin = impactCenter.add(0, 0.4, 0);

        for (int k = 0; k < DUST_COUNT; k++) {
            int s = 500 + k * 13;
            double angle = rnd(s) * Math.PI * 2.0;
            Vec3 dir = new Vec3(Math.cos(angle), 0.3 + rnd(s + 1) * 1.0, Math.sin(angle)).normalize();
            double speed = 1.0 + rnd(s + 2) * 2.2;

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
