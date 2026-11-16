package com.github.ob_yekt.simpleskills.prayer.unholy;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerShadowstepVfx {
    private PrayerShadowstepVfx() {}

    // 15. SHADOWSTEP — Um portal sombrio engole a origem enquanto uma fenda abissal rasga o espaço até o destino.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        double safeDuration = Math.max(1.0, duration);
        double t = Math.max(0.0, Math.min(1.0, age / safeDuration));

        float fade = PrayerVfxPrimitives.fade(age, duration, 2, 5);
        if (fade <= 0.005f) return;

        double vanish = PrayerVfxPrimitives.smooth(0.0, 0.45, t);
        double dash = PrayerVfxPrimitives.smooth(0.12, 0.62, t);
        double appear = PrayerVfxPrimitives.smooth(0.35, 0.78, t);
        double close = 1.0 - PrayerVfxPrimitives.smooth(0.72, 1.0, t);

        Vec3 path = destination.subtract(Vec3.ZERO);
        Vec3 forward = PrayerVfxPrimitives.safeNormalize(
                new Vec3(path.x, 0.0, path.z), new Vec3(0.0, 0.0, -1.0));
        Vec3[] basis = PrayerVfxPrimitives.basis(forward);
        Vec3 right = basis[0];

        Vec3 originCenter = new Vec3(0.0, 1.0, 0.0);
        Vec3 destCenter = destination.add(0.0, 1.0, 0.0);

        // ------------------------------------------------------------------
        // 1. PORTAL DE PARTIDA (Abre, suga as sombras e colapsa)
        // ------------------------------------------------------------------
        double originScale = Math.sin(Math.min(1.0, t / 0.55) * Math.PI);
        if (originScale > 0.01) {
            portal(m, out, originCenter, forward, 0.85 * originScale,
                    age * 0.05, fade * (float) originScale, 0.48f, 0.10f, 0.82f);

            // Selo no chão da origem
            PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, 0.04, 0), PrayerVfxPrimitives.UP,
                    0.95 * (1.0 - vanish * 0.6), 0.025, age * 0.06,
                    0.58f, 0.14f, 0.92f, fade * (float) (1.0 - vanish));
        }

        // ------------------------------------------------------------------
        // 2. RASGO DE SOMBRA ENTRE ORIGEM E DESTINO (Feixe + fitas espiraladas)
        // ------------------------------------------------------------------
        double streakEnv = Math.sin(Math.min(1.0, dash / 0.95) * Math.PI) * close;
        if (streakEnv > 0.01 && path.lengthSqr() > 0.04) {
            // Feixe central sombrio-violeta ligando os dois portais
            PrayerVfxGeometry.taperedTube(m, out, originCenter, destCenter,
                    0.12 * streakEnv, 0.04 * streakEnv,
                    0.18f, 0.02f, 0.32f, 0.65f * fade);
            PrayerVfxGeometry.taperedTube(m, out, originCenter, destCenter,
                    0.035 * streakEnv, 0.015 * streakEnv,
                    0.78f, 0.35f, 1.0f, 0.85f * fade);

            // 3 fitas de sombra espiralando ao longo do trajeto
            for (int ribbon = 0; ribbon < 3; ribbon++) {
                Vec3 prev = null;
                int steps = 14;
                for (int s = 0; s <= steps; s++) {
                    double u = s / (double) steps;
                    double env = Math.sin(u * Math.PI);
                    double twist = u * Math.PI * 3.0 + age * 0.25 + ribbon * (Math.PI * 2.0 / 3.0);
                    double rad = 0.28 * env * streakEnv;

                    Vec3 pt = originCenter.lerp(destCenter, u)
                            .add(right.scale(Math.cos(twist) * rad))
                            .add(0.0, Math.sin(twist) * rad, 0.0);

                    if (prev != null) {
                        PrayerVfxGeometry.tube(m, out, prev, pt, 0.022 * env * streakEnv,
                                0.62f, 0.16f, 0.95f, 0.82f * fade * (float) streakEnv);
                    }
                    prev = pt;
                }
            }
        }

        // ------------------------------------------------------------------
        // 3. PORTAL DE CHEGADA (Abre no destino e dissipa suavemente)
        // ------------------------------------------------------------------
        double destScale = appear * close;
        if (destScale > 0.01) {
            portal(m, out, destCenter, forward, 0.92 * destScale,
                    -age * 0.055, fade * (float) destScale, 0.62f, 0.16f, 0.96f);

            // Selo e onda de chegada no chão do destino
            PrayerVfxPrimitives.ringPlane(m, out, destination.add(0, 0.04, 0), PrayerVfxPrimitives.UP,
                    0.45 + appear * 0.85, 0.028 * close, -age * 0.05,
                    0.72f, 0.24f, 1.0f, fade * (float) (appear * close));
        }

        // ------------------------------------------------------------------
        // 4. ESTILHAÇOS DE SOMBRA ATRAVESSANDO O CAMINHO
        // ------------------------------------------------------------------
        for (int shard = 0; shard < 10; shard++) {
            double u = (shard / 10.0 + age * 0.065) % 1.0;
            double wobble = Math.sin(u * Math.PI * 4.0 + age * 0.18) * 0.24;
            Vec3 point = originCenter.lerp(destCenter, u)
                    .add(right.scale(Math.cos(age * 0.1 + shard) * wobble))
                    .add(0.0, Math.sin(age * 0.14 + shard * 2.1) * 0.30, 0.0);

            float shardAlpha = (float) (Math.sin(u * Math.PI) * close) * 0.82f * fade;
            if (shardAlpha > 0.01f) {
                PrayerVfxPrimitives.crystal(m, out, point, forward, 0.20, 0.045,
                        0.68f, 0.22f, 0.98f, shardAlpha);
            }
        }
    }

    private static void portal(Matrix4fc m, VertexConsumer out, Vec3 center, Vec3 facing,
                               double radius, double rotation,
                               float alpha, float r, float g, float b) {
        if (radius <= 0.01 || alpha <= 0.005f) return;

        Vec3[] basis = PrayerVfxPrimitives.basis(facing);
        Vec3 u = basis[0];
        Vec3 v = basis[1];

        // Núcleo escuro do portal
        PrayerVfxGeometry.ellipsoid(m, out, center,
                new Vec3(radius * 0.42, radius * 0.58, radius * 0.42),
                8, 12, 0.04f, 0.01f, 0.08f, 0.92f * alpha, rotation);

        // Anéis concêntricos orientados na direção do teleporte
        PrayerVfxPrimitives.ringPlane(m, out, center, facing, radius, 0.045, rotation,
                r, g, b, 0.90f * alpha);
        PrayerVfxPrimitives.ringPlane(m, out, center, facing, radius * 0.80, 0.020, -rotation * 1.6,
                Math.min(1f, r + 0.22f), Math.min(1f, g + 0.22f), Math.min(1f, b + 0.15f), alpha);
        PrayerVfxPrimitives.ringPlane(m, out, center, facing, radius * 0.56, 0.026, rotation * 1.3,
                r, g * 0.8f, b, 0.78f * alpha);

        // Runas radiais ao redor da borda do portal
        for (int rune = 0; rune < 10; rune++) {
            double angle = rune * (Math.PI * 2.0 / 10.0) + rotation;
            Vec3 radial = u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));
            Vec3 tangent = u.scale(-Math.sin(angle)).add(v.scale(Math.cos(angle)));

            Vec3 root = center.add(radial.scale(radius * 0.64));
            Vec3 outer = center.add(radial.scale(radius * 0.96));
            Vec3 crossA = root.subtract(tangent.scale(radius * 0.10));
            Vec3 crossB = root.add(tangent.scale(radius * 0.10));

            PrayerVfxGeometry.tube(m, out, root, outer, 0.016, r, g, b, 0.90f * alpha);
            PrayerVfxGeometry.tube(m, out, crossA, crossB, 0.015, 0.92f, 0.72f, 1.0f, 0.82f * alpha);
        }
    }
}
