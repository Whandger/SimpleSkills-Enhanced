package com.github.ob_yekt.simpleskills.prayer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

/** Camada extra de impacto visual aplicada a TODAS as habilidades de Prayer. */
final class PrayerVfxFlair {
    private PrayerVfxFlair() {}

    static void draw(Matrix4fc m, VertexConsumer o, PrayerAbility ab, Vec3 dest, float age, int dur) {
        float[][] pal = palette(ab);
        float[] c = pal[0], k = pal[1];
        float fade = (float) Math.min(smooth(0, 3, age), 1 - smooth(dur - 8, dur, age));
        if (fade <= 0.01f) return;
        if (isProjectile(ab)) projectile(m, o, ab, dest, age, dur, c, k, fade);
        else area(m, o, ab, age, dur, c, k, fade);
    }

    // ---------------------------------------------------------------- projétil
    private static void projectile(Matrix4fc m, VertexConsumer o, PrayerAbility ab, Vec3 dest,
                                   float age, int dur, float[] c, float[] k, float fade) {
        boolean orb = ab == PrayerAbility.LIGHT_ORB;
        Vec3 from = orb ? new Vec3(0, 1.18, 0) : Vec3.ZERO;
        Vec3 to = orb ? dest.add(0, 1.18, 0) : dest;
        Vec3 path = to.subtract(from);
        double len = path.length();
        Vec3 dir = len < 1.0E-4 ? new Vec3(0, 0, -1) : path.scale(1 / len);
        double arrive = dur * 0.58;
        double travel = smooth(2, arrive, age);
        Vec3 head = from.lerp(to, travel);
        double scale = 0.8 + ab.requiredLevel() * 0.012;
        float trailFade = (float) (1 - smooth(arrive, arrive + 4, age));

        // carga na mão: motes convergem
        double charge = 1 - smooth(0, 7, age);
        for (int i = 0; i < 8 && charge > 0.02; i++) {
            double a = i * Math.PI / 4 + age * 0.4, r = 0.65 * charge;
            Vec3 p = from.add(Math.cos(a) * r, Math.sin(a * 1.3) * r * 0.6, Math.sin(a) * r);
            mote(m, o, p, 0.035, c, k, fade * (float) (1 - charge * 0.5));
        }

        // brilho suave em volta da cabeça
        if (trailFade > 0.01f) {
            PrayerVfxGeometry.ellipsoid(m, o, head, new Vec3(0.46, 0.46, 0.46), 6, 10,
                    c[0], c[1], c[2], 0.10f * fade * trailFade, age * 0.1);
            PrayerVfxGeometry.ellipsoid(m, o, head, new Vec3(0.26, 0.26, 0.26), 6, 10,
                    c[0], c[1], c[2], 0.20f * fade * trailFade, -age * 0.15);
            PrayerVfxGeometry.ellipsoid(m, o, head, new Vec3(0.11, 0.11, 0.11), 5, 8,
                    k[0], k[1], k[2], 0.90f * fade * trailFade, age * 0.2);
        }

        // rastro de faíscas
        double flown = travel * len;
        for (int i = 0; i < 14; i++) {
            double u = i / 13.0, back = u * 1.2;
            if (back > flown) break;
            double wob = Math.sin(age * 0.5 + i * 1.7) * 0.07 * u;
            Vec3 p = head.subtract(dir.scale(back)).add(wob, Math.cos(age * 0.4 + i) * 0.07 * u, -wob);
            mote(m, o, p, 0.065 * (1 - u) + 0.012, c, k, fade * trailFade * (float) (1 - u));
        }

        // impacto
        if (age >= arrive - 1) {
            double ia = clamp((age - arrive + 1) / Math.max(1, dur - arrive));
            double e = easeOut(ia);
            float a = (float) ((1 - ia) * fade);
            Vec3 g = to.add(0, 0.04, 0);
            disc(m, o, g, (0.3 + e * 1.7) * scale, 28, c, 0.40f * a, 0.0f);
            ring(m, o, g.add(0, 0.02, 0), (0.2 + e * 2.0) * scale, 0.035 * (1 - e * 0.6), k, 0.85f * a);
            double flash = 1 - clamp(ia * 5);
            if (flash > 0) disc(m, o, to, 0.7 * scale, 20, k, 0.75f * (float) flash * fade, 0.0f);
            for (int i = 0; i < 10; i++) {
                double ang = i * Math.PI / 5 + 0.3;
                Vec3 d = new Vec3(Math.cos(ang), 0, Math.sin(ang));
                PrayerVfxGeometry.taperedTube(m, o, g.add(d.scale(0.15 * scale)),
                        g.add(d.scale((0.4 + e * (1.0 + (i % 3) * 0.35)) * scale)).add(0, 0.05, 0),
                        0.035, 0.002, k[0], k[1], k[2], 0.8f * a);
            }
            for (int i = 0; i < 12; i++) {
                double ang = i * 2.399963, dist = e * (0.8 + frac(i * 0.37) * 1.4) * scale;
                double h = Math.sin(Math.PI * e) * (0.4 + frac(i * 0.71) * 0.8);
                mote(m, o, to.add(Math.cos(ang) * dist, h, Math.sin(ang) * dist),
                        0.05 * (1 - e * 0.5), c, k, a);
            }
        }
    }

    // ---------------------------------------------------------------- área / céu
    private static void area(Matrix4fc m, VertexConsumer o, PrayerAbility ab, float age, int dur,
                             float[] c, float[] k, float fade) {
        double big = Math.min(4.5, 1.6 + ab.requiredLevel() * 0.03);
        double grow = easeOut(clamp(age / 10.0));
        double R = big * grow;
        Vec3 g = new Vec3(0, 0.03, 0);

        // flash inicial
        double fl = 1 - clamp(age / 5.0);
        if (fl > 0) disc(m, o, g, big * 1.5 * easeOut(age / 5.0), 28, k, 0.55f * (float) fl, 0.0f);

        // piso preenchido (gradiente centro → borda)
        double breathe = 1 + 0.04 * Math.sin(age * 0.2);
        disc(m, o, g, R * breathe, 36, c, 0.24f * fade, 0.03f * fade);
        disc(m, o, g.add(0, 0.01, 0), R * 0.45, 24, k, 0.18f * fade, 0.0f);

        // onda de choque expansiva
        double e = easeOut(clamp(age / 14.0));
        if (e < 1.0) ring(m, o, g.add(0, 0.03, 0), big * (0.3 + 1.4 * e), 0.06 * (1 - e),
                k, 0.9f * (float) (1 - e) * fade);

        // cortina de luz nas bordas
        int seg = 40;
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
            double h = (0.8 + big * 0.35) * (0.8 + 0.2 * Math.sin(age * 0.2 + i * 0.7)) * grow;
            Vec3 b0 = new Vec3(Math.cos(a0) * R * 0.97, 0.03, Math.sin(a0) * R * 0.97);
            Vec3 b1 = new Vec3(Math.cos(a1) * R * 0.97, 0.03, Math.sin(a1) * R * 0.97);
            gquad(m, o, b0, b1, b1.add(0, h, 0), b0.add(0, h, 0), c, 0.38f * fade, 0.0f);
        }

        // partículas por escola
        PrayerSpecialization sp = ab.specialization();
        for (int i = 0; i < 28; i++) {
            double ph = (age * 0.025 + frac(i * 0.618)) % 1.0;
            double ang = i * 2.399963 + age * (sp == PrayerSpecialization.UNHOLY ? -0.04 : 0.03);
            double rad = R * (0.25 + 0.75 * frac(i * 0.37));
            double hmax = 1.2 + 2.2 * frac(i * 0.71);
            double y;
            if (ab.element() == PrayerElement.ICE) { y = (1 - ph) * hmax; rad += Math.sin(age * 0.1 + i) * 0.15; }
            else y = ph * hmax;
            if (sp == PrayerSpecialization.UNHOLY) rad *= (1 - 0.5 * ph);
            if (ab.element() == PrayerElement.ELECTRICITY) { ang += Math.sin(age * 1.7 + i) * 0.12; y += Math.sin(age * 2.3 + i) * 0.1; }
            if (ab.element() == PrayerElement.FIRE) rad += Math.sin(age * 0.45 + i) * 0.1;
            float a = fade * (float) Math.sin(Math.PI * ph);
            mote(m, o, new Vec3(Math.cos(ang) * rad, y + 0.05, Math.sin(ang) * rad),
                    0.05 * (1 - ph * 0.6) + 0.02, c, k, a);
        }

        // pilar de luz para feitiços grandes
        if (ab.requiredLevel() >= 50) {
            double H = 6 + ab.requiredLevel() * 0.1, w = 0.3 + 0.05 * Math.sin(age * 0.4);
            double rot = age * 0.05;
            for (int p = 0; p < 2; p++) {
                Vec3 s = new Vec3(Math.cos(rot + p * Math.PI / 2), 0, Math.sin(rot + p * Math.PI / 2)).scale(w);
                gquad(m, o, g.subtract(s), g.add(s), g.add(s).add(0, H, 0), g.subtract(s).add(0, H, 0),
                        c, 0.30f * fade, 0.0f);
                Vec3 s2 = s.scale(0.28);
                gquad(m, o, g.subtract(s2), g.add(s2), g.add(s2).add(0, H, 0), g.subtract(s2).add(0, H, 0),
                        k, 0.65f * fade, 0.0f);
            }
        }
    }

    // ---------------------------------------------------------------- primitivas
    private static void mote(Matrix4fc m, VertexConsumer o, Vec3 c0, double size,
                             float[] c, float[] k, float alpha) {
        if (alpha <= 0.01f || size <= 0) return;
        diamond(m, o, c0, size * 2.2, c, alpha * 0.25f);
        diamond(m, o, c0, size, k, alpha);
    }

    private static void diamond(Matrix4fc m, VertexConsumer o, Vec3 c0, double s, float[] col, float a) {
        Vec3 top = c0.add(0, s * 1.5, 0), bot = c0.add(0, -s * 1.5, 0);
        Vec3[] eq = {c0.add(s, 0, 0), c0.add(0, 0, s), c0.add(-s, 0, 0), c0.add(0, 0, -s)};
        for (int i = 0; i < 4; i++) {
            PrayerVfxGeometry.triangle(m, o, top, eq[i], eq[(i + 1) % 4], col[0], col[1], col[2], a);
            PrayerVfxGeometry.triangle(m, o, bot, eq[(i + 1) % 4], eq[i], col[0] * 0.8f, col[1] * 0.8f, col[2] * 0.8f, a);
        }
    }

    private static void disc(Matrix4fc m, VertexConsumer o, Vec3 center, double radius, int seg,
                             float[] col, float aCenter, float aEdge) {
        if (radius <= 0) return;
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
            Vec3 p0 = center.add(Math.cos(a0) * radius, 0, Math.sin(a0) * radius);
            Vec3 p1 = center.add(Math.cos(a1) * radius, 0, Math.sin(a1) * radius);
            vtx(m, o, center, col, aCenter);
            vtx(m, o, p0, col, aEdge);
            vtx(m, o, p1, col, aEdge);
            vtx(m, o, p1, col, aEdge);
        }
    }

    /** a,b = base; c,d = topo. Alpha interpolado da base para o topo. */
    private static void gquad(Matrix4fc m, VertexConsumer o, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                              float[] col, float aBottom, float aTop) {
        vtx(m, o, a, col, aBottom);
        vtx(m, o, b, col, aBottom);
        vtx(m, o, c, col, aTop);
        vtx(m, o, d, col, aTop);
    }

    private static void ring(Matrix4fc m, VertexConsumer o, Vec3 center, double radius, double width,
                             float[] col, float alpha) {
        if (radius <= 0 || alpha <= 0.01f) return;
        int seg = Math.max(20, (int) (radius * 12));
        Vec3 prev = center.add(radius, 0, 0);
        for (int i = 1; i <= seg; i++) {
            double a = Math.PI * 2 * i / seg;
            Vec3 next = center.add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            PrayerVfxGeometry.taperedTube(m, o, prev, next, width, width, col[0], col[1], col[2], alpha);
            prev = next;
        }
    }

    private static void vtx(Matrix4fc m, VertexConsumer o, Vec3 p, float[] col, float a) {
        o.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(col[0], col[1], col[2], a);
    }

    // ---------------------------------------------------------------- paleta
    /** {cor base, cor do núcleo}. O núcleo sempre é claro para não sumir no blending. */
    private static float[][] palette(PrayerAbility a) {
        if (a.specialization() == PrayerSpecialization.DIVINE)
            return new float[][] {{1.0f, 0.78f, 0.28f}, {1.0f, 0.97f, 0.82f}};
        if (a.specialization() == PrayerSpecialization.UNHOLY)
            return new float[][] {{0.58f, 0.10f, 0.85f}, {1.0f, 0.55f, 1.0f}};
        return switch (a.element()) {
            case FIRE -> new float[][] {{1.0f, 0.34f, 0.04f}, {1.0f, 0.90f, 0.45f}};
            case ICE -> new float[][] {{0.35f, 0.78f, 1.0f}, {0.92f, 1.0f, 1.0f}};
            case ELECTRICITY -> new float[][] {{0.33f, 0.62f, 1.0f}, {0.95f, 1.0f, 1.0f}};
        };
    }

    private static boolean isProjectile(PrayerAbility a) {
        return switch (a) {
            case LIGHT_ORB, HOLY_BOLT, SHADOW_BOLT, LIFE_DRAIN, CURSE_OF_WEAKNESS, CORRUPTION, SPARK,
                 CHAIN_LIGHTNING, FIREBALL, ICE_SHARD, ICE_PRISON, LIGHTNING_BOLT -> true;
            default -> false;
        };
    }

    private static double easeOut(double t) { double u = 1 - clamp(t); return 1 - u * u * u; }
    private static double frac(double x) { return x - Math.floor(x); }
    private static double clamp(double v) { return Math.max(0, Math.min(1, v)); }
    private static double smooth(double s, double e, double v) {
        double t = clamp((v - s) / Math.max(1.0E-5, e - s));
        return t * t * (3 - 2 * t);
    }
}
