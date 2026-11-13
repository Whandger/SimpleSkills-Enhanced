package com.github.ob_yekt.simpleskills.prayer.unholy;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerShadowstepVfx {
    private PrayerShadowstepVfx() {}

    // 15. SHADOWSTEP — the departure silhouette collapses while a second one re-forms inside a portal.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 2, 5);
        double vanish = PrayerVfxPrimitives.smooth(0, 0.42, age / duration);
        double appear = PrayerVfxPrimitives.smooth(0.42, 0.78, age / duration);
        portal(m, out, Vec3.ZERO.add(0, 0.88, 0), 0.72 * (1 + vanish * 0.45),
                age * 0.035, fade * (float) (1 - vanish * 0.6), 0.28f, 0.06f, 0.48f);
        portal(m, out, destination.add(0, 0.88, 0), 0.72 * (0.45 + appear * 0.55),
                -age * 0.04, fade * (float) appear, 0.36f, 0.08f, 0.64f);
        silhouette(m, out, Vec3.ZERO.add(0, -0.95, 0), (float) (1 - vanish), fade,
                0.12f, 0.035f, 0.22f);
        silhouette(m, out, destination.add(0, 0.1, 0), (float) appear, fade,
                0.2f, 0.055f, 0.34f);
        Vec3 path = destination.subtract(Vec3.ZERO);
        for (int shard = 0; shard < 11; shard++) {
            double t = (shard / 11.0 + age * 0.06) % 1.0;
            double wobble = Math.sin(t * Math.PI * 5 + age * 0.16) * 0.28;
            Vec3 point = path.scale(t).add(Math.cos(age * 0.09 + shard) * wobble,
                    Math.sin(age * 0.14 + shard * 2.1) * 0.38, Math.sin(age * 0.1 + shard) * wobble);
            PrayerVfxPrimitives.crystal(m, out, point, PrayerVfxPrimitives.safeNormalize(path, PrayerVfxPrimitives.UP), 0.18, 0.065,
                    0.54f, 0.13f, 0.82f, 0.72f * fade * (float) (1 - Math.abs(0.5 - t)));
        }
    }

    private static void portal(Matrix4fc m, VertexConsumer out, Vec3 center, double radius, double rotation,
                               float alpha, float r, float g, float b) {
        Vec3 facing = new Vec3(0, 0, 1);
        PrayerVfxPrimitives.ringPlane(m, out, center, facing, radius, 0.055, rotation, r, g, b, 0.88f * alpha);
        PrayerVfxPrimitives.ringPlane(m, out, center, facing, radius * 0.84, 0.022, -rotation * 1.7,
                Math.min(1, r + 0.18f), Math.min(1, g + 0.18f), Math.min(1, b + 0.18f), alpha);
        PrayerVfxPrimitives.ringPlane(m, out, center, facing, radius * 0.62, 0.035, rotation * 1.25,
                r, g * 0.8f, b, 0.78f * alpha);
        PrayerVfxPrimitives.ringPlane(m, out, center, facing, radius * 0.22, 0.018, -rotation * 2.2,
                0.78f, 0.45f, 1.0f, 0.72f * alpha);
        for (int rune = 0; rune < 10; rune++) {
            double angle = rune * Math.PI * 2 / 10 + rotation;
            Vec3 radial = new Vec3(Math.cos(angle), Math.sin(angle), 0);
            Vec3 tangent = new Vec3(-Math.sin(angle), Math.cos(angle), 0);
            Vec3 root = center.add(radial.scale(radius * 0.66));
            Vec3 outer = center.add(radial.scale(radius * 0.97));
            Vec3 crossbar = root.add(tangent.scale(radius * 0.10));
            PrayerVfxGeometry.tube(m, out, root, outer, 0.018, r, g, b, 0.9f * alpha);
            PrayerVfxGeometry.tube(m, out, root.subtract(tangent.scale(radius * 0.11)), crossbar,
                    0.018, 1.0f, 0.88f, 0.98f, 0.84f * alpha);
            PrayerVfxGeometry.tube(m, out, crossbar, root.add(tangent.scale(radius * 0.11)),
                    0.018, 1.0f, 0.88f, 0.98f, 0.84f * alpha);
        }
        PrayerVfxGeometry.wireSphere(m, out, center, radius * 0.46, 12, -rotation * 0.8,
                r, g * 0.7f, b, 0.18f * alpha, 0.014);
    }

    private static void silhouette(Matrix4fc m, VertexConsumer out, Vec3 center, float scale, float alpha,
                                   float r, float g, float b) {
        if (scale <= 0.015f || alpha <= 0.01f) return;
        Vec3 feet = center.add(0, 0.91 * scale, 0);
        Vec3 pelvis = feet.add(0, 0.42 * scale, 0);
        Vec3 chest = feet.add(0, 0.96 * scale, 0);
        Vec3 shoulders = feet.add(0, 1.10 * scale, 0);
        Vec3 head = feet.add(0, 1.38 * scale, 0);
        Vec3 leftShoulder = shoulders.add(-0.22 * scale, 0, 0);
        Vec3 rightShoulder = shoulders.add(0.22 * scale, 0, 0);
        Vec3 leftHand = leftShoulder.add(-0.15 * scale, -0.48 * scale, 0.02 * scale);
        Vec3 rightHand = rightShoulder.add(0.15 * scale, -0.48 * scale, 0.02 * scale);
        Vec3 leftKnee = pelvis.add(-0.13 * scale, -0.34 * scale, 0);
        Vec3 rightKnee = pelvis.add(0.13 * scale, -0.34 * scale, 0);
        PrayerVfxGeometry.ellipsoid(m, out, head, new Vec3(0.15, 0.18, 0.13).scale(scale),
                7, 10, r, g, b, 0.45f * alpha, 0);
        PrayerVfxGeometry.ellipsoid(m, out, chest, new Vec3(0.27, 0.43, 0.15).scale(scale),
                8, 12, r, g, b, 0.28f * alpha, 0);
        PrayerVfxGeometry.taperedTube(m, out, pelvis, chest, 0.18 * scale, 0.12 * scale,
                r, g, b, 0.9f * alpha);
        for (Vec3[] limb : new Vec3[][] {
                {shoulders, leftShoulder, leftHand}, {shoulders, rightShoulder, rightHand},
                {pelvis, leftKnee, feet.add(-0.16 * scale, -0.43 * scale, 0)},
                {pelvis, rightKnee, feet.add(0.16 * scale, -0.43 * scale, 0)}
        }) {
            PrayerVfxGeometry.taperedTube(m, out, limb[0], limb[1], 0.085 * scale, 0.055 * scale,
                    r, g, b, 0.8f * alpha);
            PrayerVfxGeometry.taperedTube(m, out, limb[1], limb[2], 0.06 * scale, 0.025 * scale,
                    r, g, b, 0.76f * alpha);
        }
    }
}
