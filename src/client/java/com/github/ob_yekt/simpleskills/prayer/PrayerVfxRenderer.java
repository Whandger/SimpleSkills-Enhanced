package com.github.ob_yekt.simpleskills.prayer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

/** Dispatches the custom procedural 3D renderers for every Prayer ability. */
public final class PrayerVfxRenderer extends EntityRenderer<PrayerVfxEntity, PrayerVfxRenderer.State> {
    public PrayerVfxRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0;
    }

    @Override
    public boolean shouldRender(PrayerVfxEntity entity, net.minecraft.client.renderer.culling.Frustum frustum,
                               double x, double y, double z, float partialTick) {
        return true;
    }

    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(PrayerVfxEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.style = entity.style();
        state.end = entity.endOffset();
        state.hand = entity.handOffset();
        state.age = entity.life() + partialTick;
        state.duration = entity.maxLife();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        float age = state.age;
        collector.submitCustomGeometry(pose, RenderTypes.lightning(), (matrix, vertices) -> {
            Matrix4fc transform = matrix.pose();
            PrayerAbility ability = switch (state.style) {
                case 0 -> PrayerAbility.LIGHTNING_BOLT;
                case 1 -> PrayerAbility.DIVINE_HEALING;
                case 2 -> PrayerAbility.METEOR;
                default -> PrayerVisualEffects.abilityForStyle(state.style);
            };
            if (ability != null) {
                PrayerAbilityVfx.draw(transform, vertices, ability, state.hand, state.end, age, state.duration);
                PrayerVfxFlair.draw(transform, vertices, ability, state.end, age, state.duration);
            }
        });
    }

    public static final class State extends EntityRenderState {
        int style, duration;
        float age;
        Vec3 end = Vec3.ZERO;
        Vec3 hand = Vec3.ZERO;
    }
}
