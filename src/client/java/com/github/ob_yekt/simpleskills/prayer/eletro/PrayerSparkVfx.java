package com.github.ob_yekt.simpleskills.prayer.eletro;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerSparkVfx {
    private PrayerSparkVfx() {}

    // 21. SPARK — Arco voltaico contínuo e ramificações que enjaulam a hitbox do mob.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 1, 3);
        double travel = PrayerVfxPrimitives.smooth(1, duration * 0.4, age);
        Vec3 tip = Vec3.ZERO.lerp(destination, travel);
        
        // Relâmpago principal ancorado na mão
        PrayerVfxPrimitives.bolt(m, out, Vec3.ZERO, tip, (long) age / 2, 14, 0.25, fade);
        
        // Ramificações elétricas buscando o alvo
        for (int branch = 0; branch < 6; branch++) {
            double t = 0.3 + branch * 0.12;
            if (t > travel) continue;
            Vec3 root = Vec3.ZERO.lerp(destination, t);
            double angle = branch * 2.4 + age * 0.25;
            Vec3 end = root.add(Math.cos(angle) * 0.6, Math.sin(angle * 1.7) * 0.8, Math.sin(angle) * 0.6);
            PrayerVfxPrimitives.bolt(m, out, root, end, branch * 53L + (long) age, 5, 0.1, 0.82f * fade);
        }
        
        // Gaiola elétrica de impacto colando no mob
        if (travel > 0.8) {
            for (int i = 0; i < 5; i++) {
                Vec3 sparkEnd = destination.add(Math.cos(age+i)*0.8, Math.sin(age-i)*0.8, Math.sin(age*1.2+i)*0.8);
                PrayerVfxPrimitives.bolt(m, out, destination, sparkEnd, i * 19L + (long)age, 3, 0.08, 0.8f * fade);
            }
            PrayerVfxGeometry.wireSphere(m, out, destination, 0.4 + travel * 0.3, 10, age * 0.18,
                    0.42f, 0.84f, 1.0f, fade * 0.7f, 0.015);
        }
    }
}
