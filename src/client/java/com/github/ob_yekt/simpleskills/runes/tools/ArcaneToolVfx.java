package com.github.ob_yekt.simpleskills.runes.tools;

import com.github.ob_yekt.simpleskills.prayer.PrayerVfxGeometry;
import com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.ItemGeometryInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.drawOctahedralNode;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.drawRuneCircle;
import static com.github.ob_yekt.simpleskills.runes.RuneVfxPrimitives.lerpVec;

public final class ArcaneToolVfx {
    private ArcaneToolVfx() {}

    public static void submit(SubmitNodeCollector collector, PoseStack poseStack,
                              ItemGeometryInfo info, double time, int seed) {
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, vertices) ->
                draw(pose.pose(), vertices, info, time, seed));
    }

    private static void draw(Matrix4fc matrix, VertexConsumer out, ItemGeometryInfo info,
                             double time, int seed) {
        double pulse = 0.78 + 0.20 * Math.sin(time * 1.35 + seed * 0.01);
        float r = 0.76f, g = 0.36f, b = 1.0f;

        Vec3 guard = info.guardPoint();
        Vec3 bladeStart = info.bladeStart();
        Vec3 bladeTip = info.bladeTip();
        Vec3 bladeVec = bladeTip.subtract(bladeStart);
        double bladeLen = Math.max(0.08, bladeVec.length());
        Vec3 bladeAxis = bladeVec.scale(1.0 / bladeLen);
        Vec3 perpXY = new Vec3(-bladeAxis.y, bladeAxis.x, 0.0).normalize();
        Vec3 perpZ = new Vec3(0.0, 0.0, 1.0);

        // 1. Rune circle positioned higher up at the crossguard / base of blade
        drawRuneCircle(matrix, out, guard, bladeAxis, perpZ,
                0.105, time * 0.065, r, g, b, (float) (0.92 * pulse), 0.0015);

        // 2. 8 small floating runic glyphs hovering around both faces of the blade
        double faceZ = info.halfDepth() + 0.009;
        int glyphCount = 8;
        for (int i = 0; i < glyphCount; i++) {
            double t = (i + 0.5) / glyphCount;
            double side = (i % 2 == 0) ? 1.0 : -1.0;
            double zSign = ((i / 2) % 2 == 0) ? 1.0 : -1.0;
            double bob = Math.sin(time * 0.18 + i * 1.6) * 0.006;

            Vec3 anchor = lerpVec(bladeStart, bladeTip, t)
                    .add(perpXY.scale(side * (0.026 + bob)))
                    .add(0, 0, zSign * faceZ);
            drawFloatingRuneGlyph(matrix, out, anchor, bladeAxis, perpXY,
                    0.014, i + seed, 0.92f, 0.66f, 1.0f, (float) (0.90 * pulse));
        }

        // 3. 10 small orbiting octahedral arcane motes around the blade
        int motes = 10;
        for (int i = 0; i < motes; i++) {
            double t = (i + 0.5) / motes;
            Vec3 spinePt = lerpVec(bladeStart, bladeTip, t);
            double ang = time * 0.14 + i * (Math.PI * 2.0 / motes) * 2.0;
            double orbitR = info.halfDepth() + 0.014;
            Vec3 orb = spinePt
                    .add(perpXY.scale(Math.cos(ang) * 0.024))
                    .add(perpZ.scale(Math.sin(ang) * orbitR));

            drawOctahedralNode(matrix, out, orb, 0.0062, 0.88f, 0.62f, 1.0f, (float) (0.92 * pulse));
        }
    }

    private static void drawFloatingRuneGlyph(Matrix4fc matrix, VertexConsumer out, Vec3 center,
                                              Vec3 upDir, Vec3 rightDir, double size, int variant,
                                              float r, float g, float b, float alpha) {
        double stroke = 0.0010;
        Vec3 top = center.add(upDir.scale(size));
        Vec3 bottom = center.subtract(upDir.scale(size));
        Vec3 mid = center;

        PrayerVfxGeometry.taperedTube(matrix, out, bottom, top, stroke, stroke, r, g, b, alpha);

        int pattern = Math.abs(variant) % 4;
        Vec3 b1End = mid.add(upDir.scale(size * 0.65)).add(rightDir.scale(size * 0.65));
        Vec3 b2End = (pattern % 2 == 0)
                ? mid.subtract(upDir.scale(size * 0.15)).subtract(rightDir.scale(size * 0.65))
                : bottom.add(upDir.scale(size * 0.45)).add(rightDir.scale(size * 0.60));

        PrayerVfxGeometry.taperedTube(matrix, out, mid, b1End, stroke * 0.85, stroke * 0.55, r, g, b, alpha);
        PrayerVfxGeometry.taperedTube(matrix, out, lerpVec(bottom, mid, 0.5), b2End,
                stroke * 0.85, stroke * 0.55, r, g, b, alpha);
    }
}
