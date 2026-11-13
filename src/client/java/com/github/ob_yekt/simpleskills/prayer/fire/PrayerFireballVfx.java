package com.github.ob_yekt.simpleskills.prayer.fire;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerFireballVfx {
    private PrayerFireballVfx() {}

    // 26. FIREBALL — Rastro conectado à mão, núcleo turbulento e impacto cinético brutal.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 2, 4);
        double travel = PrayerVfxPrimitives.smooth(2, duration * 0.58, age);
        Vec3 center = Vec3.ZERO.lerp(destination, travel);
        Vec3 dir = PrayerVfxPrimitives.safeNormalize(destination, new Vec3(0, 0, 1));
        
        // Rastro de fogo saindo da mão
        List<Vec3> trail = PrayerVfxPrimitives.jagged(Vec3.ZERO, center, Math.max(3, (int)(travel * 12)), (long)age / 3, 0.15);
        for (int i = 0; i < trail.size() - 1; i++) {
            double t = i / (double)trail.size();
            PrayerVfxGeometry.tube(m, out, trail.get(i), trail.get(i+1), 0.18 * t, 
                    1.0f, 0.35f * (float)t, 0.02f, fade * 0.75f);
        }
        
        // Núcleo denso e caótico
        double pulse = 1 + 0.15 * Math.sin(age * 0.6);
        double radius = 0.38 * pulse;
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(radius, radius * 1.1, radius), 12, 16, 
                1.0f, 0.4f, 0.05f, 0.9f * fade, age * 0.25);
        PrayerVfxGeometry.ellipsoid(m, out, center, new Vec3(radius * 0.6, radius * 0.6, radius * 0.6), 8, 12, 
                1.0f, 0.9f, 0.2f, 0.95f * fade, -age * 0.35);
        
        // Chamas envolvendo a esfera
        Vec3[] frame = PrayerVfxPrimitives.basis(dir);
        for (int tongue = 0; tongue < 9; tongue++) {
            double angle = tongue * Math.PI * 2 / 9 + age * 0.18;
            Vec3 side = frame[0].scale(Math.cos(angle)).add(frame[1].scale(Math.sin(angle)));
            Vec3 start = center.add(side.scale(radius * 0.4));
            Vec3 end = center.subtract(dir.scale(0.5 + (tongue % 3) * 0.25))
                             .add(side.scale(radius * (1.6 + 0.5 * Math.sin(age * 0.4 + tongue))));
            PrayerVfxGeometry.taperedTube(m, out, start, end, 0.14, 0.01, 1.0f, 0.25f, 0.02f, 0.85f * fade);
        }
        
        if (travel > 0.9) flameBurstImpact(m, out, destination, age, duration, fade);
    }

    private static void flameBurstImpact(Matrix4fc m, VertexConsumer out, Vec3 center, float age,
                                         int duration, float fade) {
        double burst = PrayerVfxPrimitives.smooth(duration * 0.68, duration * 0.87, age);
        double radius = 0.18 + burst * 2.75;
        float alpha = (float) ((1 - burst) * fade);
        PrayerVfxPrimitives.ringPlane(m, out, center.add(0, 0.09, 0), PrayerVfxPrimitives.UP, radius, 0.10 * (1 - burst * 0.65),
                age * 0.055, 1.0f, 0.27f, 0.025f, 0.86f * alpha);
        PrayerVfxPrimitives.ringPlane(m, out, center.add(0, 0.13, 0), PrayerVfxPrimitives.UP, radius * 0.70, 0.035,
                -age * 0.08, 1.0f, 0.78f, 0.16f, 0.88f * alpha);
        PrayerVfxGeometry.wireSphere(m, out, center.add(0, 0.52, 0), 0.25 + burst * 1.65,
                14, age * 0.09, 1.0f, 0.39f, 0.035f, 0.37f * alpha, 0.028);
        for (int ray = 0; ray < 12; ray++) {
            double angle = ray * Math.PI / 6 + age * 0.018;
            double reach = radius * (0.72 + (ray % 3) * 0.13);
            Vec3 base = center.add(Math.cos(angle) * 0.16, 0.12, Math.sin(angle) * 0.16);
            Vec3 knee = center.add(Math.cos(angle) * reach * 0.58, 0.42 + (ray % 3) * 0.12,
                    Math.sin(angle) * reach * 0.58);
            Vec3 tip = center.add(Math.cos(angle) * reach, 0.08, Math.sin(angle) * reach);
            PrayerVfxPrimitives.flamePetal(m, out, base, knee, tip, 0.20 * (1 - burst * 0.55), alpha);
        }
    }
}
