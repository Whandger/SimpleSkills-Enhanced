package com.github.ob_yekt.simpleskills.prayer.divine;

import com.github.ob_yekt.simpleskills.prayer.PrayerImpactFeedback;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4fc;

public final class PrayerDivineSmiteVfx {
    private PrayerDivineSmiteVfx() {}

    // Assinatura padrão (detecta o chão automaticamente a partir da matriz)
    public static void draw(Matrix4fc m, VertexConsumer out, float age, int duration) {
        Vec3 worldOrigin = extractWorldOrigin(m);
        double groundOffsetY = resolveGroundOffsetY(worldOrigin);
        drawInternal(m, out, worldOrigin, groundOffsetY, age, duration);
    }

    // Sobrecarga opcional caso você já passe a posição exata do alvo no mundo
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 targetWorldPos, float age, int duration) {
        double groundOffsetY = resolveGroundOffsetY(targetWorldPos);
        drawInternal(m, out, targetWorldPos, groundOffsetY, age, duration);
    }

    private static void drawInternal(Matrix4fc m, VertexConsumer out, Vec3 worldOrigin,
                                     double groundY, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 2, 6);
        double aoeRadius = 2.75;
        double skyY = groundY + 18.0;

        // Tempo de antecipação (telégrafo): o círculo aparece e carrega antes do raio cair
        double strikeTick = Math.max(6.5, duration * 0.34);
        double telegraphProgress = PrayerVfxPrimitives.smooth(0.0, strikeTick, age);

        // O raio cai muito rápido assim que o telégrafo termina
        double boltDrop = PrayerVfxPrimitives.smooth(strikeTick, strikeTick + 1.6, age);
        double timeSinceStrike = age - (strikeTick + 1.2);

        // Dispara tremor de tela + flash de impacto uma única vez quando o raio toca o chão
        if (timeSinceStrike >= 0.0 && timeSinceStrike <= 2.0) {
            Vec3 groundWorldPos = worldOrigin != null ? worldOrigin.add(0, groundY, 0) : null;
            PrayerImpactFeedback.trigger(groundWorldPos, 2.7f, 420L, 95L);
        }

        // 1. SELO DE ÁREA NO CHÃO (Aparece primeiro para marcar a área do dano)
        drawGroundTelegraphCircle(m, out, groundY, aoeRadius, telegraphProgress, boltDrop, age, fade);

        // 2. PORTAL CELESTE NO CÉU (Abre enquanto o círculo carrega no chão)
        drawSkyPortal(m, out, skyY, telegraphProgress, boltDrop, age, fade);

        // 3. FIO GUIA DE PRÉ-DESCARGA (Nos últimos ticks antes de cair, avisa que o raio vai descer)
        if (age >= strikeTick - 2.5 && boltDrop < 0.35) {
            double leaderAlpha = PrayerVfxPrimitives.smooth(strikeTick - 2.5, strikeTick, age) * (1.0 - boltDrop);
            PrayerVfxGeometry.taperedTube(m, out,
                    new Vec3(0, skyY, 0), new Vec3(0, groundY + 0.05, 0),
                    0.04, 0.015, 1.0f, 0.96f, 0.72f, (float) (0.75 * leaderAlpha * fade));
        }

        // 4. IMPACT FRAMES 3D (2 ticks de contraste extremo branco -> preto no momento do impacto)
        if (timeSinceStrike >= 0.0 && timeSinceStrike <= 1.8) {
            PrayerImpactFeedback.drawWorldImpactFrame(
                    m, out, new Vec3(0, groundY + 0.12, 0), aoeRadius, timeSinceStrike, 1.8);
        }

        // 5. RELÂMPAGO DE ZEUS + RAMIFICAÇÕES NA ÁREA
        if (boltDrop > 0.01) {
            double remainingSpan = Math.max(1.0, duration - (strikeTick + 1.6));
            double postDecay = 1.0 - PrayerVfxPrimitives.smooth(0.0, remainingSpan, Math.max(0.0, timeSinceStrike)) * 0.55;
            float boltIntensity = (float) (fade * postDecay);

            drawZeusLightning(m, out, skyY, groundY, aoeRadius, boltDrop, age, boltIntensity);
        }

        // 6. DETONAÇÃO NO SOLO (Ondas de choque, lanças e faíscas)
        if (boltDrop >= 0.85) {
            double impactProgress = PrayerVfxPrimitives.smooth(strikeTick + 1.2, duration, age);
            drawGroundDetonation(m, out, groundY, aoeRadius, impactProgress, age, fade);
        }
    }

    /**
     * Obtém a posição do alvo no mundo a partir da entidade da câmera (compatível com 1.21.x - 26.1).
     */
    private static Vec3 extractWorldOrigin(Matrix4fc m) {
        Minecraft mc = Minecraft.getInstance();
        Entity cameraEntity = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
        if (cameraEntity == null) return null;
        return cameraEntity.getEyePosition().add(m.m30(), m.m31(), m.m32());
    }

    /**
     * Escaneia para baixo para encontrar a superfície sólida real do chão abaixo do mob mirado.
     */
    private static double resolveGroundOffsetY(Vec3 worldOrigin) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || worldOrigin == null) {
            return 0.0;
        }

        int bx = (int) Math.floor(worldOrigin.x);
        int bz = (int) Math.floor(worldOrigin.z);
        int startY = (int) Math.floor(worldOrigin.y + 0.5);

        for (int dy = 0; dy <= 16; dy++) {
            BlockPos pos = new BlockPos(bx, startY - dy, bz);
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                double topY = pos.getY() + shape.max(Direction.Axis.Y);
                if (topY <= worldOrigin.y + 0.6) {
                    return topY - worldOrigin.y;
                }
            }
        }
        return 0.0;
    }

    private static void drawGroundTelegraphCircle(Matrix4fc m, VertexConsumer out, double groundY,
                                                  double aoeRadius, double telegraph, double boltDrop,
                                                  float age, float fade) {
        Vec3 ground = new Vec3(0, groundY + 0.03, 0);

        double openCurve = Math.min(1.0, telegraph * 2.2);
        double currentRadius = aoeRadius * (0.25 + 0.75 * openCurve);
        float alpha = (float) ((0.55 + 0.45 * telegraph) * (1.0 - boltDrop * 0.30) * fade);

        // Borda externa fixa indicando exatamente a área de dano (AoE)
        PrayerVfxPrimitives.ringPlane(m, out, ground, PrayerVfxPrimitives.UP,
                currentRadius, 0.036, -age * 0.06, 1.0f, 0.80f, 0.20f, alpha);

        // Anel secundário interno
        PrayerVfxPrimitives.ringPlane(m, out, ground.add(0, 0.01, 0), PrayerVfxPrimitives.UP,
                currentRadius * 0.72, 0.020, age * 0.09, 1.0f, 0.94f, 0.58f, alpha * 0.9f);

        // Anel de carregamento que fecha de fora para dentro até o raio cair
        if (boltDrop < 0.9) {
            double chargeRingRadius = aoeRadius * (1.0 - telegraph * 0.88);
            PrayerVfxPrimitives.ringPlane(m, out, ground.add(0, 0.02, 0), PrayerVfxPrimitives.UP,
                    Math.max(0.12, chargeRingRadius), 0.028, age * 0.18,
                    1.0f, 0.98f, 0.78f, (float) (telegraph * (1.0 - boltDrop) * fade));
        }

        // Runas sagradas girando no chão
        PrayerVfxPrimitives.runeHalo(m, out, ground.add(0, 0.015, 0),
                currentRadius * 0.85, -age * 0.05, alpha, 1.0f, 0.86f, 0.34f);

        // Pequenos pilares de luz subindo da borda do círculo durante o carregamento
        if (boltDrop < 0.5) {
            int pillars = 6;
            double pillarHeight = 0.45 + telegraph * 1.1;
            float pillarAlpha = (float) (telegraph * 0.55 * (1.0 - boltDrop * 2.0) * fade);

            for (int i = 0; i < pillars; i++) {
                double angle = i * (Math.PI * 2.0 / pillars) + age * 0.06;
                Vec3 base = new Vec3(Math.cos(angle) * currentRadius, groundY + 0.03, Math.sin(angle) * currentRadius);
                PrayerVfxGeometry.taperedTube(m, out, base, base.add(0, pillarHeight, 0),
                        0.035, 0.004, 1.0f, 0.90f, 0.45f, pillarAlpha);
            }
        }
    }

    private static void drawSkyPortal(Matrix4fc m, VertexConsumer out, double skyY,
                                      double telegraph, double boltDrop, float age, float fade) {
        Vec3 crown = new Vec3(0, skyY, 0);
        double expand = 0.45 + telegraph * 0.65 + boltDrop * 0.35;

        PrayerVfxPrimitives.ringPlane(m, out, crown, PrayerVfxPrimitives.UP,
                2.5 * expand, 0.045, age * 0.045, 1.0f, 0.78f, 0.20f, 0.92f * fade);

        Vec3 tilt1 = PrayerVfxPrimitives.safeNormalize(new Vec3(0.28, 1.0, 0.12), PrayerVfxPrimitives.UP);
        Vec3 tilt2 = PrayerVfxPrimitives.safeNormalize(new Vec3(-0.22, 1.0, -0.25), PrayerVfxPrimitives.UP);

        PrayerVfxPrimitives.ringPlane(m, out, crown.subtract(0, 0.25, 0), tilt1,
                1.8 * expand, 0.025, -age * 0.10, 1.0f, 0.92f, 0.52f, 0.88f * fade);
        PrayerVfxPrimitives.ringPlane(m, out, crown.subtract(0, 0.45, 0), tilt2,
                1.3 * expand, 0.020, age * 0.14, 1.0f, 0.98f, 0.78f, 0.90f * fade);

        PrayerVfxPrimitives.runeHalo(m, out, crown, 1.5 * expand, age * 0.05, fade, 1.0f, 0.88f, 0.40f);
    }

    private static void drawZeusLightning(Matrix4fc m, VertexConsumer out, double skyY, double groundY,
                                          double aoeRadius, double boltDrop, float age, float intensity) {
        int segments = 14;
        int tickSeed = (int) (age * 0.85);
        double flicker = 0.85 + 0.15 * Math.sin(age * 2.4);
        double totalSpan = skyY - groundY;

        Vec3[] mainNodes = new Vec3[segments + 1];
        mainNodes[0] = new Vec3(0, skyY, 0);

        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            double y = skyY - totalSpan * (t * boltDrop);

            double envelope = Math.sin(t * Math.PI);
            double jitterScale = 0.85 * envelope;

            double offsetX = (hash(tickSeed * 31 + i * 7) * 2.0 - 1.0) * jitterScale;
            double offsetZ = (hash(tickSeed * 47 + i * 13 + 5) * 2.0 - 1.0) * jitterScale;

            if (i == segments && boltDrop >= 0.98) {
                offsetX = 0.0;
                offsetZ = 0.0;
            }

            mainNodes[i] = new Vec3(offsetX, y, offsetZ);
        }

        for (int i = 0; i < segments; i++) {
            double t0 = (double) i / segments;
            double t1 = (double) (i + 1) / segments;

            double r0 = (0.32 - t0 * 0.08) * flicker;
            double r1 = (0.32 - t1 * 0.08) * flicker;

            // Núcleo branco cegante
            PrayerVfxGeometry.taperedTube(m, out, mainNodes[i], mainNodes[i + 1],
                    r0 * 0.45, r1 * 0.45, 1.0f, 0.99f, 0.90f, Math.min(1.0f, intensity * 1.05f));

            // Plasma dourado
            PrayerVfxGeometry.taperedTube(m, out, mainNodes[i], mainNodes[i + 1],
                    r0 * 0.95, r1 * 0.95, 1.0f, 0.80f, 0.20f, 0.75f * intensity);

            // Aura externa
            PrayerVfxGeometry.taperedTube(m, out, mainNodes[i], mainNodes[i + 1],
                    r0 * 1.85, r1 * 1.85, 1.0f, 0.68f, 0.12f, 0.25f * intensity);
        }

        // Ramificações secundárias caindo dentro do círculo no chão
        if (boltDrop > 0.45) {
            int branches = 5;
            for (int b = 0; b < branches; b++) {
                int startNodeIdx = 4 + (b * 2) % (segments - 5);
                Vec3 branchStart = mainNodes[startNodeIdx];

                double targetAngle = (b * (2.0 * Math.PI / branches)) + hash(tickSeed + b * 19) * 0.9;
                double targetDist = aoeRadius * (0.45 + 0.50 * hash(tickSeed * 3 + b * 11));
                Vec3 branchEnd = new Vec3(
                        Math.cos(targetAngle) * targetDist,
                        groundY + 0.04,
                        Math.sin(targetAngle) * targetDist
                );

                drawJaggedBranch(m, out, branchStart, branchEnd, 5, tickSeed * 17 + b * 29, intensity);
            }
        }

        drawEnergyHelix(m, out, skyY, groundY, boltDrop, age, intensity);
    }

    private static void drawJaggedBranch(Matrix4fc m, VertexConsumer out, Vec3 start, Vec3 end,
                                         int subSegments, int seed, float intensity) {
        Vec3 prev = start;
        for (int i = 1; i <= subSegments; i++) {
            double t = (double) i / subSegments;
            Vec3 basePos = start.lerp(end, t);

            if (i < subSegments) {
                double envelope = Math.sin(t * Math.PI) * 0.42;
                double jx = (hash(seed + i * 7) * 2.0 - 1.0) * envelope;
                double jy = (hash(seed + i * 13) * 2.0 - 1.0) * envelope * 0.5;
                double jz = (hash(seed + i * 19) * 2.0 - 1.0) * envelope;
                basePos = basePos.add(jx, jy, jz);
            }

            double rStart = 0.095 * (1.0 - (i - 1.0) / subSegments * 0.85);
            double rEnd = 0.095 * (1.0 - (double) i / subSegments * 0.85);

            PrayerVfxGeometry.taperedTube(m, out, prev, basePos,
                    rStart * 0.45, rEnd * 0.45, 1.0f, 0.98f, 0.85f, 0.92f * intensity);
            PrayerVfxGeometry.taperedTube(m, out, prev, basePos,
                    rStart, rEnd, 1.0f, 0.82f, 0.24f, 0.58f * intensity);

            prev = basePos;
        }
    }

    private static void drawEnergyHelix(Matrix4fc m, VertexConsumer out, double skyY, double groundY,
                                        double boltDrop, float age, float intensity) {
        int steps = 18;
        double currentSpan = (skyY - groundY) * boltDrop;
        double bottomY = skyY - currentSpan;

        for (int strand = 0; strand < 2; strand++) {
            double phaseOffset = strand * Math.PI;
            Vec3 prev = null;

            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                double y = skyY - t * (skyY - bottomY);
                double angle = t * Math.PI * 5.0 - age * 0.45 + phaseOffset;
                double radius = 0.48 + 0.18 * Math.sin(t * Math.PI);

                Vec3 pos = new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
                if (prev != null) {
                    PrayerVfxGeometry.tube(m, out, prev, pos, 0.024,
                            1.0f, 0.92f, 0.52f, 0.65f * intensity);
                }
                prev = pos;
            }
        }
    }

    private static void drawGroundDetonation(Matrix4fc m, VertexConsumer out, double groundY,
                                             double aoeRadius, double impact, float age, float fade) {
        float blastAlpha = (float) ((1.0 - impact) * fade);
        if (blastAlpha <= 0.01f) return;

        Vec3 ground = new Vec3(0, groundY + 0.06, 0);

        double fastWave = Math.min(1.0, impact * 1.6);
        PrayerVfxPrimitives.ringPlane(m, out, ground, PrayerVfxPrimitives.UP,
                0.35 + fastWave * (aoeRadius * 1.15), 0.045, age * 0.12,
                1.0f, 0.96f, 0.72f, (float) ((1.0 - fastWave) * fade));

        PrayerVfxPrimitives.ringPlane(m, out, ground.add(0, 0.04, 0), PrayerVfxPrimitives.UP,
                0.45 + impact * (aoeRadius * 1.3), 0.036, -age * 0.08,
                1.0f, 0.80f, 0.22f, 0.90f * blastAlpha);

        PrayerVfxPrimitives.ringPlane(m, out, new Vec3(0, groundY + 0.35 + impact * 0.6, 0), PrayerVfxPrimitives.UP,
                0.30 + impact * (aoeRadius * 0.85), 0.022, age * 0.16,
                1.0f, 0.88f, 0.42f, 0.68f * blastAlpha);

        PrayerVfxPrimitives.starburst(m, out, ground.add(0, 0.04, 0), PrayerVfxPrimitives.UP,
                0.8 + impact * (aoeRadius * 1.1), 0.88f * blastAlpha, 1.0f, 0.93f, 0.58f);

        int lances = 10;
        for (int i = 0; i < lances; i++) {
            double angle = i * (2.0 * Math.PI / lances) + age * 0.02;
            double innerR = 0.25 + impact * 0.65;
            double outerR = 0.85 + impact * (aoeRadius * 1.05);
            double height = groundY + (0.75 + (i % 3) * 0.35) * (1.0 - impact * 0.35);

            Vec3 start = new Vec3(Math.cos(angle) * innerR, groundY + 0.06, Math.sin(angle) * innerR);
            Vec3 end = new Vec3(Math.cos(angle) * outerR, height, Math.sin(angle) * outerR);

            PrayerVfxGeometry.taperedTube(m, out, start, end, 0.065, 0.004,
                    1.0f, 0.90f, 0.48f, 0.78f * blastAlpha);
        }

        drawRisingSparks(m, out, groundY, aoeRadius, impact, age, blastAlpha);
    }

    private static void drawRisingSparks(Matrix4fc m, VertexConsumer out, double groundY,
                                         double aoeRadius, double impact, float age, float blastAlpha) {
        int sparkCount = 16;
        for (int i = 0; i < sparkCount; i++) {
            double angle = i * 2.39996 + age * 0.08;
            double dist = (0.25 + hash(i * 17) * 0.75) * aoeRadius * (0.4 + impact * 0.75);
            double riseHeight = groundY + (0.4 + hash(i * 31) * 2.8) * impact + ((age * 0.18 + i) % 1.2);

            Vec3 pos = new Vec3(Math.cos(angle) * dist, riseHeight, Math.sin(angle) * dist);
            double size = (0.045 + (i % 3) * 0.015) * (1.0 - impact * 0.4);

            Vec3 ax = new Vec3(size, 0, 0);
            Vec3 ay = new Vec3(0, size * 1.4, 0);
            Vec3 az = new Vec3(0, 0, size);

            PrayerVfxGeometry.quad(m, out,
                    pos.add(ay), pos.add(ax), pos.subtract(ay), pos.subtract(ax),
                    1.0f, 0.95f, 0.65f, blastAlpha);
            PrayerVfxGeometry.quad(m, out,
                    pos.add(ay), pos.add(az), pos.subtract(ay), pos.subtract(az),
                    1.0f, 0.86f, 0.35f, blastAlpha);
        }
    }

    private static double hash(int n) {
        double x = Math.sin(n * 127.1 + 311.7) * 43758.5453123;
        return x - Math.floor(x);
    }
}
