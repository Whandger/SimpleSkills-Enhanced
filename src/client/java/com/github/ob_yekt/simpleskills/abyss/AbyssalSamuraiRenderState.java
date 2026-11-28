package com.github.ob_yekt.simpleskills.abyss;

import com.github.ob_yekt.simpleskills.abyss.weapon.KatanaClientStanceState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.AnimationState;

public class AbyssalSamuraiRenderState extends LivingEntityRenderState {
    public int samuraiState = AbyssalSamuraiEntity.STATE_PETRIFIED;
    /** 0 = full stone, 1 = hairline cracks, 2 = fractured armor, 3 = exposed gold. */
    public int petrificationStage = 0;
    public boolean isDying = false;
    public final ItemStackRenderState katanaRenderState = new ItemStackRenderState();
    public final ItemStackRenderState katanaSayaRenderState = new ItemStackRenderState();
    public final ItemStackRenderState katanaSheathedRenderState = new ItemStackRenderState();
    public boolean hasKatana = false;
    /** The current pose/clip is resolved on the client while the entity is available. */
    public KatanaClientStanceState.RenderPose katanaPose = KatanaClientStanceState.RenderPose.simple(
            0, "animation.katana_ultimo_juramento.idle_unsheathed", 0.0F);

    public final AnimationState petrifiedAnimationState = new AnimationState();
    public final AnimationState awakeningAnimationState = new AnimationState();
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState walkAnimationState = new AnimationState();
    public final AnimationState attackAnimationState = new AnimationState();
    public final AnimationState deathAnimationState = new AnimationState();
}
