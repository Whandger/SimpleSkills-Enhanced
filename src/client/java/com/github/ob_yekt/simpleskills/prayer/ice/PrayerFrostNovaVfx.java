package com.github.ob_yekt.simpleskills.prayer.ice;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerFrostNovaVfx {
    private PrayerFrostNovaVfx() {}

    // ---------- Linha do tempo (frações da duração) ----------
    // Quando o gelo começa a quebrar (ajuste aqui para quebrar mais cedo/tarde).
    private static final double SHATTER_START = 0.66;
    // Atraso extra para o gelo de fora quebrar depois do de dentro (reação em cadeia de dentro pra fora).
    private static final double SHATTER_STAGGER = 0.08;
    // Quantos ticks o gelo treme/clareia antes de quebrar.
    private static final double CRACKLE_TICKS = 3.0;
    // Quantos ticks dura o impacto (anel + lascas) quando um cristal crava no chão.
    private static final double LANDING_TICKS = 4.0;

    // ---------- Layout ----------
    private static final double GROUND_Y = 0.0;
    private static final double MAX_RADIUS = 4.6;
    // Cristais arremessados que voam em arco e cravam no chão ao redor.
    private static final int SHARD_COUNT = 22;
    // Espinhos que crescem do chão em anéis (contagem / raio de cada anel).
    private static final int[] SPIKE_RING_COUNT = {6, 9, 12};
    private static final double[] SPIKE_RING_RADIUS = {1.0, 1.9, 2.8};
    // Rachaduras no chão que se espalham a partir do conjurador.
    private static final int CRACK_COUNT = 10;
    // Estilhaços pequenos extras na quebra.
    private static final int DUST_COUNT = 26;
    // Quantos cacos nascem de cada cristal.
    private static final int SHARD_PIECES = 2;
    private static final int SPIKE_PIECES = 3;

    private static final double TAU = Math.PI * 2.0;
    private static final double GOLDEN_ANGLE = 2.399963229728653;

    // 32. FROST_NOVA — o gelo explode do conjurador: cristais voam para todos os lados e cravam no chão,
    // espinhos brotam em ondas, o chão racha e congela; depois tudo treme e se estilhaça de dentro pra fora.
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        Ctx c = new Ctx(m, out, age, duration);
        drawGround(c);
        drawSpikes(c);
        drawShards(c);
        drawDust(c);
    }

    // Contexto compartilhado do frame: tempos de quebra, fade e alpha do chão.
    private static final class Ctx {
        final Matrix4fc m;
        final VertexConsumer out;
        final float age;
        final int duration;
        final float fade;
        final double shatterStart;
        final double breakSpan;
        final double stagger;
        final double crackle;
        final float groundAlpha;

        Ctx(Matrix4fc m, VertexConsumer out, float age, int duration) {
            this.m = m;
            this.out = out;
            this.age = age;
            this.duration = duration;
            this.fade = PrayerVfxPrimitives.fade(age, duration, 2, 4);

            double start = duration * SHATTER_START;
            double end = duration - 1.0;
            this.shatterStart = start;
            this.stagger = duration * SHATTER_STAGGER;
            // O último cristal (com atraso máximo) ainda precisa terminar de quebrar até o fim.
            this.breakSpan = Math.max(1.0, end - start - this.stagger);
            // Pré-rachadura: o gelo treme e clareia pouco antes de quebrar.
            this.crackle = PrayerVfxPrimitives.smooth(start - CRACKLE_TICKS, start, age);

            double global = clamp01((age - start) / Math.max(1.0, end - start));
            this.groundAlpha = (float) (this.fade * (1.0 - 0.85 * global));
        }

        // Progresso da quebra de um cristal: 0 (inteiro) a 1 (tudo virou pó).
        double breakProgress(double delay) {
            return clamp01((age - shatterStart - delay) / breakSpan);
        }
    }

    // ---------- Chão: disco de geada, anéis de choque e rachaduras ----------
    private static void drawGround(Ctx c) {
        double w1 = PrayerVfxPrimitives.smooth(1.5, c.duration * 0.38, c.age);
        double w2 = PrayerVfxPrimitives.smooth(4.0, c.duration * 0.48, c.age);
        float ga = c.groundAlpha;
        Vec3 floor = new Vec3(0, GROUND_Y + 0.06, 0);

        // Disco de geada: faixas largas e fracas que ficam no chão depois da onda passar.
        double discR = 0.3 + w1 * MAX_RADIUS;
        for (int k = 1; k <= 3; k++) {
            double r = discR * k / 3.0;
            if (r < 0.5) continue;
            PrayerVfxPrimitives.ringPlane(c.m, c.out, floor, PrayerVfxPrimitives.UP, r, 0.16, c.age * 0.01 * k,
                    0.5f, 0.85f, 1.0f, 0.11f * ga);
        }

        // Anel principal de choque (rápido) e um segundo anel mais claro logo atrás.
        PrayerVfxPrimitives.ringPlane(c.m, c.out, floor, PrayerVfxPrimitives.UP, 0.25 + w1 * MAX_RADIUS, 0.045,
                c.age * 0.025, 0.46f, 0.84f, 1.0f, 0.85f * ga * (float) (1.0 - w1 * 0.5));
        if (w2 > 0.0) {
            PrayerVfxPrimitives.ringPlane(c.m, c.out, floor, PrayerVfxPrimitives.UP, 0.2 + w2 * MAX_RADIUS * 0.8, 0.03,
                    -c.age * 0.03, 0.85f, 0.96f, 1.0f, 0.6f * ga * (float) (1.0 - w2 * 0.6));
        }

        // Rachaduras radiais: agulhas finas deitadas no chão que se alongam com a onda.
        for (int i = 0; i < CRACK_COUNT; i++) {
            int id = 300 + i * 7;
            double angle = i * TAU / CRACK_COUNT + (rnd(id) - 0.5) * 0.5;
            double reach = 1.8 + rnd(id + 1) * 2.0;
            double g = PrayerVfxPrimitives.smooth(1.5 + rnd(id + 2) * 2.0, c.duration * 0.38, c.age);
            if (g <= 0.0) continue;

            double len = reach * g;
            Vec3 dir = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            Vec3 mid = new Vec3(dir.x * (0.4 + len * 0.5), GROUND_Y + 0.05, dir.z * (0.4 + len * 0.5));
            PrayerVfxPrimitives.crystal(c.m, c.out, mid, dir, len, 0.045, 0.75f, 0.93f, 1.0f, 0.55f * ga);
        }
    }

    // ---------- Espinhos: brotam do chão em anéis e crescem com "overshoot" ----------
    private static void drawSpikes(Ctx c) {
        for (int ring = 0; ring < SPIKE_RING_COUNT.length; ring++) {
            int n = SPIKE_RING_COUNT[ring];
            for (int i = 0; i < n; i++) {
                int id = 1000 + ring * 100 + i;
                double angle = i * TAU / n + ring * 0.55 + (rnd(id) - 0.5) * 0.35;
                double radius = SPIKE_RING_RADIUS[ring] + (rnd(id + 1) - 0.5) * 0.5;

                // Cada espinho nasce quando a onda de choque passa por ele.
                double arrive = 2.0 + radius / MAX_RADIUS * c.duration * 0.16;
                double grow = easeOutBack(clamp01((c.age - arrive) / Math.max(2.0, c.duration * 0.10)));
                if (grow <= 0.0) continue;

                double fullLength = (1.0 + rnd(id + 2) * 0.9) * (1.2 - ring * 0.18);
                double length = fullLength * grow;
                double width = fullLength * 0.17 * grow;

                Vec3 radial = new Vec3(Math.cos(angle), 0, Math.sin(angle));
                double lean = 0.25 + rnd(id + 3) * 0.45;
                Vec3 axis = new Vec3(radial.x * lean, 1.0, radial.z * lean).normalize();

                // Base cravada no chão: ~12% do cristal fica enterrado.
                Vec3 base = new Vec3(radial.x * radius, GROUND_Y, radial.z * radius);
                Vec3 center = base.add(axis.scale(length * 0.38));

                iceCrystal(c, id, center, axis, length, width, radius / MAX_RADIUS * c.stagger,
                        SPIKE_PIECES, Vec3.ZERO, 0.85f * c.fade);
            }
        }
    }

    // ---------- Cristais arremessados: voam em arco para todos os lados e cravam no chão ----------
    private static void drawShards(Ctx c) {
        for (int i = 0; i < SHARD_COUNT; i++) {
            int id = 2000 + i * 17;

            // Ângulo áureo + raio em raiz quadrada = cobertura uniforme do disco em volta do conjurador.
            double angle = i * GOLDEN_ANGLE + (rnd(id) - 0.5) * 0.5;
            double landR = (1.2 + Math.sqrt((i + 0.5) / SHARD_COUNT) * 3.0) * (0.92 + rnd(id + 1) * 0.16);
            double length = 0.75 + rnd(id + 2) * 0.65;
            double width = length * 0.2;
            // Ângulo da cravada (graus abaixo da horizontal).
            double pitch = Math.toRadians(48.0 + rnd(id + 3) * 30.0);

            double start = 2.0 + rnd(id + 4) * c.duration * 0.05;
            double flight = Math.max(2.0, c.duration * (0.12 + landR * 0.022));
            double apex = 0.5 + landR * 0.25 + rnd(id + 5) * 0.6;
            double u = (c.age - start) / flight;
            if (u <= 0.0) continue;

            Vec3 radial = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            // Eixo apontando para fora e para baixo: a ponta é o que entra na terra.
            Vec3 landAxis = new Vec3(radial.x * Math.cos(pitch), -Math.sin(pitch), radial.z * Math.cos(pitch));
            Vec3 tip = new Vec3(radial.x * landR, GROUND_Y - length * 0.15, radial.z * landR);
            Vec3 landCenter = tip.add(landAxis.scale(-length * 0.5));
            Vec3 launch = new Vec3(radial.x * 0.35, 1.0 + rnd(id + 6) * 0.4, radial.z * 0.35);

            float alpha = 0.9f * c.fade;

            // --- Em voo ---
            if (u < 1.0) {
                Vec3 pos = arc(launch, landCenter, apex, u);
                Vec3 vel = sub(arc(launch, landCenter, apex, u + 0.03), arc(launch, landCenter, apex, u - 0.03)).normalize();
                // No final do voo o cristal gira suavemente para o ângulo de cravada.
                double w = PrayerVfxPrimitives.smooth(0.7, 1.0, u);
                Vec3 axis = vel.scale(1.0 - w).add(landAxis.scale(w)).normalize();
                double grow = Math.min(1.0, u * 6.0 + 0.25);

                // Rastro de gelo: dois "fantasmas" menores e mais fracos atrás do cristal.
                for (int k = 1; k <= 2; k++) {
                    double uk = u - k * 0.07;
                    if (uk <= 0.0) break;
                    double sz = grow * (1.0 - k * 0.28);
                    PrayerVfxPrimitives.crystal(c.m, c.out, arc(launch, landCenter, apex, uk), axis,
                            length * sz, width * sz, 0.6f, 0.88f, 1.0f, alpha * (k == 1 ? 0.5f : 0.25f));
                }
                PrayerVfxPrimitives.crystal(c.m, c.out, pos, axis, length * grow, width * grow,
                        0.45f, 0.82f, 1.0f, alpha);
                continue;
            }

            // --- Cravado no chão ---
            double lt = c.age - (start + flight);
            // Balanço amortecido logo após o impacto (a ponta continua fixa na terra).
            double wob = Math.exp(-lt * 0.9) * Math.sin(lt * 2.6) * 0.30;
            Vec3 tangent = new Vec3(-radial.z, 0, radial.x);
            Vec3 axis = landAxis.add(tangent.scale(wob)).normalize();
            Vec3 center = tip.add(axis.scale(-length * 0.5));

            drawLanding(c, id, tip, lt, alpha);
            iceCrystal(c, id, center, axis, length, width, landR / MAX_RADIUS * c.stagger,
                    SHARD_PIECES, Vec3.ZERO, alpha);
        }
    }

    // Impacto de um cristal no chão: anel de geada + lascas que saltam.
    private static void drawLanding(Ctx c, int id, Vec3 tip, double lt, float alpha) {
        double p = lt / LANDING_TICKS;
        if (p >= 1.0) return;
        double ease = easeOut(p);

        PrayerVfxPrimitives.ringPlane(c.m, c.out, new Vec3(tip.x, GROUND_Y + 0.06, tip.z), PrayerVfxPrimitives.UP,
                0.12 + ease * 0.6, 0.035, 0.0, 0.8f, 0.95f, 1.0f, 0.7f * alpha * (float) (1.0 - p));

        for (int j = 0; j < 3; j++) {
            int s = id * 3 + j * 7;
            double ang = rnd(s) * TAU;
            double reach = (0.3 + rnd(s + 1) * 0.5) * ease;
            double height = 4.0 * p * (1.0 - p) * (0.5 + rnd(s + 2) * 0.7);
            Vec3 pos = new Vec3(tip.x + Math.cos(ang) * reach, GROUND_Y + 0.1 + height, tip.z + Math.sin(ang) * reach);

            double spin = lt * (3.0 + rnd(s + 3) * 4.0);
            Vec3 axis = new Vec3(Math.sin(spin + s), Math.cos(spin * 1.2 + s), Math.sin(spin * 0.7 + s * 0.3)).normalize();
            double size = (0.12 + rnd(s + 4) * 0.08) * (1.0 - p * 0.6);
            PrayerVfxPrimitives.crystal(c.m, c.out, pos, axis, size * 2.0, size * 0.45,
                    0.75f, 0.93f, 1.0f, alpha * (float) (1.0 - p * 0.5));
        }
    }

    // ---------- Estilhaços pequenos: sobem do chão na hora da quebra ----------
    private static void drawDust(Ctx c) {
        for (int k = 0; k < DUST_COUNT; k++) {
            int s = 700 + k * 13;
            double angle = rnd(s) * TAU;
            double radius = 0.4 + Math.sqrt(rnd(s + 1)) * (MAX_RADIUS - 0.8);

            double ts = c.breakProgress(radius / MAX_RADIUS * c.stagger);
            if (ts <= 0.0) continue;
            double shrink = Math.pow(1.0 - ts, 0.8);
            if (shrink < 0.04) continue;

            double ease = 1.0 - (1.0 - ts) * (1.0 - ts);
            double fall = 1.2 * ts * ts;
            Vec3 origin = new Vec3(Math.cos(angle) * radius, GROUND_Y + 0.1, Math.sin(angle) * radius);
            Vec3 dir = new Vec3(
                    Math.cos(angle) * 0.5 + (rnd(s + 5) - 0.5) * 0.6,
                    0.7 + rnd(s + 2) * 1.0,
                    Math.sin(angle) * 0.5 + (rnd(s + 6) - 0.5) * 0.6).normalize();
            double speed = 0.8 + rnd(s + 3) * 1.6;

            Vec3 raw = origin.add(dir.scale(speed * ease)).add(0, -fall, 0);
            Vec3 pos = new Vec3(raw.x, Math.max(GROUND_Y + 0.05, raw.y), raw.z);

            double spin = ts * (6.0 + rnd(s + 4) * 8.0);
            Vec3 axis = new Vec3(Math.sin(spin + s), Math.cos(spin * 1.2 + s), Math.sin(spin * 0.7 + s * 0.3)).normalize();
            double size = (0.16 + rnd(s + 7) * 0.18) * shrink;

            PrayerVfxPrimitives.crystal(c.m, c.out, pos, axis, size * 2.0, size * 0.45,
                    0.72f, 0.93f, 1.0f, 0.9f * c.fade * (float) (1.0 - 0.5 * ts));
        }
    }

    // ---------- Cristal de gelo com tom próprio, brilho, pré-rachadura e quebra ----------
    private static void iceCrystal(Ctx c, int id, Vec3 center, Vec3 axis, double length, double width,
                                   double breakDelay, int pieces, Vec3 origin, float alpha) {
        double ts = c.breakProgress(breakDelay);

        // Cada cristal tem um tom um pouco diferente + um brilho que pulsa de leve (facetas).
        double variety = rnd(id * 5 + 3) * 0.28;
        double shimmer = 0.10 * (0.5 + 0.5 * Math.sin(c.age * 0.45 + id * 1.7));
        // Flash branco: sobe na rachadura e some rápido no início da quebra.
        double flash = ts > 0.0 ? Math.max(0.0, 1.0 - ts * 5.0) : c.crackle;
        double light = Math.min(1.0, flash + variety + (ts > 0.0 ? 0.0 : shimmer));

        float cr = tint(0.33f, light);
        float cg = tint(0.76f, light);
        float cb = tint(1.0f, light);

        if (ts <= 0.0) {
            Vec3 pos = center.add(shake(c.age, id, c.crackle * 0.03));
            PrayerVfxPrimitives.crystal(c.m, c.out, pos, axis, length, width, cr, cg, cb, alpha);
            return;
        }

        // O cristal original encolhe rápido no começo da quebra, enquanto os cacos já voam.
        double mainScale = 1.0 - PrayerVfxPrimitives.smooth(0.0, 0.2, ts);
        if (mainScale > 0.02) {
            PrayerVfxPrimitives.crystal(c.m, c.out, center, axis, length * mainScale, width * mainScale,
                    cr, cg, cb, alpha);
        }
        shatter(id, c, center, axis, length, width, pieces, ts, origin, cr, cg, cb, alpha);
    }

    // Quebra um cristal em cacos que voam para fora, giram, caem, deslizam no chão e encolhem até sumir.
    private static void shatter(int id, Ctx c, Vec3 center, Vec3 axis, double length, double width,
                                int pieces, double ts, Vec3 origin, float r, float g, float b, float a) {
        double shrink = Math.pow(1.0 - ts, 0.8);
        if (shrink < 0.04) return;

        double ease = 1.0 - (1.0 - ts) * (1.0 - ts); // sai rápido e desacelera
        double fall = 2.0 * ts * ts;                 // gravidade
        float alpha = (float) (a * (1.0 - 0.5 * ts));

        // Direção horizontal "para fora" a partir do centro do Nova.
        Vec3 flat = new Vec3(center.x - origin.x, 0, center.z - origin.z);
        flat = flat.lengthSqr() > 1.0E-4 ? flat.normalize() : Vec3.ZERO;

        for (int p = 0; p < pieces; p++) {
            int s = id * 37 + p * 11;

            Vec3 dir = new Vec3(
                    flat.x * 0.9 + (rnd(s) - 0.5) * 1.4,
                    0.3 + rnd(s + 1) * 1.0,
                    flat.z * 0.9 + (rnd(s + 2) - 0.5) * 1.4).normalize();
            double speed = 1.0 + rnd(s + 3) * 1.9;

            // Cada caco nasce em um ponto diferente ao longo do cristal original.
            double along = pieces > 1 ? (p / (double) (pieces - 1) - 0.5) : 0.0;
            Vec3 start = center.add(axis.scale(along * length * 0.7));
            Vec3 raw = start.add(dir.scale(speed * ease)).add(0, -fall, 0);
            // Os cacos pousam no chão em vez de atravessá-lo.
            Vec3 pos = new Vec3(raw.x, Math.max(GROUND_Y + 0.05, raw.y), raw.z);

            // Giro do caco em pleno ar.
            double spin = ts * (4.0 + rnd(s + 4) * 6.0);
            Vec3 pieceAxis = new Vec3(
                    axis.x + Math.sin(spin + s) * 0.9,
                    axis.y + Math.cos(spin * 1.3 + s) * 0.9,
                    axis.z + Math.sin(spin * 0.8 + s * 0.5) * 0.9).normalize();

            double pieceLength = length * (0.30 + rnd(s + 5) * 0.25) * shrink;
            double pieceWidth = width * (0.55 + rnd(s + 6) * 0.30) * shrink;

            PrayerVfxPrimitives.crystal(c.m, c.out, pos, pieceAxis, pieceLength, pieceWidth, r, g, b, alpha);
        }
    }

    // ---------- Utilidades ----------

    // Trajetória em arco de a até b (b é alcançado exatamente em u = 1). Horizontal com leve ease-out.
    private static Vec3 arc(Vec3 a, Vec3 b, double apex, double u) {
        double v = clamp01(u);
        double h = v * (1.25 - 0.25 * v);
        return new Vec3(
                a.x + (b.x - a.x) * h,
                a.y + (b.y - a.y) * v + 4.0 * apex * v * (1.0 - v),
                a.z + (b.z - a.z) * h);
    }

    private static Vec3 sub(Vec3 a, Vec3 b) {
        return new Vec3(a.x - b.x, a.y - b.y, a.z - b.z);
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

    private static double clamp01(double v) {
        return Math.min(1.0, Math.max(0.0, v));
    }

    private static double easeOut(double t) {
        double x = 1.0 - clamp01(t);
        return 1.0 - x * x * x;
    }

    // Cresce passando um pouco do tamanho final e assenta (efeito "pop" dos espinhos).
    private static double easeOutBack(double t) {
        double c1 = 1.70158;
        double c3 = c1 + 1.0;
        double x = t - 1.0;
        return 1.0 + c3 * x * x * x + c1 * x * x;
    }

    // Número pseudo-aleatório determinístico (0..1): o mesmo cristal age igual em todo frame, sem tremer.
    private static double rnd(int seed) {
        double x = Math.sin(seed * 12.9898 + 78.233) * 43758.5453;
        return x - Math.floor(x);
    }
}
