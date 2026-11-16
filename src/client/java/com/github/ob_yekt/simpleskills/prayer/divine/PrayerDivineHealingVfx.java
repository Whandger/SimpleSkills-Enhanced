package com.github.ob_yekt.simpleskills.prayer.divine;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerDivineHealingVfx {
    private PrayerDivineHealingVfx() {}

    // 2. DIVINE_HEALING — Runas sagradas discretas nos pés do conjurador e
    // um campo aéreo de 10x10 blocos repleto de cruzes de luz suspensas no céu.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        double safeDuration = Math.max(1.0, duration);
        double t = Math.max(0.0, Math.min(1.0, age / safeDuration));

        // Entrada serena e dissipação gradual no final (65% -> 100%)
        double fadeIn = PrayerVfxPrimitives.smooth(0.0, 0.12, t);
        double fadeOutRaw = 1.0 - PrayerVfxPrimitives.smooth(0.65, 1.0, t);
        double fadeOut = fadeOutRaw * fadeOutRaw;
        float fade = (float) (fadeIn * fadeOut);
        if (fade <= 0.005f) return;

        // ------------------------------------------------------------------
        // 1. RUNAS DISCRETAS NOS PÉS DO CONJURADOR (Centro)
        // ------------------------------------------------------------------
        Vec3 ground = new Vec3(0.0, 0.035, 0.0);
        double sealGrow = PrayerVfxPrimitives.smooth(0.0, 0.16, t) * (0.85 + 0.15 * fadeOutRaw);
        double sealR = 1.15 * sealGrow;
        double rot = age * 0.014;

        PrayerVfxPrimitives.circleSigil(m, out, ground, sealR, rot, fade * 0.85f,
                1.0f, 0.85f, 0.34f);
        PrayerVfxPrimitives.ringPlane(m, out, ground.add(0, 0.012, 0), PrayerVfxPrimitives.UP,
                sealR * 0.68, 0.016, -rot * 1.5,
                1.0f, 0.96f, 0.72f, 0.78f * fade);
        PrayerVfxPrimitives.runeHalo(m, out, ground.add(0, 0.016, 0),
                sealR * 0.84, rot, fade * 0.80f, 1.0f, 0.90f, 0.48f);

        // ------------------------------------------------------------------
        // 2. CAMPO DE 24 CRUZES DE LUZ SUSPENSAS NO CÉU (Área 10x10: -5 a +5)
        // ------------------------------------------------------------------
        int crossCount = 24;
        double halfArea = 4.75; // 10x10 blocos (-5.0 a +5.0 com margem para os braços)

        for (int i = 0; i < crossCount; i++) {
            // Cada cruz aparece num instante escalonado e fica parada no ponto dela
            double appearStart = (i % 8) * 0.025 + (i / 8) * 0.015;
            double vGrow = PrayerVfxPrimitives.smooth(appearStart, appearStart + 0.14, t) * fadeOutRaw;
            double hGrow = PrayerVfxPrimitives.smooth(appearStart + 0.06, appearStart + 0.20, t) * fadeOutRaw;
            if (vGrow <= 0.01) continue;

            // Posição fixa determinística espalhada em 10x10 (sequência de Halton)
            double nx = halton(i + 1, 2) * 2.0 - 1.0; // -1 .. 1
            double nz = halton(i + 1, 3) * 2.0 - 1.0; // -1 .. 1
            double ny = halton(i + 1, 5);              //  0 .. 1

            double x = nx * halfArea;
            double z = nz * halfArea;
            // Evita nascer bem em cima da cabeça do jogador no centro
            if (Math.hypot(x, z) < 0.75) {
                x += (x >= 0 ? 0.85 : -0.85);
            }

            // Parada no lugar, apenas com um leve flutuar estacionário
            double hover = Math.sin(age * 0.08 + i * 1.37) * 0.04;
            double y = 1.65 + ny * 2.95 + hover;
            Vec3 pos = new Vec3(x, y, z);

            // Escala variada entre cruzes médias e maiores
            double scale = 0.75 + (i % 4) * 0.18;
            // Orientação fixa para cada cruz (olhando para ângulos variados, sem girar)
            double yaw = (i * 2.399963) % (Math.PI * 2.0);

            drawSuspendedSkyCross(m, out, pos, yaw, scale, vGrow, hGrow, fade);
        }
    }

    private static void drawSuspendedSkyCross(Matrix4fc m, VertexConsumer out,
                                              Vec3 center, double yaw, double scale,
                                              double vGrow, double hGrow, float fade) {
        Vec3 right = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
        Vec3 normal = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));

        // Auréola sutil atrás da cruz parada
        double haloR = 0.34 * scale * vGrow;
        PrayerVfxPrimitives.ringPlane(m, out, center, normal,
                haloR, 0.012 * scale, 0.0,
                1.0f, 0.86f, 0.36f, 0.62f * fade);

        // Haste vertical da cruz
        double topLen = 0.44 * scale * vGrow;
        double botLen = 0.64 * scale * vGrow;
        Vec3 top = center.add(0.0, topLen, 0.0);
        Vec3 bottom = center.subtract(0.0, botLen, 0.0);

        // Aura dourada vertical
        PrayerVfxGeometry.taperedTube(m, out, bottom, top,
                0.048 * scale, 0.042 * scale,
                1.0f, 0.80f, 0.24f, 0.32f * fade);
        // Núcleo branco-perolado vertical
        PrayerVfxGeometry.taperedTube(m, out, bottom, top,
                0.020 * scale, 0.018 * scale,
                1.0f, 0.99f, 0.88f, 0.94f * fade);

        // Braços horizontais da cruz
        if (hGrow > 0.01) {
            double armLen = 0.40 * scale * hGrow;
            Vec3 leftArm = center.subtract(right.scale(armLen));
            Vec3 rightArm = center.add(right.scale(armLen));

            // Aura dourada horizontal
            PrayerVfxGeometry.tube(m, out, leftArm, rightArm,
                    0.044 * scale, 1.0f, 0.80f, 0.24f, 0.32f * fade);
            // Núcleo branco-perolado horizontal
            PrayerVfxGeometry.tube(m, out, leftArm, rightArm,
                    0.018 * scale, 1.0f, 0.99f, 0.88f, 0.94f * fade);
        }

        // Ponto de luz no cruzamento
        PrayerVfxGeometry.ellipsoid(m, out, center,
                new Vec3(0.075 * scale * vGrow, 0.075 * scale * vGrow, 0.075 * scale * vGrow),
                6, 10, 1.0f, 0.98f, 0.82f, 0.85f * fade, 0.0);
    }

    // Sequência de Halton para espalhar as cruzes uniformemente sem amontoar
    private static double halton(int index, int base) {
        double result = 0.0;
        double f = 1.0 / base;
        int i = index;
        while (i > 0) {
            result += f * (i % base);
            i /= base;
            f /= base;
        }
        return result;
    }
}
