package com.github.ob_yekt.simpleskills.prayer.ice;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.prayer.PrayerVfxPrimitives;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class PrayerIceShardVfx {
    private PrayerIceShardVfx() {}

    // 31. ICE_SHARD — Corre pelo chão rasgando a terra e explode em cristais no mob.
    public static void draw(Matrix4fc m, VertexConsumer out, Vec3 destination, float age, int duration) {
        float fade = PrayerVfxPrimitives.fade(age, duration, 2, 4);
        double travel = PrayerVfxPrimitives.smooth(2, duration * 0.58, age);
        
        // Estima o chão baseando-se na posição do jogador (Vec3.ZERO) e do alvo.
        double floorY = -1.2; 
        
        int numSpikes = (int)(travel * 22);
        for (int i = 0; i < numSpikes; i++) {
            double t = i / 22.0;
            Vec3 groundPath = new Vec3(0, floorY, 0).lerp(destination.add(0, -0.5, 0), t);
            
            double zig = Math.sin(i * 14.3) * 0.35;
            double zag = Math.cos(i * 21.7) * 0.35;
            Vec3 basePoint = groundPath.add(zig, 0, zag);
            
            double scale = 0.4 + (i % 3) * 0.25;
            Vec3 axis = new Vec3(zig * 0.3, 1.0, zag * 0.3).normalize();
            
            PrayerVfxPrimitives.crystal(m, out, basePoint.add(0, scale * 0.5, 0), axis, scale * 1.5, scale * 0.35, 
                    0.35f, 0.78f, 1.0f, 0.9f * fade);
        }
        
        // Erupção massiva ao atingir o mob
        if (travel > 0.85) {
            double impact = PrayerVfxPrimitives.smooth(0.85, 1, travel);
            Vec3 impactCenter = destination.add(0, -0.5, 0);
            for (int i = 0; i < 12; i++) {
                double angle = i * Math.PI / 6;
                Vec3 dir = new Vec3(Math.cos(angle), 0.8 + (i % 3) * 0.5, Math.sin(angle)).normalize();
                double scale = (1.5 + (i % 4) * 0.6) * impact;
                Vec3 spikeBase = impactCenter.add(Math.cos(angle) * 0.4, 0, Math.sin(angle) * 0.4);
                
                PrayerVfxPrimitives.crystal(m, out, spikeBase.add(dir.scale(scale * 0.5)), dir, scale * 2.2, scale * 0.45, 
                        0.2f, 0.85f, 1.0f, (float)(0.9 * (1 - impact * 0.2) * fade));
            }
        }
    }
}
