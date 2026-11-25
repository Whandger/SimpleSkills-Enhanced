package com.github.ob_yekt.simpleskills.abyss;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class AbyssalFallenKingRenderState extends LivingEntityRenderState {
    public int kingState = AbyssalFallenKingEntity.STATE_ACTIVE;
    public float awakeningProgress = 0.0F;
    public float suspensionHeight = 0.0F;
    public boolean isDying = false;
    public float deathProgress = 0.0F;
    public final AnimationState sittingIdleAnimationState = new AnimationState();
    public final AnimationState awakeningAnimationState = new AnimationState();
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState walkAnimationState = new AnimationState();
    public final AnimationState attackAnimationState = new AnimationState();
    public final AnimationState deathAnimationState = new AnimationState();
}
