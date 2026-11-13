package com.github.ob_yekt.simpleskills.prayer.unholy;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerShadowBoltVfx {
    private PrayerShadowBoltVfx() {}

    // 11. SHADOW_BOLT — Singularidade voraz que suga a luz em formato de espiral.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 3, 5);
        double travel = PrayerVfxPrimitives.smooth(3, duration * 0.63, age);
        Vec3 center = Vec3.ZERO.lerp(destination, travel);
        
        // Núcleo da Singularidade
        PrayerVfxGeometry.wireSphere(m, out, center, 0.35, 16, -age * 0.3, 
                0.15f, 0.02f, 0.25f, 0.9f * fade, 0.03);
        
        // Fitas escuras rasgando o ar e sendo sugadas para o núcleo
        Vec3 dir = PrayerVfxPrimitives.safeNormalize(destination, new Vec3(0, 0, -1));
        Vec3[] frame = PrayerVfxPrimitives.basis(dir);
        for (int ribbon = 0; ribbon < 6; ribbon++) {
            double angle = ribbon * Math.PI * 2 / 6 + age * 0.35;
            Vec3 previous = center.add(dir.scale(0.5));
            for (int step = 1; step <= 10; step++) {
                double t = step / 10.0;
                double radius = 0.9 * Math.sin(t * Math.PI);
                double twist = angle + t * Math.PI * 2;
                Vec3 point = center.subtract(dir.scale(t * 1.5))
                        .add(frame[0].scale(Math.cos(twist) * radius))
                        .add(frame[1].scale(Math.sin(twist) * radius));
                PrayerVfxGeometry.tube(m, out, previous, point, 0.06 * (1 - t), 
                        0.25f, 0.02f, 0.35f, 0.85f * fade);
                previous = point;
            }
        }
        
        // Implosão violenta no contato
        if (travel > 0.9) {
            double burst = PrayerVfxPrimitives.smooth(0.9, 1.0, travel);
            PrayerVfxGeometry.torus(m, out, destination, 1.8 * (1 - burst), 0.12, 32, age, 
                    0.2f, 0.01f, 0.3f, (float)(1 - burst) * fade);
        }
    }
}
