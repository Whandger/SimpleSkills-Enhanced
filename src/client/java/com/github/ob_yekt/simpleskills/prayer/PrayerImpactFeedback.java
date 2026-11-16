package com.github.ob_yekt.simpleskills.prayer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

/**
 * Sistema universal de Impact Frames (HUD + 3D) e Camera Shake para todas as skills.
 */
public final class PrayerImpactFeedback implements HudElement {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("simpleskills", "prayer_impact_feedback");
    private static boolean registered = false;

    private static long impactStartTimeMs = 0L;
    private static long shakeDurationMs = 450L;
    private static long flashDurationMs = 145L;
    private static float shakeIntensity = 0.0f;
    private static Vec3 lastImpactPos = Vec3.ZERO;

    private static float appliedYaw = 0.0f;
    private static float appliedPitch = 0.0f;

    private PrayerImpactFeedback() {}

    public static void registerClientHooks() {
        if (registered) return;
        registered = true;

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.BOSS_BAR,
                ID,
                new PrayerImpactFeedback()
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> updateCameraShake());
    }

    public static void trigger(Vec3 impactWorldPos, float intensity) {
        trigger(impactWorldPos, intensity, (long) (300 + intensity * 75), 145L);
    }

    public static void trigger(Vec3 impactWorldPos, float maxShakeDegrees,
                               long shakeDuration, long flashDuration) {
        long now = System.currentTimeMillis();

        if (now - impactStartTimeMs < 320L && impactWorldPos != null
                && impactWorldPos.distanceToSqr(lastImpactPos) < 1.5) {
            return;
        }

        float attenuation = 1.0f;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && impactWorldPos != null) {
            double dist = mc.player.position().distanceTo(impactWorldPos);
            if (dist > 32.0) return;
            attenuation = (float) Math.max(0.30, 1.0 - (dist / 32.0));
        }

        impactStartTimeMs = now;
        lastImpactPos = impactWorldPos != null ? impactWorldPos : Vec3.ZERO;
        shakeIntensity = maxShakeDegrees * attenuation;
        shakeDurationMs = Math.max(50L, shakeDuration);
        flashDurationMs = Math.max(0L, flashDuration);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        updateCameraShake();

        if (flashDurationMs <= 0L) return;
        long elapsed = System.currentTimeMillis() - impactStartTimeMs;
        if (elapsed < 0 || elapsed > flashDurationMs) return;

        int w = g.guiWidth();
        int h = g.guiHeight();
        double progress = (double) elapsed / flashDurationMs;

        int overlayColor;
        int lineColor;

        if (progress < 0.32) {
            overlayColor = 0xB8FFFFFF;
            lineColor = 0xEE101015;
        } else if (progress < 0.72) {
            overlayColor = 0xC4050508;
            lineColor = 0xF0FFFFFF;
        } else {
            overlayColor = 0x55FFD54F;
            lineColor = 0x88FFF8E1;
        }

        g.fill(0, 0, w, h, overlayColor);
        drawScreenImpactLines(g, w, h, lineColor, progress < 0.72);
    }

    private static void updateCameraShake() {
        Minecraft mc = Minecraft.getInstance();
        Entity cam = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
        if (cam == null) {
            appliedYaw = 0.0f;
            appliedPitch = 0.0f;
            return;
        }

        float[] target = getCameraShakeOffsets();
        float deltaYaw = target[0] - appliedYaw;
        float deltaPitch = target[1] - appliedPitch;

        if (deltaYaw != 0.0f || deltaPitch != 0.0f) {
            cam.setYRot(cam.getYRot() + deltaYaw);
            cam.setXRot(cam.getXRot() + deltaPitch);
            cam.yRotO += deltaYaw;
            cam.xRotO += deltaPitch;
            appliedYaw = target[0];
            appliedPitch = target[1];
        }
    }

    private static void drawScreenImpactLines(GuiGraphicsExtractor g, int w, int h, int color, boolean strong) {
        int cx = w / 2;
        int cy = h / 2;
        int count = strong ? 20 : 12;

        for (int i = 0; i < count; i++) {
            double angle = (i * (Math.PI * 2.0 / count)) + (strong ? 0.08 : 0.24);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            double innerR = Math.min(w, h) * (0.26 + (i % 3) * 0.05);
            double outerR = Math.max(w, h) * 0.75;
            int steps = 14;

            for (int s = 0; s < steps; s++) {
                double t = (double) s / steps;
                double r = innerR + (outerR - innerR) * t;
                int px = (int) (cx + cos * r);
                int py = (int) (cy + sin * r);

                int thickness = 1 + (int) (t * 4.5);
                g.fill(px - thickness, py - thickness, px + thickness, py + thickness, color);
            }
        }
    }

    public static void drawWorldImpactFrame(Matrix4fc m, VertexConsumer out, Vec3 localCenter,
                                            double radius, double elapsedTicks, double durationTicks) {
        if (elapsedTicks < 0.0 || elapsedTicks > durationTicks || durationTicks <= 0.0) {
            return;
        }

        double progress = elapsedTicks / durationTicks;
        float alpha = (float) Math.pow(1.0 - progress, 0.75);

        PrayerVfxPrimitives.starburst(m, out, localCenter, PrayerVfxPrimitives.UP,
                radius * (1.9 + progress * 1.1), alpha, 1.0f, 1.0f, 1.0f);

        double shockRadius = radius * (0.95 + progress * 1.55);
        PrayerVfxPrimitives.ringPlane(m, out, localCenter, PrayerVfxPrimitives.UP,
                shockRadius, 0.26 * (1.0 - progress * 0.4), 0.0, 1.0f, 1.0f, 1.0f, alpha);
        PrayerVfxPrimitives.ringPlane(m, out, localCenter.add(0, 0.25, 0), PrayerVfxPrimitives.UP,
                shockRadius * 0.78, 0.16 * (1.0 - progress * 0.4), 0.2, 1.0f, 0.94f, 0.65f, alpha * 0.9f);

        int spikes = 18;
        for (int i = 0; i < spikes; i++) {
            double angle = i * (Math.PI * 2.0 / spikes) + (progress < 0.45 ? 0.0 : 0.17);
            double inner = radius * (0.18 + (i % 2) * 0.10);
            double outer = radius * (1.45 + (i % 3) * 0.35 + progress * 0.5);
            double elevation = (0.20 + (i % 4) * 0.65) * (radius / 2.2);

            Vec3 p0 = localCenter.add(Math.cos(angle) * inner, 0.04, Math.sin(angle) * inner);
            Vec3 p1 = localCenter.add(Math.cos(angle) * outer, elevation, Math.sin(angle) * outer);

            PrayerVfxGeometry.taperedTube(m, out, p0, p1, 0.11 * alpha, 0.004,
                    1.0f, 0.99f, 0.92f, alpha);
        }
    }

    public static float[] getCameraShakeOffsets() {
        long elapsed = System.currentTimeMillis() - impactStartTimeMs;
        if (elapsed < 0 || elapsed > shakeDurationMs || shakeIntensity <= 0.001f) {
            return new float[]{0.0f, 0.0f};
        }

        double progress = (double) elapsed / shakeDurationMs;
        double decay = Math.pow(1.0 - progress, 1.85);
        double t = elapsed * 0.075;

        float yawOffset = (float) (Math.sin(t * 1.45) * shakeIntensity * decay);
        float pitchOffset = (float) (Math.cos(t * 1.95) * shakeIntensity * 0.95 * decay);

        return new float[]{yawOffset, pitchOffset};
    }
}